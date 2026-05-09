package net.zidou.photon_or_epicfight.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.zidou.photon_or_epicfight.command.BoneCommand;
import net.zidou.photon_or_epicfight.command.PreviewCommand;

@Mod(Photon_and_epicfight.MODID)
public class Photon_and_epicfight {

    public static final String MODID = "photon_and_epicfight";

    public Photon_and_epicfight() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(BoneCommand.create());
        event.getDispatcher().register(PreviewCommand.create());
    }
}
