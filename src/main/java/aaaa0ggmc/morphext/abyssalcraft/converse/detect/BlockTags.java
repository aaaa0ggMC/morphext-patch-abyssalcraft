package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOre;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;

/** 准星方块 → 方面。不修改世界。 */
final class BlockTags {
    private BlockTags() {
    }

    static void tag(ConverseScan scan, World world, BlockPos pos) {
        if (scan == null) {
            return;
        }
        if (world == null || pos == null) {
            scan.add(ConverseAspects.BLOCK_AIR);
            return;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state == null ? Blocks.AIR : state.getBlock();
        Material material = state == null ? Material.AIR : state.getMaterial();
        if (block == null || block == Blocks.AIR || material == Material.AIR) {
            scan.add(ConverseAspects.BLOCK_AIR);
            return;
        }
        String name = display(block, state);
        ResourceLocation id = block.getRegistryName();
        String path = id == null || id.getPath() == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id == null || id.getNamespace() == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
        scan.addNoted(ConverseAspects.BLOCK_ANY, name);
        scan.fact("block", name);
        if (id != null) {
            ConverseAspects.named(scan, "block", id, name);
        }
        boolean water = material == Material.WATER || block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        boolean lava = material == Material.LAVA || block == Blocks.LAVA || block == Blocks.FLOWING_LAVA;
        if (water) {
            scan.addNoted(ConverseAspects.BLOCK_WATER, name);
        }
        if (lava) {
            scan.addNoted(ConverseAspects.BLOCK_LAVA, name);
        }
        if (water || lava || material.isLiquid()) {
            scan.addNoted(ConverseAspects.BLOCK_LIQUID, name);
        }
        if (block == Blocks.FIRE || material == Material.FIRE) {
            scan.addNoted(ConverseAspects.BLOCK_FIRE, name);
        }
        boolean ore = block instanceof BlockOre || path.contains("ore");
        if (ore) {
            scan.addNoted(ConverseAspects.BLOCK_ORE, name);
        }
        if (material == Material.PLANTS || material == Material.VINE || material == Material.LEAVES
                || material == Material.CACTUS || block instanceof BlockBush || block instanceof BlockLeaves) {
            scan.addNoted(ConverseAspects.BLOCK_PLANT, name);
        }
        if (material == Material.WOOD || path.contains("log") || path.contains("plank")) {
            scan.addNoted(ConverseAspects.BLOCK_WOOD, name);
        }
        if ((material == Material.ROCK && !ore) || path.contains("stone") || path.contains("cobble")) {
            scan.addNoted(ConverseAspects.BLOCK_STONE, name);
        }
        if (material == Material.GROUND || path.contains("dirt") || path.contains("grass") || path.contains("podzol")) {
            scan.addNoted(ConverseAspects.BLOCK_DIRT, name);
        }
        if (material == Material.SAND || path.contains("sand")) {
            scan.addNoted(ConverseAspects.BLOCK_SAND, name);
        }
        if (material == Material.GLASS || path.contains("glass")) {
            scan.addNoted(ConverseAspects.BLOCK_GLASS, name);
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IInventory || block instanceof BlockContainer || block instanceof BlockChest) {
            scan.addNoted(ConverseAspects.BLOCK_CONTAINER, name);
        }
        boolean abyssal = "abyssalcraft".equals(namespace) || path.contains("abyssal") || path.contains("coralium")
                || path.contains("dread") || path.contains("darkstone") || path.contains("abyssalnite")
                || path.contains("ethaxium") || path.contains("omothol");
        if (abyssal) {
            scan.addNoted(ConverseAspects.BLOCK_ABYSSAL, name);
        }
        if (path.contains("coralium")) {
            scan.addNoted(ConverseAspects.BLOCK_CORALIUM, name);
        }
        if (path.contains("darkstone")) {
            scan.addNoted(ConverseAspects.BLOCK_DARKSTONE, name);
        }
        if (path.contains("dreadstone") || path.contains("dreadbrick") || path.contains("dread_")) {
            scan.addNoted(ConverseAspects.BLOCK_DREADSTONE, name);
        }
        if (path.contains("abyssalnite")) {
            scan.addNoted(ConverseAspects.BLOCK_ABYSSALNITE, name);
        }
    }

    private static String display(Block block, IBlockState state) {
        try {
            ItemStack stack = new ItemStack(block, 1, block.getMetaFromState(state));
            if (!stack.isEmpty()) {
                String named = stack.getDisplayName();
                if (named != null && !named.isEmpty()) {
                    return named;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String local = block.getLocalizedName();
            return local == null ? "" : local;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
