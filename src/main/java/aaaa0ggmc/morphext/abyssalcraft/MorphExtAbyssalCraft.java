package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalActions;
import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalBossActions;
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
        aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction.registerAdapter(new AbyssalBossMobActAdapter());
        proxy.init(event);
        LOGGER.info("[morphext-abyssalcraft] Initialized boss adapt handler, mob act adapter, and proxies");
    }

    private void registerActions() {
        // 深渊普通生物核心动作
        MorphManager.INSTANCE.actions.put("ac_acid_spit", new AbyssalActions.AcidSpit());
        MorphManager.INSTANCE.actions.put("ac_shoggoth_ooze", new AbyssalActions.ShoggothOoze());
        MorphManager.INSTANCE.actions.put("ac_dread_charge", new AbyssalActions.DreadCharge());
        MorphManager.INSTANCE.actions.put("ac_dread_slug", new AbyssalActions.DreadSlug());
        MorphManager.INSTANCE.actions.put("ac_dread_slam", new AbyssalActions.DreadSlam());
        MorphManager.INSTANCE.actions.put("ac_spectral_fireball", new AbyssalActions.SpectralFireball());
        MorphManager.INSTANCE.actions.put("ac_ghoul_pounce", new AbyssalActions.GhoulPounce());
        MorphManager.INSTANCE.actions.put("ac_coralium_squirt", new AbyssalActions.CoraliumSquirt());
        MorphManager.INSTANCE.actions.put("ac_remnant_screech", new AbyssalActions.RemnantScreech());
        MorphManager.INSTANCE.actions.put("ac_shadow_veil", new AbyssalActions.ShadowVeil());
        MorphManager.INSTANCE.actions.put("ac_anti_implosion", new AbyssalActions.AntiImplosion());
        MorphManager.INSTANCE.actions.put("ac_dread_roar", new AbyssalActions.DreadRoar());
        MorphManager.INSTANCE.actions.put("ac_acid_melt", new AbyssalActions.AcidMelt());
        MorphManager.INSTANCE.actions.put("ac_anti_matter_blast", new AbyssalActions.AntimatterBlast());

        // 深渊 Boss 原生机制复用动作 (直接调用原生代码与骨骼动画)
        MorphManager.INSTANCE.actions.put("ac_jzahar_wither_salvo", new AbyssalBossActions.JzaharWitherSalvo());
        MorphManager.INSTANCE.actions.put("ac_jzahar_shout_blast", new AbyssalBossActions.JzaharShoutBlast());
        MorphManager.INSTANCE.actions.put("ac_jzahar_singularity", new AbyssalBossActions.JzaharSingularity());
        MorphManager.INSTANCE.actions.put("ac_jzahar_earthquake", new AbyssalBossActions.JzaharEarthquake());

        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_flame", new AbyssalBossActions.SacthothShadowFlame());
        MorphManager.INSTANCE.actions.put("ac_sacthoth_shadow_step", new AbyssalBossActions.SacthothShadowStep());

        MorphManager.INSTANCE.actions.put("ac_chagaroth_dread_salvo", new AbyssalBossActions.ChagarothDreadSalvo());
        MorphManager.INSTANCE.actions.put("ac_dragon_boss_breath", new AbyssalBossActions.DragonBossBreath());

        // 兼容旧标识映射（指向全新原生实现）
        MorphManager.INSTANCE.actions.put("ac_jzahar_oblivion", new AbyssalBossActions.JzaharSingularity());
        MorphManager.INSTANCE.actions.put("ac_shadow_collapse", new AbyssalBossActions.SacthothShadowFlame());
        MorphManager.INSTANCE.actions.put("ac_shadow_step", new AbyssalBossActions.SacthothShadowStep());
        MorphManager.INSTANCE.actions.put("ac_dread_barrage", new AbyssalBossActions.ChagarothDreadSalvo());
    }
}
