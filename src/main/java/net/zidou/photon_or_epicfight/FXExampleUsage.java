package net.zidou.photon_or_epicfight;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Photon_or_epicfight.MODID, value = Dist.CLIENT)
public class FXExampleUsage {

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide) return;

        FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath(
                Photon_or_epicfight.MODID, "hit_effect"));

        if (fx != null) {
            BoneEffect.play(entity, fx, "Tool_R");
        }
    }

    public static void spawnAura(LivingEntity entity) {
        FX fx = FXHelper.getFX(ResourceLocation.fromNamespaceAndPath(
                Photon_or_epicfight.MODID, "aura_fire"));
        if (fx == null) return;

        BoneEffect.play(entity, fx, "Tool_R");
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().isAlive() && event.getEntity().level().isClientSide) {
            BoneEffect.stopAll(event.getEntity());
        }
    }
}
