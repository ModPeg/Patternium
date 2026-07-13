package com.patternium.mod;

import com.patternium.mod.crafting.BulkCrafter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatterniumMod implements ClientModInitializer {
    public static final String MOD_ID = "patternium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client ->
            BulkCrafter.getInstance().tick()
        );
    }
}
