package net.zidou.photon_or_epicfight.mixin;

import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 兼容修复：阻止 LDlib CameraEntity 触发 AttachCapabilitiesEvent。
 * <p>
 * CameraEntity 构造时继承自 Entity → CapabilityProvider 的
 * {@code gatherCapabilities()} 会触发事件，epicfight-skilltree
 * 在其中强转为 {@code Player} 导致崩溃。
 * CameraEntity 不需要任何能力，直接跳过该方法。
 * </p>
 */
@Mixin(value = CapabilityProvider.class, remap = false)
public class MixinCameraEntityCompat {

    @Inject(method = "gatherCapabilities", at = @At("HEAD"), cancellable = true, remap = false)
    private void photon$skipIfCameraEntity(CallbackInfo ci) {
        if (this.getClass().getName()
                .equals("com.lowdragmc.lowdraglib.client.scene.CameraEntity")) {
            ci.cancel();
        }
    }
}
