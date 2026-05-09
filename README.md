# Photon & EpicFight 集成模组

在 Photon 粒子编辑器中集成 EpicFight 骨骼动画预览与特效骨骼绑定的 Forge 模组。

## 用途

本模组为 [Photon VFX 编辑器](https://github.com/Low-Drag-MC/Photon) 添加 EpicFight 骨骼动画支持，让特效制作者可以：

- 在粒子编辑器中加载玩家克隆体，实时预览 EpicFight 动画
- 将编辑中的特效绑定到指定骨骼上（位置 + 旋转实时跟随）
- 在游戏内通过指令将特效绑定到玩家或实体的骨骼上

## 功能

### 编辑器集成

- **动画控制面板** — 在编辑器 View 菜单中开启「动画控制」，可搜索并播放 EpicFight 动画
- **骨骼绑定输入框** — 输入骨骼名称（如 `Root`、`Chest`、`Head`），编辑器中的特效会实时跟随该骨骼的位置和旋转
- **骨骼名称自动补全** — 输入时从 Armature 反射获取所有骨骼名，模糊匹配下拉提示，支持滚轮翻页
- **玩家实时同步** — 克隆体每帧同步玩家装备、血量、吸收值
- **预览开关** — 通过指令 `/photon_preview` 控制是否启用 EpicFight 预览

### 游戏内指令

- **`/bone play <namespace> <path> [boneName]`** — 在指定骨骼上播放特效（默认 `Tool_R`）
- **`/bone stop [boneName] [namespace] [path]`** — 停止指定骨骼上的特效
- **`/bone list`** — 列出所有可用特效
- **`/photon_preview`** — 开关编辑器 EpicFight 预览模式

所有指令均支持 Tab 补全（命名空间、路径、骨骼名称）。

### 动画驱动

- 使用 `AnimationPlayer.tick()` 自然驱动动画时间推进，`setHardPause(true)` 防止 ClientAnimator 干扰
- 绕过 `modifyPose()` / `correctRootJoint()`，直接读取 `getRawPose()` 保留 Root 骨骼位移
- 动画播完后自动复位实体位置

## 使用方法

### 编辑器预览

1. 在游戏中输入 `/photon_preview` 开启预览（再次输入关闭）
2. 打开 Photon 粒子编辑器（`/pe`）
3. 在 View 菜单中打开「动画控制」悬浮窗
4. 输入动画 ResourceLocation 并点击播放
5. 在骨骼输入框中输入骨骼名称（如 `Root`、`Chest`），特效会跟随骨骼运动

### 指令绑定

```mcfunction
# 在 Tool_R 骨骼上播放 epicfight 命名空间的 test 特效
/bone play epicfight test Tool_R

# 在 Chest 骨骼上播放
/bone play epicfight test Chest

# 停止 Chest 上的所有特效
/bone stop Chest

# 停止 Chest 上的指定特效
/bone stop Chest epicfight test

# 停止所有骨骼特效
/bone stop

# 列出可用特效
/bone list
```

## 依赖

- [Photon](https://github.com/Low-Drag-MC/Photon) — VFX 编辑器
- [EpicFight](https://github.com/Yesssman/EpicFightMod) — 骨骼动画系统
- LDLib — Photon 依赖的 UI 库
- Minecraft Forge 1.20.1
