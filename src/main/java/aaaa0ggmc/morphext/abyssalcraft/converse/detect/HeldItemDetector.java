package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** 主手与副手。主手名字先写入 {@code item} 事实。 */
public final class HeldItemDetector implements ConverseDetector {
    @Override
    public String id() {
        return "held_item";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || scan == null) {
            return;
        }
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        boolean mainEmpty = main == null || main.isEmpty();
        boolean offEmpty = off == null || off.isEmpty();
        if (mainEmpty && offEmpty) {
            scan.add(ConverseAspects.ITEM_EMPTY);
            return;
        }
        if (!mainEmpty) {
            ItemTags.tag(scan, main);
        }
        if (!offEmpty) {
            String name = off.getDisplayName();
            scan.addNoted(ConverseAspects.ITEM_OFFHAND, name == null ? "" : name);
            if (mainEmpty) {
                ItemTags.tag(scan, off);
            }
        }
    }
}
