package net.zidou.photon_or_epicfight.fxlinkage.engine;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition;
import net.zidou.photon_or_epicfight.fxlinkage.data.FxCondition.MatchContext;

import java.util.List;

public class ConditionEngine {

    public static boolean checkConditions(List<FxCondition> conditions, MatchContext ctx) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (FxCondition c : conditions) {
            if (!checkCondition(c, ctx)) return false;
        }
        return true;
    }

    public static boolean checkCondition(FxCondition c, MatchContext ctx) {
        if (c == null) return true;

        if (c.all_of != null && !c.all_of.isEmpty()) {
            return c.all_of.stream().allMatch(sub -> checkCondition(sub, ctx));
        }
        if (c.any_of != null && !c.any_of.isEmpty()) {
            return c.any_of.stream().anyMatch(sub -> checkCondition(sub, ctx));
        }
        if (c.none_of != null && !c.none_of.isEmpty()) {
            return c.none_of.stream().noneMatch(sub -> checkCondition(sub, ctx));
        }

        FxCondition.ConditionType type = c.getConditionType();
        if (type == null) return true;

        return switch (type) {
            case WEAPON_CATEGORY -> matchWeaponCategory(c.value, ctx);
            case WEAPON_ID -> matchWeaponId(c.value, ctx);
            case SKILL -> matchSkill(c.value, ctx);
            case DAMAGE_TYPE -> matchString(c.value, ctx.damageType);
            case HIT_TYPE -> matchHitType(c.value, ctx);
            case TARGET_TYPE -> matchTargetType(c.value, ctx);
            case COMBO -> compareInt(parseInt(c.min), parseInt(c.max), ctx.comboCount);
            case PHASE -> compareInt(parseInt(c.min), parseInt(c.max), ctx.phase);
            case HEALTH -> compareHealth(c, ctx);
            case DISTANCE -> compareRange(c, ctx.attackDistance);
            case ANGLE -> compareRange(c, ctx.attackAngle);
            case TARGET_STATE -> matchTargetState(c.value, ctx);
            case RANDOM -> Math.random() < parseDouble(c.value, 0.5);
            case ENTITY_TAG -> matchEntityTag(c.value, ctx);
            case BIOME -> matchBiome(c.value, ctx);
            case WEATHER -> matchWeather(c.value, ctx);
            case MOON_PHASE -> matchMoonPhase(c.value, ctx);
            case ENCHANTMENT -> matchEnchantment(c.value, ctx);
            case POTION_EFFECT -> matchPotionEffect(c.value, ctx);
            case HAS_COUNTER -> compareInt(parseInt(c.min), parseInt(c.max), ctx.phase);
            case ANIMATION_PHASE -> matchAnimationPhase(c.value, ctx);
            case WORLD_TIME -> matchWorldTime(c, ctx);
            case STAMINA -> matchStamina(c, ctx);
            default -> false;
        };
    }

    private static boolean matchWeaponCategory(String value, MatchContext ctx) {
        if (value == null || ctx.weaponCategories == null) return true;
        try {
            ResourceLocation rl = ResourceLocation.parse(value);
            return ctx.weaponCategories.stream().anyMatch(cat -> cat.equals(rl));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchWeaponId(String value, MatchContext ctx) {
        if (value == null || ctx.weaponId == null) return true;
        try {
            return ctx.weaponId.equals(ResourceLocation.parse(value));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchSkill(String value, MatchContext ctx) {
        if (value == null || ctx.skill == null) return true;
        try {
            return ctx.skill.equals(ResourceLocation.parse(value));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchString(String expected, String actual) {
        if (expected == null) return true;
        return expected.equalsIgnoreCase(actual);
    }

    private static boolean matchHitType(String value, MatchContext ctx) {
        if (value == null) return true;
        return value.equalsIgnoreCase(ctx.hitType);
    }

    private static boolean matchTargetType(String value, MatchContext ctx) {
        if (value == null || ctx.target == null) return true;
        String v = value.toLowerCase();
        return switch (v) {
            case "living" -> true;
            case "player" -> ctx.target instanceof net.minecraft.world.entity.player.Player;
            case "boss" -> !ctx.target.canChangeDimensions();
            default -> false;
        };
    }

    private static boolean matchTargetState(String value, MatchContext ctx) {
        if (value == null) return true;
        String v = value.toLowerCase();
        return switch (v) {
            case "guarding" -> ctx.isGuarding;
            case "stunned" -> ctx.isStunned;
            case "knockdown" -> ctx.isKnockdown;
            case "airborne" -> ctx.isAirborne;
            default -> false;
        };
    }

    private static boolean compareInt(Integer min, Integer max, int actual) {
        if (min != null && actual < min) return false;
        if (max != null && actual > max) return false;
        return true;
    }

    private static boolean compareRange(FxCondition c, float actual) {
        double min = c.min != null ? c.min : Double.NEGATIVE_INFINITY;
        double max = c.max != null ? c.max : Double.POSITIVE_INFINITY;
        return actual >= min && actual <= max;
    }

    private static boolean compareHealth(FxCondition c, MatchContext ctx) {
        LivingEntity target = "target".equals(c.target) ? ctx.target : ctx.attacker;
        if (target == null) return true;
        float ratio = target.getHealth() / target.getMaxHealth();
        if (c.comparator != null) {
            double val = c.min != null ? c.min : 0.0;
            return switch (c.comparator) {
                case "less_ratio" -> ratio < val;
                case "greater_ratio" -> ratio > val;
                case "less_ratio_contain" -> ratio <= val;
                case "greater_ratio_contain" -> ratio >= val;
                default -> true;
            };
        }
        return compareRange(c, ratio);
    }

    private static int parseInt(Double d) {
        return d != null ? d.intValue() : 0;
    }

    private static double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    private static boolean matchEntityTag(String value, MatchContext ctx) {
        if (value == null || ctx.target == null) return true;
        try {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(value));
            return ctx.target.getType().is(tag);
        } catch (Exception e) { return false; }
    }

    private static boolean matchBiome(String value, MatchContext ctx) {
        if (value == null || ctx.attacker == null) return true;
        try {
            ResourceLocation biomeId = ResourceLocation.parse(value);
            Holder<net.minecraft.world.level.biome.Biome> biome = ctx.attacker.level().getBiome(ctx.attacker.blockPosition());
            return biome.unwrapKey().map(key -> key.location().equals(biomeId)).orElse(false);
        } catch (Exception e) { return false; }
    }

    private static boolean matchWeather(String value, MatchContext ctx) {
        if (value == null || ctx.attacker == null) return true;
        Level level = ctx.attacker.level();
        return switch (value.toLowerCase()) {
            case "clear" -> !level.isRaining() && !level.isThundering();
            case "rain" -> level.isRaining() && !level.isThundering();
            case "thunder" -> level.isThundering();
            default -> false;
        };
    }

    private static boolean matchMoonPhase(String value, MatchContext ctx) {
        if (value == null || ctx.attacker == null) return true;
        try {
            int phase = ctx.attacker.level().dimensionType().moonPhase(ctx.attacker.level().getDayTime());
            int expected = Integer.parseInt(value);
            return phase == expected;
        } catch (Exception e) { return false; }
    }

    private static boolean matchEnchantment(String value, MatchContext ctx) {
        if (value == null || ctx.attacker == null) return true;
        try {
            ResourceLocation enchId = ResourceLocation.parse(value);
            ItemStack weapon = ctx.attacker.getMainHandItem();
            var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(enchId);
            return ench != null && weapon.getEnchantmentLevel(ench) > 0;
        } catch (Exception e) { return false; }
    }

    private static boolean matchPotionEffect(String value, MatchContext ctx) {
        if (value == null || ctx.attacker == null) return true;
        try {
            ResourceLocation effectId = ResourceLocation.parse(value);
            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            return effect != null && ctx.attacker.hasEffect(effect);
        } catch (Exception e) { return false; }
    }

    private static boolean matchAnimationPhase(String value, MatchContext ctx) {
        if (value == null) return true;
        try {
            int expected = Integer.parseInt(value);
            if (ctx.attackerPatch != null) {
                int phaseLevel = ctx.attackerPatch.getEntityState().getLevel();
                return phaseLevel == expected;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private static boolean matchWorldTime(FxCondition c, MatchContext ctx) {
        if (ctx.attacker == null) return true;
        long time = ctx.attacker.level().getDayTime() % 24000L;
        long min = c.min != null ? (long)(c.min * 24000) : 0;
        long max = c.max != null ? (long)(c.max * 24000) : 24000;
        return time >= min && time <= max;
    }

    private static boolean matchStamina(FxCondition c, MatchContext ctx) {
        Float stamina = ctx.staminaPercent;
        if (stamina == null) return false;
        double min = c.min != null ? c.min : -1;
        double max = c.max != null ? c.max : 2;
        if (c.comparator != null) {
            return switch (c.comparator) {
                case "less_ratio" -> stamina < min;
                case "greater_ratio" -> stamina > min;
                case "less_ratio_contain" -> stamina <= min;
                case "greater_ratio_contain" -> stamina >= min;
                default -> stamina >= min && stamina <= max;
            };
        }
        return stamina >= min && stamina <= max;
    }
}
