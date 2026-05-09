package net.zidou.photon_or_epicfight.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.zidou.photon_or_epicfight.animation.ArmatureResolver;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BoneCommand {

    private static final Queue<Runnable> CLIENT_ACTIONS = new ConcurrentLinkedQueue<>();

    private static final String[] BONE_FALLBACK = {
            "Tool_R", "Tool_L", "Hand_R", "Hand_L",
            "Head", "Chest", "Root"
    };

    private static final SuggestionProvider<CommandSourceStack> FX_NS_SUGGEST = (ctx, builder) -> {
        try {
            var helper = Class.forName("net.zidou.photon_or_epicfight.command.BoneCommandClientHelper");
            var method = helper.getMethod("suggestNamespaces");
            @SuppressWarnings("unchecked")
            var namespaces = (List<String>) method.invoke(null);
            return SharedSuggestionProvider.suggest(namespaces, builder);
        } catch (Exception ignored) {}
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> BONE_SUGGEST = (ctx, builder) -> {
        try {
            Entity entity = null;
            try {
                entity = EntityArgument.getEntity(ctx, "target");
            } catch (Exception ignored) {}
            if (entity == null) {
                entity = ctx.getSource().getEntity();
            }
            if (entity != null) {
                var patch = EpicFightCapabilities.getEntityPatch(
                        entity, LivingEntityPatch.class);
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
        try {
            String ns;
            try { ns = StringArgumentType.getString(ctx, "ns"); }
            catch (Exception e) { return builder.buildFuture(); }
            var helper = Class.forName("net.zidou.photon_or_epicfight.command.BoneCommandClientHelper");
            var method = helper.getMethod("suggestPaths", String.class);
            @SuppressWarnings("unchecked")
            var paths = (List<String>) method.invoke(null, ns);
            return SharedSuggestionProvider.suggest(paths, builder);
        } catch (Exception ignored) {}
        return builder.buildFuture();
    };

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("bone")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(BoneCommand::listFx))
                .then(playBranch())
                .then(stopBranch());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> playBranch() {
        var nsArg = Commands.argument("ns", StringArgumentType.word()).suggests(FX_NS_SUGGEST);
        var pathArg = Commands.argument("path", StringArgumentType.word()).suggests(FX_PATH_SUGGEST);
        var selfTarget = pathArg
                .executes(ctx -> playFxOn(ctx, ctx.getSource().getEntityOrException().getId()))
                .then(Commands.argument("boneName", StringArgumentType.word())
                        .suggests(BONE_SUGGEST)
                        .executes(ctx -> playFxOn(ctx, ctx.getSource().getEntityOrException().getId())));

        return Commands.literal("play")
                .then(nsArg.then(selfTarget))
                .then(Commands.literal("on")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(nsArg.then(pathArg
                                        .executes(ctx -> playFxOn(ctx, EntityArgument.getEntity(ctx, "target").getId()))
                                        .then(Commands.argument("boneName", StringArgumentType.word())
                                                .suggests(BONE_SUGGEST)
                                                .executes(ctx -> playFxOn(ctx, EntityArgument.getEntity(ctx, "target").getId())))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stopBranch() {
        return Commands.literal("stop")
                .executes(ctx -> stopAllOn(ctx, ctx.getSource().getEntityOrException().getId()))
                .then(Commands.argument("boneName", StringArgumentType.word())
                        .suggests(BONE_SUGGEST)
                        .executes(ctx -> stopBoneOn(ctx, ctx.getSource().getEntityOrException().getId()))
                        .then(Commands.argument("ns", StringArgumentType.word())
                                .suggests(FX_NS_SUGGEST)
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests(FX_PATH_SUGGEST)
                                        .executes(ctx -> stopSpecificOn(ctx, ctx.getSource().getEntityOrException().getId())))))
                .then(Commands.literal("on")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> stopAllOn(ctx, EntityArgument.getEntity(ctx, "target").getId()))
                                .then(Commands.argument("boneName", StringArgumentType.word())
                                        .suggests(BONE_SUGGEST)
                                        .executes(ctx -> stopBoneOn(ctx, EntityArgument.getEntity(ctx, "target").getId()))
                                        .then(Commands.argument("ns", StringArgumentType.word())
                                                .suggests(FX_NS_SUGGEST)
                                                .then(Commands.argument("path", StringArgumentType.word())
                                                        .suggests(FX_PATH_SUGGEST)
                                                        .executes(ctx -> stopSpecificOn(ctx, EntityArgument.getEntity(ctx, "target").getId())))))));
    }

    private static int listFx(CommandContext<CommandSourceStack> ctx) {
        try {
            var helper = Class.forName("net.zidou.photon_or_epicfight.command.BoneCommandClientHelper");
            var method = helper.getMethod("listFX", java.util.function.Consumer.class);
            java.util.function.Consumer<Component> sender = msg -> ctx.getSource().sendSuccess(() -> msg, false);
            method.invoke(null, sender);
        } catch (Exception ignored) {}
        return Command.SINGLE_SUCCESS;
    }

    private static int playFxOn(CommandContext<CommandSourceStack> ctx, int targetId) {
        String ns = StringArgumentType.getString(ctx, "ns");
        String path = StringArgumentType.getString(ctx, "path");
        var rl = ResourceLocation.fromNamespaceAndPath(ns, path);
        String boneName = getBoneName(ctx);

        CLIENT_ACTIONS.add(() -> runClientHelper("playFXOn",
                new Class<?>[]{ResourceLocation.class, String.class, int.class},
                rl, boneName, targetId));

        ctx.getSource().sendSuccess(() -> Component.literal("§a播放 " + rl + " → [" + boneName + "]"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopAllOn(CommandContext<CommandSourceStack> ctx, int targetId) {
        CLIENT_ACTIONS.add(() -> runClientHelper("stopAllOn", new Class<?>[]{int.class}, targetId));
        ctx.getSource().sendSuccess(() -> Component.literal("§c已停止所有骨骼特效"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopBoneOn(CommandContext<CommandSourceStack> ctx, int targetId) {
        String boneName = StringArgumentType.getString(ctx, "boneName");
        CLIENT_ACTIONS.add(() -> runClientHelper("stopBoneOn",
                new Class<?>[]{String.class, int.class}, boneName, targetId));
        ctx.getSource().sendSuccess(() -> Component.literal("§c已停止 [" + boneName + "] 上的特效"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopSpecificOn(CommandContext<CommandSourceStack> ctx, int targetId) {
        String boneName = StringArgumentType.getString(ctx, "boneName");
        String ns = StringArgumentType.getString(ctx, "ns");
        String path = StringArgumentType.getString(ctx, "path");
        var rl = ResourceLocation.fromNamespaceAndPath(ns, path);
        CLIENT_ACTIONS.add(() -> runClientHelper("stopSpecificOn",
                new Class<?>[]{String.class, ResourceLocation.class, int.class},
                boneName, rl, targetId));
        ctx.getSource().sendSuccess(() -> Component.literal("§c已停止 [" + boneName + "] 上的 " + rl), true);
        return Command.SINGLE_SUCCESS;
    }

    private static String getBoneName(CommandContext<CommandSourceStack> ctx) {
        try { return StringArgumentType.getString(ctx, "boneName"); }
        catch (Exception e) { return "Tool_R"; }
    }

    private static void runClientHelper(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            var helper = Class.forName("net.zidou.photon_or_epicfight.command.BoneCommandClientHelper");
            var method = helper.getMethod(methodName, paramTypes);
            method.invoke(null, args);
        } catch (Exception ignored) {}
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
