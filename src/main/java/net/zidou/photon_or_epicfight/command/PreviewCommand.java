package net.zidou.photon_or_epicfight.command;

import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.menu.ViewMenu;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.zidou.photon_or_epicfight.config.PhotonEditorConfig;

public class PreviewCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("photon_EpicFight_Editor")
                .requires(src -> src.getEntity() != null)
                .executes(ctx -> {
                    PhotonEditorConfig.epicfightPreview = !PhotonEditorConfig.epicfightPreview;
                    var text = PhotonEditorConfig.epicfightPreview ?
                            Component.translatable("message.photon_or_epicfight.preview.on") :
                            Component.translatable("message.photon_or_epicfight.preview.off");
                    ctx.getSource().sendSuccess(() -> text, true);

                    if (!PhotonEditorConfig.epicfightPreview && Editor.INSTANCE != null) {
                        ViewMenu viewMenu = Editor.INSTANCE.getMenuPanel().getTab("view");
                        if (viewMenu != null) {
                            viewMenu.removeView("animation_control");
                        }
                    }

                    return Command.SINGLE_SUCCESS;
                });
    }
}
