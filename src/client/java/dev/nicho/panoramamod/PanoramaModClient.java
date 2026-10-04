package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaNamingScreen;
import dev.nicho.panoramamod.PanoramaSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PanoramaModClient implements ClientModInitializer {
    private static KeyMapping captureKey;
    private static KeyMapping settingsKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("panoramamod", "general"));
        captureKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.panoramamod.capture", InputUtil.Type.KEYSYM, 295, category));
        settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.panoramamod.settings", InputUtil.Type.KEYSYM, 296, category));
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