package net.zidou.photon_or_epicfight.store;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhotonPatchStore {
    public static final Map<Integer, EntityPatch<?>> PATCHES = new ConcurrentHashMap<>();
    public static volatile int CLONE_ID = -1;
    public static volatile boolean IS_PLAYING = false;

    @javax.annotation.Nullable
    public static volatile Object FX_RUNTIME;
    @javax.annotation.Nullable
    public static volatile Vector3f BONE_POSITION;
    @javax.annotation.Nullable
    public static volatile Quaternionf BONE_ROTATION;

    public static void syncCloneToPlayer() {
        if (CLONE_ID < 0) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var entry = PATCHES.get(CLONE_ID);
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
