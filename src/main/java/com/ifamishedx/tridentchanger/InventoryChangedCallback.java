package com.ifamishedx.tridentchanger;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Custom Fabric event fired whenever a slot in a {@link PlayerInventory} changes
 * on the server side. Backed by {@link PlayerInventoryMixin}.
 */
@FunctionalInterface
public interface InventoryChangedCallback {

    Event<InventoryChangedCallback> EVENT = EventFactory.createArrayBacked(
            InventoryChangedCallback.class,
            listeners -> (player, inventory, slot, stack) -> {
                for (InventoryChangedCallback listener : listeners) {
                    listener.onInventoryChanged(player, inventory, slot, stack);
                }
            }
    );

    void onInventoryChanged(ServerPlayerEntity player, PlayerInventory inventory, int slot, ItemStack stack);
}
