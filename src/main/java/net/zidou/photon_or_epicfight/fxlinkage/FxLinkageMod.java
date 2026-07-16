package net.zidou.photon_or_epicfight.fxlinkage;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.zidou.photon_or_epicfight.fxlinkage.command.FxLinkageNetworking;
import net.zidou.photon_or_epicfight.fxlinkage.handler.CEExecutionCompat;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxLinkageLoader;
import net.zidou.photon_or_epicfight.fxlinkage.loader.FxProfileLoader;
import net.zidou.photon_or_epicfight.fxlinkage.mob.CEMobEventHandler;
import net.zidou.photon_or_epicfight.fxlinkage.mob.FxMobLinkageLoader;

public class FxLinkageMod {

    private static FxLinkageLoader linkageLoader;
    private static FxProfileLoader profileLoader;
    private static FxMobLinkageLoader mobLinkageLoader;

    public static void init() {
        FxLinkageNetworking.init();

        linkageLoader = new FxLinkageLoader();
        profileLoader = new FxProfileLoader();
        mobLinkageLoader = new FxMobLinkageLoader();

        CEExecutionCompat.init();
        CEMobEventHandler.init();
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(linkageLoader);
        event.addListener(profileLoader);
        event.addListener(mobLinkageLoader);
    }
}
