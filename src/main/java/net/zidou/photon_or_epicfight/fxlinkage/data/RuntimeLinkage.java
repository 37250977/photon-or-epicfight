package net.zidou.photon_or_epicfight.fxlinkage.data;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class RuntimeLinkage {
    public final FxLinkageData raw;
    public final ResourceLocation id;
    public final int priority;
    public final List<ResourceLocation> weaponCategories;
    public final List<ResourceLocation> weapons;
    public final List<ResourceLocation> skills;
    public final String hand;
    public final Map<String, List<FxEffect>> eventEffects;
    public final RuntimeStates states;

    public static class RuntimeStates {
        public int phase = 0;
        public int maxPhase = 3;
        public int cooldown = 0;
        public int currentCooldown = 0;
        public boolean phaselock = false;
        public boolean global = false;

        public RuntimeStates() {}
    }

    public RuntimeLinkage(FxLinkageData raw, ResourceLocation id) {
        this.raw = raw;
        this.id = id;
        this.priority = raw.priority;
        this.weaponCategories = parseRLList(raw.weapon_categories);
        this.weapons = parseRLList(raw.weapons);
        this.skills = parseRLList(raw.skills);
        this.hand = raw.hand;

        this.eventEffects = new HashMap<>();
        if (raw.on_skill_start != null) {
            eventEffects.computeIfAbsent("on_skill_start", k -> new ArrayList<>()).addAll(raw.on_skill_start);
        }
        if (raw.on_skill_end != null) {
            eventEffects.computeIfAbsent("on_skill_end", k -> new ArrayList<>()).addAll(raw.on_skill_end);
        }
        if (raw.on_hit != null) {
            eventEffects.computeIfAbsent("on_hit", k -> new ArrayList<>()).addAll(raw.on_hit);
        }
        if (raw.on_guard != null) {
            eventEffects.computeIfAbsent("on_guard", k -> new ArrayList<>()).addAll(raw.on_guard);
        }
        if (raw.on_combo != null) {
            eventEffects.computeIfAbsent("on_combo", k -> new ArrayList<>()).addAll(raw.on_combo);
        }
        if (raw.on_charged != null) {
            eventEffects.computeIfAbsent("on_charged", k -> new ArrayList<>()).addAll(raw.on_charged);
        }
        if (raw.on_dodge != null) {
            eventEffects.computeIfAbsent("on_dodge", k -> new ArrayList<>()).addAll(raw.on_dodge);
        }
        if (raw.on_parry != null) {
            eventEffects.computeIfAbsent("on_parry", k -> new ArrayList<>()).addAll(raw.on_parry);
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
        if (raw.on_phase_change != null) {
            eventEffects.computeIfAbsent("on_phase_change", k -> new ArrayList<>()).addAll(raw.on_phase_change);
        }
        if (raw.on_airborne != null) {
            eventEffects.computeIfAbsent("on_airborne", k -> new ArrayList<>()).addAll(raw.on_airborne);
        }
        if (raw.on_stun != null) {
            eventEffects.computeIfAbsent("on_stun", k -> new ArrayList<>()).addAll(raw.on_stun);
        }
        if (raw.on_knockdown != null) {
            eventEffects.computeIfAbsent("on_knockdown", k -> new ArrayList<>()).addAll(raw.on_knockdown);
        }
        if (raw.on_execution != null) {
            eventEffects.computeIfAbsent("on_execution", k -> new ArrayList<>()).addAll(raw.on_execution);
        }
        if (raw.events != null) {
            for (FxEffect effect : raw.events) {
                if (effect.trigger != null) {
                    eventEffects.computeIfAbsent(effect.trigger, k -> new ArrayList<>()).add(effect);
                }
            }
        }

        if (raw.states != null) {
            this.states = new RuntimeStates();
            this.states.maxPhase = raw.states.max_phase;
            this.states.phase = raw.states.phase;
            this.states.phaselock = raw.states.phaselock;
            this.states.global = raw.states.global;
            this.states.cooldown = raw.states.cooldown;
        } else {
            this.states = new RuntimeStates();
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
