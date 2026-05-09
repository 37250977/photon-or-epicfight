package net.zidou.photon_or_epicfight.command;

import com.lowdragmc.photon.client.fx.FXHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.zidou.photon_or_epicfight.animation.ArmatureResolver;
import net.zidou.photon_or_epicfight.animation.BoneEffect;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BoneCommand {

    private static final Queue<Runnable> CLIENT_ACTIONS = new ConcurrentLinkedQueue<>();

    private static final String[] BONE_FALLBACK = {
            "Tool_R", "Tool_L", "Hand_R", "Hand_L",
            "Head", "Chest", "Root"
    };

    private static final SuggestionProvider<CommandSourceStack> FX_NS_SUGGEST = (ctx, builder) -> {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return builder.buildFuture();
        var namespaces = mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .map(ResourceLocation::getNamespace)
                .distinct()
                .toList();
        return SharedSuggestionProvider.suggest(namespaces, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BONE_SUGGEST = (ctx, builder) -> {
        try {
            var source = ctx.getSource();
            if (source.getEntity() != null) {
                var patch = EpicFightCapabilities.getEntityPatch(
                        source.getEntity(), LivingEntityPatch.class);
                if (patch != null) {
                    var armature = patch.getArmature();
                    if (armature != null) {
                        return SharedSuggestionProvider.suggest(
                                ArmatureResolver.allJointNames(armature), builder);
                    }
                }
            }
        } catch (Exception ignored) { }
        return SharedSuggestionProvider.suggest(BONE_FALLBACK, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> FX_PATH_SUGGEST = (ctx, builder) -> {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return builder.buildFuture();
        String ns;
        try { ns = StringArgumentType.getString(ctx, "ns"); }
        catch (Exception e) { return builder.buildFuture(); }
        var paths = mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .filter(rl -> rl.getNamespace().equals(ns))
                .map(rl -> rl.getPath().substring(3, rl.getPath().length() - 3))
                .toList();
        return SharedSuggestionProvider.suggest(paths, builder);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("bone")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(BoneCommand::listFx))
                .then(Commands.literal("play")
                        .then(Commands.argument("ns", StringArgumentType.word())
                                .suggests(FX_NS_SUGGEST)
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests(FX_PATH_SUGGEST)
                                        .executes(ctx -> playFx(ctx, "Tool_R"))
                                        .then(Commands.argument("boneName", StringArgumentType.word())
                                                .suggests(BONE_SUGGEST)
                                                .executes(ctx -> playFx(ctx, StringArgumentType.getString(ctx, "boneName")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("stop")
                        .executes(BoneCommand::stopAll)
                        .then(Commands.argument("boneName", StringArgumentType.word())
                                .suggests(BONE_SUGGEST)
                                .executes(ctx -> stopBoneFx(ctx, StringArgumentType.getString(ctx, "boneName")))
                                .then(Commands.argument("ns", StringArgumentType.word())
                                        .suggests(FX_NS_SUGGEST)
                                        .then(Commands.argument("path", StringArgumentType.word())
                                                .suggests(FX_PATH_SUGGEST)
                                                .executes(ctx -> stopSpecificFx(ctx,
                                                        StringArgumentType.getString(ctx, "boneName"),
                                                        StringArgumentType.getString(ctx, "ns"),
                                                        StringArgumentType.getString(ctx, "path")))
                                        )
                                )
                        )
                );
    }

    private static int listFx(CommandContext<CommandSourceStack> ctx) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        var fxs = mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .map(rl -> rl.getNamespace() + ":" + rl.getPath().substring(3, rl.getPath().length() - 3))
                .toList();
        ctx.getSource().sendSuccess(() -> Component.literal("§6[FX] 可用特效:"), true);
        for (var fx : fxs) {
            ctx.getSource().sendSuccess(() -> Component.literal("  §e" + fx), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int playFx(CommandContext<CommandSourceStack> ctx, String boneName) {
        if (!(ctx.getSource().getEntity() instanceof Player)) return 0;

        String ns = StringArgumentType.getString(ctx, "ns");
        String path = StringArgumentType.getString(ctx, "path");
        var rl = ResourceLocation.fromNamespaceAndPath(ns, path);

        CLIENT_ACTIONS.add(() -> {
            var mcPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (mcPlayer == null) return;
            var fx = FXHelper.getFX(rl);
            if (fx == null) {
                mcPlayer.sendSystemMessage(Component.literal("§c找不到 FX: " + rl));
                return;
            }
            var result = BoneEffect.play(mcPlayer, fx, boneName);
            if (result != null) {
                mcPlayer.sendSystemMessage(Component.literal("§a播放 " + rl + " → [" + boneName + "]"));
            } else {
                mcPlayer.sendSystemMessage(Component.literal("§e播放失败 (patch/joint/armature 为空)"));
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int stopAll(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() == null) return 0;
        CLIENT_ACTIONS.add(() -> {
            var mcPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (mcPlayer == null) return;
            BoneEffect.stopAll(mcPlayer);
            mcPlayer.sendSystemMessage(Component.literal("§c已停止所有骨骼特效"));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int stopBoneFx(CommandContext<CommandSourceStack> ctx, String boneName) {
        if (ctx.getSource().getEntity() == null) return 0;
        CLIENT_ACTIONS.add(() -> {
            var mcPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (mcPlayer == null) return;
            BoneEffect.stop(mcPlayer, boneName);
            mcPlayer.sendSystemMessage(Component.literal("§c已停止 [" + boneName + "] 上的特效"));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int stopSpecificFx(CommandContext<CommandSourceStack> ctx, String boneName, String ns, String path) {
        if (ctx.getSource().getEntity() == null) return 0;
        var rl = ResourceLocation.fromNamespaceAndPath(ns, path);
        CLIENT_ACTIONS.add(() -> {
            var mcPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (mcPlayer == null) return;
            BoneEffect.stop(mcPlayer, boneName, rl);
            mcPlayer.sendSystemMessage(Component.literal("§c已停止 [" + boneName + "] 上的 " + rl));
        });
        return Command.SINGLE_SUCCESS;
    }

    @Mod.EventBusSubscriber(modid = "photon_or_epicfight", value = Dist.CLIENT)
    public static class ClientTick {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Runnable task;
                while ((task = CLIENT_ACTIONS.poll()) != null) {
                    task.run();
                }
            }
        }
    }
}
