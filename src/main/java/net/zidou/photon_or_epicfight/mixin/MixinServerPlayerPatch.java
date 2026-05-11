package net.zidou.photon_or_epicfight.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.zidou.photon_or_epicfight.fxlinkage.handler.FxPlayerEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mixin(value = ServerPlayerPatch.class, remap = false)
public class MixinServerPlayerPatch {

    @Inject(method = "onJoinWorld", at = @At("TAIL"))
    private void photon$registerFxEventListeners(ServerPlayer player, EntityJoinLevelEvent event, CallbackInfo ci) {
        FxPlayerEventListener.register((ServerPlayerPatch) (Object) this);
    }
}
