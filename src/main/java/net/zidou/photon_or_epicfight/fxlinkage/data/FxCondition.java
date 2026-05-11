package net.zidou.photon_or_epicfight.fxlinkage.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class FxCondition {
    public String type;
    public String value;
    public Double min;
    public Double max;
    public String comparator;
    public List<String> values;
    public String side;
    public String target;
    public String attr;
    public List<FxCondition> all_of;
    public List<FxCondition> any_of;
    public List<FxCondition> none_of;
    public String command;

    public enum ConditionType {
        WEAPON_CATEGORY, WEAPON_ID, SKILL, DAMAGE_TYPE, HIT_TYPE,
        TARGET_TYPE, COMBO, PHASE, HEALTH, STAMINA, DISTANCE, ANGLE,
        TARGET_STATE, ENTITY_TAG, RANDOM, BIOME, WEATHER, MOON_PHASE,
        ENCHANTMENT, POTION_EFFECT, HAS_COUNTER, ANIMATION_PHASE,
        WORLD_TIME, ALL_OF, ANY_OF, NONE_OF;

        public static ConditionType fromString(String s) {
            try {
                return valueOf(s.toUpperCase().replace(":", "_"));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public ConditionType getConditionType() {
        return ConditionType.fromString(type);
    }

    public static class MatchContext {
        public ResourceLocation weaponId;
        public List<ResourceLocation> weaponCategories;
        public ResourceLocation skill;
        public String damageType;
        public String hitType;
        public LivingEntity attacker;
        public LivingEntity target;
        public LivingEntityPatch<?> attackerPatch;
        public LivingEntityPatch<?> targetPatch;
        public int comboCount;
        public int phase;
        public boolean isGuarding;
        public boolean isAirborne;
        public boolean isKnockdown;
        public boolean isStunned;
        public float attackDistance;
        public float attackAngle;
        public String hand;
        public Float staminaPercent;

        public MatchContext() {}
    }
}
