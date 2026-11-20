package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/** 日夜与天气。雷雨同时带 rain，方便只写雨的句子也能命中。 */
public final class WorldStateDetector implements ConverseDetector {
    @Override
    public String id() {
        return "world";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || player.world == null || scan == null) {
            return;
        }
        World world = player.world;
        long time = world.getWorldTime() % 24000L;
        if (time < 0L) {
            time += 24000L;
        }
        if (time >= 23000L || time < 500L) {
            scan.add(ConverseAspects.WORLD_DAWN);
        } else if (time < 12000L) {
            scan.add(ConverseAspects.WORLD_DAY);
        } else if (time < 13500L) {
            scan.add(ConverseAspects.WORLD_DUSK);
        } else {
            scan.add(ConverseAspects.WORLD_NIGHT);
        }
        if (world.isThundering()) {
            scan.add(ConverseAspects.WORLD_THUNDER);
            scan.add(ConverseAspects.WORLD_RAIN);
        } else if (world.isRaining()) {
            scan.add(ConverseAspects.WORLD_RAIN);
        } else {
            scan.add(ConverseAspects.WORLD_CLEAR);
        }
    }
}
