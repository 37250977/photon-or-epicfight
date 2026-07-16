package net.zidou.photon_or_epicfight.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;
import net.zidou.photon_or_epicfight.fxlinkage.handler.EpicFightEventHandler;
import net.zidou.photon_or_epicfight.fxlinkage.mob.FxMobLinkageEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = LivingEntity.class)
public class MixinLivingEntityDodge {

    @Inject(at = @At("RETURN"), method = "hurt")
    private void photon$detectDodge(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) return;
        if (!isCeDodge(self)) return;

        LivingEntity attacker = damageSource.getEntity() instanceof LivingEntity living ? living : null;
        MatchContext ctx = EpicFightEventHandler.buildMatchContext(self, attacker, damageSource);
        FxMobLinkageEngine.fireEvent("on_dodge", ctx, self.position());
    }

    private static boolean isCeDodge(LivingEntity entity) {
        try {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
            if (patch == null) return false;

            Class<?> ceDataClass = Class.forName("net.shelmarow.combat_evolution.ai.iml.ILivingEntityData");
            if (!ceDataClass.isInstance(patch)) return false;

            Object ceData = ceDataClass.cast(patch);
            boolean guarding = (boolean) ceDataClass.getMethod("combat_evolution$isGuard").invoke(ceData);
            boolean inCounter = (boolean) ceDataClass.getMethod("combat_evolution$isInCounter").invoke(ceData);
            return !guarding && !inCounter;
        } catch (Exception e) {
            return false;
        }
    }
}
