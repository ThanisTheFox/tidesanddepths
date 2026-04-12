package net.tidal.tidesanddepths.mixin.client;

import net.minecraft.client.input.KeyboardInput;
import net.tidal.tidesanddepths.client.hud.fishing.ClientFishingMinigame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tidesanddepths$disableJumpDuringFishingMinigame(boolean slowDown, float f, CallbackInfo ci) {
        if (ClientFishingMinigame.isActive()) {
            ((KeyboardInput) (Object) this).jumping = false;
        }
    }
}
