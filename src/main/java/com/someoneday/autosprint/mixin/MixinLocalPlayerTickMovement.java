package com.someoneday.autosprint.mixin;

import com.someoneday.autosprint.AutoSprintManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerTickMovement {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void autosprint$beforeAiStep(CallbackInfo ci) {
        AutoSprintManager.onClientMove((LocalPlayer) (Object) this);
    }
}
