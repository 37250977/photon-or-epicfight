package net.zidou.photon_or_epicfight.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

@Mixin(value = EpicFightCapabilities.class, remap = false)
public class EpicFightCapsMixin {

    @Inject(method = "getEntityPatch", at = @At("HEAD"), cancellable = true)
    private static <T extends EntityPatch> void photon$getEntityPatch(Entity entity, Class<T> type, CallbackInfoReturnable<T> cir) {
        if (entity == null) return;
        if (entity.getId() != PhotonPatchStore.CLONE_ID) return;
        EntityPatch<?> patch = PhotonPatchStore.PATCHES.get(entity.getId());
        if (patch != null && type.isAssignableFrom(patch.getClass())) {
            @SuppressWarnings("unchecked")
            T result = (T) patch;
            cir.setReturnValue(result);
        }
    }
}
