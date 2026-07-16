package net.zidou.photon_or_epicfight.fxlinkage.engine;

import com.lowdragmc.photon.PhotonNetworking;
import com.lowdragmc.photon.command.BlockEffectCommand;
import com.lowdragmc.photon.command.EntityEffectCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.zidou.photon_or_epicfight.fxlinkage.command.BoneEffectCommand;
import net.zidou.photon_or_epicfight.fxlinkage.command.FxLinkageNetworking;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCommand;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxEffect;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxProfileData;
import net.zidou.photon_or_epicfight.fxlinkage.data.RuntimeLinkage;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxProfileLoader;

import java.util.List;

public class EffectExecutor {

    private static Class<?> autoRotateClass;
    private static Object autoRotateNone;
    private static Object autoRotateForward;
    private static Object autoRotateLook;
    private static Object autoRotateXrot;

    static {
        try {
            autoRotateClass = Class.forName("com.lowdragmc.photon.client.fx.EntityEffect$AutoRotate");
            autoRotateNone = Enum.valueOf((Class<Enum>) autoRotateClass, "NONE");
            autoRotateForward = Enum.valueOf((Class<Enum>) autoRotateClass, "FORWARD");
            autoRotateLook = Enum.valueOf((Class<Enum>) autoRotateClass, "LOOK");
            autoRotateXrot = Enum.valueOf((Class<Enum>) autoRotateClass, "XROT");
        } catch (Exception ignored) {}
    }

    public static void executeEffects(List<FxEffect> effects, RuntimeLinkage linkage,
                                      LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if (effects == null) return;
        for (FxEffect effect : effects) {
            executeEffect(effect, linkage, attacker, target, hitPos);
        }
    }

    public static void executeEffect(FxEffect effect, RuntimeLinkage linkage,
                                     LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if (effect.profile != null && !effect.profile.isEmpty()) {
            FxProfileData profile = FxProfileLoader.getProfile(effect.profile);
            if (profile != null) {
                FxEffect merged = mergeWithProfile(effect, profile);
                executeMergedEffect(merged, linkage, attacker, target, hitPos);
                return;
            }
        }

        executeMergedEffect(effect, linkage, attacker, target, hitPos);
    }

    public static void executeEffect(FxEffect effect, String side,
                                     LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if (effect.profile != null && !effect.profile.isEmpty()) {
            FxProfileData profile = FxProfileLoader.getProfile(effect.profile);
            if (profile != null) {
                FxEffect merged = mergeWithProfile(effect, profile);
                executeMergedEffect(merged, side, attacker, target, hitPos);
                return;
            }
        }

        executeMergedEffect(effect, side, attacker, target, hitPos);
    }

    private static FxEffect mergeWithProfile(FxEffect effect, FxProfileData profile) {
        FxEffect merged = new FxEffect();
        merged.fx = effect.fx != null ? effect.fx : profile.fx;
        merged.position = effect.position != null ? effect.position : profile.position;
        merged.follow = effect.follow != null ? effect.follow : profile.follow;
        merged.bone = effect.bone != null ? effect.bone : profile.bone;
        merged.follow_rotation = effect.follow_rotation != null ? effect.follow_rotation : profile.follow_rotation;
        merged.allow_multi = effect.allow_multi != null ? effect.allow_multi : profile.allow_multi;
        merged.scale = effect.scale != null ? effect.scale : profile.scale;
        merged.duration = effect.duration != null ? effect.duration : profile.duration;
        merged.inherit_color = effect.inherit_color != null ? effect.inherit_color : profile.inherit_color;
        merged.commands = effect.commands != null ? effect.commands : profile.commands;
        return merged;
    }

    private static void executeMergedEffect(FxEffect effect, RuntimeLinkage linkage,
                                            LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        String side = linkage.raw.side != null ? linkage.raw.side : "both";
        boolean allowMulti = effect.allow_multi != null && effect.allow_multi;

        if (effect.commands != null) {
            for (FxCommand cmd : effect.commands) {
                executeCommand(cmd, linkage, attacker, target, hitPos, side);
            }
        }
        if (effect.fx != null && !effect.fx.isEmpty()) {
            if (!"server".equals(side)) {
                boolean follow = effect.follow != null && effect.follow;
                spawnFX(effect.fx, effect.position, follow, effect.scale,
                        effect.bone, effect.follow_rotation, allowMulti,
                        attacker, target, hitPos);
            }
        }
    }

    private static void executeMergedEffect(FxEffect effect, String side,
                                            LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        boolean allowMulti = effect.allow_multi != null && effect.allow_multi;

        if (effect.commands != null) {
            for (FxCommand cmd : effect.commands) {
                executeCommand(cmd, side, attacker, target, hitPos);
            }
        }
        if (effect.fx != null && !effect.fx.isEmpty()) {
            if (!"server".equals(side)) {
                boolean follow = effect.follow != null && effect.follow;
                spawnFX(effect.fx, effect.position, follow, effect.scale,
                        effect.bone, effect.follow_rotation, allowMulti,
                        attacker, target, hitPos);
            }
        }
    }

    public static void executeCommand(FxCommand cmd, RuntimeLinkage linkage, LivingEntity attacker,
                                      LivingEntity target, Vec3 hitPos) {
        executeCommand(cmd, linkage, attacker, target, hitPos, "both");
    }

    public static void executeCommand(FxCommand cmd, String side, LivingEntity attacker,
                                      LivingEntity target, Vec3 hitPos) {
        executeCommand(cmd, null, attacker, target, hitPos, side);
    }

    private static void executeCommand(FxCommand cmd, RuntimeLinkage linkage, LivingEntity attacker,
                                       LivingEntity target, Vec3 hitPos, String side) {
        if (cmd == null) return;
        FxCommand.CommandType type = cmd.getCommandType();
        if (type == null) return;

        switch (type) {
            case SPAWN_FX, SPAWN_FX_BURST -> {
                boolean cmdAllowMulti = cmd.allow_multi != null && cmd.allow_multi;
                if (!"server".equals(side))
                    spawnFX(cmd.fx, cmd.position, cmd.follow != null && cmd.follow,
                            cmd.scale != null ? cmd.scale.floatValue() : null,
                            cmd.bone, cmd.follow_rotation, cmdAllowMulti,
                            attacker, target, hitPos);
            }
            case PLAY_SOUND -> {
                if (!"server".equals(side))
                    playSound(cmd.sound, cmd.volume, cmd.pitch, attacker, target, hitPos);
            }
            case COMMAND -> {
                if (!"client".equals(side))
                    executeRawCommand(cmd.command, attacker);
            }
            case DAMAGE -> {
                if (!"client".equals(side))
                    applyDamage(cmd, attacker, target);
            }
            case SET_PHASE -> { if (linkage != null) setPhase(cmd, linkage); }
            case SET_COOLDOWN -> { if (linkage != null) setCooldown(cmd, linkage); }
            case INCREMENT_COUNTER -> { if (linkage != null) incrementCounter(cmd, linkage); }
            case RESET_COUNTER -> { if (linkage != null) resetCounter(linkage); }
        }
    }

    private static Vec3 resolveSpawnPos(String position, LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if ("target".equals(position) && hitPos != null) return hitPos;
        if ("target".equals(position) && target != null) return target.position();
        if ("weapon".equals(position) && attacker != null) return attacker.position().add(0, attacker.getEyeHeight() * 0.6, 0);
        if ("self".equals(position) && attacker != null) return attacker.position();
        if ("ground".equals(position)) {
            Vec3 ref = target != null ? target.position() : (attacker != null ? attacker.position() : Vec3.ZERO);
            return new Vec3(ref.x, 0, ref.z);
        }
        if (attacker != null) return attacker.position();
        if (hitPos != null) return hitPos;
        if (target != null) return target.position();
        return Vec3.ZERO;
    }

    private static Entity resolveSpawnEntity(String position, LivingEntity attacker, LivingEntity target) {
        if ("weapon".equals(position) || "self".equals(position)) return attacker;
        if ("target".equals(position)) return target;
        return null;
    }

    private static void spawnFX(String fxPath, String position, boolean follow,
                                 Float scale, String bone, Object followRotation, boolean allowMulti,
                                 LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if (fxPath == null || fxPath.isEmpty()) return;
        try {
            ResourceLocation fxId = ResourceLocation.parse(fxPath);
            if (attacker == null || !(attacker.level() instanceof ServerLevel)) return;

            // 骨骼模式: 发 BoneEffectCommand，客户端用 BoneEffect 逐帧更新位置+旋转
            if (bone != null) {
                Entity targetEntity = resolveSpawnEntity(position, attacker, target);
                if (targetEntity != null) {
                    FxLinkageNetworking.NETWORK.sendToAll(
                            new BoneEffectCommand(fxId, targetEntity.getId(), bone, allowMulti));
                }
                return;
            }

            // 普通 follow 模式: EntityEffectCommand + AutoRotate
            Entity followEntity = follow ? resolveSpawnEntity(position, attacker, target) : null;
            if (follow && followEntity != null) {
                Object autoRotate = resolveAutoRotate(followRotation);

                EntityEffectCommand command = new EntityEffectCommand();
                command.setLocation(fxId);
                command.setEntities(List.of(followEntity));
                command.setAllowMulti(true);
                try {
                    command.getClass().getMethod("setAutoRotate", autoRotateClass).invoke(command, autoRotate);
                } catch (Exception ignored) {}
                if (scale != null) command.setScale(new Vec3(scale, scale, scale));
                PhotonNetworking.NETWORK.sendToAll(command);
            } else {
                Vec3 spawnPos = resolveSpawnPos(position, attacker, target, hitPos);
                BlockEffectCommand command = new BlockEffectCommand();
                command.setLocation(fxId);
                command.setPos(BlockPos.containing(spawnPos));
                command.setOffset(new Vec3(
                        spawnPos.x - Math.floor(spawnPos.x),
                        spawnPos.y - Math.floor(spawnPos.y),
                        spawnPos.z - Math.floor(spawnPos.z)
                ));
                command.setAllowMulti(true);
                if (scale != null) command.setScale(new Vec3(scale, scale, scale));
                PhotonNetworking.NETWORK.sendToAll(command);
            }
        } catch (Exception ignored) {}
    }

    private static Object resolveAutoRotate(Object followRotation) {
        if (followRotation == null) return autoRotateNone;
        if (followRotation instanceof Boolean b) {
            return b ? autoRotateForward : autoRotateNone;
        }
        String s = followRotation.toString().toLowerCase();
        return switch (s) {
            case "forward" -> autoRotateForward;
            case "look" -> autoRotateLook;
            case "xrot" -> autoRotateXrot;
            default -> autoRotateNone;
        };
    }

    private static void playSound(String soundPath, Float volume, Float pitch,
                                   LivingEntity attacker, LivingEntity target, Vec3 hitPos) {
        if (soundPath == null) return;
        try {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse(soundPath));
            if (sound == null) return;
            Vec3 pos = attacker != null ? attacker.position() : (hitPos != null ? hitPos : Vec3.ZERO);
            float vol = volume != null ? volume : 1.0f;
            float pit = pitch != null ? pitch : 1.0f;

            if (attacker != null && attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, vol, pit);
            }
        } catch (Exception ignored) {}
    }

    private static void executeRawCommand(String command, LivingEntity attacker) {
        if (command == null || attacker == null) return;
        if (attacker instanceof ServerPlayer player) {
            try {
                player.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(), command);
            } catch (Exception ignored) {}
        }
    }

    private static void applyDamage(FxCommand cmd, LivingEntity attacker, LivingEntity target) {
        if (cmd.fx_damage == null || target == null) return;
        float dmg = cmd.fx_damage.floatValue();
        if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), dmg);
        } else {
            target.hurt(target.damageSources().generic(), dmg);
        }
    }

    private static void setPhase(FxCommand cmd, RuntimeLinkage linkage) {
        if (cmd.value != null) {
            try {
                linkage.states.phase = Integer.parseInt(cmd.value);
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void setCooldown(FxCommand cmd, RuntimeLinkage linkage) {
        if (cmd.value != null) {
            try {
                linkage.states.currentCooldown = Integer.parseInt(cmd.value);
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void incrementCounter(FxCommand cmd, RuntimeLinkage linkage) {
        if (linkage.states.phaselock) return;
        linkage.states.phase = Math.min(linkage.states.phase + 1, linkage.states.maxPhase);
    }

    private static void resetCounter(RuntimeLinkage linkage) {
        linkage.states.phase = 0;
        linkage.states.currentCooldown = 0;
    }
}
