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
 * 将深渊国度专属的复杂形态特性（如次级/普通/巨型修格斯的 5 种 ShoggothType 阶数、反物质动物幼年、
 * 恶魔动物幼年、邪恶动物幼年等）通过 morphext 的 SPI 接口注入给通用变种系统，
 * 最终由 morphext 统一挂载至 Metamorph 的创造模式选单与强化检索界面。
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

        // 1. 修格斯全系变体 (次级、普通、巨型修格斯各自 0~4 阶)
        registerShoggothSeries(defs, "abyssalcraft:lessershoggoth", "次级修格斯", "Lesser Shoggoth");
        registerShoggothSeries(defs, "abyssalcraft:shoggoth", "修格斯", "Shoggoth");
        registerShoggothSeries(defs, "abyssalcraft:greatershoggoth", "巨型修格斯", "Greater Shoggoth");

        // 2. 深渊动物与生物幼年变种 (带 IsBaby 与 Age 标签)
        // 反物质系
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:anticow", "反物质牛", "Anti-Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antichicken", "反物质鸡", "Anti-Chicken", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antipig", "反物质猪", "Anti-Pig", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:antizombie", "反物质僵尸", "Anti-Zombie", "abyssalcraft"));

        // 恶魔系
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demoncow", "恶魔牛", "Demon Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demonsheep", "恶魔羊", "Demon Sheep", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:demonschicken", "恶魔鸡", "Demon Chicken", "abyssalcraft"));

        // 邪恶系与深渊系
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilcow", "邪恶牛", "Evil Cow", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilchicken", "邪恶鸡", "Evil Chicken", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:evilpig", "邪恶猪", "Evil Pig", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:abyssalzombie", "深渊僵尸", "Abyssal Zombie", "abyssalcraft"));
        defs.add(CreativeVariantDef.createBaby("abyssalcraft:dreadspawn", "恐惧仆从", "Dread Spawn", "abyssalcraft"));

        return defs;
    }

    private static void registerShoggothSeries(List<CreativeVariantDef> defs, String entityId, String zhName, String enName) {
        for (ShoggothTierDef tier : SHOGGOTH_TIERS) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("ShoggothType", tier.type);
            data.setInteger("FoodLevel", 0);
            data.setString("morphext_variant", "shoggoth_type_" + tier.type);

            String displayName = zhName + " (" + tier.zhName + ") - " + tier.enName + " " + enName;
            defs.add(new CreativeVariantDef(entityId, displayName, "abyssalcraft", data));
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
