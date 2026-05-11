package net.zidou.photon_or_epicfight.command;

import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.zidou.photon_or_epicfight.animation.BoneEffect;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class BoneCommandClientHelper {

    public static List<String> suggestNamespaces() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return List.of();
        return mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .map(ResourceLocation::getNamespace)
                .distinct()
                .toList();
    }

    public static List<String> suggestPaths(String ns) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return List.of();
        return mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .filter(rl -> rl.getNamespace().equals(ns))
                .map(rl -> rl.getPath().substring(3, rl.getPath().length() - 3))
                .toList();
    }

    public static void listFX(Consumer<Component> sender) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        sender.accept(Component.translatable("message.photon_or_epicfight.bone.list_title"));
        mc.getResourceManager()
                .listResources("fx", arg -> arg.getPath().endsWith(".fx"))
                .keySet().stream()
                .map(rl -> rl.getNamespace() + ":" + rl.getPath().substring(3, rl.getPath().length() - 3))
                .forEach(fx -> sender.accept(Component.translatable("message.photon_or_epicfight.bone.list_entry", fx)));
    }

    public static void playFXOn(ResourceLocation rl, String boneName, int targetEntityId) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var target = mc.level.getEntity(targetEntityId);
        if (!(target instanceof LivingEntity livingTarget)) return;
        var fx = FXHelper.getFX(rl);
        if (fx == null) return;
        BoneEffect.play(livingTarget, fx, boneName);
    }

    public static void stopAllOn(int targetEntityId) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var target = mc.level.getEntity(targetEntityId);
        if (target == null) return;
        BoneEffect.stopAll(target);
    }

    public static void stopBoneOn(String boneName, int targetEntityId) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var target = mc.level.getEntity(targetEntityId);
        if (target == null) return;
        BoneEffect.stop(target, boneName);
    }

    public static void stopSpecificOn(String boneName, ResourceLocation rl, int targetEntityId) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        var target = mc.level.getEntity(targetEntityId);
        if (target == null) return;
        BoneEffect.stop(target, boneName, rl);
    }
}
