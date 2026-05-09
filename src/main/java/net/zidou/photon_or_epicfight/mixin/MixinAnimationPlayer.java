package net.zidou.photon_or_epicfight.mixin;

import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = AnimationPlayer.class, remap = false)
public class MixinAnimationPlayer {

    @Shadow
    protected float elapsedTime;
    @Shadow
    protected float prevElapsedTime;

    @Inject(method = "getCurrentPose", at = @At("HEAD"), cancellable = true)
    private void photon$bypassModifyPose(LivingEntityPatch<?> entitypatch, float partialTicks, CallbackInfoReturnable<Pose> cir) {
        if (!PhotonPatchStore.IS_PLAYING) return;

        float interpolatedTime = this.prevElapsedTime + (this.elapsedTime - this.prevElapsedTime) * partialTicks;
        AnimationPlayer self = (AnimationPlayer)(Object)this;
        cir.setReturnValue(self.getAnimation().get().getRawPose(interpolatedTime));
    }
}
