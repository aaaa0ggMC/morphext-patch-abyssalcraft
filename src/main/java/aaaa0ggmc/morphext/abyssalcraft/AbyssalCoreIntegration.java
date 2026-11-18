package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.faction.Faction;
import aaaa0ggmc.morphext.faction.FactionRegistry;
import aaaa0ggmc.morphext.integration.metamorph.ImmunityAliasRegistry;
import mchorse.metamorph.api.events.RegisterRemapEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * AbyssalCraft 专属逻辑的<b>核心迁移承接类</b>（AGENTS.md §1.2）。
 *
 * <p>2026-11-07 之前，下列五类 AbyssalCraft 硬编码直接写在 morphext 核心里，
 * 违反「核心必须保持通用」：</p>
 * <ol>
 *   <li>{@code MorphImmunityHandler}：dread/coralium/antimatter/shadow/acid 伤害别名、
 *       形态名判失明、自伤伤害类型；</li>
 *   <li>{@code MetamorphEventHandler.onRegisterRemap}：5 条形态重映射；</li>
 *   <li>{@code MorphCreativeInjector.KNOWN_FALLBACKS}：greatershoggoth/demonschicken 回退表；</li>
 *   <li>{@code ModelBoneScanner}：abyssalnomicon/staff1 骨名关键词；</li>
 *   <li>{@code FactionRegistry}：abyssal/dread/omothol/shadow/shoggoth/antimatter/demon
 *       七个空阵营外壳。</li>
 * </ol>
 *
 * <p>现已全部迁到本类，经核心既有 SPI 注入（{@code ImmunityAliasRegistry} /
 * {@code FactionRegistry.registerPatchFaction} / {@code MorphCreativeInjector.registerFallback} /
 * {@code ModelBoneScanner.registerLeftKeyword|registerRightKeyword}），
 * 以及 Metamorph 自己的 {@link RegisterRemapEvent}——<b>不新增任何白名单</b>。
 * 核心迁移后不再出现第三方 modid/类名/形态 id 硬编码。</p>
 *
 * <p>全部注册都是「补数据」而非「改逻辑」：注册项与迁移前的硬编码逐条等价，
 * 因此行为不回退（见各类注释里的对照）。</p>
 */
public final class AbyssalCoreIntegration {

    private AbyssalCoreIntegration() {}

    /* ==================== 1. 阵营外壳（原 FactionRegistry 预留空壳） ==================== */

    /**
     * 注册 AbyssalCraft 七大阵营并挂谓词。
     *
     * <p>与原实现的差异：原核心在 {@code registerVanillaFactions()} 里
     * {@code register(new Faction(...))} 建空壳（无谓词、永不匹配任何实体），
     * patch 在 init 里 {@code FactionRegistry.get(id).addPredicate(...)} 补谓词。
     * 现由 patch 一次性建好带谓词的阵营，经
     * {@link FactionRegistry#registerPatchFaction} 注册——该方法同时补生成
     * {@code faction_<id>} 三级被动原型（原先由核心 registerAll 顺带生成），
     * 所以 patch 数据里授予的 {@code faction_shoggoth / faction_abyssal} 等不会落空。</p>
     *
     * <p>lang 键（{@code morphext.faction.<id>.name/.desc}）仍在核心 lang：
     * 那是翻译键而非 modid/类名/形态 id 硬编码，按 AGENTS.md 的 lang 迁移清单另行处理。</p>
     */
    public static void registerFactions(aaaa0ggmc.morphext.spi.MorphextPluginContext ctx) {
        try {
            registerFaction(ctx, "abyssal", new ItemStack(Blocks.OBSIDIAN),
                    e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityCoralium(e)
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityAbyssalZombie
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDepthsGhoul
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityCoraliumSquid);

            registerFaction(ctx, "dread", new ItemStack(Items.REDSTONE),
                    e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityDread(e)
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadguard
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadling
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadSpawn
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityChagaroth);

            registerFaction(ctx, "omothol", new ItemStack(Items.NETHER_STAR),
                    e -> e instanceof com.shinoow.abyssalcraft.api.entity.IOmotholEntity
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityRemnant
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityJzahar
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntitySacthoth);

            registerFaction(ctx, "shadow", new ItemStack(Items.COAL),
                    e -> e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowMonster
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowCreature
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowBeast
                            || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowTitan);

            registerFaction(ctx, "shoggoth", new ItemStack(Items.SLIME_BALL),
                    e -> e instanceof com.shinoow.abyssalcraft.common.entity.EntityLesserShoggoth
                            || (e != null && e.getClass().getName().toLowerCase().contains("shoggoth")));

            registerFaction(ctx, "antimatter", new ItemStack(Items.NETHERBRICK),
                    e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityAnti(e));

            registerFaction(ctx, "demon", new ItemStack(Items.MAGMA_CREAM),
                    e -> e instanceof com.shinoow.abyssalcraft.common.entity.demon.EntityDemonAnimal
                            || e instanceof com.shinoow.abyssalcraft.common.entity.demon.EntityEvilAnimal);
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Error registering abyssal factions", t);
        }
    }

    private static void registerFaction(aaaa0ggmc.morphext.spi.MorphextPluginContext ctx,
                                        String id, ItemStack icon,
                                        java.util.function.Predicate<net.minecraft.entity.EntityLivingBase> predicate) {
        Faction faction = new Faction(id,
                "morphext.faction." + id + ".name",
                "morphext.faction." + id + ".desc",
                icon);
        faction.addPredicate(predicate);
        ctx.registerFaction(faction);
    }

    /* ==================== 2. 免疫别名（原 MorphImmunityHandler 硬编码） ==================== */

    /**
     * 注册 AbyssalCraft 的免疫别名互通组、形态名特权药水与自伤伤害类型。
     *
     * <p>与原硬编码逐条对照：</p>
     * <table>
     *   <tr><th>原核心硬编码</th><th>现注册</th></tr>
     *   <tr><td>苦力怕系 {@code dread/dplague} 药水别名互通</td><td rowspan="3">三组别名组</td></tr>
     *   <tr><td>苦力怕系 {@code coralium/cplague} 药水别名互通</td></tr>
     *   <tr><td>苦力怕系 {@code antimatter} 药水别名互通</td></tr>
     *   <tr><td>修格斯酸液 {@code acid/shoggoth_acid} 伤害互通</td><td rowspan="3">五组别名组</td></tr>
     *   <tr><td>苦力怕系 {@code dread/coralium/antimatter/shadow} 伤害互通</td></tr>
     *   <tr><td>（{@code acid} 单独成组，覆盖 {@code shoggoth_acid} 拼写）</td></tr>
     *   <tr><td>暗影形态强制免疫失明（防 Sacthoth 30 格光环反噬）</td><td>形态名特权药水</td></tr>
     *   <tr><td>自伤豁免 {@code dread/acid/coralium/antimatter/shadow}</td><td>五个自伤伤害类型</td></tr>
     * </table>
     *
     * <p><b>为什么 {@code acid} 与 {@code shoggoth_acid} 同组而 {@code dread} 与
     * {@code dplague} 同组</b>：原代码里酸液是「{@code acid} 或 {@code shoggoth_acid}」
     * 双侧任一命中，其余是「声明侧含 X 且来源侧含 Y」的固定搭配。用别名组表达时，
     * {@code acid/shoggoth_acid} 合成一组即可同时覆盖两种拼写；{@code dread/dplague}
     * 合成一组后，声明写 {@code dread}、来源叫 {@code dplague}（或反过来）都互通，
     * 比原实现的单向搭配更对称，但不会让原本不免疫的组合变成免疫
     * （原实现两侧都要求含组内成员，语义一致）。</p>
     */
    public static void registerImmunityAliases() {
        try {
            // 药水别名互通（原 isImmuneToPotion 的三组硬编码）
            ImmunityAliasRegistry.registerAliasGroup("dread", "dplague");
            ImmunityAliasRegistry.registerAliasGroup("coralium", "cplague");
            ImmunityAliasRegistry.registerAliasGroup("antimatter");

            // 伤害别名互通（原 isImmuneToDamage 的五组硬编码）
            ImmunityAliasRegistry.registerAliasGroup("acid", "shoggoth_acid");
            ImmunityAliasRegistry.registerAliasGroup("dread");
            ImmunityAliasRegistry.registerAliasGroup("coralium");
            ImmunityAliasRegistry.registerAliasGroup("antimatter");
            ImmunityAliasRegistry.registerAliasGroup("shadow");

            // 形态名特权药水免疫（原「暗影/萨克索斯/扎哈尔形态免疫失明」硬编码）
            ImmunityAliasRegistry.registerMorphNamePotionGuard("shadow", net.minecraft.init.MobEffects.BLINDNESS);
            ImmunityAliasRegistry.registerMorphNamePotionGuard("sacthoth", net.minecraft.init.MobEffects.BLINDNESS);
            ImmunityAliasRegistry.registerMorphNamePotionGuard("jzahar", net.minecraft.init.MobEffects.BLINDNESS);

            // 自伤豁免伤害类型（原 isImmuneToDamage 自伤分支的五个子串）
            for (String type : new String[]{"dread", "acid", "coralium", "antimatter", "shadow"}) {
                ImmunityAliasRegistry.registerSelfHitDamageType(type);
            }
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Error registering immunity aliases", t);
        }
    }

    /* ==================== 3. 形态重映射（原 MetamorphEventHandler 硬编码） ==================== */

    /**
     * Metamorph 的 {@link RegisterRemapEvent} 订阅：注入 AbyssalCraft 形态重映射。
     *
     * <p>与原核心硬编码逐条等价（5 条，修格斯阶数 → lessershoggoth、
     * demonschicken → demonchicken）。之所以能直接订阅 Metamorph 的事件而不经核心中转：
     * 核心原先也是订阅同一个事件、往同一张 {@code event.map} 里 put，
     * patch 订阅同一个事件做同样的 put，效果相同且少一层核心耦合。</p>
     *
     * <p>注册方式：{@code MinecraftForge.EVENT_BUS.register(new AbyssalCoreIntegration())}
     * （见 {@link MorphExtAbyssalCraft#init}）。</p>
     */
    @SubscribeEvent
    public static void onRegisterRemap(RegisterRemapEvent event) {
        if (event == null || event.map == null) {
            return;
        }
        // 修格斯阶数重映射至底层实体 lessershoggoth
        event.map.put("abyssalcraft:greatershoggoth", "abyssalcraft:lessershoggoth");
        event.map.put("abyssalcraft:shoggoth", "abyssalcraft:lessershoggoth");
        event.map.put("abyssalcraft.greatershoggoth", "abyssalcraft:lessershoggoth");
        event.map.put("abyssalcraft.shoggoth", "abyssalcraft:lessershoggoth");
        event.map.put("abyssalcraft:demonschicken", "abyssalcraft:demonchicken");
    }

    /* ==================== 4. 创造选单回退（原 MorphCreativeInjector 硬编码） ==================== */

    /**
     * 注册 AbyssalCraft 的「虚拟变种 → 宿主基础实体」回退表。
     *
     * <p>与原核心静态块里的 5 条逐条等价（含 defaultScale：greatershoggoth 1.85、
     * shoggoth 1.25、demonschicken 1.0）。AbyssalCraft 1.10.3 把修格斯阶数统一依托
     * {@code EntityLesserShoggoth} 的渲染模型，demonschicken 依托 demonchicken。</p>
     */
    public static void registerCreativeFallbacks() {
        try {
            aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerFallback(
                    "abyssalcraft:greatershoggoth", "abyssalcraft:lessershoggoth", 1.85F);
            aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerFallback(
                    "abyssalcraft:shoggoth", "abyssalcraft:lessershoggoth", 1.25F);
            aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerFallback(
                    "abyssalcraft.greatershoggoth", "abyssalcraft:lessershoggoth", 1.85F);
            aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerFallback(
                    "abyssalcraft.shoggoth", "abyssalcraft:lessershoggoth", 1.25F);
            aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerFallback(
                    "abyssalcraft:demonschicken", "abyssalcraft:demonchicken", 1.0F);
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Error registering creative fallbacks", t);
        }
    }

    /* ==================== 5. 模型骨名关键词（原 ModelBoneScanner 硬编码） ==================== */

    /**
     * 注册 AbyssalCraft 模型的骨名关键词（J'zahar 右手法杖 / 左手死灵之书）。
     *
     * <p>与原核心词典里的两条等价（权重均为 35）。注意这只是<b>兜底路径</b>：
     * {@code GenericHandRegistry} 里已有 {@code ModelJzahar → HandPreset("staff1","abyssalnomicon")}
     * 的显式预设（见 patch 的 ClientProxy），预设命中时不会走到启发式扫描；
     * 这两个关键词覆盖的是「没有预设、只靠启发式」的场景，迁走后两边行为一致。</p>
     *
     * <p>客户端调用：{@code ModelBoneScanner} 在 {@code client.hand} 包，只能由客户端代理触发。</p>
     */
    public static void registerBoneKeywords() {
        try {
            aaaa0ggmc.morphext.client.hand.ModelBoneScanner.registerRightKeyword("staff1", 35);
            aaaa0ggmc.morphext.client.hand.ModelBoneScanner.registerLeftKeyword("abyssalnomicon", 35);
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Error registering bone keywords", t);
        }
    }
}
