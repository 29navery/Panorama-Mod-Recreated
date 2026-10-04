package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaMod;
import dev.nicho.panoramamod.PanoramaRenameScreen;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaOptionsScreen extends Screen {
    private final Screen parent;
    private final PanoramaManager.PanoramaCapture capture;

    public PanoramaOptionsScreen(Screen parent, PanoramaManager.PanoramaCapture capture) {
        super(Component.translatable("screen.panoramamod.options_title"));
        this.parent = parent;
        this.capture = capture;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 - 34;

        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.export"), button -> this.exportCapture())
                .bounds(centerX - 104, buttonY, 208, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.rename"), button -> this.minecraft.setScreenAndShow(new PanoramaRenameScreen(this.parent, this.capture)))
                .bounds(centerX - 104, buttonY + 24, 208, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.delete"), button -> this.deleteCapture())
                .bounds(centerX - 104, buttonY + 48, 208, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(centerX - 104, buttonY + 82, 208, 20)
                .build()
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
    }

    private void exportCapture() {
        try {
            String packName = PanoramaManager.exportPanorama(this.minecraft, this.capture);
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)),
                Component.translatable("screen.panoramamod.export_success_title"),
                Component.translatable("screen.panoramamod.export_success_body", packName)
            ));
        } catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to export panorama {}", this.capture.directory(), exception);
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)),
                Component.translatable("screen.panoramamod.export_failed_title"),
                Component.translatable("screen.panoramamod.export_failed_body", this.capture.name())
            ));
        }
    }

    private void deleteCapture() {
        try {
            PanoramaManager.deletePanorama(this.capture);
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)),
                Component.translatable("screen.panoramamod.delete_success_title"),
                Component.translatable("screen.panoramamod.delete_success_body", this.capture.name())
            ));
        } catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to delete panorama {}", this.capture.directory(), exception);
            this.minecraft.setScreenAndShow(new AlertScreen(
                () -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)),
                Component.translatable("screen.panoramamod.delete_failed_title"),
                Component.translatable("screen.panoramamod.delete_failed_body", this.capture.name())
            ));
        }
    }
}