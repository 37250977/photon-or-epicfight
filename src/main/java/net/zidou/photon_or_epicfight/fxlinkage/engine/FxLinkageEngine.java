package net.zidou.photon_or_epicfight.fxlinkage.engine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxEffect;
import net.zidou.photon_or_epicfight.fxlinkage.data.RuntimeLinkage;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxLinkageLoader;

import java.util.ArrayList;
import java.util.List;

public class FxLinkageEngine {

    public static void fireEvent(String event, MatchContext ctx, Vec3 hitPos) {
        if (ctx.attacker == null) return;

        List<RuntimeLinkage> candidates = FxLinkageLoader.getLinkagesForEvent(event);
        if (candidates.isEmpty()) return;

        for (RuntimeLinkage linkage : candidates) {
            if (!matchLinkage(linkage, ctx)) continue;
            if (linkage.states.currentCooldown > 0) continue;

            List<FxEffect> effects = linkage.eventEffects.get(event);
            if (effects == null || effects.isEmpty()) continue;

            boolean anyExecuted = false;
            for (FxEffect effect : effects) {
                if (effect.conditions == null || ConditionEngine.checkConditions(effect.conditions, ctx)) {
                    EffectExecutor.executeEffect(effect, linkage, ctx.attacker, ctx.target, hitPos);
                    anyExecuted = true;
                }
            }

            if (anyExecuted && linkage.states.cooldown > 0) {
                linkage.states.currentCooldown = linkage.states.cooldown;
            }
        }
    }

    public static boolean matchLinkage(RuntimeLinkage linkage, MatchContext ctx) {
        // 匹配武器分类
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

        // 匹配具体物品 ID
        if (!linkage.weapons.isEmpty()) {
            if (ctx.weaponId == null || !linkage.weapons.contains(ctx.weaponId)) return false;
        }

        // 匹配技能
        if (!linkage.skills.isEmpty()) {
            if (ctx.skill == null || !linkage.skills.contains(ctx.skill)) return false;
        }

        // 匹配手部
        if (linkage.hand != null && !linkage.hand.isEmpty()) {
            if (!linkage.hand.equals(ctx.hand)) return false;
        }

        // 匹配全局条件
        if (linkage.raw.conditions != null && !linkage.raw.conditions.isEmpty()) {
            if (!ConditionEngine.checkConditions(linkage.raw.conditions, ctx)) return false;
        }

        return true;
    }

    public static void tickCooldowns() {
        for (RuntimeLinkage l : FxLinkageLoader.getAllLinkages()) {
            if (l.states.currentCooldown > 0) {
                l.states.currentCooldown--;
            }
        }
    }
}
