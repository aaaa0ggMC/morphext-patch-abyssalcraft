# Morph Extension Patch: AbyssalCraft

> [!WARNING]
> **免责声明 / Disclaimer**
> 
> 本项目处于 WIP（Work In Progress）阶段。作者不对本模组当前以及将来的质量、稳定性、可用性做任何形式的明示或暗示保证与担保，代码可能包含 AI 生成内容（AI Slop）。请在测试环境中评估并自行承担使用风险。
> 
> This project is a Work In Progress (WIP). The author makes NO warranties or guarantees of any kind regarding the quality, stability, or fitness for any purpose of this mod now or in the future. The codebase may contain AI-generated slop. Use strictly at your own risk.

## 简介

为 Minecraft 1.12.2 深渊国度（AbyssalCraft）生物提供 Metamorph 与 Morph Extension 能力扩展与免疫补丁。
- 采用 **依赖注入 (DI)** 与 **数据驱动 (JSON)** 设计；
- 赋予深渊国度各个生物形态独特的主动能力（`<前缀>_<能力>` 如 `ac_acid_spit`, `ac_dread_charge`, `ac_shadow_veil` 等）；
- 自动为形态赋予原生效能与负面抗性（如恐惧生物免疫恐惧瘟疫、珊瑚生物免疫珊瑚瘟疫、反物质生物免疫反物质等）；
- 消除原有部分技能硬编码检测玩家的缺陷，射线检测与范围判定面向任意生命实体（`EntityLivingBase`）。
