package net.zidou.photon_or_epicfight.fxlinkage.handler;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.engine.FxLinkageEngine;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * CombatEvolution 可选兼容模块
 * 运行时检测 CE 是否加载，只有在 CE 存在时才注册处决事件和耐力数据
 * 通过反射调用 CE API，无编译期依赖
 */
public class CEExecutionCompat {

    private static final String CE_MOD_ID = "combat_evolution";
    private static boolean ceLoaded = false;
    private static TagKey<DamageType> executionTag = null;
    private static TagKey<DamageType> executionFinishedTag = null;
    private static Object staminaPercentMethod = null;

    public static boolean isCeLoaded() {
        return ceLoaded;
    }

    public static void init() {
        ceLoaded = ModList.get().isLoaded(CE_MOD_ID);
        if (ceLoaded) {
            executionTag = TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(CE_MOD_ID, "execution"));
            executionFinishedTag = TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(CE_MOD_ID, "execution_finished"));

            try {
                Class<?> patchUtils = Class.forName("net.shelmarow.combat_evolution.ai.util.CEPatchUtils");
                staminaPercentMethod = patchUtils.getMethod("getStaminaPercent", LivingEntityPatch.class);
            } catch (Exception ignored) {}
        }
    }

    public static void fillStamina(MatchContext ctx, LivingEntityPatch<?> patch) {
        if (!ceLoaded || staminaPercentMethod == null || patch == null) return;
        try {
            float pct = (float) ((java.lang.reflect.Method) staminaPercentMethod).invoke(null, patch);
            ctx.staminaPercent = pct;
        } catch (Exception ignored) {}
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ceLoaded || executionTag == null) return;

        if (event.getSource().is(executionTag)) {
            LivingEntity target = event.getEntity();
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
                FxLinkageEngine.fireEvent("on_execution", ctx, target.position());
            }
        }

        if (executionFinishedTag != null && event.getSource().is(executionFinishedTag)) {
            LivingEntity target = event.getEntity();
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                MatchContext ctx = EpicFightEventHandler.buildMatchContext(attacker, target, event.getSource());
                FxLinkageEngine.fireEvent("on_kill", ctx, target.position());
            }
        }
    }
}
