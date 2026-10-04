package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaNamingScreen extends Screen {
    private final Screen parent;
    private EditBox nameField;
    private Button createButton;

    public PanoramaNamingScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.name_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.nameField = new EditBox(this.font, centerX - 120, this.height / 2 - 10, 240, 20, Component.translatable("screen.panoramamod.name_field"));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(PanoramaManager.defaultCaptureName());
        this.nameField.setResponder(value -> {
            if (this.createButton != null) {
                this.createButton.active = !value.trim().isEmpty();
            }
        });
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);
        
        this.createButton = this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.start_capture"), button -> this.startCapture())
                .bounds(centerX - 120, this.height / 2 + 24, 116, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX + 4, this.height / 2 + 24, 116, 20)
                .build()
        );
        this.createButton.active = !this.nameField.getValue().trim().isEmpty();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
    }

    private void startCapture() {
        if (PanoramaManager.requestCapture(this.minecraft, this.nameField.getValue())) {
            this.minecraft.setScreenAndShow(null);
        }
    }
}