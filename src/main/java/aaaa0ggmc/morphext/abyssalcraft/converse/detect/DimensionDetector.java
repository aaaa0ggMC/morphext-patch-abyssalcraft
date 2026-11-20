package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import com.shinoow.abyssalcraft.api.biome.ACBiomes;
import com.shinoow.abyssalcraft.lib.ACLib;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Locale;

/** 维度与生物群系。名字走语言键，不调用仅客户端的 {@code Biome.getBiomeName()}。 */
public final class DimensionDetector implements ConverseDetector {
    @Override
    public String id() {
        return "dimension";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || player.world == null || scan == null) {
            return;
        }
        tagDimension(player.world, scan);
        tagBiome(player.world, player, scan);
    }

    private static void tagDimension(World world, ConverseScan scan) {
        int dim = world.provider == null ? 0 : world.provider.getDimension();
        if (dim == 0) {
            scan.add(ConverseAspects.DIM_OVERWORLD);
            scan.fact("dimension", label("morphext.converse.dim.overworld", "主世界"));
            return;
        }
        if (dim == -1) {
            scan.add(ConverseAspects.DIM_NETHER);
            scan.fact("dimension", label("morphext.converse.dim.nether", "下界"));
            return;
        }
        if (dim == 1) {
            scan.add(ConverseAspects.DIM_END);
            scan.fact("dimension", label("morphext.converse.dim.end", "末地"));
            return;
        }
        if (dim == ACLib.abyssal_wasteland_id) {
            scan.add(ConverseAspects.DIM_ABYSSAL_WASTELAND);
            scan.fact("dimension", label("morphext.converse.dim.abyssal_wasteland", "深渊荒地"));
            return;
        }
        if (dim == ACLib.dreadlands_id) {
            scan.add(ConverseAspects.DIM_DREADLANDS);
            scan.fact("dimension", label("morphext.converse.dim.dreadlands", "恐惧之地"));
            return;
        }
        if (dim == ACLib.omothol_id) {
            scan.add(ConverseAspects.DIM_OMOTHOL);
            scan.fact("dimension", label("morphext.converse.dim.omothol", "奥穆索"));
            return;
        }
        if (dim == ACLib.dark_realm_id) {
            scan.add(ConverseAspects.DIM_DARK_REALM);
            scan.fact("dimension", label("morphext.converse.dim.dark_realm", "黑暗国度"));
            return;
        }
        scan.add(ConverseAspects.DIM_OTHER);
        String other = "other";
        try {
            if (world.provider.getDimensionType() != null && world.provider.getDimensionType().getName() != null) {
                other = world.provider.getDimensionType().getName();
            }
        } catch (Throwable ignored) {
        }
        scan.fact("dimension", other);
    }

    private static void tagBiome(World world, EntityPlayer player, ConverseScan scan) {
        Biome biome = world.getBiome(player.getPosition());
        if (biome == null) {
            return;
        }
        ResourceLocation id = biome.getRegistryName();
        String path = id == null || id.getPath() == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        String name = biomeLabel(path);
        scan.fact("biome", name);
        if (id != null) {
            ConverseAspects.named(scan, "biome", id, name);
        }
        if (biome == ACBiomes.abyssal_wastelands || path.contains("abyssal_wasteland")) {
            scan.addNoted(ConverseAspects.BIOME_ABYSSAL, name);
        }
        if (biome == ACBiomes.dreadlands || biome == ACBiomes.purified_dreadlands
                || biome == ACBiomes.dreadlands_mountains || biome == ACBiomes.dreadlands_forest
                || path.contains("dreadland")) {
            scan.addNoted(ConverseAspects.BIOME_DREAD, name);
        }
        if (biome == ACBiomes.omothol || path.contains("omothol")) {
            scan.addNoted(ConverseAspects.BIOME_OMOTHOL, name);
        }
        if (biome == ACBiomes.dark_realm || path.contains("dark_realm")) {
            scan.addNoted("biome:dark_realm", name);
        }
        if (biome == ACBiomes.darklands || biome == ACBiomes.darklands_forest
                || biome == ACBiomes.darklands_plains || biome == ACBiomes.darklands_hills
                || biome == ACBiomes.darklands_mountains || biome == ACBiomes.coralium_infested_swamp
                || path.startsWith("darklands") || path.contains("coralium_infested")) {
            scan.addNoted("biome:darklands", name);
        }
        if (path.contains("ocean")) {
            scan.addNoted(ConverseAspects.BIOME_OCEAN, name);
        }
        if (path.contains("forest") || path.contains("taiga")) {
            scan.addNoted(ConverseAspects.BIOME_FOREST, name);
        }
        if (path.contains("desert")) {
            scan.addNoted(ConverseAspects.BIOME_DESERT, name);
        }
        if (path.contains("extreme_hills") || path.contains("mountain")) {
            scan.addNoted(ConverseAspects.BIOME_MOUNTAIN, name);
        }
        if (path.contains("swamp")) {
            scan.addNoted(ConverseAspects.BIOME_SWAMP, name);
        }
        if (path.contains("hell")) {
            scan.addNoted(ConverseAspects.BIOME_HELL, name);
        }
        if (path.contains("ice") || path.contains("cold") || path.contains("frozen") || path.contains("taiga")) {
            scan.addNoted(ConverseAspects.BIOME_COLD, name);
        }
        if (path.contains("jungle")) {
            scan.addNoted(ConverseAspects.BIOME_JUNGLE, name);
        }
        if (path.contains("mesa")) {
            scan.addNoted(ConverseAspects.BIOME_MESA, name);
        }
        if (path.contains("plains") || path.contains("savanna")) {
            scan.addNoted(ConverseAspects.BIOME_PLAINS, name);
        }
        if (path.contains("river")) {
            scan.addNoted(ConverseAspects.BIOME_RIVER, name);
        }
        if (path.contains("beach")) {
            scan.addNoted(ConverseAspects.BIOME_BEACH, name);
        }
        if (path.contains("mushroom")) {
            scan.addNoted(ConverseAspects.BIOME_MUSHROOM, name);
        }
        if (path.contains("void")) {
            scan.addNoted(ConverseAspects.BIOME_VOID, name);
        }
        if ("sky".equals(path) || path.contains("the_end")) {
            scan.addNoted(ConverseAspects.BIOME_END, name);
        }
    }

    private static String biomeLabel(String path) {
        if (path == null || path.isEmpty()) {
            return "?";
        }
        String own = "morphext.converse.biome." + path;
        if (can(own)) {
            return I18n.translateToLocal(own);
        }
        String vanilla = "biome." + path + ".name";
        if (can(vanilla)) {
            return I18n.translateToLocal(vanilla);
        }
        return path.replace('_', ' ');
    }

    private static String label(String key, String fallback) {
        return can(key) ? I18n.translateToLocal(key) : fallback;
    }

    private static boolean can(String key) {
        try {
            return I18n.canTranslate(key);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
