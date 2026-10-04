/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.AlertScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 */
package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaMod;
import dev.nicho.panoramamod.PanoramaRenameScreen;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class PanoramaOptionsScreen
extends Screen {
    private final Screen parent;
    private final PanoramaManager.PanoramaCapture capture;

    public PanoramaOptionsScreen(Screen parent, PanoramaManager.PanoramaCapture capture) {
        super((Component)Component.translatable((String)"screen.panoramamod.options_title"));
        this.parent = parent;
        this.capture = capture;
    }

    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 - 34;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.export"), button -> this.exportCapture()).bounds(centerX - 104, buttonY, 208, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.rename"), button -> this.minecraft.setScreenAndShow((Screen)new PanoramaRenameScreen(this.parent, this.capture))).bounds(centerX - 104, buttonY + 24, 208, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.delete"), button -> this.deleteCapture()).bounds(centerX - 104, buttonY + 48, 208, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.back"), button -> this.onClose()).bounds(centerX - 104, buttonY + 82, 208, 20).build());
    }

    public void onClose() {
        this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent));
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, this.width / 2 - this.font.width((FormattedText)this.title) / 2, this.height / 2 - 72, 0xFFFFFF, true);
        guiGraphics.text(this.font, (Component)Component.literal((String)this.capture.name()), this.width / 2 - this.font.width(this.capture.name()) / 2, this.height / 2 - 56, 0xAFAFAF, true);
    }

    private void exportCapture() {
        try {
            String packName = PanoramaManager.exportPanorama(this.minecraft, this.capture);
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent)), (Component)Component.translatable((String)"screen.panoramamod.export_success_title"), (Component)Component.translatable((String)"screen.panoramamod.export_success_body", (Object[])new Object[]{packName})));
        }
        catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to export panorama {}", (Object)this.capture.directory(), (Object)exception);
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent)), (Component)Component.translatable((String)"screen.panoramamod.export_failed_title"), (Component)Component.translatable((String)"screen.panoramamod.export_failed_body", (Object[])new Object[]{this.capture.name()})));
        }
    }

    private void deleteCapture() {
        try {
            PanoramaManager.deletePanorama(this.capture);
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent)), (Component)Component.translatable((String)"screen.panoramamod.delete_success_title"), (Component)Component.translatable((String)"screen.panoramamod.delete_success_body", (Object[])new Object[]{this.capture.name()})));
        }
        catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to delete panorama {}", (Object)this.capture.directory(), (Object)exception);
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent)), (Component)Component.translatable((String)"screen.panoramamod.delete_failed_title"), (Component)Component.translatable((String)"screen.panoramamod.delete_failed_body", (Object[])new Object[]{this.capture.name()})));
        }
    }
}

