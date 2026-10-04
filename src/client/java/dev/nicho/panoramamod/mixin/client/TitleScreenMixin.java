/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraft.network.chat.Component
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.nicho.panoramamod.mixin.client;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TitleScreen.class})
public abstract class TitleScreenMixin
extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void panoramamod$addPanoramasButton(CallbackInfo ci) {
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"button.panoramamod.browser"), button -> this.minecraft.setScreenAndShow((Screen)new PanoramaBrowserScreen(this))).bounds(this.width - 122, 8, 114, 20).build());
    }
}

