package net.zidou.photon_or_epicfight.fxlinkage.handler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.engine.FxLinkageEngine;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FxPlayerEventListener {
    public static final UUID FX_EVENT_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    private static final Map<Player, Boolean> firstHitTracker = new HashMap<>();
    private static final Map<Player, Boolean> airborneTracker = new HashMap<>();
    private static final Map<Player, Integer> chargedTracker = new HashMap<>();
    private static final Map<Player, Integer> lastPhaseTracker = new HashMap<>();

    public static void register(PlayerPatch<?> playerPatch) {
        var listener = playerPatch.getEventListener();
        var player = playerPatch.getOriginal();

        // EntityStunEvent is a Forge event, register once
        MinecraftForge.EVENT_BUS.register(StunEventHandler.class);

        // SKILL_CAST_EVENT → on_skill_start + on_charged
        listener.addEventListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, FX_EVENT_UUID, (SkillCastEvent event) -> {
            if (event.isExecutable() && event.getSkillContainer().hasSkill()) {
                ResourceLocation skillId = event.getSkillContainer().getSkill().getRegistryName();
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
                ctx.skill = skillId;
                FxLinkageEngine.fireEvent("on_skill_start", ctx, player.position());

                // 蓄力检测: 如果蓄力量 > 0，触发 on_charged
                int charged = playerPatch.getAccumulatedChargeAmount();
                if (charged > 0) {
                    Integer prev = chargedTracker.get(player);
                    if (prev == null || charged > prev) {
                        FxLinkageEngine.fireEvent("on_charged", ctx, player.position());
                    }
                    chargedTracker.put(player, charged);
                }
            }
        });

        // SKILL_CANCEL_EVENT → on_skill_end
        listener.addEventListener(PlayerEventListener.EventType.SKILL_CANCEL_EVENT, FX_EVENT_UUID, (SkillCancelEvent event) -> {
            if (event.getSkillContainer().hasSkill()) {
                ResourceLocation skillId = event.getSkillContainer().getSkill().getRegistryName();
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
                ctx.skill = skillId;
                FxLinkageEngine.fireEvent("on_skill_end", ctx, player.position());
            }
        });

        // DODGE_SUCCESS_EVENT → on_dodge
        listener.addEventListener(PlayerEventListener.EventType.DODGE_SUCCESS_EVENT, FX_EVENT_UUID, (DodgeSuccessEvent event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
            FxLinkageEngine.fireEvent("on_dodge", ctx, player.position());
        });

        // TAKE_DAMAGE_EVENT_ATTACK → 你被攻击时触发（防守视角）
        //   on_guard  : 你成功格挡了对方的攻击（BLOCKED）
        //   on_parry  : 你成功招架了对方的攻击（parried）
        listener.addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, FX_EVENT_UUID, (TakeDamageEvent.Attack event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);

            if (event.getResult() == AttackResult.ResultType.BLOCKED) {
                LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity living ? living : null;
                ctx = EpicFightEventHandler.buildMatchContext(player, attacker, event.getDamageSource());
                ctx.isGuarding = true;
                FxLinkageEngine.fireEvent("on_guard", ctx, player.position());
            }

            if (event.isParried()) {
                FxLinkageEngine.fireEvent("on_parry", ctx, player.position());
            }
        });

        // DEAL_DAMAGE_EVENT_ATTACK → 你攻击时触发（攻击视角）
        //   on_hit     : 命中目标
        //   on_blocked : 攻击被目标格挡（目标正在格挡中）
        listener.addEventListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, FX_EVENT_UUID, (DealDamageEvent.Attack event) -> {
            if (event.getDamageSource().isBasicAttack()) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, event.getTarget(), event.getDamageSource());
                FxLinkageEngine.fireEvent("on_hit", ctx, event.getTarget().position());

                Boolean alreadyHit = firstHitTracker.get(player);
                if (alreadyHit == null || !alreadyHit) {
                    firstHitTracker.put(player, true);
                    FxLinkageEngine.fireEvent("on_first_hit", ctx, event.getTarget().position());
                }
            }

            // 目标是否在格挡 → on_blocked（攻击视角）
            if (event.getTarget() instanceof Player targetPlayer) {
                var targetPatch = EpicFightCapabilities.getEntityPatch(targetPlayer, PlayerPatch.class);
                if (targetPatch != null) {
                    SkillContainer guard = targetPatch.getSkill(SkillSlots.GUARD);
                    if (guard != null && guard.isActivated()) {
                        MatchContext blockCtx = EpicFightEventHandler.buildMatchContext(player, event.getTarget(), event.getDamageSource());
                        FxLinkageEngine.fireEvent("on_blocked", blockCtx, event.getTarget().position());
                    }
                }
            }
        });

        // COMBO_COUNTER_HANDLE_EVENT → track combo for conditions + on_combo
        listener.addEventListener(PlayerEventListener.EventType.COMBO_COUNTER_HANDLE_EVENT, FX_EVENT_UUID, (ComboCounterHandleEvent event) -> {
            int next = event.getNextValue();

            if (next > event.getPrevValue()) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
                ctx.comboCount = next;
                FxLinkageEngine.fireEvent("on_combo", ctx, player.position());
            }
        });

        // BASIC_ATTACK_EVENT → on_hit (fallback)
        listener.addEventListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT, FX_EVENT_UUID, (BasicAttackEvent event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
            FxLinkageEngine.fireEvent("on_hit", ctx, player.position());
        });

        // PLAYER_KILLED_EVENT → on_kill
        listener.addEventListener(PlayerEventListener.EventType.PLAYER_KILLED_EVENT, FX_EVENT_UUID, (PlayerKilledEvent event) -> {
            if (event.getDamageSource().getEntity() instanceof LivingEntity target) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, target, event.getDamageSource());
                FxLinkageEngine.fireEvent("on_kill", ctx, target.position());
            }
        });

        // ANIMATION_BEGIN_EVENT → on_airborne (跳/飞行动画), on_phase_change
        listener.addEventListener(PlayerEventListener.EventType.ANIMATION_BEGIN_EVENT, FX_EVENT_UUID, (AnimationBeginEvent event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);

            // 检测滞空动画
            String animName = event.getAnimation().toString().toLowerCase();
            if (animName.contains("jump") || animName.contains("fly") || animName.contains("float") || animName.contains("fall")) {
                Boolean wasAirborne = airborneTracker.get(player);
                if (wasAirborne == null || !wasAirborne) {
                    airborneTracker.put(player, true);
                    ctx.isAirborne = true;
                    FxLinkageEngine.fireEvent("on_airborne", ctx, player.position());
                }
            } else if (animName.contains("land") || animName.contains("recovery")) {
                airborneTracker.put(player, false);
            }

            // 检测动画阶段变化
            int phaseLevel = playerPatch.getEntityState().getLevel();
            Integer lastPhase = lastPhaseTracker.get(player);
            if (lastPhase != null && lastPhase != phaseLevel) {
                ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
                ctx.phase = phaseLevel;
                FxLinkageEngine.fireEvent("on_phase_change", ctx, player.position());
            }
            lastPhaseTracker.put(player, phaseLevel);
        });

        // ATTACK_PHASE_END_EVENT → on_phase_change (备选)
        listener.addEventListener(PlayerEventListener.EventType.ATTACK_PHASE_END_EVENT, FX_EVENT_UUID, (AttackPhaseEndEvent event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
            ctx.phase = event.getPhaseOrder();
            FxLinkageEngine.fireEvent("on_phase_change", ctx, player.position());
        });

        // ATTACK_ANIMATION_END_EVENT → on_skill_end (攻击动画结束)
        listener.addEventListener(PlayerEventListener.EventType.ATTACK_ANIMATION_END_EVENT, FX_EVENT_UUID, (AttackEndEvent event) -> {
            MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);
            FxLinkageEngine.fireEvent("on_skill_end", ctx, player.position());
        });
    }

    // 独立的 Forge 事件监听器: EntityStunEvent → on_stun / on_knockdown
    public static class StunEventHandler {
        @SubscribeEvent
        public static void onEntityStun(EntityStunEvent event) {
            if (event.getStunnedEntityPatch().getOriginal() instanceof Player player) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(player, null, null);

                if (event.getStunType() == StunType.KNOCKDOWN) {
                    FxLinkageEngine.fireEvent("on_knockdown", ctx, player.position());
                } else if (event.getStunType() != StunType.NONE) {
                    FxLinkageEngine.fireEvent("on_stun", ctx, player.position());
                }
            }
        }
    }
}
