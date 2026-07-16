package net.zidou.photon_or_epicfight.fxlinkage.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.handler.EpicFightEventHandler;

/**
 * 生物视角事件处理器 — 基础层
 *
 * 只在无 CombatEvolution 时提供最基础的 mob 联动：
 *   on_hit, on_kill
 *
 * 安装 CE 后此处理器将被 CEMobEventHandler 取代，
 * 后者提供完整的 on_hit / on_kill / on_blocked / on_guard / on_parry / on_stun / on_knockdown
 */
public class MobFxEventHandler {

    @SubscribeEvent
    public static void onMobAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return;

        LivingEntity target = event.getEntity();
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
        ctx.hitType = event.getAmount() > target.getMaxHealth() * 0.3f ? "critical" : "normal";

        FxMobLinkageEngine.fireEvent("on_hit", ctx, target.position());
    }

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return;

        LivingEntity target = event.getEntity();
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
        FxMobLinkageEngine.fireEvent("on_kill", ctx, target.position());
    }
}
