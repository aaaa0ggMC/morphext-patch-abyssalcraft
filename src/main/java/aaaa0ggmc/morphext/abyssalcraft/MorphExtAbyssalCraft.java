package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalActions;
import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalBossActions;
import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalExtraActions;
import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalExtraActions2;
import aaaa0ggmc.morphext.abyssalcraft.action.AntiChickenEggAction;
import aaaa0ggmc.morphext.abyssalcraft.action.AntiCowMilkAction;
import aaaa0ggmc.morphext.abyssalcraft.action.DemonCowMilkAction;
import aaaa0ggmc.morphext.abyssalcraft.action.DemonSheepShearAction;
import aaaa0ggmc.morphext.abyssalcraft.action.EvilCowMilkAction;
import mchorse.metamorph.api.MorphManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = MorphExtAbyssalCraft.MODID,
        name = MorphExtAbyssalCraft.NAME,
        version = MorphExtAbyssalCraft.VERSION,
        dependencies = "required-after:aaaa0ggmc_morph_extension;required-after:abyssalcraft;required-after:metamorph"
)
public class MorphExtAbyssalCraft {
    public static final String MODID = "morphext_patch_abyssalcraft";
    public static final String NAME = "Morph Extension Patch: AbyssalCraft";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MorphExtAbyssalCraft.MODID)
    public static MorphExtAbyssalCraft instance;

    @net.minecraftforge.fml.common.SidedProxy(
            clientSide = "aaaa0ggmc.morphext.abyssalcraft.proxy.ClientProxy",
            serverSide = "aaaa0ggmc.morphext.abyssalcraft.proxy.CommonProxy"
    )
    public static aaaa0ggmc.morphext.abyssalcraft.proxy.CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        // 确保核心动作调度器已挂载（ac_dread_barf 等引导技依赖；幂等，重复调用无副作用）
        aaaa0ggmc.morphext.integration.metamorph.vanilla.ServerActionScheduler.init();
        /*
         * AGENTS.md §1.2 迁移承接：原先写在 morphext 核心里的 AbyssalCraft 专属逻辑，
         * 现在由 patch 在最早的初始化阶段经核心 SPI 注册进来（阵营 / 免疫别名）。
         * 放在 preInit（而非原先的 init）是为了让阵营在任何实体关系解析之前就位——
         * registerPatchFaction 内部会失效图的解析缓存，但早注册更稳。
         */
        AbyssalCoreIntegration.registerImmunityAliases();
        registerActions();
        LOGGER.info("[morphext-abyssalcraft] Pre-initialized actions and immunity aliases");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(EvilCowMilkAction.class);
        /*
         * Metamorph 形态重映射（原核心 MetamorphEventHandler.onRegisterRemap 的 5 条
         * AbyssalCraft 硬编码）：patch 直接订阅 Metamorph 自己的 RegisterRemapEvent，
         * 往同一张 event.map 里注入，不经过核心中转。
         */
        MinecraftForge.EVENT_BUS.register(AbyssalCoreIntegration.class);
        proxy.init(event);
        LOGGER.info("[morphext-abyssalcraft] Initialized boss adapt handler, remap, and proxies");
    }

    private void registerActions() {
        // 深渊特殊动物联动动作
        MorphManager.INSTANCE.actions.put("ac_anti_cow_milk", new AntiCowMilkAction());
        MorphManager.INSTANCE.actions.put("ac_demon_cow_milk", new DemonCowMilkAction());
        MorphManager.INSTANCE.actions.put("ac_evil_cow_milk", new EvilCowMilkAction());
        MorphManager.INSTANCE.actions.put("ac_anti_chicken_egg", new AntiChickenEggAction());
        MorphManager.INSTANCE.actions.put("ac_demon_sheep_shear", new DemonSheepShearAction());

        // 暗影生物核心动作
        MorphManager.INSTANCE.actions.put("ac_shadow_step", new AbyssalActions.ShadowStepAction());
        MorphManager.INSTANCE.actions.put("ac_shadow_slash", new AbyssalActions.ShadowSlashAction());
        MorphManager.INSTANCE.actions.put("ac_shadow_veil", new AbyssalActions.ShadowVeil());
        MorphManager.INSTANCE.actions.put("ac_shadow_collapse", new AbyssalActions.ShadowCollapse());

        // 恐惧生物核心动作
        MorphManager.INSTANCE.actions.put("ac_dread_slam", new AbyssalActions.DreadSlamAction());
        MorphManager.INSTANCE.actions.put("ac_dread_katana", new AbyssalActions.DreadKatanaAction());
        MorphManager.INSTANCE.actions.put("ac_dread_charge", new AbyssalActions.DreadCharge());
        MorphManager.INSTANCE.actions.put("ac_dread_slug", new AbyssalActions.DreadSlug());
        MorphManager.INSTANCE.actions.put("ac_dread_barrage", new AbyssalActions.DreadBarrage());
        MorphManager.INSTANCE.actions.put("ac_dread_roar", new AbyssalActions.DreadRoar());

        // 反物质生物核心动作
        MorphManager.INSTANCE.actions.put("ac_anti_player_slash", new AbyssalActions.AntiPlayerSlashAction());
        MorphManager.INSTANCE.actions.put("ac_antimatter_burst", new AbyssalActions.AntimatterBurstAction());
        MorphManager.INSTANCE.actions.put("ac_anti_creeper_fuse", new AbyssalActions.AntiCreeperAnnihilationAction());
        MorphManager.INSTANCE.actions.put("ac_anti_implosion", new AbyssalActions.AntiImplosion());
        MorphManager.INSTANCE.actions.put("ac_anti_matter_blast", new AbyssalActions.AntimatterBlast());
        MorphManager.INSTANCE.actions.put("ac_anti_spider_leap", new AbyssalActions.AntiSpiderLeapAction());
        MorphManager.INSTANCE.actions.put("ac_anti_skeleton_arrow", new AbyssalActions.AntiSkeletonArrowAction());

        // 远古歌利亚与傀儡
        MorphManager.INSTANCE.actions.put("ac_goliath_smash", new AbyssalActions.GoliathSmashAction());

        // 邪恶动物突进
        MorphManager.INSTANCE.actions.put("ac_evil_charge", new AbyssalActions.EvilChargeAction());

        // 珊瑚感染生物核心动作
        MorphManager.INSTANCE.actions.put("ac_coralium_ink", new AbyssalActions.CoraliumInkAction());
        MorphManager.INSTANCE.actions.put("ac_coralium_squirt", new AbyssalActions.CoraliumSquirt());
        MorphManager.INSTANCE.actions.put("ac_coralium_ink_storm", new AbyssalActions.CoraliumInkStormAction());
        MorphManager.INSTANCE.actions.put("ac_ghoul_pounce", new AbyssalActions.GhoulPounceAction());
        MorphManager.INSTANCE.actions.put("ac_ghoul_underwater_ambush", new AbyssalActions.GhoulUnderwaterAmbushAction());
        MorphManager.INSTANCE.actions.put("ac_coralium_claws", new AbyssalActions.CoraliumClawsAction());

        // 恶魔动物地狱机制
        MorphManager.INSTANCE.actions.put("ac_demon_fire_charge", new AbyssalActions.DemonFireChargeAction());

        // 修格斯与遗民动作
        MorphManager.INSTANCE.actions.put("ac_acid_spit", new AbyssalActions.AcidSpit());
        MorphManager.INSTANCE.actions.put("ac_shoggoth_ooze", new AbyssalActions.ShoggothOoze());
        MorphManager.INSTANCE.actions.put("ac_acid_melt", new AbyssalActions.AcidMelt());
        MorphManager.INSTANCE.actions.put("ac_remnant_screech", new AbyssalActions.RemnantScreech());
        MorphManager.INSTANCE.actions.put("ac_spectral_fireball", new AbyssalActions.SpectralFireball());

        // 深渊 Boss 原生机制复用动作
        MorphManager.INSTANCE.actions.put("ac_jzahar_wither_salvo", new AbyssalBossActions.JzaharWitherSalvo());
        MorphManager.INSTANCE.actions.put("ac_jzahar_shout_blast", new AbyssalBossActions.JzaharShoutBlast());
        MorphManager.INSTANCE.actions.put("ac_jzahar_singularity", new AbyssalBossActions.JzaharSingularity());
        MorphManager.INSTANCE.actions.put("ac_jzahar_earthquake", new AbyssalBossActions.JzaharEarthquake());
        // NEW#4 绑定修复：ac_jzahar_oblivion 改绑真·终焉 AOE（原与 singularity 同类属重复注册）
        MorphManager.INSTANCE.actions.put("ac_jzahar_oblivion", new AbyssalActions.JzaharOblivion());

        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_flame", new AbyssalBossActions.SacthothShadowFlame());
        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_step", new AbyssalBossActions.SacthothShadowStep());

        MorphManager.INSTANCE.actions.put("ac_chagaroth_dread_salvo", new AbyssalBossActions.ChagarothDreadSalvo());
        MorphManager.INSTANCE.actions.put("ac_chagaroth_fist", new AbyssalBossActions.ChagarothFist());
        MorphManager.INSTANCE.actions.put("ac_chagaroth_spawn", new AbyssalBossActions.ChagarothSpawn());

        MorphManager.INSTANCE.actions.put("ac_dragon_breath", new AbyssalBossActions.DragonBossBreath());
        MorphManager.INSTANCE.actions.put("ac_dragon_boss_breath", new AbyssalBossActions.DragonBossBreath());
        MorphManager.INSTANCE.actions.put("ac_dragon_dive", new AbyssalBossActions.DragonBossDiveRush());
        MorphManager.INSTANCE.actions.put("ac_asorah_dive", new AbyssalBossActions.DragonBossDiveRush());

        // morph-abilities-audit NEW 需求扩展动作
        MorphManager.INSTANCE.actions.put("ac_dread_barf", new AbyssalExtraActions.DreadBarfAction());
        MorphManager.INSTANCE.actions.put("ac_omothol_touch", new AbyssalExtraActions.OmotholTouchAction());
        MorphManager.INSTANCE.actions.put("ac_sacthoth_dark_aura", new AbyssalExtraActions.SacthothDarkAuraAction());
        MorphManager.INSTANCE.actions.put("ac_anti_door_break", new AbyssalExtraActions.AntiDoorBreakAction());
        MorphManager.INSTANCE.actions.put("ac_evil_demonize", new AbyssalExtraActions.EvilDemonizeAction());
        MorphManager.INSTANCE.actions.put("ac_evil_animal_shear", new AbyssalExtraActions.EvilAnimalShearAction());
        MorphManager.INSTANCE.actions.put("ac_shoggoth_feast", new AbyssalExtraActions.ShoggothFeastAction());
        MorphManager.INSTANCE.actions.put("ac_dread_split", new AbyssalExtraActions.DreadSplitAction());
        MorphManager.INSTANCE.actions.put("ac_dread_merge", new AbyssalExtraActions.DreadMergeAction());
        MorphManager.INSTANCE.actions.put("ac_remnant_rally", new AbyssalExtraActions.RemnantRallyAction());

        // morph-abilities-audit E2 生物能力增补动作（AbyssalExtraActions2）
        MorphManager.INSTANCE.actions.put("ac_ghoul_rake", new AbyssalExtraActions2.GhoulRakeAction());
        MorphManager.INSTANCE.actions.put("ac_ghoul_immolate", new AbyssalExtraActions2.GhoulImmolateAction());
        MorphManager.INSTANCE.actions.put("ac_dread_contagion", new AbyssalExtraActions2.DreadContagionAction());
        MorphManager.INSTANCE.actions.put("ac_dragon_gale", new AbyssalExtraActions2.DragonGaleAction());
        MorphManager.INSTANCE.actions.put("ac_chagaroth_legion", new AbyssalExtraActions2.ChagarothLegionAction());
        MorphManager.INSTANCE.actions.put("ac_shadow_crush", new AbyssalExtraActions2.ShadowCrushAction());
        MorphManager.INSTANCE.actions.put("ac_anti_reinforce", new AbyssalExtraActions2.AntiReinforceAction());
        MorphManager.INSTANCE.actions.put("ac_anti_flame_arrow", new AbyssalExtraActions2.AntiFlameArrowAction());
        MorphManager.INSTANCE.actions.put("ac_anti_overcharge", new AbyssalExtraActions2.AntiOverchargeAction());
        MorphManager.INSTANCE.actions.put("ac_anti_matter_rage", new AbyssalExtraActions2.AntimatterRageAction());
        MorphManager.INSTANCE.actions.put("ac_demon_frenzy", new AbyssalExtraActions2.DemonFrenzyAction());
        MorphManager.INSTANCE.actions.put("ac_remnant_blink", new AbyssalExtraActions2.RemnantBlinkAction());
        MorphManager.INSTANCE.actions.put("ac_jzahar_tremor_field", new AbyssalExtraActions2.JzaharTremorFieldAction());
        MorphManager.INSTANCE.actions.put("ac_shub_bleat", new AbyssalExtraActions2.ShubBleatAction());
        MorphManager.INSTANCE.actions.put("ac_gatekeeper_crush", new AbyssalExtraActions2.GatekeeperCrushAction());
        MorphManager.INSTANCE.actions.put("ac_shoggoth_crush", new AbyssalExtraActions2.ShoggothCrushAction());
    }
}
