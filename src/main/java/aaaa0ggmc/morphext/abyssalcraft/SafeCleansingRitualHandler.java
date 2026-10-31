package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.profiler.ProfilerBridge;
import com.shinoow.abyssalcraft.common.network.PacketDispatcher;
import com.shinoow.abyssalcraft.common.network.client.CleansingRitualMessage;
import com.shinoow.abyssalcraft.common.util.BiomeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * 安全的深渊国度群系转换网络包处理器。
 *
 * 彻底解决深渊生物（如查伽洛斯 Chagaroth）或恐惧弹幕触发群系转换时，
 * 服务端广播 CleansingRitualMessage 到客户端，因客户端目标 Chunk 尚未加载返回 blankChunk，
 * 进而导致 RoughlyEnoughIDs (REID) 抛出 Preconditions.checkArgument(pos.getX() >> 4 == chunk.x)
 * 导致客户端崩溃（IllegalArgumentException）的问题。
 */
public class SafeCleansingRitualHandler implements IMessageHandler<CleansingRitualMessage, IMessage> {

    private static Field fieldX;
    private static Field fieldZ;
    private static Field fieldBiomeID;
    private static Field fieldBatched;
    private static boolean fieldsInitialized = false;

    private static void initFields() {
        if (fieldsInitialized) {
            return;
        }
        try {
            fieldX = CleansingRitualMessage.class.getDeclaredField("x");
            fieldX.setAccessible(true);
            fieldZ = CleansingRitualMessage.class.getDeclaredField("z");
            fieldZ.setAccessible(true);
            fieldBiomeID = CleansingRitualMessage.class.getDeclaredField("biomeID");
            fieldBiomeID.setAccessible(true);
            fieldBatched = CleansingRitualMessage.class.getDeclaredField("batched");
            fieldBatched.setAccessible(true);
            fieldsInitialized = true;
        } catch (Exception e) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Failed to reflect CleansingRitualMessage fields", e);
        }
    }

    /**
     * 将 AbyssalCraft 的 CleansingRitualMessage 重新绑定到本安全处理器
     */
    public static void install() {
        try {
            Field f = PacketDispatcher.class.getDeclaredField("dispatcher");
            f.setAccessible(true);
            SimpleNetworkWrapper dispatcher = (SimpleNetworkWrapper) f.get(null);
            if (dispatcher != null) {
                // CleansingRitualMessage 是 PacketDispatcher 中第 8 个注册的包（discriminator 索引为 7）
                dispatcher.registerMessage(new SafeCleansingRitualHandler(), CleansingRitualMessage.class, 7, Side.CLIENT);
                MorphExtAbyssalCraft.LOGGER.info("[morphext-abyssalcraft] Successfully installed SafeCleansingRitualHandler (REID protection active)");
            }
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.warn("[morphext-abyssalcraft] Could not intercept CleansingRitualMessage dispatcher", t);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(CleansingRitualMessage msg, MessageContext ctx) {
        initFields();
        if (!fieldsInitialized) {
            return null;
        }

        try {
            final int x = fieldX.getInt(msg);
            final int z = fieldZ.getInt(msg);
            final int biomeID = fieldBiomeID.getInt(msg);
            final boolean batched = fieldBatched.getBoolean(msg);

            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.world == null || mc.player == null) {
                    return;
                }

                BlockPos pos = new BlockPos(x, 0, z);
                // 核心安全防线：若客户端当前尚未加载此区块，直接安全略过！
                // 绝不让 player.world.getChunk(pos) 返回坐标为 (0,0) 的 blankChunk 导致 REID 断言暴毙
                if (!mc.world.isBlockLoaded(pos)) {
                    return;
                }

                Chunk chunk = mc.world.getChunk(pos);
                if (chunk == null || (chunk.x != (x >> 4)) || (chunk.z != (z >> 4))) {
                    return;
                }

                // 安全更新群系与渲染缓存
                long start = ProfilerBridge.begin();
                try {
                    BiomeUtil.updateBiome(mc.world, pos, biomeID, false);
                    if (x % 14 == 0 || z % 14 == 0 || !batched) {
                        mc.renderGlobal.markBlockRangeForRenderUpdate(x - 7, 0, z - 7, x + 7, 255, z + 7);
                    }
                } catch (Throwable t) {
                    MorphExtAbyssalCraft.LOGGER.debug("[morphext-abyssalcraft] Safely caught biome update anomaly at " + pos, t);
                } finally {
                    ProfilerBridge.end("abyssal:cleansing_ritual", start);
                }
            });
        } catch (Exception e) {
            MorphExtAbyssalCraft.LOGGER.error("[morphext-abyssalcraft] Error processing safe CleansingRitualMessage", e);
        }

        return null;
    }
}
