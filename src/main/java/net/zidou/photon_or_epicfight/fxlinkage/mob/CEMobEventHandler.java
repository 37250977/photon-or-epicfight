package net.zidou.photon_or_epicfight.fxlinkage.mob;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.handler.EpicFightEventHandler;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CombatEvolution 驱动的生物联动事件
 * 只有 CE 加载时才注册此监听器
 *
 * CE 为生物提供了格挡/招架/闪避能力，以及生物对目标状态（眩晕/击倒）的精确追踪。
 * 此处理器利用 CE 的 CEPatchUtils API（通过反射）来触发完整的生物联动事件。
 *
 * 无 CE 时退化为 MobFxEventHandler 提供的基础事件（on_hit / on_kill）。
 *
 * on_dodge 通过 MixinLivingEntityDodge 在 LivingEntity.hurt() RETURN 处检测：
 * 当 hurt() 返回 false（伤害未生效）且目标为非玩家、拥有 CE 能力、且不处于格挡/招架状态时触发。
 */
public class CEMobEventHandler {

    private static final String CE_MOD_ID = "combat_evolution";
    private static boolean ceLoaded = false;

    private static Object methodIsGuard = null;
    private static Object methodIsInCounter = null;

    private static final Map<UUID, Boolean> firstHitTracker = new HashMap<>();

    public static boolean isCeLoaded() {
        return ceLoaded;
    }

    public static void init() {
        ceLoaded = ModList.get().isLoaded(CE_MOD_ID);
        if (!ceLoaded) return;

        try {
            // CEPatchUtils 中这两个方法签名已验证存在
            Class<?> patchUtils = Class.forName("net.shelmarow.combat_evolution.ai.util.CEPatchUtils");
            methodIsGuard = patchUtils.getMethod("isGuard", LivingEntityPatch.class);
            methodIsInCounter = patchUtils.getMethod("isInCounter", LivingEntityPatch.class);
        } catch (Exception ignored) {}
    }

    // ======================== 反射工具方法 ========================

    private static boolean isMobileGuarding(LivingEntity entity) {
        if (!ceLoaded || methodIsGuard == null) return false;
        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch == null) return false;
            return (boolean) ((java.lang.reflect.Method) methodIsGuard).invoke(null, patch);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isMobileInCounter(LivingEntity entity) {
        if (!ceLoaded || methodIsInCounter == null) return false;
        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch == null) return false;
            return (boolean) ((java.lang.reflect.Method) methodIsInCounter).invoke(null, patch);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 复制 CE 的 isBlockableSource 逻辑
     * CEHumanoidPatch.isBlockableSource:
     *   !BYPASSES_INVULNERABILITY && !UNBLOCKABLE && !GUARD_PUNCTURE
     */
    private static boolean isBlockableSource(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(EpicFightDamageTypeTags.UNBLOCKALBE)
                && !source.is(EpicFightDamageTypeTags.GUARD_PUNCTURE);
    }

    /**
     * 检测实体是否处于眩晕状态（通过 EpicFight 的 EntityState.hurt()）
     */
    private static boolean isStunned(LivingEntity entity) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
        if (patch == null) return false;
        return patch.getEntityState().hurt();
    }

    // ======================== 生物作为攻击者（offensive） ========================

    @SubscribeEvent
    public static void onMobAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return;

        LivingEntity target = event.getEntity();
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
        ctx.hitType = event.getAmount() > target.getMaxHealth() * 0.3f ? "critical" : "normal";

        // on_hit: 生物命中目标
        FxMobLinkageEngine.fireEvent("on_hit", ctx, target.position());

        // on_first_hit: 生物单次战斗首次命中
        UUID mobId = attacker.getUUID();
        Boolean alreadyHit = firstHitTracker.get(mobId);
        if (alreadyHit == null || !alreadyHit) {
            firstHitTracker.put(mobId, true);
            FxMobLinkageEngine.fireEvent("on_first_hit", ctx, target.position());
        }

        // on_blocked: 生物攻击被目标格挡
        // 检测玩家格挡（原版 EpicFight GuardSkill）和 CE 生物格挡
        boolean targetIsGuarding = false;
        if (target instanceof Player targetPlayer) {
            var targetPatch = EpicFightCapabilities.getEntityPatch(targetPlayer,
                    yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.class);
            if (targetPatch != null) {
                var guard = targetPatch.getSkill(yesman.epicfight.skill.SkillSlots.GUARD);
                targetIsGuarding = guard != null && guard.isActivated();
            }
        } else {
            targetIsGuarding = isMobileGuarding(target);
        }
        if (targetIsGuarding) {
            FxMobLinkageEngine.fireEvent("on_blocked", ctx, target.position());
        }
    }

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return;

        LivingEntity target = event.getEntity();
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
        FxMobLinkageEngine.fireEvent("on_kill", ctx, target.position());
    }

    // ======================== 生物作为防御者（defensive — 需 CE） ========================

    @SubscribeEvent
    public static void onMobDefensive(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof Player) return;
        if (!isMobileGuarding(target) && !isMobileInCounter(target)) return;

        DamageSource source = event.getSource();
        // 复制 CE 的守卫条件：isBlockableSource && !isStunned
        boolean canGuard = isBlockableSource(source) && !isStunned(target);

        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(target, attacker, source);

        if (canGuard && isMobileGuarding(target)) {
            FxMobLinkageEngine.fireEvent("on_guard", ctx, target.position());
        }
        if (canGuard && isMobileInCounter(target)) {
            FxMobLinkageEngine.fireEvent("on_parry", ctx, target.position());
        }
    }

    // ======================== 眩晕/击倒 ========================

    @SubscribeEvent
    public static void onMobStun(EntityStunEvent event) {
        LivingEntity target = event.getStunnedEntityPatch().getOriginal();

        // EntityStunEvent 不提供攻击者，从目标的最后一次伤害源推断
        LivingEntity attacker = null;
        var lastDamage = target.getLastDamageSource();
        if (lastDamage != null && lastDamage.getEntity() instanceof LivingEntity living) {
            attacker = living;
        }

        MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, null);

        if (event.getStunType() == StunType.KNOCKDOWN) {
            FxMobLinkageEngine.fireEvent("on_knockdown", ctx, target.position());
        } else if (event.getStunType() != StunType.NONE) {
            FxMobLinkageEngine.fireEvent("on_stun", ctx, target.position());
        }
    }
}
