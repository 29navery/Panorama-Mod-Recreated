package dev.nicho.panoramamod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaSettingsScreen
extends Screen {
    private final Screen parent;

    public PanoramaSettingsScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.settings_title"));
        this.parent = parent;
    }

    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 - 34;
        Button captureButton = this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.capture"), button -> {
            if (PanoramaManager.canCapture(this.minecraft)) {
                this.minecraft.setScreenAndShow(new PanoramaNamingScreen(this));
            } else {
                PanoramaManager.sendClientMessage(this.minecraft, Component.translatable("message.panoramamod.must_be_ingame"));
            }
        }).bounds(centerX - 104, buttonY, 208, 20).build());
        captureButton.active = PanoramaManager.canCapture(this.minecraft);
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.browser"), button -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent))).bounds(centerX - 104, buttonY + 24, 208, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.folder"), button -> PanoramaManager.openPanoramaFolder(this.minecraft)).bounds(centerX - 104, buttonY + 48, 208, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(centerX - 104, buttonY + 82, 208, 20).build());
    }

    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, this.height / 2 - 68, 0xFFFFFFFF, true);
        guiGraphics.text(this.font, Component.translatable("screen.panoramamod.settings_subtitle"), this.width / 2 - 104, this.height / 2 - 54, 0xFFAFAFAF, true);
    }
}
