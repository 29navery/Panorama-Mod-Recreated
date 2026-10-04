/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 */
package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaNamingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class PanoramaSettingsScreen
extends Screen {
    private final Screen parent;

    public PanoramaSettingsScreen(Screen parent) {
        super((Component)Component.translatable((String)"screen.panoramamod.settings_title"));
        this.parent = parent;
    }

    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 - 34;
        Button captureButton = (Button)this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.capture"), button -> {
            if (PanoramaManager.canCapture(this.minecraft)) {
                this.minecraft.setScreenAndShow((Screen)new PanoramaNamingScreen(this));
            } else {
                PanoramaManager.sendClientMessage(this.minecraft, (Component)Component.translatable((String)"message.panoramamod.must_be_ingame"));
            }
        }).bounds(centerX - 104, buttonY, 208, 20).build());
        captureButton.active = PanoramaManager.canCapture(this.minecraft);
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.browser"), button -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent))).bounds(centerX - 104, buttonY + 24, 208, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.folder"), button -> PanoramaManager.openPanoramaFolder(this.minecraft)).bounds(centerX - 104, buttonY + 48, 208, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.done"), button -> this.onClose()).bounds(centerX - 104, buttonY + 82, 208, 20).build());
    }

    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, this.width / 2 - this.font.width((FormattedText)this.title) / 2, this.height / 2 - 68, 0xFFFFFF, true);
        guiGraphics.text(this.font, (Component)Component.translatable((String)"screen.panoramamod.settings_subtitle"), this.width / 2 - 104, this.height / 2 - 54, 0xAFAFAF, true);
    }
}

