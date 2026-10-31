package aaaa0ggmc.morphext.abyssalcraft.variant;

import aaaa0ggmc.morphext.variant.CreativeVariantDef;
import aaaa0ggmc.morphext.variant.ICreativeVariantProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 深渊国度复杂变体与阶数提供者 (AbyssalCraft Creative Variant Provider)。
 * 
 * 核心机制：
 * 1. 完整覆盖修格斯阶数生态：次级修格斯 (0.8x)、普通修格斯 (1.25x)、巨型修格斯 (1.85x)、幼年修格斯 (0.45x) 各自 5 种 ShoggothType 阶数；
 * 2. 覆盖深渊食尸鬼 3 种特殊面孔特异体 (Pete, Mr. Wilson, Dr. Orange) 及幼年体；
 * 3. 覆盖高压充能反物质苦力怕；
 * 4. 覆盖反物质、恶魔、邪恶三大生态全套幼年动物与深渊怪物变种。
 */
public class AbyssalCraftVariantProvider implements ICreativeVariantProvider {

    private static final List<ShoggothTierDef> SHOGGOTH_TIERS = Arrays.asList(
            new ShoggothTierDef(0, "普通", "Normal"),
            new ShoggothTierDef(1, "深渊", "Abyssal"),
            new ShoggothTierDef(2, "恐惧", "Dreaded"),
            new ShoggothTierDef(3, "奥穆索", "Omothol"),
            new ShoggothTierDef(4, "暗影", "Shadow")
    );

    @Override
    public List<CreativeVariantDef> getVariants(World world) {
        List<CreativeVariantDef> defs = new ArrayList<>();

        // 1. 修格斯全系变体 (次级、普通、巨型修格斯各自 0~4 阶与模型 scale 缩放)
        registerShoggothSeries(defs, "abyssalcraft:lessershoggoth", "次级修格斯", "Lesser Shoggoth", 0.8F);
        registerShoggothSeries(defs, "abyssalcraft:shoggoth", "修格斯", "Shoggoth", 1.25F);
        registerShoggothSeries(defs, "abyssalcraft:greatershoggoth", "巨型修格斯", "Greater Shoggoth", 1.85F);

        // 幼年修格斯 (5 种元素阶数)
        for (ShoggothTierDef tier : SHOGGOTH_TIERS) {
            NBTTagCompound babyData = new NBTTagCompound();
            babyData.setBoolean("IsBaby", true);
            babyData.setInteger("Age", -24000);
            babyData.setInteger("ShoggothType", tier.type);
            babyData.setFloat("morphext_scale", 0.45F);
            babyData.setString("morphext_variant", "baby_shoggoth_type_" + tier.type);
            String displayName = "幼年修格斯 (" + tier.zhName + ") - Baby " + tier.enName + " Shoggoth";
            defs.add(new CreativeVariantDef("abyssalcraft:lessershoggoth", displayName, "abyssalcraft", babyData, 0.45F));
        }

        // 2. 深渊食尸鬼特异面孔变种 (Pete, Mr. Wilson, Dr. Orange) 与幼年
        String[][] ghoulVariants = new String[][]{
                {"皮特", "Pete"},
                {"威尔逊先生", "Mr. Wilson"},
                {"橙子博士", "Dr. Orange"}
        };
        for (int g = 0; g < ghoulVariants.length; g++) {
            NBTTagCompound gData = new NBTTagCompound();
            gData.setInteger("GhoulType", g + 1);
            gData.setString("morphext_variant", "ghoul_type_" + (g + 1));
            defs.add(new CreativeVariantDef("abyssalcraft:depthsghoul", "深渊食尸鬼 (" + ghoulVariants[g][0] + ") - Depths Ghoul (" + ghoulVariants[g][1] + ")", "abyssalcraft", gData));
        }
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:depthsghoul", "深渊食尸鬼", "Depths Ghoul", "abyssalcraft"));

        // 3. 反物质生态幼年与高压反物质苦力怕
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:anticow", "反物质牛", "Anti-Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antichicken", "反物质鸡", "Anti-Chicken", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antipig", "反物质猪", "Anti-Pig", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antibat", "反物质蝙蝠", "Anti-Bat", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antighoul", "反物质食尸鬼", "Anti-Ghoul", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antizombie", "反物质僵尸", "Anti-Zombie", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antiabyssalzombie", "反物质深渊僵尸", "Anti-Abyssal Zombie", "abyssalcraft"));

        NBTTagCompound chargedAntiCreeper = new NBTTagCompound();
        chargedAntiCreeper.setBoolean("powered", true);
        chargedAntiCreeper.setString("morphext_variant", "charged_anti_creeper");
        defs.add(new CreativeVariantDef("abyssalcraft:anticreeper", "反物质苦力怕 (高压充能) - Charged Anti-Creeper", "abyssalcraft", chargedAntiCreeper));

        // 4. 恶魔生态幼年动物
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demoncow", "恶魔牛", "Demon Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demonsheep", "恶魔羊", "Demon Sheep", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demonchicken", "恶魔鸡", "Demon Chicken", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demonpig", "恶魔猪", "Demon Pig", "abyssalcraft"));

        // 5. 邪恶生态幼年动物
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilcow", "邪恶牛", "Evil Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilsheep", "邪恶羊", "Evil Sheep", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilchicken", "邪恶鸡", "Evil Chicken", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilpig", "邪恶猪", "Evil Pig", "abyssalcraft"));

        // 6. 深渊与恐惧生灵幼年
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:abyssalzombie", "深渊僵尸", "Abyssal Zombie", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:dreadspawn", "恐惧仆从", "Dread Spawn", "abyssalcraft"));

        return defs;
    }

    private static void registerShoggothSeries(List<CreativeVariantDef> defs, String entityId, String zhName, String enName, float scale) {
        for (ShoggothTierDef tier : SHOGGOTH_TIERS) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("ShoggothType", tier.type);
            data.setInteger("FoodLevel", 0);
            data.setFloat("morphext_scale", scale);
            data.setString("morphext_variant", "shoggoth_type_" + tier.type);

            String displayName = zhName + " (" + tier.zhName + ") - " + tier.enName + " " + enName;
            defs.add(new CreativeVariantDef(entityId, "abyssalcraft:lessershoggoth", displayName, "abyssalcraft", data, scale));
        }
    }

    private static class ShoggothTierDef {
        final int type;
        final String zhName;
        final String enName;

        ShoggothTierDef(int type, String zhName, String enName) {
            this.type = type;
            this.zhName = zhName;
            this.enName = enName;
        }
    }
}
