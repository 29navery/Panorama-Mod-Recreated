package dev.nicho.panoramamod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PanoramaModClient
implements ClientModInitializer {
    private static KeyMapping captureKey;
    private static KeyMapping settingsKey;

    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("panoramamod", "general"));
        captureKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.panoramamod.capture", InputConstants.Type.KEYBOARD, InputConstants.KEY_F6, category));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.panoramamod.settings", InputConstants.Type.KEYBOARD, InputConstants.KEY_F7, category));
        ClientTickEvents.END_CLIENT_TICK.register(PanoramaManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(PanoramaModClient::handleKeybinds);
    }

    private static void handleKeybinds(Minecraft client) {
        while (captureKey.consumeClick()) {
            if (PanoramaManager.canCapture(client)) {
                client.setScreenAndShow(new PanoramaNamingScreen(null));
                continue;
            }
            PanoramaManager.sendClientMessage(client, Component.translatable("message.panoramamod.must_be_ingame"));
        }
        while (settingsKey.consumeClick()) {
            client.setScreenAndShow(new PanoramaSettingsScreen(null));
        }
    }
}
