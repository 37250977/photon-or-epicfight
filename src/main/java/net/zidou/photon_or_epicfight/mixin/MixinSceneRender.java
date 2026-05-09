package net.zidou.photon_or_epicfight.mixin;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.photon.client.fx.FXRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SceneWidget.class, remap = false)
public class MixinSceneRender {

    @Inject(method = "drawInBackground", at = @At("HEAD"))
    private void photon$applyBoneTransform(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Object runtime = PhotonPatchStore.FX_RUNTIME;
        if (runtime instanceof FXRuntime fxRuntime) {
            if (PhotonPatchStore.BONE_POSITION != null) {
                fxRuntime.root.updatePos(PhotonPatchStore.BONE_POSITION);
            }
            if (PhotonPatchStore.BONE_ROTATION != null) {
                fxRuntime.root.updateRotation(PhotonPatchStore.BONE_ROTATION);
            }
        }
    }
}
