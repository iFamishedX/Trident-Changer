package com.ifamishedx.tridentchanger;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central handler that registers every event required to prevent Gungnir
 * tridents from persisting anywhere in the game world.
 *
 * <p><strong>Coverage:</strong>
 * <ol>
 *   <li>Loot tables – via {@code LootTableEvents.MODIFY} (applies
 *       {@link GungnirNormalizeLootFunction} to every loot table at load time).</li>
 *   <li>World item entities – via {@code ServerTickEvents.END_WORLD_TICK}
 *       (scans all {@link net.minecraft.entity.ItemEntity} each tick).</li>
 *   <li>Inventory changes / {@code /give} – via {@link InventoryChangedCallback}
 *       (backed by {@link com.ifamishedx.tridentchanger.mixin.PlayerInventoryMixin}).</li>
 *   <li>Player join – via {@code ServerPlayConnectionEvents.JOIN}.</li>
 *   <li>Datapack reload – via {@code ServerLifecycleEvents.END_DATA_PACK_RELOAD}.</li>
 * </ol>
 */
public final class GungnirNormalizationHandler {

    private static final String GUNGNIR_ID = "yggdrasil.asgard.treasure.gungnir";

    private GungnirNormalizationHandler() {}

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public static void register() {

        // 1. Loot table blocking – applied to EVERY loot table at (re)load time.
        //    GungnirNormalizeLootFunction.process() is a fast no-op for non-Gungnir
        //    items, so the performance impact on unrelated tables is negligible.
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registryLookup) ->
                tableBuilder.apply(GungnirNormalizeLootFunction.builder()));

        // 2. World item-entity scan – catches tridents dropped on the ground
        //    (e.g. from loot chests, mob drops, or /drop commands) before they
        //    are picked up by a player.
        ServerTickEvents.END_WORLD_TICK.register(world ->
                world.getEntitiesByType(EntityType.ITEM,
                                entity -> isGungnir(entity.getStack()))
                        .forEach(entity ->
                                entity.setStack(createNormalizedTrident(
                                        world.getRegistryManager()))));

        // 3 & 4. Inventory change – covers /give, /loot, crafting, trading,
        //         and any other route by which an item enters a player's inventory.
        InventoryChangedCallback.EVENT.register((player, inventory, slot, stack) -> {
            if (isGungnir(stack)) {
                inventory.setStack(slot,
                        createNormalizedTrident(player.getServer().getRegistryManager()));
            }
        });

        // 5. Player join – sanitizes tridents that were already in the inventory
        //    before this session (stored in level data).
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                normalizeInventory(handler.getPlayer()));

        // 6. Datapack / server reload – re-sanitizes every online player so that
        //    tridents granted by a removed datapack are cleaned up immediately.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    normalizeInventory(player);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Detection
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code stack} is a Gungnir trident, i.e. an
     * {@link Items#TRIDENT} whose {@link DataComponentTypes#CUSTOM_DATA} contains
     * {@code yggdrasil.id == "yggdrasil.asgard.treasure.gungnir"}.
     *
     * <p>This method is intentionally fast: it short-circuits on the item check
     * and the component presence check before touching NBT.</p>
     */
    public static boolean isGungnir(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.TRIDENT)) {
            return false;
        }
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        NbtCompound nbt = customData.copyNbt();
        if (!nbt.contains("yggdrasil", NbtElement.COMPOUND_TYPE)) {
            return false;
        }
        NbtCompound yggdrasil = nbt.getCompound("yggdrasil");
        // "literal".equals(value) is null-safe; getString returns "" for missing keys.
        return GUNGNIR_ID.equals(yggdrasil.getString("id"));
    }

    // -------------------------------------------------------------------------
    // Inventory helpers
    // -------------------------------------------------------------------------

    /**
     * Iterates <em>every</em> slot in {@code player}'s inventory and normalizes
     * any Gungnir tridents found in-place.
     */
    static void normalizeInventory(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        RegistryWrapper.WrapperLookup registryLookup =
                player.getServer().getRegistryManager();
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isGungnir(inventory.getStack(slot))) {
                inventory.setStack(slot, createNormalizedTrident(registryLookup));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Normalization
    // -------------------------------------------------------------------------

    /**
     * Builds a clean, normalized vanilla trident with exactly three enchantments:
     * Impaling V, Unbreaking III, and Mending I.
     *
     * <p>Starting from {@code new ItemStack(Items.TRIDENT)} guarantees that no
     * custom components (name, lore, attributes, custom data, …) are present.
     * The explicit {@link ItemStack#remove} calls below satisfy the idempotency
     * requirement: running normalization on an already-normalized trident is a
     * no-op because {@link #isGungnir} returns {@code false} for a clean stack.</p>
     */
    static ItemStack createNormalizedTrident(RegistryWrapper.WrapperLookup registryLookup) {
        ItemStack stack = new ItemStack(Items.TRIDENT);

        // Strip ALL custom components – explicit removal for clarity even
        // though a fresh ItemStack already lacks them.
        stack.remove(DataComponentTypes.CUSTOM_DATA);
        stack.remove(DataComponentTypes.CUSTOM_NAME);
        stack.remove(DataComponentTypes.LORE);
        stack.remove(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        stack.remove(DataComponentTypes.DAMAGE);
        stack.remove(DataComponentTypes.ENCHANTMENTS);
        // TOOLTIP_DISPLAY was introduced in 1.21.4+; remove if present via the
        // generic component map to stay forward-compatible without breaking 1.21.1.

        // Apply clean enchantments using the component API.
        RegistryEntryLookup<Enchantment> enchLookup =
                registryLookup.getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Mutable mutable =
                new ItemEnchantmentsComponent.Mutable(ItemEnchantmentsComponent.DEFAULT);
        mutable.set(enchLookup.getOrThrow(Enchantments.IMPALING), 5);
        mutable.set(enchLookup.getOrThrow(Enchantments.UNBREAKING), 3);
        mutable.set(enchLookup.getOrThrow(Enchantments.MENDING), 1);
        stack.set(DataComponentTypes.ENCHANTMENTS, mutable.toImmutable());

        return stack;
    }
}
