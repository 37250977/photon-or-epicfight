package net.zidou.photon_or_epicfight.fxlinkage.command;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.zidou.photon_or_epicfight.animation.BoneEffect;
import com.lowdragmc.photon.client.fx.FXHelper;

public class BoneEffectCommand implements IPacket {
    private ResourceLocation fxId;
    private int entityId;
    private String boneName;
    private boolean allowMulti;

    public BoneEffectCommand() {}

    public BoneEffectCommand(ResourceLocation fxId, int entityId, String boneName, boolean allowMulti) {
        this.fxId = fxId;
        this.entityId = entityId;
        this.boneName = boneName;
        this.allowMulti = allowMulti;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(fxId);
        buf.writeVarInt(entityId);
        buf.writeUtf(boneName);
        buf.writeBoolean(allowMulti);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        fxId = buf.readResourceLocation();
        entityId = buf.readVarInt();
        boneName = buf.readUtf();
        allowMulti = buf.readBoolean();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void execute(IHandlerContext handler) {
        var level = handler.getLevel();
        if (level == null) return;
        var entity = level.getEntity(entityId);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;

        var fx = FXHelper.getFX(fxId);
        if (fx == null) return;

        if (!allowMulti) {
            BoneEffect.stop(living, boneName, fxId);
        }
        BoneEffect.play(living, fx, boneName, allowMulti);
    }
}
