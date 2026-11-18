package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.ability.JzaharBanishAbility;
import aaaa0ggmc.morphext.abyssalcraft.ability.JzaharBanishServerAbility;
import aaaa0ggmc.morphext.abyssalcraft.passive.*;
import aaaa0ggmc.morphext.spi.IMorphextPlugin;
import aaaa0ggmc.morphext.spi.MorphextPluginContext;
import mchorse.metamorph.api.MorphManager;

/**
 * AbyssalCraft 适配补丁 SPI 插件入口（ServiceLoader 自动发现）。
 */
public final class AbyssalMorphextPlugin implements IMorphextPlugin {

    @Override
    public String id() {
        return MorphExtAbyssalCraft.MODID;
    }

    @Override
    public void onPreInit(MorphextPluginContext ctx) {
        AbyssalCoreIntegration.registerFactions(ctx);
        registerPassive(ctx, "ac_photophobia", new ShadowPhotophobiaPassive());
        registerPassive(ctx, "ac_dread_plague", new DreadPlaguePassive());
        registerPassive(ctx, "ac_dreadgolem_toughness", new DreadgolemToughnessPassive());
        registerPassive(ctx, "ac_antimatter_annihilation", new AntimatterAnnihilationPassive());
        registerPassive(ctx, "ac_shoggoth_acid", new ShoggothAcidPassive());
        registerPassive(ctx, "ac_coralium_carapace", new CoraliumCarapacePassive());
        registerPassive(ctx, "ac_shoggoth_ooze_trail", new ShoggothOozeTrailPassive());
        registerPassive(ctx, "ac_coralium_contact", new CoraliumContactPassive());
        registerPassive(ctx, "ac_shadow_phase", new ShadowPhasePassive());
        registerPassive(ctx, "ac_anti_predator", new AntiPredatorPassive());

        ctx.registerAbility(new JzaharBanishAbility());
        ctx.registerServerAbility("morphext:jzahar", new JzaharBanishServerAbility());
    }

    @Override
    public void onInit(MorphextPluginContext ctx) {
        ctx.registerMobActAdapter(new AbyssalBossMobActAdapter());
        if (ctx.isClientSide()) {
            ctx.registerRightBoneKeyword("staff1", 35);
            ctx.registerLeftBoneKeyword("abyssalnomicon", 35);
            ctx.registerRotate180("abyssalcraft:dragonboss");
            ctx.registerRotate180("abyssalcraft.dragonboss");
            ctx.registerRotate180("abyssalcraft:dragonminion");
            ctx.registerRotate180("abyssalcraft.dragonminion");
        }
    }

    private static <T extends aaaa0ggmc.morphext.ability.IAbility & mchorse.metamorph.api.abilities.IAbility> void registerPassive(
            MorphextPluginContext ctx, String metaKey, T passive) {
        ctx.registerAbility(passive);
        MorphManager.INSTANCE.abilities.put(metaKey, passive);
    }
}
