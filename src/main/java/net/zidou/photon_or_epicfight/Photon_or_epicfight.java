package net.zidou.photon_or_epicfight;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Photon_or_epicfight.MODID)
public class Photon_or_epicfight {

    public static final String MODID = "photon_or_epicfight";

    public Photon_or_epicfight() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(BoneCommand.create());
    }
}
