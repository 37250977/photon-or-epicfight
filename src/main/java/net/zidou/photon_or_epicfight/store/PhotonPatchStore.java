package net.zidou.photon_or_epicfight.store;

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
}
