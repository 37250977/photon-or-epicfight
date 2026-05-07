package net.zidou.photon_or_epicfight;

import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.IFXObject;
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
    public final int jointId;
    public final Joint joint;

    public BoneEffect(FX fx, Level level, Entity entity, Joint joint, int jointId, String boneName) {
        super(fx, level, entity, AutoRotate.NONE);
        this.entity = entity;
        this.joint = joint;
        this.jointId = jointId;
        this.boneName = boneName;
    }

    public static BoneEffect play(LivingEntity entity, FX fx, String boneName) {
        return play(entity, fx, boneName, true);
    }

    public static BoneEffect play(LivingEntity entity, FX fx, String boneName, boolean allowMulti) {
        Level level = entity.level();
        if (!level.isClientSide) return null;
        var patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (patch == null) return null;
        var armature = patch.getArmature();
        if (armature == null) return null;
        var joint = ArmatureResolver.jointByName(armature, boneName);
        if (joint == null) return null;
        return play(entity, fx, joint, boneName, allowMulti);
    }

    public static BoneEffect play(LivingEntity entity, FX fx, Joint joint,
                                   String boneName, boolean allowMulti) {
        Level level = entity.level();
        if (!level.isClientSide) return null;

        var oldEffects = com.lowdragmc.photon.client.fx.EntityEffect.CACHE.get(entity);
        if (oldEffects != null) {
            oldEffects.removeIf(e -> true);
            if (oldEffects.isEmpty()) com.lowdragmc.photon.client.fx.EntityEffect.CACHE.remove(entity);
        }

        var cache = CACHE.computeIfAbsent(entity, k -> new ArrayList<>());
        if (!allowMulti) {
            cache.removeIf(e -> {
                if (e.runtime != null && !e.runtime.isAlive()) return true;
                return e.boneName.equals(boneName) && Objects.equals(e.fx.getFxLocation(), fx.getFxLocation());
            });
        }

        BoneEffect effect = new BoneEffect(fx, level, entity, joint, joint.getId(), boneName);
        effect.setAllowMulti(allowMulti);
        effect.start();
        if (!cache.contains(effect)) cache.add(effect);
        return effect;
    }

    public static void stop(Entity entity, String boneName) {
        var effects = CACHE.get(entity);
        if (effects != null) {
            effects.removeIf(e -> {
                if (e.boneName.equals(boneName) && e.runtime != null) {
                    e.runtime.destroy(true);
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
                if (e.runtime != null) e.runtime.destroy(true);
            }
        }
    }

    @Override
    public void start() {
        if (!entity.isAlive()) return;
        var oldEffects = com.lowdragmc.photon.client.fx.EntityEffect.CACHE.get(entity);
        if (oldEffects != null) {
            oldEffects.removeIf(e -> true);
            if (oldEffects.isEmpty()) com.lowdragmc.photon.client.fx.EntityEffect.CACHE.remove(entity);
        }
        this.runtime = fx.createRuntime();
        var root = this.runtime.getRoot();
        Vec3 pos = jointWorldPos(entity, 1.0F);
        root.updatePos(new Vector3f((float) pos.x, (float) pos.y, (float) pos.z));
        root.updateRotation(rotation);
        root.updateScale(scale);
        this.runtime.emmit(this);
        List<BoneEffect> effects = CACHE.computeIfAbsent(entity, k -> new ArrayList<>());
        if (!effects.contains(this)) effects.add(this);
    }

    @Override
    public void updateFXObjectTick(IFXObject fxObject) {
        super.updateFXObjectTick(fxObject);
        if (runtime != null && fxObject == runtime.root && !entity.isAlive()) {
            runtime.destroy(forcedDeath);
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

    private Vec3 jointWorldPos(Entity entity, float partialTicks) {
        if (joint == null) return entity.getPosition(partialTicks);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (patch == null) return entity.getPosition(partialTicks);
        Pose pose = patch.getAnimator().getPose(partialTicks);
        Vec3 pos = entity.getPosition(partialTicks);
        OpenMatrix4f modelTf = OpenMatrix4f.createTranslation((float) pos.x, (float) pos.y, (float) pos.z)
                .mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS).mulBack(patch.getModelMatrix(partialTicks)));
        OpenMatrix4f boneTf = new OpenMatrix4f(patch.getArmature().getBindedTransformFor(pose, joint)).mulFront(modelTf);
        return OpenMatrix4f.transform(boneTf, Vec3.ZERO);
    }

    private static Quaternionf extractQuaternion(OpenMatrix4f mat) {
        float m00 = mat.m00, m01 = mat.m01, m02 = mat.m02;
        float m10 = mat.m10, m11 = mat.m11, m12 = mat.m12;
        float m20 = mat.m20, m21 = mat.m21, m22 = mat.m22;
        float tr = m00 + m11 + m22;
        float qw, qx, qy, qz;
        if (tr > 0) {
            float s = (float) (Math.sqrt(tr + 1.0) * 2);
            qw = 0.25f * s;
            qx = (m21 - m12) / s;
            qy = (m02 - m20) / s;
            qz = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            float s = (float) (Math.sqrt(1.0 + m00 - m11 - m22) * 2);
            qw = (m21 - m12) / s;
            qx = 0.25f * s;
            qy = (m01 + m10) / s;
            qz = (m02 + m20) / s;
        } else if (m11 > m22) {
            float s = (float) (Math.sqrt(1.0 + m11 - m00 - m22) * 2);
            qw = (m02 - m20) / s;
            qx = (m01 + m10) / s;
            qy = 0.25f * s;
            qz = (m12 + m21) / s;
        } else {
            float s = (float) (Math.sqrt(1.0 + m22 - m00 - m11) * 2);
            qw = (m10 - m01) / s;
            qx = (m02 + m20) / s;
            qy = (m12 + m21) / s;
            qz = 0.25f * s;
        }
        return new Quaternionf(qx, qy, qz, qw).normalize();
    }

    private void removeFromCache() {
        List<BoneEffect> effects = CACHE.get(entity);
        if (effects != null) {
            effects.remove(this);
            if (effects.isEmpty()) CACHE.remove(entity);
        }
    }
}
