================================================================================
 Photon + EpicFight 特效联动数据包 —— 配置格式参考
 文件路径: data/<命名空间>/fx_linkage/<任意文件名>.json
 数据包类型: epicfight_fx:linkage
================================================================================

本文档说明 fx_linkage 和 fx_profiles 两类 JSON 配置文件的全部字段格式。
面向数据包作者，不涉及 Java 代码实现。


一、fx_linkage —— 特效联动配置
───────────────────────────────────────────────────────────────────────────────

  data/<命名空间>/fx_linkage/ 下的每个 JSON 文件定义一条"联动规则"：
  当某个战斗事件发生时，如果匹配条件满足，则在指定位置播放特效。


  1.1 根字段
  ───────────────────────────────────────────────────────────────────────

  type: "epicfight_fx:linkage"
    → 固定值，告诉系统这是一条特效联动配置
    → 必填，其他 type 值会被忽略

  priority: 10
    → 匹配优先级，数字越大越优先执行
    → 可选，默认 10
    → 范围: 0 ~ 100

  side: "both"
    → 执行端限制
    → 可选，默认 "both"
    → "both"   → 服务端和客户端都执行
    → "client" → 只在客户端执行 (特效/音效)
    → "server" → 只在服务端执行 (伤害/命令)

  weapon_categories: [ "epicfight:katana", "epicfight:sword" ]
    → 匹配 EpicFight 武器类型
    → 可选，不写则不限制武器类型
    → 列表里任意一项匹配即可
    → 已知类型: katana / sword / greatsword / dagger / spear
                 axe / tachi / longsword / glove / fist

  weapons: [ "minecraft:diamond_sword" ]
    → 匹配具体物品的注册 ID
    → 可选，不写则不限制
    → 和 weapon_categories 是 OR 关系 (满足任一即匹配)

  skills: [ "epicfight:katana_autoguard" ]
    → 匹配技能 ID (EpicFight 技能的注册名)
    → 可选，不写则不限制技能

  conditions: [ ... ]
    → 全局条件列表 (配置参考第二章)
    → 可选，不写则无条件
    → 所有条件必须全部满足 (AND) 此 linkage 才会生效
    → 注意: 这里的条件控制"这个 linkage 是否匹配当前上下文"
             events 里的 conditions 控制"具体某个特效是否执行"
             两者是 AND 关系

  states: { }
    → 阶段/冷却控制 (配置参考第 1.3 节)
    → 可选，不写则使用默认值

  hand: "mainhand"
    → 仅在文档中标记，当前版本未在匹配逻辑中强制执行
    → 可选: "mainhand" / "offhand"


  1.2 事件字段
  ───────────────────────────────────────────────────────────────────────

  每个事件可以用两种形式定义：
  A) 命名事件字段（单条效果）  → on_skill_start: { FxEffect }
  B) 命名事件数组（多条效果）  → on_hit: [ FxEffect, FxEffect, ... ]
  C) 通用 events 数组          → events: [ { trigger, FxEffect }, ... ]

  A/B 和 C 可以同时使用。

  on_skill_start  → 技能成功释放时触发  (单条)
  on_skill_end    → 技能结束/取消时      (单条)
  on_hit          → 命中目标时           (数组)
  on_guard        → 成功格挡时 (防守方)   (单条)
  on_combo        → 连击变化时           (单条)
  on_charged      → 蓄力释放时           (单条)
  on_dodge        → 成功闪避时           (单条)
  on_parry        → 成功招架时 (防守方)   (单条)
  on_kill         → 击杀目标时           (单条)

  events: [
    { "trigger": "on_hit", "fx": "photon:fire", ... }
  ]
    → 通用格式，通过 trigger 字段指定事件名
    → 可以放任何事件名
    → 同一个事件可以有多条

  FxEffect 的结构见第 1.4 节。


  1.3 states —— 阶段与冷却
  ───────────────────────────────────────────────────────────────────────

  "states": {
    "phaselock": false,
      → 是否锁定阶段（锁定后不会自动递增）
      → 可选，默认 false
    "phase": 0,
      → 初始阶段数
      → 可选，默认 0
    "max_phase": 3,
      → 最大阶段数
      → 可选，默认 3
    "cooldown": 5
      → 全局冷却 (tick)
      → 此 linkage 每执行一次后，等待 N tick 后才可再次执行
      → 可选，默认 0（无冷却）
  }

  阶段机制 (phase):
    通过命令 (set_phase / increment_counter / reset_counter) 控制 phase 值。
    可在 conditions 中用 type: "phase" 判断当前阶段，实现"不同阶段不同特效"。


  1.4 FxEffect —— 特效效果定义
  ───────────────────────────────────────────────────────────────────────

  {
    "trigger": "on_hit",
      → 事件名，仅在 events 数组中需要
      → 命名事件字段 (如 on_skill_start: {...}) 不需要此字段

    "fx": "photon:fire",
      → 特效 ResourceLocation
      → 格式: <命名空间>:<路径>
      → 对应加载: assets/<命名空间>/fx/<路径>.fx
      → 可选，不写则不播放特效（只执行 commands）
      → 常见: photon:fire / photon:orb / photon:trail 等

    "profile": "katana_slash",
      → 引用 fx_profiles 中的配置档
      → 可选，引用后该配置档的字段作为默认值
      → 本 effect 中显式写的字段会覆盖 profile 中的值

    "position": "target",
      → 特效生成位置
      → 可选，默认 "self"
      → "target"  → 目标实体位置 / 命中点 (优先命中点)
      → "weapon"  → 攻击者武器位置
      → "self"    → 攻击者自身位置
      → "ground"  → 地面投影

    "follow": false,
      → 是否跟随实体移动
      → 可选，默认 false
      → true  → 特效跟随目标/攻击者移动
      → false → 特效固定在播放位置

    "scale": 1.0,
      → 特效大小倍率
      → 可选，默认 1.0

    "duration": 0,
      → 特效持续 tick 数
      → 可选，默认 0（播放一次）

    "inherit_color": false,
      → 是否继承武器附魔颜色
      → 可选，默认 false

    "conditions": [ ... ]
      → 局部条件列表
      → 此条件的字段格式和全局 conditions 完全一致 (见第二章)
      → 可选，不写则无条件

    "commands": [ ... ]
      → 指令列表 (见第 1.5 节)
      → 可选，不写则无额外指令
  }


  1.5 FxCommand —— 指令定义
  ───────────────────────────────────────────────────────────────────────

  指令作为 FxEffect 的子级，支持以下类型:

  --- 播放音效 ---
  {
    "type": "play_sound",
    "sound": "minecraft:entity.player.attack.crit",
      → 音效 ResourceLocation
    "volume": 1.0,
      → 音量，可选，默认 1.0
    "pitch": 1.0
      → 音调，可选，默认 1.0
  }

  --- 特效 (在指令中二次触发) ---
  {
    "type": "spawn_fx",
    "fx": "photon:orb",
    "position": "target",
    "follow": false
    → 字段与 FxEffect 中的 fx/position/follow 一致
  }

  --- 执行命令 ---
  {
    "type": "command",
    "command": "say 触发了特效联动！"
      → 以攻击者身份执行的命令字符串
  }

  --- 造成伤害 ---
  {
    "type": "damage",
    "fx_damage": 5.0,
      → 伤害数值 (float)
    "bypass_iframe": false
      → 是否无视无敌帧 (预留，当前未实现)
  }

  --- 控制阶段 ---
  { "type": "set_phase", "value": "2" }
    → 设置 linkage.states.phase 为指定值

  { "type": "set_cooldown", "value": "10" }
    → 设置 linkage 冷却为指定 tick

  { "type": "increment_counter" }
    → phase 值 +1 (不超过 max_phase)

  { "type": "reset_counter" }
    → phase = 0, cooldown = 0

  1.6 完整示例
  ───────────────────────────────────────────────────────────────────────

  {
    "type": "epicfight_fx:linkage",
    "priority": 10,
    "weapon_categories": ["epicfight:katana"],
    "skills": ["epicfight:katana_autoguard"],
    "states": { "cooldown": 5 },

    "on_skill_start": {
      "fx": "photon:fire",
      "position": "weapon",
      "follow": true,
      "duration": 20
    },

    "on_hit": [
      {
        "fx": "photon:orb_bloom",
        "position": "target",
        "scale": 1.5,
        "conditions": [
          { "type": "hit_type", "value": "critical" }
        ],
        "commands": [
          { "type": "play_sound",
            "sound": "minecraft:entity.player.attack.crit",
            "volume": 1.0, "pitch": 1.3 }
        ]
      },
      {
        "fx": "photon:fire",
        "position": "target",
        "conditions": [
          { "type": "hit_type", "value": "normal" }
        ]
      }
    ],

    "events": [
      {
        "trigger": "on_hit",
        "fx": "photon:color_tail",
        "position": "target",
        "follow": true,
        "conditions": [
          { "type": "combo", "min": 3 }
        ]
      }
    ]
  }


  1.7 事件触发视角速查
  ───────────────────────────────────────────────────────────────────────

  攻击视角 (你攻击别人时触发):
    on_hit         → 命中目标
    on_first_hit   → 首次命中 (同次战斗中仅一次)
    on_blocked     → 攻击被目标格挡

  防守视角 (你被攻击时触发):
    on_guard       → 你成功格挡
    on_parry       → 你成功招架
    on_dodge       → 你成功闪避

  通用视角:
    on_skill_start → 你释放技能
    on_skill_end   → 技能结束
    on_combo       → 连击数增加
    on_stun        → 你被眩晕
    on_knockdown   → 你被击倒
    on_charged     → 你蓄力释放
    on_airborne    → 你进入滞空
    on_phase_change→ 攻击动画阶段变化
    on_kill        → 你击杀目标
    on_execution   → 你发动处决 (需要 CE)


二、条件格式说明 (conditions)
───────────────────────────────────────────────────────────────────────────────

  conditions 可以出现在两个层级:
  1. linkage 的 "conditions": [...]  — 控制 linkage 是否匹配
  2. effect 的 "conditions": [...]   — 控制具体特效是否执行
  两层的字段格式完全一致。


  2.1 数值比较 (min/max)
  ───────────────────────────────────────────────────────────────────────

  { "type": "phase",  "min": 0, "max": 3 }
    → 当前阶段数在 0~3 范围

  { "type": "combo",  "min": 1 }
    → 连击数 ≥ 1

  { "type": "distance", "min": 0, "max": 5 }
    → 攻击距离在 0~5 格


  2.2 血量比较 (target/comparator/min/max)
  ───────────────────────────────────────────────────────────────────────

  {
    "type": "health",
    "target": "target",
      → "self"  = 攻击者自己
      → "target" = 目标
    "max": 0.3,
    "comparator": "less_ratio"
      → "less_ratio"           = 低于 ( < )
      → "greater_ratio"        = 高于 ( > )
      → "less_ratio_contain"   = 低于或等于 ( ≤ )
      → "greater_ratio_contain"= 高于或等于 ( ≥ )
      → 不写 comparator = 用 min/max 范围
  }

  { "type": "stamina", "target": "self", "max": 0.5, "comparator": "less_ratio" }
    → 类似 health，判断耐力值


  2.3 字符串匹配 (value)
  ───────────────────────────────────────────────────────────────────────

  { "type": "weapon_category", "value": "epicfight:katana" }
    → 当前武器类别

  { "type": "weapon_id", "value": "minecraft:diamond_sword" }
    → 当前物品 ID

  { "type": "skill", "value": "epicfight:katana_autoguard" }
    → 当前使用的技能

  { "type": "hit_type", "value": "critical" }
    → 命中类型: "normal" / "critical"
    → critical = 单次伤害 > 目标最大血量 × 30%

  { "type": "damage_type", "value": "mob_attack" }
    → 伤害类型 (预留，当前未写入)

  { "type": "target_type", "value": "boss" }
    → 目标类型: "living" (任意生物) / "player" (玩家) / "boss" (BOSS)

  { "type": "target_state", "value": "guarding" }
    → 目标状态:
      "guarding"  → 格挡中 (目标正在按住右键格挡)
      "stunned"   → 眩晕 (目标正在硬直中)
      "knockdown" → 击倒 (目标被击飞/倒地)
      "airborne"  → 滞空 (目标在空中)


  2.4 实体/环境匹配 (value)
  ───────────────────────────────────────────────────────────────────────

  { "type": "entity_tag", "value": "minecraft:skeletons" }
    → 目标实体的 EntityType 标签
    → 例如: minecraft:skeletons / minecraft:raiders / forge:bosses

  { "type": "biome", "value": "minecraft:plains" }
    → 攻击者当前所在生物群系

  { "type": "weather", "value": "rain" }
    → 当前天气: "clear" / "rain" / "thunder"

  { "type": "moon_phase", "value": "0" }
    → 月相 (0~7): 0=满月, 4=新月

  { "type": "world_time", "min": 0.5, "max": 0.75 }
    → 世界时间比例 (0~1): 0=日出, 0.25=正午, 0.5=日落, 0.75=午夜

  { "type": "animation_phase", "value": "2" }
    → EpicFight 攻击动画阶段: 1=蓄力, 2=攻击中, 3=收招

  { "type": "has_counter", "min": 0, "max": 3 }
    → 等价于 phase 判断 (兼容命名)


  2.5 装备检测 (value)
  ───────────────────────────────────────────────────────────────────────

  { "type": "enchantment", "value": "minecraft:sharpness" }
    → 主手武器有指定附魔

  { "type": "potion_effect", "value": "minecraft:strength" }
    → 攻击者身上有指定药水效果


  2.6 概率 (value)
  ───────────────────────────────────────────────────────────────────────

  { "type": "random", "value": "0.5" }
    → 50% 概率触发 (0.0 ~ 1.0)


  2.7 逻辑组合
  ───────────────────────────────────────────────────────────────────────

  { "all_of": [ 条件1, 条件2, ... ] }
    → 所有条件必须全部满足 (AND)

  { "any_of": [ 条件1, 条件2, ... ] }
    → 任意一个条件满足即可 (OR)

  { "none_of": [ 条件1, 条件2, ... ] }
    → 所有条件必须全部不满足 (NOT)

  嵌套示例:
  {
    "all_of": [
      { "type": "weapon_category", "value": "epicfight:katana" },
      { "type": "hit_type", "value": "critical" },
      { "none_of": [
        { "type": "target_state", "value": "guarding" }
      ]}
    ]
  }
  → 使用太刀 + 暴击 + 目标没有在格挡


三、fx_profiles —— 特效配置档模板
───────────────────────────────────────────────────────────────────────────────

  data/<命名空间>/fx_profiles/ 下的 JSON 文件定义可复用的特效模板。
  通过 fx_linkage 中的 "profile" 字段引用。


  3.1 根字段
  ───────────────────────────────────────────────────────────────────────

  {
    "type": "epicfight_fx:profile",
      → 固定值，必填

    "name": "katana_slash",
      → 配置档名称，必填
      → 在 fx_linkage 中通过 profile: "katana_slash" 引用
      → 索引方式: 按 name 字段，不是按文件名

    "fx": "photon:trail",
      → 默认特效路径 (可选)

    "position": "weapon",
      → 默认位置 (可选，默认 "self")
      → 可选值: target / weapon / self / ground

    "follow": true,
      → 默认是否跟随 (可选，默认 false)

    "scale": 1.0,
      → 默认大小 (可选，默认 1.0)

    "duration": 0,
      → 默认持续 tick (可选，默认 0)

    "inherit_color": false,
      → 默认是否继承颜色 (可选，默认 false)

    "conditions": [ ... ],
      → 全局条件 (可选)
      → 引用此 profile 时，这些条件也会被检查
      → 和 fx_linkage effect 中的 conditions 是 AND 关系

    "commands": [ ... ],
      → 默认指令列表 (可选)

    "overrides": { ... }
      → 覆盖配置 (见第 3.2 节)
  }


  3.2 overrides —— 条件覆盖
  ───────────────────────────────────────────────────────────────────────

  overrides 的键名是自定义的场景名称（无固定列表），
  值是完整的 FxEffect 对象（不含 trigger 字段）。

  当 fx_linkage 引用此 profile 时:
    1. 先执行合并后的默认效果
    2. 遍历所有 overrides，如果 override 的 conditions 满足，
       则额外执行该 override 效果

  "overrides": {
    "on_critical": {
      "fx": "photon:orb_bloom",
      "scale": 1.3,
      "conditions": [
        { "type": "hit_type", "value": "critical" }
      ],
      "commands": [
        { "type": "play_sound",
          "sound": "minecraft:entity.player.attack.crit",
          "volume": 0.8, "pitch": 1.2 }
      ]
    }
  }


  3.3 fx_linkage 中引用 profile 的写法
  ───────────────────────────────────────────────────────────────────────

  {
    "trigger": "on_hit",
    "profile": "katana_slash",
    // profile 提供了默认 fx/position/follow/scale/duration
    // 以下字段会覆盖 profile 中的对应值:
    "scale": 1.2,
    "commands": [
      { "type": "play_sound", "sound": "...", "volume": 0.5, "pitch": 1.0 }
    ]
    // 等价于写成:
    //   fx=photon:trail (从 profile)
    //   position=weapon (从 profile)
    //   follow=true (从 profile)
    //   scale=1.2 (覆盖)
    //   commands=[play_sound] (覆盖——不继承 profile 原有的 commands)
  }


四、特效路径 (fx 字段)
───────────────────────────────────────────────────────────────────────────────

  fx 字段是标准 Minecraft ResourceLocation，格式为:

    <命名空间>:<路径>

  对应加载:

    assets/<命名空间>/fx/<路径>.fx

  示例:
    photon:fire    → assets/photon/fx/fire.fx
    photon:orb     → assets/photon/fx/orb.fx
    your_mod:myfx  → assets/your_mod/fx/myfx.fx

  注意:
    - Photon 自身不附带任何 .fx 文件
    - .fx 文件需要由其他 mod 或资源包提供
    - 开发环境中，LDlib 在 run/ldlib/assets/photon/fx/ 下提供演示特效:
      fire / fire_bloom / new_fire / orb / orb_bloom
      trail / portal / test / matrix / waterfall
      color_tail / fancy_line / fancy_line2 / fire_opaque
      trail_shader / trail_shader2 / aaaaa
    - 如果你自己做 .fx 文件，用 Photon 编辑器 (游戏内 /photon 指令)


五、数据包文件路径
───────────────────────────────────────────────────────────────────────────────

  特效联动:
    data/<你的命名空间>/fx_linkage/<任意文件名>.json
    type: "epicfight_fx:linkage"

  特效配置档 (可选):
    data/<你的命名空间>/fx_profiles/<任意文件名>.json
    type: "epicfight_fx:profile"

  .fx 特效文件:
    assets/<命名空间>/fx/<文件名>.fx

  所以一个完整的数据包结构示例:
    my_datapack.zip
    ├── pack.mcmeta
    ├── data/
    │   └── my_mod/
    │       ├── fx_linkage/
    │       │   ├── katana_combo.json
    │       │   └── greatsword_skill.json
    │       └── fx_profiles/
    │           └── katana_slash.json
    └── assets/
        └── my_mod/
            └── fx/
                └── custom_slash.fx
