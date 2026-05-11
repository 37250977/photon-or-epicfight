package net.zidou.photon_or_epicfight.fxlinkage;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.zidou.photon_or_epicfight.fxlinkage.command.FxLinkageNetworking;
import net.zidou.photon_or_epicfight.fxlinkage.handler.CEExecutionCompat;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxLinkageLoader;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxProfileLoader;

@Mod.EventBusSubscriber(modid = "photon_and_epicfight")
public class FxLinkageMod {

    private static FxLinkageLoader linkageLoader;
    private static FxProfileLoader profileLoader;

    public static void init() {
        FxLinkageNetworking.init();

        linkageLoader = new FxLinkageLoader();
        profileLoader = new FxProfileLoader();

        CEExecutionCompat.init();
        if (CEExecutionCompat.isCeLoaded()) {
            MinecraftForge.EVENT_BUS.register(CEExecutionCompat.class);
        }

        MinecraftForge.EVENT_BUS.register(FxLinkageMod.class);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(linkageLoader);
        event.addListener(profileLoader);
    }
}
