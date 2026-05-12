# Photon + EpicFight 特效联动模组 — 使用文档

> 本模组允许通过数据包（JSON 文件）配置 EpicFight 战斗中的特效触发规则。
> 数据包文件放在 `data/<你的modid>/fx_linkage/` 目录下。

## 前置依赖

**必装：**
- Epic Fight — mc1.20.1-20.14.17
- Photon — mc1.20.1-1.1.17
- LDLib — mc1.20.1-1.0.49

**可选联动：**
- Combat Evolution — 耐力系统 + 处决事件

---

## 一、基础结构

每个 linkage 文件是一个 JSON 对象，包含三个部分：
1. **匹配条件** — 匹配什么样的攻击
2. **事件定义** — 触发什么样的效果
3. **效果配置** — 播什么、播哪里、怎么播

### 基本框架

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

### 顶层字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | 必填 | 固定为 `"epicfight_fx:linkage"` |
| `priority` | int | 10 | 优先级（0~100），越高越优先匹配 |
| `side` | String | `"both"` | 运行端：`"both"` / `"client"` / `"server"` |
| `weapon_categories` | String[] | 可选 | 匹配武器类型（自动识别所属 mod） |
| `weapons` | String[] | 可选 | 匹配具体物品 ID |
| `skills` | String[] | 可选 | 匹配技能 ID |
| `hand` | String | 可选 | 手部限制：`"mainhand"` / `"offhand"` |
| `conditions` | Condition[] | 可选 | 全局条件，全部满足才匹配 |
| `on_xxx` | FxEffect / FxEffect[] | 可选 | 事件触发效果 |
| `events` | FxEffect[] | 可选 | 通用事件数组 |
| `states` | Object | 可选 | 阶段与冷却管理 |

---

## 二、武器匹配系统

三种方式之间是**"或"的关系**（满足任一即匹配）。

### weapon_categories — 武器类型

自动识别类别所属的 mod。

**内置 EpicFight 武器类型：**

| 值 | 说明 |
|----|------|
| `epicfight:sword` | 剑 |
| `epicfight:greatsword` | 大剑 |
| `epicfight:katana` | 太刀 |
| `epicfight:dagger` | 匕首 |
| `epicfight:spear` | 矛 |
| `epicfight:tachi` | 长太刀 |
| `epicfight:longsword` | 长剑 |
| `epicfight:uchigatana` | 打刀 |
| `epicfight:axe` | 斧 |
| `epicfight:fist` | 拳套 |
| `epicfight:shield` | 盾 |
| `epicfight:bow` | 弓 |
| `epicfight:crossbow` | 弩 |

**其他 mod 注册的类型示例：**
- `invincible:sword`
- `rpgcombat:rpg_greatsword`
- `cdmoveset:s_sword`

### weapons — 物品 ID

匹配具体物品的注册 ID，格式为 `<命名空间>:<路径>`。

```json
"weapons": ["minecraft:diamond_sword", "my_mod:legendary_blade"]
```

### skills — 技能 ID

匹配攻击者当前使用的 EpicFight 技能。

```json
"skills": ["epicfight:katana_autoguard"]
```

### hand — 手部限制

限制武器所在的持握手：`"mainhand"`（主手）或 `"offhand"`（副手）。不填则不限制。

---

## 三、条件系统（conditions）

所有条件**同时满足**才算通过。可以放在两个层级：

- **顶层**：全局条件，整个 linkage 匹配时才触发
- **FxEffect 内部**：事件级别条件，同一事件的不同分支使用

`min` 和 `max` 可单独使用，省略的一端不做限制。
不写 conditions = 无条件（永远匹配）。

### 数值范围条件

| 条件 | 说明 | 数据源 |
|------|------|--------|
| `phase` | 阶段计数器（手动管理，通过 commands 增减） | `increment_counter` / `set_phase` 命令操作 |
| `combo` | EpicFight 连击数（自动管理，命中 +1，脱战重置） | 由 EpicFight 自动维护 |
| `distance` | 攻击者与目标之间的距离（格） | `attacker.distanceTo(target)` |
| `angle` | 攻击者朝向与目标间的水平夹角（度），0=正对，180=背对 | 视线向量与目标方向向量的点积 |
| `has_counter` | 阶段性计数器，与 phase 使用相同的值 | 仅放在 FxEffect 级别 |

```json
{ "type": "combo", "min": 5 }                  // 连击 >= 5
{ "type": "angle", "min": 0, "max": 60 }       // 正面 60° 扇形
{ "type": "distance", "max": 3 }               // 距离 <= 3 格
```

### 血量比例条件

- `target`：`"self"` = 攻击者自己，`"target"` = 被攻击者
- `comparator`：

| 值 | 含义 |
|----|------|
| `less_ratio` | 当前血量比例 < 指定值 |
| `greater_ratio` | 当前血量比例 > 指定值 |
| `less_ratio_contain` | 当前血量比例 <= 指定值 |
| `greater_ratio_contain` | 当前血量比例 >= 指定值 |
| 省略（只有 min/max） | 在 [min, max] 范围内 |

```json
// 目标血量低于 30% 时触发
{ "type": "health", "target": "target", "max": 0.3, "comparator": "less_ratio" }
```

### 匹配判断条件

| 条件 | 说明 | 可选值 |
|------|------|--------|
| `weapon_category` | 武器类型判断 | 任意 ResourceLocation |
| `weapon_id` | 武器注册 ID | 任意物品 ResourceLocation |
| `skill` | 技能 ID | 任意技能 ResourceLocation |
| `hit_type` | 命中类型 | `"normal"` / `"critical"` |
| `damage_type` | 伤害源类型 | `"mob"` / `"player"` / `"indirectMagic"` / `"onFire"` / `"fall"` 等 |
| `target_type` | 目标实体类型 | `"living"` / `"player"` / `"boss"` |
| `target_state` | 目标状态 | `"guarding"` / `"stunned"` / `"knockdown"` / `"airborne"` |

> `hit_type` 判断：单次伤害 > 目标最大血量 × 30% 则为 `"critical"`，否则为 `"normal"`。

> `damage_type` 来自 `DamageSource.getMsgId()`。

> `target_type` 中 `"boss"` 的判断依据是 `canChangeDimensions()` 返回 false 的实体。

### 环境条件

| 条件 | 说明 | 示例值 |
|------|------|--------|
| `entity_tag` | EntityType 标签 | `"minecraft:skeletons"` / `"minecraft:raiders"` |
| `biome` | 生物群系 | `"minecraft:plains"` / `"minecraft:nether_wastes"` |
| `weather` | 天气 | `"clear"` / `"rain"` / `"thunder"` |
| `moon_phase` | 月相（0-7） | `"0"`=满月, `"4"`=新月 |

月相完整列表：0=满月, 1=亏凸月, 2=下弦月, 3=残月, 4=新月, 5=蛾眉月, 6=上弦月, 7=盈凸月。

### 特殊条件

| 条件 | 说明 |
|------|------|
| `enchantment` | 武器上有指定附魔（至少 1 级） |
| `potion_effect` | 攻击者身上有指定药水效果 |
| `animation_phase` | EpicFight 动画阶段：1=蓄力, 2=攻击中, 3=收招 |
| `world_time` | 世界时间比例（0~1）：0=日出, 0.25=正午, 0.5=日落, 0.75=午夜 |
| `random` | 概率触发：`"0.5"` = 50% 概率 |
| `stamina` | 攻击者耐力比例（需 CombatEvolution） |

### 逻辑组合

| 逻辑 | 写法 | 说明 |
|------|------|------|
| AND | `all_of: [条件1, 条件2]` | 数组内条件**全部**满足 |
| OR | `any_of: [条件1, 条件2]` | 数组内条件满足**任意一个** |
| NOT | `none_of: [条件1]` | 数组内条件**全部不**满足 |

```json
// "必须是太刀 + 暴击" 才触发
{ "all_of": [
  { "type": "weapon_category", "value": "epicfight:katana" },
  { "type": "hit_type", "value": "critical" }
]}

// "连击>=5 或 目标血量低于 20%" 时触发
{ "any_of": [
  { "type": "combo", "min": 5 },
  { "type": "health", "target": "target", "max": 0.2, "comparator": "less_ratio" }
]}

// "目标不在格挡" 时才触发
{ "none_of": [
  { "type": "target_state", "value": "guarding" }
]}
```

---

## 四、阶段与冷却（states）

```json
"states": {
  "phaselock": false,
  "phase": 0,
  "max_phase": 3,
  "cooldown": 5,
  "global": false
}
```

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `phaselock` | false | 是否锁定阶段（锁定后 `increment_counter` 不生效） |
| `phase` | 0 | 初始阶段值 |
| `max_phase` | 3 | 最大阶段值（到达后不再递增） |
| `cooldown` | 0 | 冷却时间（tick，20 tick = 1 秒），触发后才开始计时 |
| `global` | false | ⚠️ 字段已定义但全局冷却逻辑尚未实现，目前不生效 |

> phase 需要通过 commands 管理（`increment_counter` / `set_phase` / `reset_counter`），不会自动递增。

---

## 五、事件（16 种触发器）

所有事件均支持 `{}`（单特效）和 `[]`（多特效）两种写法，也支持通过 `events` 数组管理。

```json
"on_parry": { "fx": "photon:fire" }                          // 简写（单特效）
"on_hit": [ { "fx": "photon:a" }, { "fx": "photon:b" } ]    // 完整（多特效）
"events": [ { "trigger": "on_hit", "fx": "photon:fire" } ]   // events 数组
```

### 攻击视角（你攻击别人时触发）

| 事件 | 触发条件 |
|------|---------|
| `on_hit` | **成功命中目标**。`LivingHurtEvent` 或 `DEAL_DAMAGE_EVENT` |
| `on_first_hit` | **单次战斗中首次命中目标**。命中计数器从 0 变为 1 时触发 |
| `on_blocked` | **你的攻击被目标成功格挡**。目标为玩家且 `GuardSkill.isActivated` |

### 防守视角（你被攻击时触发）

| 事件 | 触发条件 |
|------|---------|
| `on_guard` | **你成功格挡了对方的攻击**。`TAKE_DAMAGE_EVENT` + BLOCKED 状态 |
| `on_parry` | **你成功招架了对方的攻击**。`TAKE_DAMAGE_EVENT` + parried 状态 |
| `on_dodge` | **你成功闪避了对方的攻击**。`DODGE_SUCCESS_EVENT` |

### 技能视角

| 事件 | 触发条件 |
|------|---------|
| `on_skill_start` | **技能开始施放**。`SKILL_CAST_EVENT`（任何 EpicFight 技能） |
| `on_skill_end` | **技能结束/取消**。`SKILL_CANCEL_EVENT` 或 `ATTACK_ANIMATION_END_EVENT` |
| `on_charged` | **蓄力完成/达到最大蓄力阶段**。`SKILL_CAST_EVENT` + 蓄力量达到上限 |

### 状态视角

| 事件 | 触发条件 |
|------|---------|
| `on_combo` | **连击数增加时触发**。`COMBO_COUNTER_HANDLE_EVENT` |
| `on_phase_change` | **动画阶段/combo 阶段变化**。`ANIMATION_BEGIN` 或 `ATTACK_PHASE_END_EVENT` |
| `on_airborne` | **进入滞空状态**（跳跃/下落/飞行）。`ANIMATION_BEGIN` + 滞空动画检测 |
| `on_stun` | **对目标造成眩晕**（不含击倒）。`EntityStunEvent` + `stunType != KNOCKDOWN` |
| `on_knockdown` | **将目标击倒**。`EntityStunEvent` + `stunType == KNOCKDOWN` |
| `on_kill` | **击杀目标**。`PLAYER_KILLED_EVENT`（玩家击杀）或 CE 的 `execution_finished` |

### 其他

| 事件 | 触发条件 |
|------|---------|
| `on_execution` | **处决触发**。需 CombatEvolution，`LivingHurtEvent` + execution 伤害标签 |

---

## 六、特效效果配置（FxEffect）

### 基本效果字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `fx` | String | 特效路径，格式 `<ns>:<path>`，对应 `assets/<ns>/fx/<path>.fx` |
| `profile` | String | 引用 profile 文件，提供默认值 |
| `position` | String | 特效生成位置（见下方） |
| `follow` | boolean | 是否跟随实体移动 |
| `bone` | String | 绑定骨骼名（见下方） |
| `follow_rotation` | Object | 旋转跟随模式（见下方） |
| `allow_multi` | boolean | 是否允许同特效叠加（false=先停旧再播新，true=叠加） |
| `inherit_color` | boolean | 是否继承武器附魔颜色 |
| `scale` | float | 缩放倍数（骨骼模式下无效） |
| `duration` | int | 持续 tick 数（0 = 只播一次） |

### position — 特效位置

| 值 | 说明 |
|----|------|
| `"weapon"` | 武器位置：攻击者坐标 + 眼睛高度 × 0.6。适用于挥砍/斩击等随武器移动的特效 |
| `"self"` | 攻击者自身位置。适用于全身类特效（光环/护盾/爆发） |
| `"target"` | 命中点位置。适用于命中特效（血液/爆裂/火花）。优先用 hitPos，否则用目标坐标 |
| `"ground"` | 地面（Y=0）。适用于地面裂缝/地刺等场景特效 |

> 当 `follow=true` 且指定了 `bone` 时，`position` 决定跟随哪个实体：
> - `"weapon"` / `"self"` → 跟随攻击者
> - `"target"` → 跟随目标

### bone — 骨骼名（HumanoidArmature）

| 骨骼 | 说明 |
|------|------|
| `"Tool_R"` | 右手武器 |
| `"Tool_L"` | 左手武器 |
| `"Hand_R"` | 右手 |
| `"Hand_L"` | 左手 |
| `"Head"` | 头 |
| `"Chest"` | 胸 |

> 非玩家实体有不同的骨骼结构，请参考各 Armature 实现。

### follow_rotation — 旋转跟随

| 值 | 说明 |
|----|------|
| `true` / `"forward"` | 跟随实体前进方向（默认） |
| `"look"` | 跟随实体视线方向。适用于"眼睛发射光线"类特效 |
| `"xrot"` | 跟随实体 X 轴旋转。适用于俯仰角也跟随的特效 |
| `false` / `"none"` | 不跟随旋转 |

> 当绑定 bone 时，推荐启用（bone 位置 + 骨骼旋转同时跟随效果最佳）。

### profile — 配置档引用

profile 文件放在 `data/<modid>/fx_profiles/` 下，引用时写文件名（不含 `.json` 后缀）。

**合并规则：** linkage 中显式写的字段 > profile 中的字段（覆盖）。未在 linkage 中指定的字段，使用 profile 中的值作为默认值。

```json
// 引用 data/photon_and_epicfight/fx_profiles/katana_slash.json
"profile": "katana_slash"
```

### conditions — 效果级条件

与顶层的 conditions 语法完全相同。所有条件同时满足才执行此特效。适用于同一个事件中根据不同命中类型播放不同特效的情况。

### commands — 指令系统

commands 数组可以在特效触发时同时执行多个指令，按顺序依次执行。

| type | 作用 | 执行端 |
|------|------|--------|
| `spawn_fx` | 生成持续特效 | 客户端 |
| `spawn_fx_burst` | 生成一次性爆发特效 | 客户端 |
| `play_sound` | 播放音效 | 客户端 |
| `damage` | 对目标造成固定伤害（无视无敌帧） | 服务端 |
| `set_phase` | 设置阶段计数器 | 服务端 |
| `increment_counter` | 阶段计数器 +1 | 服务端 |
| `reset_counter` | 阶段计数器归零 | 服务端 |
| `set_cooldown` | 设置冷却时间（tick） | 服务端 |
| `command` | 执行任意 Minecraft 命令 | 服务端 |

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

## 七、完整示例 — 长剑联动配置

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

## 八、Profile 配置档

Profile 文件放在 `data/<modid>/fx_profiles/` 下，用于定义可复用的特效模板。

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

## 九、注意事项

1. **JSON 注释不合法** — 以上所有注释只是为了演示，实际使用前务必删除所有 `//` 开头的内容。

2. **Photon 不附带 `.fx` 文件** — 特效文件需要由其他 mod 或资源包提供。运行时可能有的演示特效包括 `fire` / `orb_bloom` / `trail` / `portal` / `test` 等。

3. **`states.global` 字段** — 数据模型中存在但全局冷却逻辑尚未实现，目前不生效。

4. **`on_hit` 双重触发已修复** — 删除了冗余的 `BASIC_ATTACK_EVENT`，现在只会命中时触发一次。

5. **CombatEvolution 可选** — 耐力条件和处决事件需要安装 CombatEvolution，不安装不影响其他功能。

6. **纯服务端兼容** — 所有客户端类引用均已通过反射替换，可以在纯服务端正常运行。
