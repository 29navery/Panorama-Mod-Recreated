package dev.nicho.panoramamod.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PanoramaCaptureMixin {
    @Inject(method = "grabPanoramixScreenshot", at = @At("HEAD"))
    private void panoramamod$submitBeforeCapture(File folder, CallbackInfoReturnable<Component> cir) {
        // Capture runs outside the normal frame submission boundary.
        RenderSystem.getDevice().createCommandEncoder().submit();
    }

    @Inject(method = "grabPanoramixScreenshot", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
        shift = At.Shift.AFTER))
    private void panoramamod$submitFace(File folder, CallbackInfoReturnable<Component> cir) {
        // Six views reuse three-slot GPU buffers. Submit each view and its screenshot copy
        // before the next view can wait on one of those buffers' fences.
        RenderSystem.getDevice().createCommandEncoder().submit();
    }
}
