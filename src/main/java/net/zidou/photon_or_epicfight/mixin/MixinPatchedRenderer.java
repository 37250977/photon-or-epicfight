package net.zidou.photon_or_epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import net.zidou.photon_or_epicfight.RawAnimator;

@Mixin(value = ClientAnimator.class, remap = false)
public class MixinPatchedRenderer implements RawAnimator {

    @Unique
    public Pose photon$getRawAnimationPose(float partialTicks) {
        ClientAnimator self = (ClientAnimator) (Object) this;

        Pose composed = new Pose();
        // 从 baseLayer 取 pose
        composed.load(self.baseLayer.animationPlayer.getCurrentPose(self.getEntityPatch(), partialTicks), Pose.LoadOperation.OVERWRITE);

        // 从复合层取 pose
        for (Layer.Priority priority : self.baseLayer.getBaseLayerPriority().highers()) {
            Layer layer = self.baseLayer.getLayer(priority);
            if (!layer.isOff()) {
                Pose layerPose = layer.animationPlayer.getCurrentPose(self.getEntityPatch(), partialTicks);
                composed.load(layerPose, Pose.LoadOperation.OVERWRITE);
            }
        }

        return composed;
    }
}
