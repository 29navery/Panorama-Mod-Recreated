package dev.nicho.panoramamod;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaSettingsScreen extends Screen {
    private final Screen parent;

    public PanoramaSettingsScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 - 40;

        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.capture"), button -> {
                this.minecraft.setScreen(null);
                this.minecraft.setScreenAndShow(new PanoramaNamingScreen(this.parent));
            })
            .bounds(centerX - 104, buttonY, 208, 20)
            .build()
        );

        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.browser"), button -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this.parent)))
                .bounds(centerX - 104, buttonY + 24, 208, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.folder"), button -> PanoramaManager.openPanoramaFolder(this.minecraft))
                .bounds(centerX - 104, buttonY + 48, 208, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(centerX - 104, buttonY + 82, 208, 20)
                .build()
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}