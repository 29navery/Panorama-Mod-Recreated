package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaMod;
import dev.nicho.panoramamod.PanoramaOptionsScreen;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaRenameScreen extends Screen {
    private final Screen parent;
    private final PanoramaManager.PanoramaCapture capture;
    private EditBox nameField;
    private Button renameButton;

    public PanoramaRenameScreen(Screen parent, PanoramaManager.PanoramaCapture capture) {
        super(Component.translatable("screen.panoramamod.rename_title"));
        this.parent = parent;
        this.capture = capture;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.nameField = new EditBox(this.font, centerX - 120, this.height / 2 - 10, 240, 20, Component.translatable("screen.panoramamod.name_field"));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(this.capture.name());
        this.nameField.setResponder(value -> {
            if (this.renameButton != null) {
                this.renameButton.active = !value.trim().isEmpty();
            }
        });
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);
        
        this.renameButton = this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.rename"), button -> this.renameCapture())
                .bounds(centerX - 120, this.height / 2 + 24, 116, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX + 4, this.height / 2 + 24, 116, 20)
                .build()
        );
        this.renameButton.active = !this.nameField.getValue().trim().isEmpty();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(new PanoramaOptionsScreen(this.parent, this.capture));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
    }

    private void renameCapture() {
        try {
            PanoramaManager.renamePanorama(this.capture, this.nameField.getValue());
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)),
                Component.translatable("screen.panoramamod.rename_success_title"),
                Component.translatable("screen.panoramamod.rename_success_body", this.nameField.getValue().trim())
            ));
        } catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to rename panorama {}", this.capture.directory(), exception);
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaOptionsScreen(this.parent, this.capture)),
                Component.translatable("screen.panoramamod.rename_failed_title"),
                Component.translatable("screen.panoramamod.rename_failed_body", this.capture.name())
            ));
        }
    }
}