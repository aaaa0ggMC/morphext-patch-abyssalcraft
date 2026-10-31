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
        registerActions();
        LOGGER.info("[morphext-abyssalcraft] Pre-initialized actions and abilities");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(MorphBossAdaptHandler.class);
        MinecraftForge.EVENT_BUS.register(EvilCowMilkAction.class);
        aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction.registerAdapter(new AbyssalBossMobActAdapter());
        registerAbyssalFactions();
        proxy.init(event);
        LOGGER.info("[morphext-abyssalcraft] Initialized boss adapt handler, mob act adapter, factions, and proxies");
    }

    private void registerAbyssalFactions() {
        try {
            aaaa0ggmc.morphext.faction.Faction abyssal = aaaa0ggmc.morphext.faction.FactionRegistry.get("abyssal");
            if (abyssal != null) {
                abyssal.addPredicate(e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityCoralium(e)
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityAbyssalZombie
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDepthsGhoul
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityCoraliumSquid);
            }

            aaaa0ggmc.morphext.faction.Faction dread = aaaa0ggmc.morphext.faction.FactionRegistry.get("dread");
            if (dread != null) {
                dread.addPredicate(e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityDread(e)
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadguard
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadling
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityDreadSpawn
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityChagaroth);
            }

            aaaa0ggmc.morphext.faction.Faction omothol = aaaa0ggmc.morphext.faction.FactionRegistry.get("omothol");
            if (omothol != null) {
                omothol.addPredicate(e -> e instanceof com.shinoow.abyssalcraft.api.entity.IOmotholEntity
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityRemnant
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityJzahar
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntitySacthoth);
            }

            aaaa0ggmc.morphext.faction.Faction shadow = aaaa0ggmc.morphext.faction.FactionRegistry.get("shadow");
            if (shadow != null) {
                shadow.addPredicate(e -> e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowMonster
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowCreature
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowBeast
                        || e instanceof com.shinoow.abyssalcraft.common.entity.EntityShadowTitan);
            }

            aaaa0ggmc.morphext.faction.Faction shoggoth = aaaa0ggmc.morphext.faction.FactionRegistry.get("shoggoth");
            if (shoggoth != null) {
                shoggoth.addPredicate(e -> e instanceof com.shinoow.abyssalcraft.common.entity.EntityLesserShoggoth
                        || (e != null && e.getClass().getName().toLowerCase().contains("shoggoth")));
            }

            aaaa0ggmc.morphext.faction.Faction antimatter = aaaa0ggmc.morphext.faction.FactionRegistry.get("antimatter");
            if (antimatter != null) {
                antimatter.addPredicate(e -> com.shinoow.abyssalcraft.api.entity.EntityUtil.isEntityAnti(e));
            }

            aaaa0ggmc.morphext.faction.Faction demon = aaaa0ggmc.morphext.faction.FactionRegistry.get("demon");
            if (demon != null) {
                demon.addPredicate(e -> e instanceof com.shinoow.abyssalcraft.common.entity.demon.EntityDemonAnimal
                        || e instanceof com.shinoow.abyssalcraft.common.entity.demon.EntityEvilAnimal);
            }
        } catch (Throwable t) {
            LOGGER.error("[morphext-abyssalcraft] Error registering abyssal faction predicates", t);
        }
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

        // 被动能力注册 (兼容 MorphExtension 与 Metamorph)
        aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhotophobiaPassive photophobia = new aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhotophobiaPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(photophobia);
        MorphManager.INSTANCE.abilities.put("ac_photophobia", photophobia);

        aaaa0ggmc.morphext.abyssalcraft.passive.DreadPlaguePassive dreadPlague = new aaaa0ggmc.morphext.abyssalcraft.passive.DreadPlaguePassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(dreadPlague);
        MorphManager.INSTANCE.abilities.put("ac_dread_plague", dreadPlague);

        aaaa0ggmc.morphext.abyssalcraft.passive.DreadgolemToughnessPassive dreadgolem = new aaaa0ggmc.morphext.abyssalcraft.passive.DreadgolemToughnessPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(dreadgolem);
        MorphManager.INSTANCE.abilities.put("ac_dreadgolem_toughness", dreadgolem);

        aaaa0ggmc.morphext.abyssalcraft.passive.AntimatterAnnihilationPassive antimatter = new aaaa0ggmc.morphext.abyssalcraft.passive.AntimatterAnnihilationPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(antimatter);
        MorphManager.INSTANCE.abilities.put("ac_antimatter_annihilation", antimatter);

        aaaa0ggmc.morphext.abyssalcraft.passive.ShoggothAcidPassive shoggothAcid = new aaaa0ggmc.morphext.abyssalcraft.passive.ShoggothAcidPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(shoggothAcid);
        MorphManager.INSTANCE.abilities.put("ac_shoggoth_acid", shoggothAcid);

        aaaa0ggmc.morphext.abyssalcraft.passive.CoraliumCarapacePassive coraliumCarapace = new aaaa0ggmc.morphext.abyssalcraft.passive.CoraliumCarapacePassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(coraliumCarapace);
        MorphManager.INSTANCE.abilities.put("ac_coralium_carapace", coraliumCarapace);

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

        // morph-abilities-audit E2 生物能力增补被动
        aaaa0ggmc.morphext.abyssalcraft.passive.ShoggothOozeTrailPassive oozeTrail = new aaaa0ggmc.morphext.abyssalcraft.passive.ShoggothOozeTrailPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(oozeTrail);
        MorphManager.INSTANCE.abilities.put("ac_shoggoth_ooze_trail", oozeTrail);

        aaaa0ggmc.morphext.abyssalcraft.passive.CoraliumContactPassive coraliumContact = new aaaa0ggmc.morphext.abyssalcraft.passive.CoraliumContactPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(coraliumContact);
        MorphManager.INSTANCE.abilities.put("ac_coralium_contact", coraliumContact);

        aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhasePassive shadowPhase = new aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhasePassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(shadowPhase);
        MorphManager.INSTANCE.abilities.put("ac_shadow_phase", shadowPhase);

        aaaa0ggmc.morphext.abyssalcraft.passive.AntiPredatorPassive antiPredator = new aaaa0ggmc.morphext.abyssalcraft.passive.AntiPredatorPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(antiPredator);
        MorphManager.INSTANCE.abilities.put("ac_anti_predator", antiPredator);
    }
}
