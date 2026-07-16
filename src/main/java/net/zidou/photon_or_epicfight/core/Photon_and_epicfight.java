package net.zidou.photon_or_epicfight.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.zidou.photon_or_epicfight.command.BoneCommand;
import net.zidou.photon_or_epicfight.command.PreviewCommand;
import net.zidou.photon_or_epicfight.fxlinkage.FxLinkageMod;
import net.zidou.photon_or_epicfight.fxlinkage.handler.CEExecutionCompat;
import net.zidou.photon_or_epicfight.fxlinkage.handler.EpicFightEventHandler;
import net.zidou.photon_or_epicfight.fxlinkage.mob.CEMobEventHandler;
import net.zidou.photon_or_epicfight.fxlinkage.mob.MobFxEventHandler;

@Mod(Photon_and_epicfight.MODID)
public class Photon_and_epicfight {

    public static final String MODID = "photon_and_epicfight";

    public Photon_and_epicfight() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        FxLinkageMod.init();

        // 注册数据包重载监听器
        MinecraftForge.EVENT_BUS.register(FxLinkageMod.class);

        // 注册玩家联动事件（EpicFight 攻击/冷却等）
        MinecraftForge.EVENT_BUS.register(EpicFightEventHandler.class);

        // 注册 CE 联动（处决事件）
        if (CEExecutionCompat.isCeLoaded()) {
            MinecraftForge.EVENT_BUS.register(CEExecutionCompat.class);
        }

        // 注册生物联动
        if (CEMobEventHandler.isCeLoaded()) {
            MinecraftForge.EVENT_BUS.register(CEMobEventHandler.class);
        } else {
            MinecraftForge.EVENT_BUS.register(MobFxEventHandler.class);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(BoneCommand.create());
        event.getDispatcher().register(PreviewCommand.create());
    }
}
