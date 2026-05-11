package net.zidou.photon_or_epicfight.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.zidou.photon_or_epicfight.editor_config.PhotonEditorConfig;

public class PreviewCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("photon_EpicFight")
                .requires(src -> src.getEntity() != null)
                .executes(ctx -> {
                    PhotonEditorConfig.epicfightPreview = !PhotonEditorConfig.epicfightPreview;
                    var text = PhotonEditorConfig.epicfightPreview ?
                            Component.translatable("message.photon_or_epicfight.preview.on") :
                            Component.translatable("message.photon_or_epicfight.preview.off");
                    ctx.getSource().sendSuccess(() -> text, true);

                    if (!PhotonEditorConfig.epicfightPreview && FMLEnvironment.dist.isClient()) {
                        photon$closePanel();
                    }

                    return Command.SINGLE_SUCCESS;
                });
    }

    private static void photon$closePanel() {
        try {
            var editorClass = Class.forName("com.lowdragmc.lowdraglib.gui.editor.ui.Editor");
            var instance = editorClass.getField("INSTANCE").get(null);
            if (instance == null) return;
            var menuPanel = editorClass.getMethod("getMenuPanel").invoke(instance);
            var viewMenu = menuPanel.getClass().getMethod("getTab", String.class).invoke(menuPanel, "view");
            if (viewMenu != null) {
                viewMenu.getClass().getMethod("removeView", String.class).invoke(viewMenu, "animation_control");
            }
        } catch (Exception ignored) {}
    }
}
