package aaaa0ggmc.morphext.abyssalcraft.proxy;

import aaaa0ggmc.morphext.abyssalcraft.MorphExtAbyssalCraft;
import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole;
import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(
                new ResourceLocation(MorphExtAbyssalCraft.MODID, "morph_black_hole"),
                MorphEntityBlackHole.class,
                "morph_black_hole",
                1,
                MorphExtAbyssalCraft.instance,
                64,
                10,
                true
        );
        EntityRegistry.registerModEntity(
                new ResourceLocation(MorphExtAbyssalCraft.MODID, "morph_implosion"),
                MorphEntityImplosion.class,
                "morph_implosion",
                2,
                MorphExtAbyssalCraft.instance,
                64,
                10,
                true
        );
    }

    public void init(FMLInitializationEvent event) {
        aaaa0ggmc.morphext.ability.ServerAbilityService.register("morphext:jzahar",
                new aaaa0ggmc.morphext.abyssalcraft.ability.JzaharBanishServerAbility());
    }
}
