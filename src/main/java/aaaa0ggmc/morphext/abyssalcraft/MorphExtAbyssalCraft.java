package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalActions;
import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalBossActions;
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
        registerActions();
        LOGGER.info("[morphext-abyssalcraft] Pre-initialized actions and abilities");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(MorphBossAdaptHandler.class);
        MinecraftForge.EVENT_BUS.register(EvilCowMilkAction.class);
        aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction.registerAdapter(new AbyssalBossMobActAdapter());
        proxy.init(event);
        LOGGER.info("[morphext-abyssalcraft] Initialized boss adapt handler, mob act adapter, and proxies");
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
        MorphManager.INSTANCE.actions.put("ac_dread_charge", new AbyssalActions.DreadCharge());
        MorphManager.INSTANCE.actions.put("ac_dread_slug", new AbyssalActions.DreadSlug());
        MorphManager.INSTANCE.actions.put("ac_dread_barrage", new AbyssalActions.DreadBarrage());
        MorphManager.INSTANCE.actions.put("ac_dread_roar", new AbyssalActions.DreadRoar());

        // 反物质生物核心动作
        MorphManager.INSTANCE.actions.put("ac_antimatter_burst", new AbyssalActions.AntimatterBurstAction());
        MorphManager.INSTANCE.actions.put("ac_anti_creeper_fuse", new AbyssalActions.AntiCreeperAnnihilationAction());
        MorphManager.INSTANCE.actions.put("ac_anti_implosion", new AbyssalActions.AntiImplosion());
        MorphManager.INSTANCE.actions.put("ac_anti_matter_blast", new AbyssalActions.AntimatterBlast());

        // 珊瑚感染生物核心动作
        MorphManager.INSTANCE.actions.put("ac_coralium_ink", new AbyssalActions.CoraliumInkAction());
        MorphManager.INSTANCE.actions.put("ac_coralium_squirt", new AbyssalActions.CoraliumSquirt());
        MorphManager.INSTANCE.actions.put("ac_ghoul_pounce", new AbyssalActions.GhoulPounceAction());

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
        MorphManager.INSTANCE.actions.put("ac_jzahar_oblivion", new AbyssalBossActions.JzaharSingularity());

        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_flame", new AbyssalBossActions.SacthothShadowFlame());
        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_step", new AbyssalBossActions.SacthothShadowStep());

        MorphManager.INSTANCE.actions.put("ac_chagaroth_dread_salvo", new AbyssalBossActions.ChagarothDreadSalvo());
        MorphManager.INSTANCE.actions.put("ac_chagaroth_fist", new AbyssalBossActions.ChagarothFist());
        MorphManager.INSTANCE.actions.put("ac_chagaroth_spawn", new AbyssalBossActions.ChagarothSpawn());

        MorphManager.INSTANCE.actions.put("ac_dragon_breath", new AbyssalBossActions.DragonBossBreath());
        MorphManager.INSTANCE.actions.put("ac_dragon_boss_breath", new AbyssalBossActions.DragonBossBreath());
        MorphManager.INSTANCE.actions.put("ac_dragon_dive", new AbyssalBossActions.DragonBossDiveRush());
        MorphManager.INSTANCE.actions.put("ac_asorah_dive", new AbyssalBossActions.DragonBossDiveRush());

        // 暗影被动能力注册 (兼容 MorphExtension 与 Metamorph)
        aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhotophobiaPassive photophobia = new aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhotophobiaPassive();
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(photophobia);
        MorphManager.INSTANCE.abilities.put("ac_photophobia", photophobia);
    }
}
