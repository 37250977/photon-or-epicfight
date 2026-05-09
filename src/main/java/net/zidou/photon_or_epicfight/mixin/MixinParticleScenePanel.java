package net.zidou.photon_or_epicfight.mixin;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.photon.gui.editor.ParticleScenePanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.zidou.photon_or_epicfight.config.PhotonEditorConfig;
import net.zidou.photon_or_epicfight.store.PhotonPatchStore;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;

@Mixin(value = ParticleScenePanel.class, remap = false)
public class MixinParticleScenePanel {

    @Shadow
    protected TrackedDummyWorld level;

    private static void ensureClone(TrackedDummyWorld level) {
        if (!PhotonEditorConfig.epicfightPreview) return;
        if (!PhotonPatchStore.PATCHES.isEmpty()) return;

        try {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            var player = mc.player;
            var clone = new RemotePlayer(mc.level, player.getGameProfile());

            for (var slot : EquipmentSlot.values()) {
                clone.setItemSlot(slot, player.getItemBySlot(slot));
            }

            clone.setPos(0.5, 1.0, 0.5);
            clone.xOld = clone.getX();
            clone.yOld = clone.getY();
            clone.zOld = clone.getZ();
            clone.tickCount = 100;
            clone.setYRot(0);
            clone.setXRot(0);
            clone.yRotO = 0;
            clone.xRotO = 0;
            clone.yBodyRot = 0;
            clone.yBodyRotO = 0;
            clone.yHeadRot = 0;
            clone.yHeadRotO = 0;

            EntityPatchProvider provider = new EntityPatchProvider(clone);
            if (!provider.hasCapability()) return;

            EntityPatch<?> patch = provider.get();
            if (patch != null) {
                ((yesman.epicfight.world.capabilities.entitypatch.EntityPatch) patch).onConstructed(clone);
                if (patch instanceof PlayerPatch<?> pp) {
                    pp.toEpicFightMode(false);
                    pp.setModelYRot(0, false);
                }
                PhotonPatchStore.PATCHES.put(clone.getId(), patch);
                PhotonPatchStore.CLONE_ID = clone.getId();
            }

            level.addFreshEntity(clone);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "onPanelSelected", at = @At("HEAD"))
    private void photon$clearOldPatch(CallbackInfo ci) {
        PhotonPatchStore.PATCHES.clear();
    }

    @Inject(method = "onPanelSelected", at = @At("TAIL"))
    private void photon$onPanelSelected(CallbackInfo ci) {
        ensureClone(level);
    }

    @Inject(method = "resetScene", at = @At("TAIL"))
    private void photon$onResetScene(CallbackInfo ci) {
        PhotonPatchStore.PATCHES.clear();
        PhotonPatchStore.CLONE_ID = -1;
        ensureClone(level);
    }

    @Inject(method = "onPanelDeselected", at = @At("TAIL"))
    private void photon$onPanelDeselected(CallbackInfo ci) {
        PhotonPatchStore.PATCHES.clear();
        PhotonPatchStore.CLONE_ID = -1;
    }
}
