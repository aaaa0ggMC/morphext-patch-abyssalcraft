package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalActions;
import mchorse.metamorph.api.MorphManager;
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        registerActions();
        LOGGER.info("[morphext-abyssalcraft] Pre-initialized actions and abilities");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            SafeCleansingRitualHandler.install();
        }
        LOGGER.info("[morphext-abyssalcraft] Initialized");
    }

    private void registerActions() {
        MorphManager.INSTANCE.actions.put("ac_acid_spit", new AbyssalActions.AcidSpit());
        MorphManager.INSTANCE.actions.put("ac_shoggoth_ooze", new AbyssalActions.ShoggothOoze());
        MorphManager.INSTANCE.actions.put("ac_dread_charge", new AbyssalActions.DreadCharge());
        MorphManager.INSTANCE.actions.put("ac_dread_slug", new AbyssalActions.DreadSlug());
        MorphManager.INSTANCE.actions.put("ac_dread_barrage", new AbyssalActions.DreadBarrage());
        MorphManager.INSTANCE.actions.put("ac_dread_slam", new AbyssalActions.DreadSlam());
        MorphManager.INSTANCE.actions.put("ac_spectral_fireball", new AbyssalActions.SpectralFireball());
        MorphManager.INSTANCE.actions.put("ac_ghoul_pounce", new AbyssalActions.GhoulPounce());
        MorphManager.INSTANCE.actions.put("ac_coralium_squirt", new AbyssalActions.CoraliumSquirt());
        MorphManager.INSTANCE.actions.put("ac_remnant_screech", new AbyssalActions.RemnantScreech());
        MorphManager.INSTANCE.actions.put("ac_shadow_veil", new AbyssalActions.ShadowVeil());
        MorphManager.INSTANCE.actions.put("ac_shadow_collapse", new AbyssalActions.ShadowCollapse());
        MorphManager.INSTANCE.actions.put("ac_anti_implosion", new AbyssalActions.AntiImplosion());
        MorphManager.INSTANCE.actions.put("ac_jzahar_oblivion", new AbyssalActions.JzaharOblivion());
        MorphManager.INSTANCE.actions.put("ac_dread_roar", new AbyssalActions.DreadRoar());
        MorphManager.INSTANCE.actions.put("ac_shadow_step", new AbyssalActions.ShadowStep());
        MorphManager.INSTANCE.actions.put("ac_acid_melt", new AbyssalActions.AcidMelt());
        MorphManager.INSTANCE.actions.put("ac_anti_matter_blast", new AbyssalActions.AntimatterBlast());
    }
}
