package aaaa0ggmc.morphext.abyssalcraft.converse;

import aaaa0ggmc.morphext.abyssalcraft.MorphExtAbyssalCraft;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.DimensionDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.HeldItemDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.LookDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.NearbyEntityDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.SelfStateDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.detect.WorldStateDetector;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 检测器注册表。只应在客户端调用 {@link #scan(EntityPlayer)}。 */
public final class ConverseDetectors {
    private static final List<ConverseDetector> DETECTORS = new ArrayList<ConverseDetector>();
    private static final Set<String> IDS = new LinkedHashSet<String>();
    private static final Set<String> FAILED = new LinkedHashSet<String>();
    private static boolean defaultsInstalled;

    private ConverseDetectors() {
    }

    public static void register(ConverseDetector detector) {
        if (detector == null || detector.id() == null || detector.id().isEmpty()) {
            return;
        }
        synchronized (ConverseDetectors.class) {
            if (!IDS.add(detector.id())) {
                return;
            }
            DETECTORS.add(detector);
        }
    }

    /** 内置方面。重复调用无副作用。由客户端代理安装，服务端不调用。 */
    public static void installDefaults() {
        synchronized (ConverseDetectors.class) {
            if (defaultsInstalled) {
                return;
            }
            defaultsInstalled = true;
        }
        register(new NearbyEntityDetector());
        register(new LookDetector());
        register(new HeldItemDetector());
        register(new SelfStateDetector());
        register(new WorldStateDetector());
        register(new DimensionDetector());
    }

    public static ConverseScan scan(EntityPlayer player) {
        ConverseScan scan = new ConverseScan();
        if (player == null || player.world == null || !player.world.isRemote) {
            return scan;
        }
        List<ConverseDetector> snapshot;
        synchronized (ConverseDetectors.class) {
            snapshot = new ArrayList<ConverseDetector>(DETECTORS);
        }
        for (ConverseDetector detector : snapshot) {
            try {
                detector.detect(player, scan);
            } catch (Throwable t) {
                if (FAILED.add(detector.id())) {
                    MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] detector {} failed", detector.id(), t);
                }
            }
        }
        return scan;
    }
}
