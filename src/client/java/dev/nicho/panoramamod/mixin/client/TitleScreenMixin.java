package dev.nicho.panoramamod.mixin.client;

import dev.nicho.panoramamod.PanoramaBrowserScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void panoramamod$addTitleScreenButton(CallbackInfo ci) {
        this.addRenderableWidget(
            Button.builder(Component.translatable("button.panoramamod.browser"), button -> this.minecraft.setScreenAndShow(new PanoramaBrowserScreen(this)))
                .bounds(this.width - 122, 8, 114, 20)
                .build()
        );
    }
}