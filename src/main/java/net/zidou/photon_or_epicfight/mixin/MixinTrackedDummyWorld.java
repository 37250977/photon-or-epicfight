package net.zidou.photon_or_epicfight.mixin;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TrackedDummyWorld.class, remap = false)
public class MixinTrackedDummyWorld {

    @Redirect(method = "tickWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void photon$freezeCloneTick(Entity entity) {
        if (PhotonPatchStore.PATCHES.containsKey(entity.getId())) {
            entity.tickCount++;
            entity.setOldPosAndRot();

            if (PhotonPatchStore.IS_PLAYING) {
                entity.tickCount++;
                entity.setOldPosAndRot();
            } else {
                entity.setPos(0.5, 1.0, 0.5);
                entity.setDeltaMovement(0, 0, 0);
                entity.setYRot(0);
                entity.setXRot(0);
                entity.yRotO = 0;
                entity.xRotO = 0;

                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = 0;
                    living.yBodyRotO = 0;
                    living.yHeadRot = 0;
                    living.yHeadRotO = 0;
                }
            }
        } else {
            entity.tick();
        }
    }
}
