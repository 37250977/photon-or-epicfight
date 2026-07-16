package net.zidou.photon_or_epicfight.fxlinkage.mob;

import net.minecraft.resources.ResourceLocation;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxEffect;

import java.util.*;

public class RuntimeMobLinkage {
    public final FxMobLinkageData raw;
    public final ResourceLocation id;
    public final int priority;
    public final List<ResourceLocation> weaponCategories;
    public final List<ResourceLocation> weapons;
    public final Map<String, List<FxEffect>> eventEffects;
    public final RuntimeMobStates states;

    public static class RuntimeMobStates {
        public int phase = 0;
        public int maxPhase = 3;
        public int cooldown = 0;
        public int currentCooldown = 0;
        public boolean phaselock = false;
        public boolean global = false;

        public RuntimeMobStates() {}
    }

    public RuntimeMobLinkage(FxMobLinkageData raw, ResourceLocation id) {
        this.raw = raw;
        this.id = id;
        this.priority = raw.priority;
        this.weaponCategories = parseRLList(raw.weapon_categories);
        this.weapons = parseRLList(raw.weapons);

        this.eventEffects = new HashMap<>();
        if (raw.on_hit != null) {
            eventEffects.computeIfAbsent("on_hit", k -> new ArrayList<>()).addAll(raw.on_hit);
        }
        if (raw.on_kill != null) {
            eventEffects.computeIfAbsent("on_kill", k -> new ArrayList<>()).addAll(raw.on_kill);
        }
        if (raw.on_blocked != null) {
            eventEffects.computeIfAbsent("on_blocked", k -> new ArrayList<>()).addAll(raw.on_blocked);
        }
        if (raw.on_first_hit != null) {
            eventEffects.computeIfAbsent("on_first_hit", k -> new ArrayList<>()).addAll(raw.on_first_hit);
        }
        if (raw.on_stun != null) {
            eventEffects.computeIfAbsent("on_stun", k -> new ArrayList<>()).addAll(raw.on_stun);
        }
        if (raw.on_knockdown != null) {
            eventEffects.computeIfAbsent("on_knockdown", k -> new ArrayList<>()).addAll(raw.on_knockdown);
        }
        if (raw.on_guard != null) {
            eventEffects.computeIfAbsent("on_guard", k -> new ArrayList<>()).addAll(raw.on_guard);
        }
        if (raw.on_parry != null) {
            eventEffects.computeIfAbsent("on_parry", k -> new ArrayList<>()).addAll(raw.on_parry);
        }
        if (raw.on_dodge != null) {
            eventEffects.computeIfAbsent("on_dodge", k -> new ArrayList<>()).addAll(raw.on_dodge);
        }
        if (raw.events != null) {
            for (FxEffect effect : raw.events) {
                if (effect.trigger != null) {
                    eventEffects.computeIfAbsent(effect.trigger, k -> new ArrayList<>()).add(effect);
                }
            }
        }

        if (raw.states != null) {
            this.states = new RuntimeMobStates();
            this.states.maxPhase = raw.states.max_phase;
            this.states.phase = raw.states.phase;
            this.states.phaselock = raw.states.phaselock;
            this.states.global = raw.states.global;
            this.states.cooldown = raw.states.cooldown;
        } else {
            this.states = new RuntimeMobStates();
        }
    }

    private static List<ResourceLocation> parseRLList(List<String> strings) {
        if (strings == null) return Collections.emptyList();
        List<ResourceLocation> result = new ArrayList<>();
        for (String s : strings) {
            try {
                result.add(ResourceLocation.parse(s));
            } catch (Exception ignored) {}
        }
        return result;
    }
}
