package net.zidou.photon_or_epicfight.fxlinkage.data;

import java.util.List;

public class FxLinkageData {
    public String type;
    public int priority = 10;
    public String side = "both";

    public List<String> weapon_categories;
    public List<String> weapons;
    public List<String> skills;
    public String hand;

    public List<FxCondition> conditions;
    public List<FxEffect> events;
    public List<FxEffect> on_skill_start;
    public List<FxEffect> on_skill_end;
    public List<FxEffect> on_hit;
    public List<FxEffect> on_guard;
    public List<FxEffect> on_combo;
    public List<FxEffect> on_charged;
    public List<FxEffect> on_dodge;
    public List<FxEffect> on_parry;
    public List<FxEffect> on_kill;
    public List<FxEffect> on_blocked;
    public List<FxEffect> on_first_hit;
    public List<FxEffect> on_phase_change;
    public List<FxEffect> on_airborne;
    public List<FxEffect> on_stun;
    public List<FxEffect> on_knockdown;
    public List<FxEffect> on_execution;

    public static class States {
        public boolean phaselock = false;
        public int phase = 0;
        public int max_phase = 3;
        public int cooldown = 0;
        public boolean global = false;
    }

    public States states;
}
