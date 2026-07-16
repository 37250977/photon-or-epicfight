package net.zidou.photon_or_epicfight.fxlinkage.mob;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxEffect;
import net.zidou.photon_or_epicfight.fxlinkage.engine.ConditionEngine;
import net.zidou.photon_or_epicfight.fxlinkage.engine.EffectExecutor;

import java.util.List;

public class FxMobLinkageEngine {

    public static void fireEvent(String event, MatchContext ctx, Vec3 hitPos) {
        if (ctx.attacker == null) return;

        List<RuntimeMobLinkage> candidates = FxMobLinkageLoader.getLinkagesForEvent(event);
        if (candidates.isEmpty()) return;

        for (RuntimeMobLinkage linkage : candidates) {
            if (!matchLinkage(linkage, ctx)) continue;
            if (linkage.states.currentCooldown > 0) continue;

            List<FxEffect> effects = linkage.eventEffects.get(event);
            if (effects == null || effects.isEmpty()) continue;

            String side = linkage.raw.side != null ? linkage.raw.side : "both";
            boolean anyExecuted = false;
            for (FxEffect effect : effects) {
                if (effect.conditions == null || ConditionEngine.checkConditions(effect.conditions, ctx)) {
                    EffectExecutor.executeEffect(effect, side, ctx.attacker, ctx.target, hitPos);
                    executeStateCommands(effect, linkage);
                    anyExecuted = true;
                }
            }

            if (anyExecuted && linkage.states.cooldown > 0) {
                linkage.states.currentCooldown = linkage.states.cooldown;
            }
        }
    }

    private static void executeStateCommands(FxEffect effect, RuntimeMobLinkage linkage) {
        if (effect.commands == null) return;
        for (var cmd : effect.commands) {
            if (cmd == null) continue;
            String type = cmd.type;
            if (type == null) continue;
            switch (type) {
                case "set_phase" -> {
                    if (cmd.value != null) {
                        try { linkage.states.phase = Integer.parseInt(cmd.value); } catch (Exception ignored) {}
                    }
                }
                case "set_cooldown" -> {
                    if (cmd.value != null) {
                        try { linkage.states.currentCooldown = Integer.parseInt(cmd.value); } catch (Exception ignored) {}
                    }
                }
                case "increment_counter" -> {
                    if (!linkage.states.phaselock) {
                        linkage.states.phase = Math.min(linkage.states.phase + 1, linkage.states.maxPhase);
                    }
                }
                case "reset_counter" -> {
                    linkage.states.phase = 0;
                    linkage.states.currentCooldown = 0;
                }
            }
        }
    }

    public static boolean matchLinkage(RuntimeMobLinkage linkage, MatchContext ctx) {
        if (!linkage.weaponCategories.isEmpty() && ctx.weaponCategories != null) {
            boolean matchWeapon = false;
            for (ResourceLocation cat : linkage.weaponCategories) {
                if (ctx.weaponCategories.contains(cat)) {
                    matchWeapon = true;
                    break;
                }
            }
            if (!matchWeapon) return false;
        }

        if (!linkage.weapons.isEmpty()) {
            if (ctx.weaponId == null || !linkage.weapons.contains(ctx.weaponId)) return false;
        }

        if (linkage.raw.conditions != null && !linkage.raw.conditions.isEmpty()) {
            if (!ConditionEngine.checkConditions(linkage.raw.conditions, ctx)) return false;
        }

        return true;
    }

    public static void tickCooldowns() {
        for (RuntimeMobLinkage l : FxMobLinkageLoader.getAllLinkages()) {
            if (l.states.currentCooldown > 0) {
                l.states.currentCooldown--;
            }
        }
    }
}
