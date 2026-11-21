# Gear Swap

Minecraft 装备预设模组：为每名玩家保存 3 套装备预设（主手、副手、头盔、胸甲、护腿、靴子），
用背包界面左侧的按钮打开预设面板，或按住组合键（默认 `R`）再按 `1 / 2 / 3` 在战斗中整体换装。
装备始终回到自己所属的预设格，多套预设之间不会串装。

Three equipment presets per player (main hand, offhand, helmet, chestplate, leggings, boots).
Open the preset panel with the button next to the inventory, or hold the selector key
(default `R`) and press `1 / 2 / 3` to swap the whole loadout in combat. Items always return
to their own preset slot, so loadouts never mix.

## 分支 / Branches

| 分支 / Branch | Minecraft | 加载器 / Loader |
| --- | --- | --- |
| `1.20.1-Forge` | 1.20.1 | Forge |
| `1.21.1-NeoForge` | 1.21.1 | NeoForge |
| `26.2-NeoForge` | 26.2 | NeoForge |
| `26.3-NeoForge` | 26.3 | NeoForge |

每个分支包含对应版本的完整源码、资源与构建脚本；直接 `./gradlew build` 即可。
Each branch contains the full source, resources and build scripts for that version.

## 配置 / Configuration

客户端配置位于 `config/gearswap-client.toml`，支持调整背包界面按钮位置：

- `LEFT` 左侧、`TOP_LEFT` 左上、`TOP_CENTER` 中上、`TOP_RIGHT` 右上、`RIGHT` 右侧（默认 `LEFT`）。

改完即时生效，无需重启。Changes apply immediately without restarting.

## 许可 / License

[MIT](LICENSE)
