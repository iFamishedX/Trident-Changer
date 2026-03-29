package com.ifamishedx.tridentchanger;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TridentChangerMod implements ModInitializer {

    public static final String MOD_ID = "trident-changer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Trident Changer initializing – Gungnir normalization active.");
        GungnirNormalizationHandler.register();
    }
}
