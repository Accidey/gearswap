# Gear Swap · Minecraft 1.20.1 (Forge)

Minecraft 装备预设模组：为每名玩家保存 3 套装备预设（主手、副手、头盔、胸甲、护腿、靴子），
用背包界面的按钮打开预设面板，或按住组合键（默认 `R`）再按 `1 / 2 / 3` 在战斗中整体换装。

Three equipment presets per player. Open the preset panel with the button next to the
inventory, or hold the selector key (default `R`) and press `1 / 2 / 3` to swap the whole
loadout in combat.

- Minecraft: `1.20.1`
- Loader: `Forge`
- Version: `1.1.0-1.20.1-Forge`

## 构建 / Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## 配置 / Configuration

Client config lives in `config/gearswap-client.toml`. `buttonPosition` accepts
`LEFT` / `TOP_LEFT` / `TOP_CENTER` / `TOP_RIGHT` / `RIGHT` and is applied without restarting.

## 许可 / License

[MIT](LICENSE)
