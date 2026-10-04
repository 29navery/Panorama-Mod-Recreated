/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.KeyMapping$Category
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 */
package dev.nicho.panoramamod;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaNamingScreen;
import dev.nicho.panoramamod.PanoramaSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PanoramaModClient
implements ClientModInitializer {
    private static KeyMapping captureKey;
    private static KeyMapping settingsKey;

    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register((Identifier)Identifier.fromNamespaceAndPath((String)"panoramamod", (String)"general"));
        captureKey = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.panoramamod.capture", InputConstants.Type.KEYBOARD, 295, category));
        settingsKey = KeyMappingHelper.registerKeyMapping((KeyMapping)new KeyMapping("key.panoramamod.settings", InputConstants.Type.KEYBOARD, 296, category));
        ClientTickEvents.END_CLIENT_TICK.register(PanoramaManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(PanoramaModClient::handleKeybinds);
    }

    private static void handleKeybinds(Minecraft client) {
        while (captureKey.consumeClick()) {
            if (PanoramaManager.canCapture(client)) {
                client.setScreenAndShow((Screen)new PanoramaNamingScreen(null));
                continue;
            }
            PanoramaManager.sendClientMessage(client, (Component)Component.translatable((String)"message.panoramamod.must_be_ingame"));
        }
        while (settingsKey.consumeClick()) {
            client.setScreenAndShow((Screen)new PanoramaSettingsScreen(null));
        }
    }
}

