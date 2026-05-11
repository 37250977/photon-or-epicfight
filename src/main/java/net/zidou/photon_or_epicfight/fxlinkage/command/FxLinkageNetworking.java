package net.zidou.photon_or_epicfight.fxlinkage.command;

import com.lowdragmc.lowdraglib.networking.INetworking;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import net.minecraft.resources.ResourceLocation;
import net.zidou.photon_or_epicfight.core.Photon_and_epicfight;

public class FxLinkageNetworking {
    public static final INetworking NETWORK = LDLNetworking.createNetworking(
            ResourceLocation.fromNamespaceAndPath(Photon_and_epicfight.MODID, "fx_linkage"), "1.0");

    public static void init() {
        NETWORK.registerS2C(BoneEffectCommand.class);
    }
}
