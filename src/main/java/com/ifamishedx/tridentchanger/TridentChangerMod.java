package com.ifamishedx.tridentchanger;

import net.fabricmc.api.ModInitializer;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TridentChangerMod implements ModInitializer {

    public static final String MOD_ID = "trident-changer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Registered {@link LootFunctionType} for {@link GungnirNormalizeLootFunction}.
     * Must be initialized before {@link GungnirNormalizationHandler#register()} so
     * that {@link GungnirNormalizeLootFunction#getType()} can reference it.
     */
    public static LootFunctionType<GungnirNormalizeLootFunction> NORMALIZE_GUNGNIR_FUNCTION_TYPE;

    @Override
    public void onInitialize() {
        NORMALIZE_GUNGNIR_FUNCTION_TYPE = Registry.register(
                Registries.LOOT_FUNCTION_TYPE,
                Identifier.of(MOD_ID, "normalize_gungnir"),
                new LootFunctionType<>(GungnirNormalizeLootFunction.CODEC)
        );

        LOGGER.info("Trident Changer initializing – Gungnir normalization active.");
        GungnirNormalizationHandler.register();
    }
}

