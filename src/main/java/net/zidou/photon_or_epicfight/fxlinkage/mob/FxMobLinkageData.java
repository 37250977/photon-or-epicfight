package net.zidou.photon_or_epicfight.fxlinkage.mob;

import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxEffect;

import java.util.List;

/**
 * 生物联动简化数据模型（与玩家联动独立）
 * 只保留对生物攻击者有意义的字段
 */
public class FxMobLinkageData {
    public String type;
    public int priority = 10;
    public String side = "both";

    public List<String> weapon_categories;
    public List<String> weapons;

    public List<FxCondition> conditions;
    public List<FxEffect> events;
    public List<FxEffect> on_hit;
    public List<FxEffect> on_kill;
    public List<FxEffect> on_blocked;
    public List<FxEffect> on_first_hit;
    public List<FxEffect> on_stun;
    public List<FxEffect> on_knockdown;
    public List<FxEffect> on_guard;
    public List<FxEffect> on_parry;
    public List<FxEffect> on_dodge;

    public static class States {
        public boolean phaselock = false;
        public int phase = 0;
        public int max_phase = 3;
        public int cooldown = 0;
        public boolean global = false;
    }

    public States states;
}
