package net.zidou.photon_or_epicfight.fxlinkage.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.engine.FxLinkageEngine;
import net.zidou.photon_or_epicfight.fxlinkage.mob.FxMobLinkageEngine;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

public class EpicFightEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            MatchContext ctx = buildMatchContext(attacker, event.getEntity(), event.getSource());
            ctx.hitType = event.getAmount() > event.getEntity().getMaxHealth() * 0.3f ? "critical" : "normal";
            FxLinkageEngine.fireEvent("on_hit", ctx, event.getEntity().position());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FxLinkageEngine.tickCooldowns();
            FxMobLinkageEngine.tickCooldowns();
        }
    }

    public static MatchContext buildMatchContext(LivingEntity attacker, LivingEntity target, DamageSource source) {
        MatchContext ctx = new MatchContext();
        ctx.attacker = attacker;
        ctx.target = target;

        if (attacker != null) {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(attacker, LivingEntityPatch.class);
            ctx.attackerPatch = patch;

            // 检测手部 — 主手优先，如果主手有物品则判定为主手，否则判断副手是否有 EpicFight 武器
            ItemStack mainHand = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand = attacker.getItemInHand(InteractionHand.OFF_HAND);
            if (!mainHand.isEmpty()) {
                ctx.weaponId = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
                ctx.hand = "mainhand";
            } else if (!offHand.isEmpty()) {
                ctx.weaponId = BuiltInRegistries.ITEM.getKey(offHand.getItem());
                ctx.hand = "offhand";
            }

            if (patch != null) {
                CapabilityItem cap = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (cap != null && cap.getWeaponCategory() != null) {
                    ctx.weaponCategories = new ArrayList<>();
                    ctx.weaponCategories.add(resolveWeaponCategory(cap.getWeaponCategory()));
                } else {
                    cap = patch.getHoldingItemCapability(InteractionHand.OFF_HAND);
                    if (cap != null && cap.getWeaponCategory() != null) {
                        ctx.weaponCategories = new ArrayList<>();
                        ctx.weaponCategories.add(resolveWeaponCategory(cap.getWeaponCategory()));
                        if (ctx.hand == null) ctx.hand = "offhand";
                    }
                }

                CEExecutionCompat.fillStamina(ctx, patch);
            }
        }

        if (target != null) {
            ctx.targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);

            // 玩家: 通过 GuardSkill 是否激活判断格挡
            if (target instanceof Player) {
                PlayerPatch<?> playerPatch = EpicFightCapabilities.getEntityPatch(target, PlayerPatch.class);
                if (playerPatch != null) {
                    SkillContainer guardContainer = playerPatch.getSkill(SkillSlots.GUARD);
                    ctx.isGuarding = guardContainer != null && guardContainer.isActivated();
                    ctx.isAirborne = playerPatch.isAirborneState();
                }
            }

            // 非玩家: 从 EntityState 读取状态
            if (ctx.targetPatch != null) {
                EntityState state = ctx.targetPatch.getEntityState();
                ctx.isStunned = state.hurt();
                ctx.isKnockdown = state.knockDown();
                if (!(target instanceof Player)) {
                    ctx.isAirborne = ctx.targetPatch.isAirborneState();
                }
            }
        }

        if (source != null) {
            ctx.damageType = source.getMsgId();
        }

        if (attacker != null && target != null) {
            ctx.attackDistance = (float) attacker.distanceTo(target);
            // 计算攻击角度: 攻击者朝向与目标方向之间的水平夹角（度），0 = 正对目标，180 = 背对目标
            Vec3 lookVec = attacker.getLookAngle();
            Vec3 toTarget = target.position().subtract(attacker.position()).normalize();
            double dot = Math.max(-1.0, Math.min(1.0, lookVec.dot(toTarget)));
            ctx.attackAngle = (float) Math.toDegrees(Math.acos(dot));
        }

        return ctx;
    }

    /**
     * 将 WeaponCategory 解析为带命名空间的 ResourceLocation
     * <p>EpicFight 的 {@code WeaponCategory} 是 {@code ExtendableEnum}，允许其他 mod 注册自定义类别。
     * 通过反射读取 {@code ExtendableEnumManager.enums} (modid → class) 来找到该类别所属的 modid。</p>
     */
    private static ResourceLocation resolveWeaponCategory(WeaponCategory category) {
        String name = category.toString().toLowerCase();
        try {
            Field enumsField = WeaponCategory.ENUM_MANAGER.getClass().getDeclaredField("enums");
            enumsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Class<?>> enums = (Map<String, Class<?>>) enumsField.get(WeaponCategory.ENUM_MANAGER);
            Class<?> categoryClass = category.getClass();
            for (var entry : enums.entrySet()) {
                if (entry.getValue().isAssignableFrom(categoryClass)) {
                    return ResourceLocation.parse(entry.getKey() + ":" + name);
                }
            }
        } catch (Exception ignored) {}
        return ResourceLocation.parse("epicfight:" + name);
    }
}
