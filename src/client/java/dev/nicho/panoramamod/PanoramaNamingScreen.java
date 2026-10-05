package dev.nicho.panoramamod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaNamingScreen
extends Screen {
    private final Screen parent;
    private EditBox nameField;
    private Button createButton;

    public PanoramaNamingScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.name_title"));
        this.parent = parent;
    }

    protected void init() {
        int centerX = this.width / 2;
        this.nameField = new EditBox(this.font, centerX - 120, this.height / 2 - 10, 240, 20, Component.translatable("screen.panoramamod.name_field"));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(PanoramaManager.defaultCaptureName());
        this.nameField.setResponder(value -> {
            this.createButton.active = !value.trim().isEmpty();
        });
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);
        this.createButton = this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.start_capture"), button -> this.startCapture()).bounds(centerX - 120, this.height / 2 + 24, 116, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose()).bounds(centerX + 4, this.height / 2 + 24, 116, 20).build());
        this.createButton.active = !this.nameField.getValue().trim().isEmpty();
    }

    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, this.width / 2 - 120, this.height / 2 - 38, 0xFFFFFFFF, true);
        guiGraphics.text(this.font, Component.translatable("screen.panoramamod.name_subtitle"), this.width / 2 - 120, this.height / 2 - 24, 0xFFAFAFAF, true);
    }

    private void startCapture() {
        if (PanoramaManager.requestCapture(this.minecraft, this.nameField.getValue())) {
            this.minecraft.setScreenAndShow(null);
        }
    }
}
