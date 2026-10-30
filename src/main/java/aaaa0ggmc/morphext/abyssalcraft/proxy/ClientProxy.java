package aaaa0ggmc.morphext.abyssalcraft.proxy;

import aaaa0ggmc.morphext.abyssalcraft.SafeCleansingRitualHandler;
import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole;
import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion;
import com.shinoow.abyssalcraft.client.render.entity.RenderBlackHole;
import com.shinoow.abyssalcraft.client.render.entity.RenderImplosion;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(MorphEntityBlackHole.class, RenderBlackHole::new);
        RenderingRegistry.registerEntityRenderingHandler(MorphEntityImplosion.class, RenderImplosion::new);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        SafeCleansingRitualHandler.install();
    }
}
