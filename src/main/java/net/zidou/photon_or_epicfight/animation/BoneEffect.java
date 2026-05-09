package net.zidou.photon_or_epicfight.animation;

import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class BoneEffect extends EntityEffect {

    public static final ConcurrentHashMap<Entity, List<BoneEffect>> CACHE = new ConcurrentHashMap<>();

    public final Entity entity;
    public final String boneName;
    public final Joint joint;

    public BoneEffect(FX fx, Level level, Entity entity, Joint joint, String boneName) {
        super(fx, level, entity, AutoRotate.NONE);
        this.entity = entity;
        this.joint = joint;
        this.boneName = boneName;
    }

    public static BoneEffect play(LivingEntity entity, FX fx, String boneName) {
        Level level = entity.level();
        if (!level.isClientSide) return null;
        var patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (patch == null) return null;
        var armature = patch.getArmature();
        if (armature == null) return null;
        var joint = ArmatureResolver.jointByName(armature, boneName);
        if (joint == null) return null;

        BoneEffect effect = new BoneEffect(fx, level, entity, joint, boneName);
        effect.start();
        CACHE.computeIfAbsent(entity, k -> new ArrayList<>()).add(effect);
        return effect;
    }

    public static void stop(Entity entity, String boneName) {
        var effects = CACHE.get(entity);
        if (effects != null) {
            effects.removeIf(e -> {
                if (e.boneName.equals(boneName) && e.runtime != null) {
                    e.runtime.destroy(true);
                    EntityEffect.CACHE.computeIfAbsent(entity, p -> new ArrayList<>()).remove(e);
                    return true;
                }
                return false;
            });
            if (effects.isEmpty()) CACHE.remove(entity);
        }
    }

    public static void stop(Entity entity, String boneName, ResourceLocation fxId) {
        var effects = CACHE.get(entity);
        if (effects != null) {
            effects.removeIf(e -> {
                if (e.boneName.equals(boneName) && e.fx != null && e.fx.getFxLocation().equals(fxId) && e.runtime != null) {
                    e.runtime.destroy(true);
                    EntityEffect.CACHE.computeIfAbsent(entity, p -> new ArrayList<>()).remove(e);
                    return true;
                }
                return false;
            });
            if (effects.isEmpty()) CACHE.remove(entity);
        }
    }

    public static void stopAll(Entity entity) {
        var effects = CACHE.remove(entity);
        if (effects != null) {
            for (var e : effects) {
                if (e.runtime != null) {
                    e.runtime.destroy(true);
                    EntityEffect.CACHE.computeIfAbsent(entity, p -> new ArrayList<>()).remove(e);
                }
            }
        }
    }

    @Override
    public void updateFXObjectTick(IFXObject fxObject) {
        if (runtime != null && fxObject == runtime.root && !entity.isAlive()) {
            runtime.destroy(forcedDeath);
            EntityEffect.CACHE.computeIfAbsent(entity, p -> new ArrayList<>()).remove(this);
            if (EntityEffect.CACHE.get(entity).isEmpty()) {
                EntityEffect.CACHE.remove(entity);
            }
            removeFromCache();
        }
    }

    @Override
    public void updateFXObjectFrame(IFXObject fxObject, float partialTicks) {
        if (runtime == null || fxObject != runtime.root) return;
        if (!(entity instanceof LivingEntity living)) return;

        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (patch == null) return;

        Vec3 pos = living.getPosition(partialTicks);
        Pose rawPose;

        if (patch.getAnimator() instanceof RawAnimator rawProvider) {
            rawPose = rawProvider.photon$getRawAnimationPose(partialTicks);
        } else {
            rawPose = patch.getAnimator().getPose(partialTicks);
        }

        OpenMatrix4f modelTf = OpenMatrix4f.createTranslation((float) pos.x, (float) pos.y, (float) pos.z)
                .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS).mulBack(patch.getModelMatrix(partialTicks)));
        OpenMatrix4f boneTf = new OpenMatrix4f(patch.getArmature().getBindedTransformFor(rawPose, joint)).mulFront(modelTf);
        Vec3 bonePos = OpenMatrix4f.transform(boneTf, Vec3.ZERO);
        runtime.root.updatePos(new Vector3f(
                (float) bonePos.x + offset.x,
                (float) bonePos.y + offset.y,
                (float) bonePos.z + offset.z
        ));
        boneTf = boneTf.removeScale();
        boneTf.m30 = 0; boneTf.m31 = 0; boneTf.m32 = 0;
        var jomlMat = new org.joml.Matrix4f(
                boneTf.m00, boneTf.m01, boneTf.m02, 0,
                boneTf.m10, boneTf.m11, boneTf.m12, 0,
                boneTf.m20, boneTf.m21, boneTf.m22, 0,
                0, 0, 0, 1);
        runtime.root.updateRotation(new Quaternionf().setFromUnnormalized(jomlMat));
    }

    private void removeFromCache() {
        List<BoneEffect> effects = CACHE.get(entity);
        if (effects != null) {
            effects.remove(this);
            if (effects.isEmpty()) CACHE.remove(entity);
        }
    }
}
