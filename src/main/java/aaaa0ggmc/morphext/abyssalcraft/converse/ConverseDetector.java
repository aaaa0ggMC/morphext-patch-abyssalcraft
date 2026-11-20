package aaaa0ggmc.morphext.abyssalcraft.converse;

import net.minecraft.entity.player.EntityPlayer;

/**
 * 客户端检测方面。实现不得修改世界，不得发网络包。
 * 新方面注册到 {@link ConverseDetectors}，不要改已有检测器的分支来塞特例。
 */
public interface ConverseDetector {
    String id();

    void detect(EntityPlayer player, ConverseScan scan);
}
