================================================================================
 Photon + EpicFight 特效联动数据包 —— 系统逻辑设计文档
 文件: .trae/logic_design.md（此文件仅用于了解系统，不参与运行）
================================================================================

本系统是一个运行在 Minecraft Forge (1.20.1) 上的数据驱动特效触发器，
将 Photon 特效引擎与 EpicFight 战斗系统的事件体系对接。

通过 JSON 配置文件声明"当什么战斗事件发生时，在哪里播放什么特效"，
无需修改 Java 代码即可扩展。


一、系统架构概览
───────────────────────────────────────────────────────────────────────────────

    数据包 (JSON)
         │
         ▼  资源重载 (AddReloadListenerEvent)
  ┌─────────────────┐
  │  FxLinkageLoader │  ← SimpleJsonResourceReloadListener, 读取 data/*/fx_linkage/
  └────────┬────────┘
           │ 解析为 FxLinkageData → RuntimeLinkage（运行时可执行对象）
           ▼
  ┌─────────────────┐
  │  FxLinkageEngine │  ← 核心引擎, match + fire
  └────┬────────────┘
       │ 调用
       ▼
  ┌────────────────────┐       ┌─────────────────┐
  │  ConditionEngine   │       │  EffectExecutor  │
  │  条件匹配引擎       │       │  特效执行器       │
  └────────────────────┘       └────────┬────────┘
                                        │
                         ┌──────────────┼──────────────┐
                         ▼              ▼              ▼
                    BlockEffectCmd  EntityEffectCmd  音效/指令/伤害
                    (光子特效-定位)  (光子特效-跟随)    (原生MC)

    事件触发来源:
  ┌─────────────────────┐
  │ EpicFightEventHandler │  ← 通过 Forge EventBus 监听基础事件
  │   (Mod订阅)          │     LivingHurtEvent → on_hit
  └─────────────────────┘     ServerTickEvent → 冷却递减

  ┌─────────────────────┐
  │ FxPlayerEventListener│  ← 通过 EpicFight PlayerEventListener 监听
  │   (Mixin注入注册)    │     12 个 EpicFight 专有事件
  └─────────────────────┘

  ┌─────────────────────┐
  │ FxProfileLoader      │  ← 独立资源加载器, 读取 data/*/fx_profiles/
  │   (可选模板系统)     │     提供 FxEffect 的默认值 + 覆盖(override)
  └─────────────────────┘


二、数据流全链路
───────────────────────────────────────────────────────────────────────────────

  步骤 1: 加载
  ──────────
  游戏启动 / /reload 时:
    AddReloadListenerEvent
      → FxLinkageLoader.apply()
        → 读取 data/<所有命名空间>/fx_linkage/*.json
        → 只处理 type == "epicfight_fx:linkage" 的配置
        → 每条配置解析为 RuntimeLinkage 对象
        → 按 priority 从高到低排序
      → FxProfileLoader.apply()
        → 读取 data/<所有命名空间>/fx_profiles/*.json
        → 只处理 type == "epicfight_fx:profile" 的配置
        → 以 name 为键存入 Map<String, FxProfileData>


  步骤 2: 事件注册
  ──────────
  玩家加入世界时 (EntityJoinLevelEvent):
    MixinServerPlayerPatch.onJoinWorld @TAIL
      → FxPlayerEventListener.register(ServerPlayerPatch)
        → 在 this.eventListeners 上注册 12 个事件监听器
           (会随玩家销毁自动移除, 无残留)
        → 向 MinecraftForge.EVENT_BUS 注册 StunEventHandler


  步骤 3: 事件触发 & 匹配
  ──────────
  战斗行为发生:
    ↓
  EpicFight 内部触发 PlayerEventListener
    ↓
  FxPlayerEventListener 收到回调
    → 调用 EpicFightEventHandler.buildMatchContext()
       构造 MatchContext 对象: 攻击者、目标、武器、状态等
    → 调用 FxLinkageEngine.fireEvent("事件名", ctx, pos)
      │
      ├─ 1. 从 FxLinkageLoader 获取对此事件感兴趣的所有 linkage
      │    (eventEffects.containsKey(事件名))
      │
      ├─ 2. 按 priority 排序遍历, 逐个调用 matchLinkage()
      │    ├─ 武器分类匹配 (weaponCategories)
      │    ├─ 物品 ID 匹配 (weapons)
      │    ├─ 技能 ID 匹配 (skills)
      │    └─ 全局条件匹配 (raw.conditions)
      │
      ├─ 3. 跳过冷却中的 linkage (currentCooldown > 0)
      │
      ├─ 4. 遍历 linkage 中对应该事件的所有 FxEffect
      │    ├─ 调用 ConditionEngine.checkConditions(effect.conditions)
      │    │   └─ 全部条件通过 → 执行
      │    └─ 调用 EffectExecutor.executeEffect()
      │
      └─ 5. 如果 linkage 有冷却 → 设置 currentCooldown


  步骤 4: 效果执行
  ──────────
  EffectExecutor.executeEffect(effect, linkage, attacker, target, pos)
    │
    ├─ 检查 effect.profile 字段
    │   ├─ 有 → 加载 FxProfileData
    │   │   ├─ 合并 effect 字段覆盖 profile 字段 (effect优先)
    │   │   ├─ 执行合并后的效果
    │   │   └─ 遍历 profile.overrides, 检查每个 override.conditions
    │   │       └─ 匹配则执行 override 效果
    │   └─ 无 → 直接执行 effect
    │
    └─ executeMergedEffect(effect)
        ├─ 遍历 commands → executeCommand()
        │   ├─ SPAWN_FX         → resolveSpawnPos() + Photon Network
        │   ├─ SPAWN_FX_BURST   → 同上 (不跟随)
        │   ├─ PLAY_SOUND       → ServerLevel.playSound()
        │   ├─ COMMAND          → 以玩家身份执行命令
        │   ├─ DAMAGE           → target.hurt()
        │   ├─ SET_PHASE        → linkage.states.phase = value
        │   ├─ SET_COOLDOWN     → linkage.states.currentCooldown = value
        │   ├─ INCREMENT_COUNTER→ phase++
        │   ├─ RESET_COUNTER    → phase=0, cooldown=0
        │   └─ SPAWN_BEAM/SPAWN_TRAIL/CAMERA_SHAKE/TIME_SLOW/PARTICLE/
        │      LIGHT_FLASH/SCREEN_OVERLAY → 暂未实现
        │
        └─ 检查 effect.fx → spawnFX()
            ├─ follow=true 且 有跟随实体 → EntityEffectCommand (Photon 网络包)
            │   └─ 客户端接收 → FXHelper.getFX() → EntityEffect.start()
            └─ follow=false 或 无实体 → BlockEffectCommand (Photon 网络包)
                └─ 客户端接收 → FXHelper.getFX() → BlockEffect.start()


三、MatchContext 上下文对象
───────────────────────────────────────────────────────────────────────────────

  MatchContext 是整个条件系统的数据总线, buildMatchContext() 负责填充:

  ┌──────────────────────┬────────────────────────────────────────────────────┐
  │ 字段                 │ 来源                                               │
  ├──────────────────────┼────────────────────────────────────────────────────┤
  │ attacker             │ buildMatchContext(attacker, ...) 的参数             │
  │ target               │ buildMatchContext(..., target, ...) 的参数          │
  │ attackerPatch        │ EpicFightCapabilities.getEntityPatch()              │
  │ targetPatch          │ EpicFightCapabilities.getEntityPatch()              │
  │ weaponId             │ BuiltInRegistries.ITEM.getKey(主手物品)             │
  │ weaponCategories     │ 1) getHoldingItemCapability() 反射获取             │
  │                      │ 2) 反射失败则按物品 ID 关键字推断 (sword/katana...) │
  │ skill                │ event.getSkillContainer().getSkill().getRegistryName│
  │ hitType              │ on_hit 中: >30% 血量 = "critical", 否则 "normal"  │
  │ comboCount           │ COMBO_COUNTER_HANDLE_EVENT.getNextValue()         │
  │ phase                │ ANIMATION_BEGIN / ATTACK_PHASE_END                 │
  │ isGuarding           │ 目标玩家: SkillSlots.GUARD.isActivated()           │
  │ isStunned            │ EntityState.hurt() 反射调用 (hurtLevel > 0)       │
  │ isKnockdown          │ EntityState.knockDown() 反射调用                   │
  │ isAirborne           │ LivingEntityPatch.isAirborneState() 反射调用       │
  │ attackDistance       │ attacker.distanceTo(target)                        │
  │ attackAngle          │ 未写入, 预留                                       │
  │ damageType           │ 未写入, 预留                                       │
  └──────────────────────┴────────────────────────────────────────────────────┘


四、事件列表 & 触发链路
───────────────────────────────────────────────────────────────────────────────

  攻击视角 (你攻击别人时触发):

    on_hit
      触发: LivingHurtEvent (Forge)
         + DEAL_DAMAGE_EVENT_ATTACK (EpicFight, 基本攻击时)
         + BASIC_ATTACK_EVENT (EpicFight, 备选)
      ctx.attacker = 你, ctx.target = 被打的实体

    on_first_hit
      触发: DEAL_DAMAGE_EVENT_ATTACK (首次命中检测)
      每个玩家每场战斗仅触发一次 (Map<Player, Boolean> 跟踪)

    on_blocked
      触发: DEAL_DAMAGE_EVENT_ATTACK (攻击者视角)
      检测: 目标玩家的 SkillSlots.GUARD.isActivated() == true
      ctx.attacker = 你, ctx.target = 格挡的目标


  防守视角 (你被攻击时触发):

    on_guard
      触发: TAKE_DAMAGE_EVENT_ATTACK + result == BLOCKED
      含义: 你按住右键(格挡技能激活中), 成功挡住了对方攻击
      ctx.attacker = 打你的敌人, ctx.target = 你 (防守方)

    on_parry
      触发: TAKE_DAMAGE_EVENT_ATTACK + isParried() == true
      含义: 你的招架技能在完美时机激活, 招架了对方攻击
      ctx.attacker = 打你的敌人, ctx.target = 你 (防守方)

    on_dodge
      触发: DODGE_SUCCESS_EVENT
      含义: 你在受到伤害前成功翻滚/闪避


  通用视角 (与攻击/防守双方无关):

    on_skill_start
      触发: SKILL_CAST_EVENT
      含义: 玩家成功释放了一个 EpicFight 技能
      ctx.skill = 技能 ID (如 epicfight:katana_autoguard)

    on_skill_end
      触发: SKILL_CANCEL_EVENT + ATTACK_ANIMATION_END_EVENT
      含义: 技能被取消或攻击动画播放完毕

    on_combo
      触发: COMBO_COUNTER_HANDLE_EVENT
      含义: 连击计数器增加 (上次值 < 下次值)
      ctx.comboCount = 当前连击数

    on_stun
      触发: EntityStunEvent (Forge 事件) + stunType != KNOCKDOWN
      含义: 你被眩晕 (SHORT / LONG / HOLD)

    on_knockdown
      触发: EntityStunEvent + stunType == KNOCKDOWN
      含义: 你被击倒

    on_charged
      触发: SKILL_CAST_EVENT + getAccumulatedChargeAmount() > 0
      含义: 你释放了一个蓄力技能 (蓄力量 > 0)

    on_airborne
      触发: ANIMATION_BEGIN_EVENT
      检测: 动画名含 jump/fly/float/fall
      含义: 你进入滞空状态

    on_phase_change
      触发: ANIMATION_BEGIN_EVENT + ATTACK_PHASE_END_EVENT
      含义: 攻击动画阶段改变 (1蓄力→2攻击中→3收招)

    on_kill
      触发: PLAYER_KILLED_EVENT
      含义: 你击杀了另一个玩家

    on_execution
      触发: LivingHurtEvent + 伤害标签为 combat_evolution:execution
      含义: 你对可处决目标发动了处决 (需要安装 CE 模组)


五、条件系统 (ConditionEngine)
───────────────────────────────────────────────────────────────────────────────

  条件分两层:
    1. linkage 全局 conditions — 决定 linkage 是否匹配当前上下文
    2. effect 局部 conditions — 决定 linkage 匹配后, 具体执行哪个 effect

  条件类型:

  数值比较 (min/max):
    phase       阶段数 (linkage.states.phase)
    combo       连击数 (ctx.comboCount)
    distance    攻击距离 (ctx.attackDistance, 单次匹配时快照)
    angle       攻击角度 (ctx.attackAngle, 未写入, 预留)

  血量比较 (min/max/comparator/target):
    health      血量比例 (比较对象: "self"/"target")
    stamina     耐力比例 (同上)
    comparator: "less_ratio" / "greater_ratio" / "_contain" 含等于

  字符串匹配 (value):
    weapon_category   ctx.weaponCategories.contains(ResourceLocation)
    weapon_id         ctx.weaponId.equals(ResourceLocation)
    skill             ctx.skill.equals(ResourceLocation)
    hit_type          "normal" / "critical"
    damage_type       与 ctx.damageType 比较 (未写入)
    target_type       "living" / "player" / "boss"
    target_state      "guarding" / "stunned" / "knockdown" / "airborne"
    enchantment        主手武器附魔检测
    potion_effect      攻击者药水效果检测

  环境匹配:
    entity_tag        目标实体的 EntityType tag
    biome             攻击者当前群系
    weather           clear / rain / thunder
    moon_phase        0-7
    world_time        0-1 (比例, 0=日出 0.5=正午)
    animation_phase   1(蓄力) / 2(攻击中) / 3(收招) — 攻击者视角

  逻辑/概率:
    random            概率, 值为 0-1
    all_of            AND
    any_of            OR
    none_of           NOT
    has_counter       与 phase 同义 (兼容命名)

  全部条件同时满足时 (AND) 结果为 true。


六、特效配置档 (FxProfile) 系统
───────────────────────────────────────────────────────────────────────────────

  Profile 是一种可选的可复用模板, 存储在 data/*/fx_profiles/ 下。

  作用:
    1. 提供默认值: 在 fx_linkage 中只写 profile 名, 不写 fx/position 等,
       自动继承 profile 的字段
    2. 覆盖 (overrides): 根据附加 conditions, 在特定场景下替换默认特效

  合并规则:
    effect.fx != null       → 用 effect.fx
    effect.fx == null       → 用 profile.fx

    effect.position != null  → 用 effect.position
    effect.position == null  → 用 profile.position

    ...以此类推, 字段级覆盖, 不覆盖的用 profile 的

  Override 执行:
    profile.overrides 里的每一项都是一个完整 FxEffect,
    每个 override 有独立的 conditions,
    如果满足条件, override 作为额外效果叠加执行。

  用例:
    profile katana_slash 定义:
      fx = photon:trail, position = weapon

    overrides:
      on_critical → conditions hit_type=critical → fx = photon:orb_bloom, scale=1.3
      on_guard_break → ... → fx = photon:fire_bloom, scale=2.0

    fx_linkage 中引用:
      { "trigger": "on_hit", "profile": "katana_slash", "scale": 1.2 }

    实际执行效果:
      1. 合并: fx=photon:trail, position=weapon, scale=1.2 (覆盖了 profile 默认)
      2. 如果命中是 critical → 额外执行: fx=photon:orb_bloom, scale=1.3
      3. 如果命中是破防 → 额外执行: fx=photon:fire_bloom, scale=2.0


七、Photon 特效发送机制
───────────────────────────────────────────────────────────────────────────────

  服务端 → Photon 网络包 → 客户端 → FXHelper → 播放特效

  ┌─────────────────────┐
  │  服务端 (EffectExecutor) │
  │                      │
  │  spawnFX("photon:fire", pos, follow, ...)       │
  │    │                                             │
  │    ├─ follow=true + 有跟随实体                     │
  │    │   → new EntityEffectCommand()               │
  │    │     .setLocation(ResourceLocation)           │
  │    │     .setEntities([跟随实体])                  │
  │    │     .setAllowMulti(true)                     │
  │    │   → PhotonNetworking.NETWORK.sendToAll()     │
  │    │                                             │
  │    └─ follow=false / 无实体                        │
  │        → new BlockEffectCommand()                 │
  │          .setLocation(ResourceLocation)            │
  │          .setPos(BlockPos.containing(spawnPos))    │
  │          .setOffset(小数偏移部分)                   │
  │          .setAllowMulti(true)                      │
  │        → PhotonNetworking.NETWORK.sendToAll()      │
  └─────────────────────┘
           │ 网络传输 (S2C)
           ▼
  ┌─────────────────────┐
  │  客户端 (Photon)     │
  │                      │
  │  BlockEffectCommand.execute()                      │
  │    → FXHelper.getFX(location)                     │
  │      → 读取 assets/<命名空间>/fx/<路径>.fx          │
  │      → 未命中缓存则从资源包/Mod Jar 加载            │
  │      → 反序列化 NBT → new FX()                     │
  │    → new BlockEffect(fx, level, pos)               │
  │      .setOffset(...) .setAllowMulti(true)           │
  │      → effect.start()                              │
  │                                                    │
  │  EntityEffectCommand.execute()                     │
  │    → FXHelper.getFX(location)                      │
  │    → new EntityEffect(fx, level, entity, autoRotate)│
  │      → effect.start()                              │
  └─────────────────────┘

  注意:
    .fx 文件存放于 assets/<命名空间>/fx/<路径>.fx
    Photon 自身不附带任何 .fx 文件, 需要其他 Mod 或资源包提供。
    开发环境中 LDlib 会提供演示特效 (fire, orb, trail 等)。


八、位置系统 (position)
───────────────────────────────────────────────────────────────────────────────

  resolveSpawnPos(position, attacker, target, hitPos):

    "target" + hitPos != null → hitPos (精确命中点)
    "target" + target != null → target.position() (目标脚底)
    "weapon" + attacker != null  → attacker.position() + up(eyeHeight*0.6) (武器高度)
    "self"   + attacker != null  → attacker.position() (攻击者脚底)
    "ground" → target/attacker 位置的 (x, 0, z) (地面投影)

  跟随:
    resolveSpawnEntity(position, attacker, target):
      "weapon" / "self" → attacker (跟随攻击者)
      "target"          → target (跟随目标)

  实际播放:
    follow=true  + 有效实体 → EntityEffectCommand (特效随实体移动)
    follow=false / 无实体   → BlockEffectCommand (特效固定在 BlockPos)


九、JSON 数据模型概览
───────────────────────────────────────────────────────────────────────────────

  FxLinkageData (data/*/fx_linkage/*.json):
  ─────────────
  {
    type: "epicfight_fx:linkage",   // 固定, 识别用
    priority: 10,                    // 排序, 高优先
    side: "both",                    // "both"/"client"/"server"

    // 匹配条件 (顶级, linkage 级别的筛选)
    weapon_categories: ["epicfight:katana"],
    weapons: ["minecraft:diamond_sword"],
    skills: ["epicfight:katana_autoguard"],
    hand: "mainhand",
    conditions: [ ... ],

    // 阶段/冷却
    states: { phaselock, phase, max_phase, cooldown, global },

    // 事件定义 (两种形式: 命名事件字段 + 通用 events 数组)
    on_skill_start: { FxEffect },
    on_skill_end:   { FxEffect },
    on_hit:         [ FxEffect, ... ],
    on_guard:       { FxEffect },
    on_combo:       { FxEffect },
    on_charged:     { FxEffect },
    on_dodge:       { FxEffect },
    on_parry:       { FxEffect },
    on_kill:        { FxEffect },
    events: [ { trigger, FxEffect }, ... ]  // 通用
  }

  FxEffect (每个 event 的具体效果):
  ───────
  {
    trigger: "on_hit",               // 事件名
    fx: "photon:fire",               // 特效 ResourceLocation
    profile: "katana_slash",         // 引用配置档 (可选)
    position: "target",              // 位置: target/weapon/self/ground
    follow: false,                   // 是否跟随实体
    scale: 1.0,                      // 大小倍率
    duration: 20,                    // 持续 tick
    inherit_color: false,            // 继承武器附魔颜色
    conditions: [ ... ],             // 局部条件
    commands: [
      { type: "play_sound", sound, volume, pitch },
      { type: "camera_shake", intensity, duration },
      { type: "command", command: "say hi" },
      { type: "damage", fx_damage: 5.0 },
      { type: "set_phase", value: "2" },
      { type: "set_cooldown", value: "10" },
      { type: "increment_counter" },
      { type: "reset_counter" }
    ]
  }

  FxProfileData (data/*/fx_profiles/*.json):
  ─────────────
  {
    type: "epicfight_fx:profile",
    name: "katana_slash",
    fx: "photon:trail",              // 默认特效
    position: "weapon",              // 默认位置
    follow: true,                    // 默认跟随
    scale: 1.0, duration: 0, inherit_color: false,
    conditions: [ ... ],             // 全局条件
    commands: [ ... ],
    overrides: {
      "on_critical": { FxEffect, conditions: [ hit_type=critical ] },
      "on_guard_break": { FxEffect, conditions: [ ... ] }
    }
  }


十、命令系统 (FxCommand)
───────────────────────────────────────────────────────────────────────────────

  已实现:
    SPAWN_FX        播放特效 (定点/跟随)
    SPAWN_FX_BURST  爆发特效 (不跟随, 同 SPAWN_FX)
    PLAY_SOUND      播放音效 (全范围)
    COMMAND         以玩家身份执行命令
    DAMAGE          造成伤害
    SET_PHASE       设 linkage.states.phase
    SET_COOLDOWN    设 linkage.states.currentCooldown
    INCREMENT_COUNTER  phase++
    RESET_COUNTER    phase=0, cooldown=0


十一、与 EpicFight 的集成点
───────────────────────────────────────────────────────────────────────────────

  1. 事件系统 (PlayerEventListener, 12个):
     SKILL_CAST_EVENT
     SKILL_CANCEL_EVENT
     DODGE_SUCCESS_EVENT
     TAKE_DAMAGE_EVENT_ATTACK
     DEAL_DAMAGE_EVENT_ATTACK
     COMBO_COUNTER_HANDLE_EVENT
     BASIC_ATTACK_EVENT
     PLAYER_KILLED_EVENT
     ANIMATION_BEGIN_EVENT
     ATTACK_PHASE_END_EVENT
     ATTACK_ANIMATION_END_EVENT

  2. Forge 事件 (1个):
     EntityStunEvent → on_stun / on_knockdown

  3. Forge 事件 (1个):
     LivingHurtEvent → on_hit (通用, 非玩家可用)

  4. API 调用:
     EntityState.getLevel()           → 动画阶段
     EntityState.hurt()               → 眩晕检测
     EntityState.knockDown()          → 击倒检测
     LivingEntityPatch.isAirborneState() → 滞空检测
     LivingEntityPatch.getHoldingItemCapability() → 武器职业 (反射)
     SkillSlots.GUARD / isActivated() → 格挡检测
     EpicFightCapabilities.getEntityPatch() → Patch 获取

  5. Mixin 注入 (1个):
     MixinServerPlayerPatch → ServerPlayerPatch.onJoinWorld() @TAIL
       在玩家初始化完成后立即注册事件监听器


十二、CombatEvolution (CE) 兼容
───────────────────────────────────────────────────────────────────────────────

  CEExecutionCompat 模块:

    运行时检测 ModList.get().isLoaded("combat_evolution")
    无编译期依赖, CE 不存在时自动跳过。

    标记:
      combat_evolution:execution         → on_execution
      combat_evolution:execution_finished → on_kill


十三、待改进 / 已知限制
───────────────────────────────────────────────────────────────────────────────

  [条件] damageType 和 attackAngle 字段定义了但从未写入 MatchContext
  [命令] camera_shake / time_slow / beam / trail 等未实现
  [匹配] hand 字段 (主/副手) 未在 matchLinkage 中强制执行
  [匹配] 非玩家实体没有 PlayerEventListener, 不会收到 EpicFight 专有事件
         (但 on_hit 通过 LivingHurtEvent 仍然可用)
  [状态] isGuarding / isStunned / isKnockdown 是单次快照,
         不反映实时状态变化 (只在事件触发时采集)
  [Profile] overrides 的 conditions 使用独立 MatchContext,
           缺少完整上下文 (含 weaponCategories / skill / phase 等)
  [网络] PhotonNetworking.NETWORK.sendToAll 发送到所有玩家,
         未按距离/视野裁剪
================================================================================
