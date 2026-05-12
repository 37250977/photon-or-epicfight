# Photon + EpicFight FX Linkage Mod — User Guide

> This mod allows you to configure EpicFight combat FX trigger rules via datapack (JSON) files.
> Place your files in `data/<your_modid>/fx_linkage/`.

## Required Dependencies

**Required:**
- Epic Fight — mc1.20.1-20.14.17
- Photon — mc1.20.1-1.1.17
- LDLib — mc1.20.1-1.0.49

**Optional:**
- Combat Evolution — Stamina system + execution events

---

## 1. Basic Structure

Each linkage file is a JSON object with three parts:
1. **Match conditions** — what kind of attack to match
2. **Event definitions** — what effect to trigger
3. **Effect config** — what to play, where, and how

### Template

```json
{
  "type": "epicfight_fx:linkage",
  "priority": 10,
  "side": "both",
  "weapon_categories": ["epicfight:longsword"],
  "weapons": ["minecraft:diamond_sword"],
  "skills": ["epicfight:katana_autoguard"],
  "hand": "mainhand",
  "conditions": [],
  "on_hit": {},
  "states": {}
}
```

### Top-Level Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | String | required | Must be `"epicfight_fx:linkage"` |
| `priority` | int | 10 | Priority (0-100). Higher = matched first |
| `side` | String | `"both"` | Execution side: `"both"` / `"client"` / `"server"` |
| `weapon_categories` | String[] | optional | Match weapon categories (auto-detects owner mod) |
| `weapons` | String[] | optional | Match specific item IDs |
| `skills` | String[] | optional | Match skill IDs |
| `hand` | String | optional | Hand restriction: `"mainhand"` / `"offhand"` |
| `conditions` | Condition[] | optional | Global conditions, all must pass |
| `on_xxx` | FxEffect / FxEffect[] | optional | Event-triggered effects |
| `events` | FxEffect[] | optional | Generic event array |
| `states` | Object | optional | Phase and cooldown management |

---

## 2. Weapon Matching

All three matching methods are **OR** (any one match is sufficient).

### weapon_categories

Automatically detects which mod registered the category.

**Built-in EpicFight weapon types:**

| Value | Description |
|-------|-------------|
| `epicfight:sword` | Sword |
| `epicfight:greatsword` | Greatsword |
| `epicfight:katana` | Katana |
| `epicfight:dagger` | Dagger |
| `epicfight:spear` | Spear |
| `epicfight:tachi` | Tachi |
| `epicfight:longsword` | Longsword |
| `epicfight:uchigatana` | Uchigatana |
| `epicfight:axe` | Axe |
| `epicfight:fist` | Fist |
| `epicfight:shield` | Shield |
| `epicfight:bow` | Bow |
| `epicfight:crossbow` | Crossbow |

**Examples from other mods:**
- `invincible:sword`
- `rpgcombat:rpg_greatsword`
- `cdmoveset:s_sword`

### weapons — Item ID

Match by registry ID: `<namespace>:<path>`.

```json
"weapons": ["minecraft:diamond_sword", "my_mod:legendary_blade"]
```

### skills — Skill ID

Match the attacker's currently active EpicFight skill.

```json
"skills": ["epicfight:katana_autoguard"]
```

### hand — Hand Restriction

Restrict to `"mainhand"` or `"offhand"`. Omit to allow either hand.

---

## 3. Condition System (conditions)

All conditions must pass simultaneously. Conditions can be placed at two levels:

- **Top-level**: global conditions — the entire linkage must pass them
- **Inside FxEffect**: effect-level conditions — different branches for the same event

`min` and `max` can be used independently; the omitted side is unbounded.
No conditions = always match.

### Numeric Range Conditions

| Condition | Description | Data Source |
|-----------|-------------|-------------|
| `phase` | Phase counter (manual, via commands) | `increment_counter` / `set_phase` commands |
| `combo` | EpicFight combo counter (auto, +1 per hit, resets out of combat) | Maintained by EpicFight |
| `distance` | Distance between attacker and target (blocks) | `attacker.distanceTo(target)` |
| `angle` | Horizontal angle between attacker's facing and target direction (degrees), 0=front, 180=behind | Dot product of look vector and target direction |
| `has_counter` | Phase counter (same value as phase), for FxEffect level only | Same as phase |

```json
{ "type": "combo", "min": 5 }                // combo >= 5
{ "type": "angle", "min": 0, "max": 60 }     // frontal 60° cone
{ "type": "distance", "max": 3 }             // distance <= 3 blocks
```

### Health Ratio Conditions

- `target`: `"self"` = attacker, `"target"` = the one being attacked
- `comparator`:

| Value | Meaning |
|-------|---------|
| `less_ratio` | Current health ratio < specified value |
| `greater_ratio` | Current health ratio > specified value |
| `less_ratio_contain` | Current health ratio <= specified value |
| `greater_ratio_contain` | Current health ratio >= specified value |
| omitted (only min/max) | Within [min, max] range |

```json
// Triggers when target's health is below 30%
{ "type": "health", "target": "target", "max": 0.3, "comparator": "less_ratio" }
```

### Match Conditions

| Condition | Description | Values |
|-----------|-------------|--------|
| `weapon_category` | Weapon category | Any category ResourceLocation |
| `weapon_id` | Weapon registry ID | Any item ResourceLocation |
| `skill` | Skill ID | Any skill ResourceLocation |
| `hit_type` | Hit type | `"normal"` / `"critical"` |
| `damage_type` | Damage source type | `"mob"` / `"player"` / `"indirectMagic"` / `"onFire"` / `"fall"` etc. |
| `target_type` | Target entity type | `"living"` / `"player"` / `"boss"` |
| `target_state` | Target state | `"guarding"` / `"stunned"` / `"knockdown"` / `"airborne"` |

> `hit_type`: `"critical"` when damage > target's max health × 30%, otherwise `"normal"`.

> `damage_type` comes from `DamageSource.getMsgId()`.

> `"boss"` detection: entities where `canChangeDimensions()` returns false.

### Environment Conditions

| Condition | Description | Examples |
|-----------|-------------|----------|
| `entity_tag` | EntityType tag | `"minecraft:skeletons"` / `"minecraft:raiders"` |
| `biome` | Biome | `"minecraft:plains"` / `"minecraft:nether_wastes"` |
| `weather` | Weather | `"clear"` / `"rain"` / `"thunder"` |
| `moon_phase` | Moon phase (0-7) | `"0"`=full moon, `"4"`=new moon |

Full moon phase list: 0=full, 1=waning gibbous, 2=last quarter, 3=waning crescent, 4=new, 5=waxing crescent, 6=first quarter, 7=waxing gibbous.

### Special Conditions

| Condition | Description |
|-----------|-------------|
| `enchantment` | Weapon has the specified enchantment (at least level 1) |
| `potion_effect` | Attacker has the specified potion effect active |
| `animation_phase` | EpicFight animation phase: 1=windup, 2=strike, 3=recovery |
| `world_time` | World time ratio (0-1): 0=sunrise, 0.25=noon, 0.5=sunset, 0.75=midnight |
| `random` | Probability trigger: `"0.5"` = 50% chance |
| `stamina` | Attacker stamina ratio (requires CombatEvolution) |

### Logic Gates

| Gate | Syntax | Description |
|------|--------|-------------|
| AND | `all_of: [cond1, cond2]` | **All** conditions must pass |
| OR | `any_of: [cond1, cond2]` | **Any** condition must pass |
| NOT | `none_of: [cond1]` | **None** of the conditions must pass |

```json
// Must be katana + critical hit
{ "all_of": [
  { "type": "weapon_category", "value": "epicfight:katana" },
  { "type": "hit_type", "value": "critical" }
]}

// Combo >= 5 OR target health below 20%
{ "any_of": [
  { "type": "combo", "min": 5 },
  { "type": "health", "target": "target", "max": 0.2, "comparator": "less_ratio" }
]}

// Target must NOT be guarding
{ "none_of": [
  { "type": "target_state", "value": "guarding" }
]}
```

---

## 4. Phase & Cooldown (states)

```json
"states": {
  "phaselock": false,
  "phase": 0,
  "max_phase": 3,
  "cooldown": 5,
  "global": false
}
```

| Field | Default | Description |
|-------|---------|-------------|
| `phaselock` | false | Lock phase (prevents `increment_counter`) |
| `phase` | 0 | Initial phase value |
| `max_phase` | 3 | Maximum phase value (stops incrementing) |
| `cooldown` | 0 | Cooldown in ticks (20 ticks = 1 second), starts counting after triggering |
| `global` | false | ⚠️ Field exists but global cooldown logic is not yet implemented |

> Phase is managed via commands (`increment_counter` / `set_phase` / `reset_counter`). It does not auto-increment.

---

## 5. Events (16 Triggers)

All events support both `{}` (single effect) and `[]` (multiple effects). You can also use the `events` array.

```json
"on_parry": { "fx": "photon:fire" }                            // shorthand (single)
"on_hit": [ { "fx": "photon:a" }, { "fx": "photon:b" } ]      // full (multiple)
"events": [ { "trigger": "on_hit", "fx": "photon:fire" } ]     // events array
```

### Offensive (you attack someone)

| Event | Trigger Condition |
|-------|------------------|
| `on_hit` | **Successfully hit target**. `LivingHurtEvent` or `DEAL_DAMAGE_EVENT` |
| `on_first_hit` | **First hit in a fight**. Hit counter goes from 0 to 1 |
| `on_blocked` | **Your attack was blocked**. Target is a player with `GuardSkill.isActivated` |

### Defensive (you are attacked)

| Event | Trigger Condition |
|-------|------------------|
| `on_guard` | **You successfully blocked an attack**. `TAKE_DAMAGE_EVENT` + BLOCKED status |
| `on_parry` | **You successfully parried an attack**. `TAKE_DAMAGE_EVENT` + parried status |
| `on_dodge` | **You successfully dodged an attack**. `DODGE_SUCCESS_EVENT` |

### Skill

| Event | Trigger Condition |
|-------|------------------|
| `on_skill_start` | **Skill started casting**. `SKILL_CAST_EVENT` (any EpicFight skill) |
| `on_skill_end` | **Skill ended/cancelled**. `SKILL_CANCEL_EVENT` or `ATTACK_ANIMATION_END_EVENT` |
| `on_charged` | **Charge completed / max charge reached**. `SKILL_CAST_EVENT` + accumulated charge at max |

### State

| Event | Trigger Condition |
|-------|------------------|
| `on_combo` | **Combo counter increased**. `COMBO_COUNTER_HANDLE_EVENT` |
| `on_phase_change` | **Animation/combo phase changed**. `ANIMATION_BEGIN` or `ATTACK_PHASE_END_EVENT` |
| `on_airborne` | **Entered airborne state** (jump/fall/fly). `ANIMATION_BEGIN` + airborne animation detection |
| `on_stun` | **Stunned target** (not knockdown). `EntityStunEvent` + `stunType != KNOCKDOWN` |
| `on_knockdown` | **Knocked down target**. `EntityStunEvent` + `stunType == KNOCKDOWN` |
| `on_kill` | **Killed target**. `PLAYER_KILLED_EVENT` or CE's `execution_finished` |

### Other

| Event | Trigger Condition |
|-------|------------------|
| `on_execution` | **Execution triggered**. Requires CombatEvolution, `LivingHurtEvent` + execution damage tag |

---

## 6. Effect Configuration (FxEffect)

### Basic Fields

| Field | Type | Description |
|-------|------|-------------|
| `fx` | String | Effect path: `<ns>:<path>`, loads `assets/<ns>/fx/<path>.fx` |
| `profile` | String | Reference a profile file for default values |
| `position` | String | Spawn position (see below) |
| `follow` | boolean | Whether to follow the entity |
| `bone` | String | Bone name to attach to (see below) |
| `follow_rotation` | Object | Rotation follow mode (see below) |
| `allow_multi` | boolean | Allow stacking: false=stop old then play new, true=stack |
| `inherit_color` | boolean | Inherit weapon enchantment glow color |
| `scale` | float | Scale multiplier (ignored in bone mode) |
| `duration` | int | Duration in ticks (0 = one-shot) |

### position — Spawn Position

| Value | Description |
|-------|-------------|
| `"weapon"` | Weapon position: attacker position + eye height × 0.6. For slashes and weapon-trailing effects |
| `"self"` | Attacker's own position. For aura/shield/burst effects |
| `"target"` | Hit point position. For blood/explosion/spark effects. Uses hitPos if available, otherwise target position |
| `"ground"` | Ground level (Y=0). For ground cracks and terrain effects |

> When `follow=true` with a `bone` specified, `position` determines which entity to follow:
> - `"weapon"` / `"self"` → follows attacker
> - `"target"` → follows target

### bone — Bone Names (HumanoidArmature)

| Bone | Description |
|------|-------------|
| `"Tool_R"` | Right hand weapon |
| `"Tool_L"` | Left hand weapon |
| `"Hand_R"` | Right hand |
| `"Hand_L"` | Left hand |
| `"Head"` | Head |
| `"Chest"` | Chest |

> Non-player entities have different bone structures. Refer to each Armature implementation.

### follow_rotation — Rotation Follow Mode

| Value | Description |
|-------|-------------|
| `true` / `"forward"` | Follow entity's forward direction (default) |
| `"look"` | Follow entity's look direction. For "eye laser" effects |
| `"xrot"` | Follow entity's X-axis rotation. For pitch-following effects |
| `false` / `"none"` | No rotation following |

> Recommended when using bone mode (bone position + rotation tracking works best together).

### profile — Profile Reference

Profile files go in `data/<modid>/fx_profiles/`. Reference by filename (without `.json`).

**Merge rule:** Fields explicitly set in the linkage override the profile. Fields not set in the linkage inherit from the profile.

```json
// References data/photon_and_epicfight/fx_profiles/katana_slash.json
"profile": "katana_slash"
```

### conditions — Effect-Level Conditions

Same syntax as top-level conditions. All must pass for this effect to execute. Useful for different effect branches within the same event.

### commands — Command System

Commands run sequentially when the effect triggers.

| Type | Action | Side |
|------|--------|------|
| `spawn_fx` | Spawn continuous effect | Client |
| `spawn_fx_burst` | Spawn one-shot burst effect | Client |
| `play_sound` | Play a sound | Client |
| `damage` | Deal fixed damage (ignores invincibility frames) | Server |
| `set_phase` | Set phase counter | Server |
| `increment_counter` | Increment phase counter by 1 | Server |
| `reset_counter` | Reset phase counter to 0 | Server |
| `set_cooldown` | Set cooldown (ticks) | Server |
| `command` | Execute any Minecraft command | Server |

```json
"commands": [
  { "type": "play_sound",
    "sound": "minecraft:entity.player.attack.crit",
    "volume": 1.0, "pitch": 1.3 },
  { "type": "spawn_fx",
    "fx": "photon:fire", "position": "target", "scale": 1.5 }
]
```

---

## 7. Complete Example — Longsword Configuration

```json
{
  "type": "epicfight_fx:linkage",
  "priority": 10,
  "weapon_categories": ["epicfight:longsword"],
  "on_parry": {
    "trigger": "on_parry",
    "fx": "photon:gd_text",
    "position": "weapon",
    "follow": true,
    "bone": "Tool_R",
    "follow_rotation": false,
    "allow_multi": true,
    "duration": 20
  },
  "on_hit": {
    "trigger": "on_hit",
    "fx": "photon:xie",
    "position": "target",
    "follow": false,
    "allow_multi": true,
    "duration": 20
  }
}
```

---

## 8. Profile Configuration

Profile files go in `data/<modid>/fx_profiles/`. They define reusable effect templates.

```json
{
  "type": "epicfight_fx:profile",
  "name": "my_profile",
  "fx": "photon:fire",
  "position": "target",
  "follow": false,
  "bone": "Tool_R",
  "follow_rotation": false,
  "allow_multi": false,
  "scale": 1.0,
  "duration": 0,
  "inherit_color": false,
  "conditions": [],
  "commands": []
}
```

---

## 9. Important Notes

1. **JSON comments are invalid** — All `//` comments above are for demonstration only. Remove them before use.

2. **Photon does not ship `.fx` files** — Effect files must come from other mods or resource packs. Demo effects that may be available at runtime: `fire` / `orb_bloom` / `trail` / `portal` / `test`.

3. **`states.global` field** — The field exists in the data model but global cooldown logic is not yet implemented. It has no effect currently.

4. **`on_hit` double-trigger fixed** — Removed the redundant `BASIC_ATTACK_EVENT`. `on_hit` now fires only once per actual hit.

5. **CombatEvolution is optional** — Stamina conditions and execution events require CombatEvolution. The rest works fine without it.

6. **Dedicated server compatible** — All client class references have been replaced with reflection. Works on pure servers.
