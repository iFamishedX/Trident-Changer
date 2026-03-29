package com.ifamishedx.tridentchanger;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;

import java.util.List;

/**
 * A {@link LootFunction} that replaces any Gungnir trident with a clean,
 * normalized vanilla trident. Applied to <em>every</em> loot table via
 * {@link net.fabricmc.fabric.api.loot.v3.LootTableEvents#MODIFY} so that
 * Gungnir tridents can never enter the game through loot.
 *
 * <p>The function has no configuration parameters, so its {@link MapCodec}
 * is a unit codec that always decodes to the same instance.</p>
 */
public class GungnirNormalizeLootFunction extends ConditionalLootFunction {

    /** Singleton instance – the function has no state beyond its (empty) condition list. */
    static final GungnirNormalizeLootFunction INSTANCE =
            new GungnirNormalizeLootFunction(List.of());

    /**
     * Unit codec: encodes nothing, always decodes to {@link #INSTANCE}.
     * Sufficient because this function is applied only at runtime via
     * {@code LootTableEvents.MODIFY} and is never serialized to disk.
     */
    public static final MapCodec<GungnirNormalizeLootFunction> CODEC =
            MapCodec.unit(INSTANCE);

    private GungnirNormalizeLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType<GungnirNormalizeLootFunction> getType() {
        return TridentChangerMod.NORMALIZE_GUNGNIR_FUNCTION_TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        if (GungnirNormalizationHandler.isGungnir(stack)) {
            return GungnirNormalizationHandler.createNormalizedTrident(
                    context.getWorld().getRegistryManager());
        }
        return stack;
    }

    /** Returns a {@link Builder} that creates {@link GungnirNormalizeLootFunction} instances. */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConditionalLootFunction.Builder<Builder> {

        @Override
        protected Builder getThisBuilder() {
            return this;
        }

        @Override
        public LootFunction build() {
            return new GungnirNormalizeLootFunction(getConditions());
        }
    }
}
