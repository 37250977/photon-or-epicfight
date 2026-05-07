package net.zidou.photon_or_epicfight;

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
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * /bone 指令
 * <p>
 * 用法：
 *   /bone list                             列出所有可用 .fx
 *   /bone play <namespace> <path>           播放，绑定到 Tool_R
 *   /bone play <namespace> <path> <bone>    指定骨骼
 *   /bone stop                             停止所有
 *   /bone stop <bone>                      停止指定骨骼上的所有特效
 *   /bone stop <bone> <namespace> <path>   停止指定骨骼上的指定特效
 */
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
                                        .then(Commands.argument("bone", StringArgumentType.word())
                                                .suggests(BONE_SUGGEST)
                                                .executes(ctx -> playFx(ctx,
                                                        StringArgumentType.getString(ctx, "bone")))
                                        ))))
                .then(Commands.literal("stop")
                        .executes(BoneCommand::stopAll)
                        .then(Commands.argument("bone", StringArgumentType.word())
                                .suggests(BONE_SUGGEST)
                                .executes(ctx -> stopBone(ctx, null))
                                .then(Commands.argument("ns", StringArgumentType.word())
                                        .suggests(FX_NS_SUGGEST)
                                        .then(Commands.argument("path", StringArgumentType.word())
                                                .suggests(FX_PATH_SUGGEST)
                                                .executes(ctx -> stopBone(ctx,
                                                        StringArgumentType.getString(ctx, "ns") + ":"
                                                        + StringArgumentType.getString(ctx, "path")))
                                        ))));
    }

    private static int listFx(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        CLIENT_ACTIONS.add(() -> {
            var resources = Minecraft.getInstance().getResourceManager()
                    .listResources("fx", arg -> arg.getPath().endsWith(".fx"));
            if (resources.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7[Photon] §e没有找到任何 .fx 文件"), false);
                return;
            }
            source.sendSuccess(() -> Component.literal("§7[Photon] §f可用特效 (" + resources.size() + "):"), false);
            for (var entry : resources.entrySet()) {
                var rl = entry.getKey();
                String id = rl.getNamespace() + ":" + rl.getPath().substring(3, rl.getPath().length() - 3);
                source.sendSuccess(() -> Component.literal(" §7- §a" + id), false);
            }
        });
        source.sendSuccess(() -> Component.literal("§7[Photon] §e正在查询..."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int playFx(CommandContext<CommandSourceStack> ctx, String bone) {
        var source = ctx.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }
        String ns = StringArgumentType.getString(ctx, "ns");
        String path = StringArgumentType.getString(ctx, "path");
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ns, path);

        int playerId = player.getId();
        CLIENT_ACTIONS.add(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var entity = mc.level.getEntity(playerId);
            if (!(entity instanceof Player p)) return;
            var fx = FXHelper.getFX(location);
            if (fx == null) {
                source.sendFailure(Component.literal("§c未找到特效: " + location));
                return;
            }
            BoneEffect.play(p, fx, bone);
            source.sendSuccess(() -> Component.literal(
                    "§7[Photon] §a播放 §f" + location + " §7→ §f" + bone), false);
        });
        source.sendSuccess(() -> Component.literal(
                "§7[Photon] §e正在播放: §f" + location + " §7→ §f" + bone), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopAll(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }
        int playerId = player.getId();
        CLIENT_ACTIONS.add(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var entity = mc.level.getEntity(playerId);
            if (entity != null) BoneEffect.stopAll(entity);
        });
        source.sendSuccess(() -> Component.literal("§7[Photon] §a已停止所有骨骼特效"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int stopBone(CommandContext<CommandSourceStack> ctx, String fxId) {
        var source = ctx.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }
        String boneName = StringArgumentType.getString(ctx, "bone");
        int playerId = player.getId();

        CLIENT_ACTIONS.add(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var entity = mc.level.getEntity(playerId);
            if (entity == null) return;

            var effects = BoneEffect.CACHE.get(entity);
            if (effects == null || effects.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7[Photon] §e没有正在播放的特效"), false);
                return;
            }
            if (fxId == null) {
                boolean removed = effects.removeIf(e -> {
                    if (e.boneName.equals(boneName) && e.getRuntime() != null) {
                        e.getRuntime().destroy(true);
                        return true;
                    }
                    return false;
                });
                if (removed) source.sendSuccess(() -> Component.literal("§7[Photon] §a已停止 §f" + boneName + " §7上的特效"), false);
                else source.sendSuccess(() -> Component.literal("§7[Photon] §e骨骼 §f" + boneName + " §e上没有正在播放的特效"), false);
            } else {
                ResourceLocation location;
                try { location = ResourceLocation.parse(fxId); }
                catch (Exception e) { source.sendFailure(Component.literal("§c无效的特效 ID")); return; }
                boolean removed = effects.removeIf(e -> {
                    if (e.boneName.equals(boneName) && e.getRuntime() != null && location.equals(e.fx.getFxLocation())) {
                        e.getRuntime().destroy(true);
                        return true;
                    }
                    return false;
                });
                if (removed) source.sendSuccess(() -> Component.literal("§7[Photon] §a已停止 §f" + location + " §7在 §f" + boneName), false);
                else source.sendSuccess(() -> Component.literal("§7[Photon] §e未找到匹配的特效"), false);
            }
            if (effects.isEmpty()) BoneEffect.CACHE.remove(entity);
        });
        source.sendSuccess(() -> Component.literal("§7[Photon] §e正在停止..."), false);
        return Command.SINGLE_SUCCESS;
    }

    static void drain() {
        Runnable action;
        while ((action = CLIENT_ACTIONS.poll()) != null) action.run();
    }

    @Mod.EventBusSubscriber(modid = Photon_or_epicfight.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) BoneCommand.drain();
        }
    }
}
