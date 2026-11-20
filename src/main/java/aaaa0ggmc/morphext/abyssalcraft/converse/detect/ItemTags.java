package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

/** 手持物品 → 方面。主手的显示名先写入 {@code item} 事实。 */
final class ItemTags {
    private ItemTags() {
    }

    static void tag(ConverseScan scan, ItemStack stack) {
        if (scan == null || stack == null || stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        if (item == null) {
            return;
        }
        String name = stack.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = "?";
        }
        ResourceLocation id = item.getRegistryName();
        String path = id == null || id.getPath() == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id == null || id.getNamespace() == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
        scan.addNoted(ConverseAspects.ITEM_ANY, name);
        scan.fact("item", name);
        if (id != null) {
            ConverseAspects.named(scan, "item", id, name);
        }
        if (item instanceof ItemSword || path.contains("sword")) {
            scan.addNoted(ConverseAspects.ITEM_SWORD, name);
        }
        if (item instanceof ItemBow || path.contains("bow")) {
            scan.addNoted(ConverseAspects.ITEM_BOW, name);
        }
        if (item instanceof ItemPickaxe || path.contains("pickaxe")) {
            scan.addNoted(ConverseAspects.ITEM_PICKAXE, name);
            scan.addNoted(ConverseAspects.ITEM_TOOL, name);
        }
        if (item instanceof ItemAxe || (path.contains("axe") && !path.contains("pickaxe"))) {
            scan.addNoted(ConverseAspects.ITEM_AXE, name);
            scan.addNoted(ConverseAspects.ITEM_TOOL, name);
        }
        if (item instanceof ItemTool) {
            scan.addNoted(ConverseAspects.ITEM_TOOL, name);
        }
        if (item instanceof ItemFood || path.contains("flesh") || path.contains("food")) {
            scan.addNoted(ConverseAspects.ITEM_FOOD, name);
        }
        if (item instanceof ItemArmor || path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots")) {
            scan.addNoted(ConverseAspects.ITEM_ARMOR, name);
        }
        if (item instanceof ItemPotion || path.contains("potion")) {
            scan.addNoted(ConverseAspects.ITEM_POTION, name);
        }
        boolean book = item instanceof ItemBook || item instanceof ItemEnchantedBook
                || item instanceof ItemWritableBook || item instanceof ItemWrittenBook
                || path.contains("book") || path.contains("necronomicon") || path.contains("nomicon");
        if (book) {
            scan.addNoted(ConverseAspects.ITEM_BOOK, name);
        }
        if ("abyssalcraft".equals(namespace) || path.contains("abyssal") || path.contains("coralium")
                || path.contains("dread") || path.contains("shadow") || path.contains("ethaxium")) {
            scan.addNoted(ConverseAspects.ITEM_ABYSSAL, name);
        }
        if (path.contains("necronomicon") || path.contains("abyssalnomicon") || path.contains("nomicon")) {
            scan.addNoted(ConverseAspects.ITEM_NECRONOMICON, name);
        }
    }
}
