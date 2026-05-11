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
    public FxEffect on_skill_start;
    public FxEffect on_skill_end;
    public List<FxEffect> on_hit;
    public FxEffect on_guard;
    public FxEffect on_combo;
    public FxEffect on_charged;
    public FxEffect on_dodge;
    public FxEffect on_parry;
    public FxEffect on_kill;
    public FxEffect on_blocked;
    public FxEffect on_first_hit;
    public FxEffect on_phase_change;
    public FxEffect on_airborne;
    public FxEffect on_stun;
    public FxEffect on_knockdown;
    public FxEffect on_execution;

    public static class States {
        public boolean phaselock = false;
        public int phase = 0;
        public int max_phase = 3;
        public int cooldown = 0;
        public boolean global = false;
    }

    public States states;
}
