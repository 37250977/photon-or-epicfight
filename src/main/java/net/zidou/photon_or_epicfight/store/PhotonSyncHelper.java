package net.zidou.photon_or_epicfight.store;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhotonSyncHelper {

    public static void syncCloneToPlayer() {
        if (PhotonPatchStore.CLONE_ID < 0) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var entry = PhotonPatchStore.PATCHES.get(PhotonPatchStore.CLONE_ID);
        if (entry == null) return;
        var entity = entry.getOriginal();
        if (!(entity instanceof RemotePlayer clone)) return;

        for (var slot : EquipmentSlot.values()) {
            clone.setItemSlot(slot, mc.player.getItemBySlot(slot));
        }
        clone.setHealth(mc.player.getHealth());
        clone.setAbsorptionAmount(mc.player.getAbsorptionAmount());
    }
}
