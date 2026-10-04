/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.AlertScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 */
package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaMod;
import dev.nicho.panoramamod.PanoramaOptionsScreen;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PanoramaRenameScreen
extends Screen {
    private final Screen parent;
    private final PanoramaManager.PanoramaCapture capture;
    private EditBox nameField;
    private Button renameButton;

    public PanoramaRenameScreen(Screen parent, PanoramaManager.PanoramaCapture capture) {
        super((Component)Component.translatable((String)"screen.panoramamod.rename_title"));
        this.parent = parent;
        this.capture = capture;
    }

    protected void init() {
        int centerX = this.width / 2;
        this.nameField = new EditBox(this.font, centerX - 120, this.height / 2 - 10, 240, 20, (Component)Component.translatable((String)"screen.panoramamod.name_field"));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(this.capture.name());
        this.nameField.setResponder(value -> {
            this.renameButton.active = !value.trim().isEmpty();
        });
        this.addRenderableWidget((GuiEventListener)this.nameField);
        this.setInitialFocus((GuiEventListener)this.nameField);
        this.renameButton = (Button)this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.rename"), button -> this.renameCapture()).bounds(centerX - 120, this.height / 2 + 24, 116, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.cancel"), button -> this.onClose()).bounds(centerX + 4, this.height / 2 + 24, 116, 20).build());
        this.renameButton.active = !this.nameField.getValue().trim().isEmpty();
    }

    public void onClose() {
        this.minecraft.setScreenAndShow((Screen)new PanoramaOptionsScreen(this.parent, this.capture));
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, this.width / 2 - 120, this.height / 2 - 38, 0xFFFFFF, true);
        guiGraphics.text(this.font, (Component)Component.translatable((String)"screen.panoramamod.rename_subtitle"), this.width / 2 - 120, this.height / 2 - 24, 0xAFAFAF, true);
    }

    private void renameCapture() {
        try {
            PanoramaManager.renamePanorama(this.capture, this.nameField.getValue());
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this.parent)), (Component)Component.translatable((String)"screen.panoramamod.rename_success_title"), (Component)Component.translatable((String)"screen.panoramamod.rename_success_body", (Object[])new Object[]{this.nameField.getValue().trim()})));
        }
        catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to rename panorama {}", (Object)this.capture.directory(), (Object)exception);
            this.minecraft.setScreenAndShow((Screen)new AlertScreen(() -> this.minecraft.setScreenAndShow((Screen)new PanoramaOptionsScreen(this.parent, this.capture)), (Component)Component.translatable((String)"screen.panoramamod.rename_failed_title"), (Component)Component.translatable((String)"screen.panoramamod.rename_failed_body", (Object[])new Object[]{this.capture.name()})));
        }
    }
}

