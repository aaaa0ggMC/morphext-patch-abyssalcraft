package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 反物质鸡专属动作：虚空反物质下蛋 (ID: ac_anti_chicken_egg)
 * 1. 原生机制：产下一枚弥漫虚空微粒的反物质鸡蛋，激荡微型反冲重力波击退近身目标，冷却 10s；
 * 2. 被动特性：天生具备 prevent_fall（完全免疫一切坠落伤害）。
 */
public class AntiChickenEggAction extends AbyssalCraftAction {

    public AntiChickenEggAction() {
        super(200); // 10 秒冷却
    }

    @Override
    protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;

        // 构建反物质虚空鸡蛋
        ItemStack eggStack = new ItemStack(Items.EGG);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("AntiVoidEgg", true);

        NBTTagCompound display = new NBTTagCompound();
        String name = I18n.translateToLocal("item.morphext_patch_abyssalcraft.anti_egg.name");
        display.setString("Name", TextFormatting.AQUA + (name.isEmpty() ? "虚空反物质鸡蛋" : name));
        tag.setTag("display", display);
        eggStack.setTagCompound(tag);

        entity.entityDropItem(eggStack, 0.2F);

        // 伴随微型引力反冲冲击波，击退近身生物
        List<EntityLivingBase> nearby = findNearbyEntities(entity, 3.5D);
        for (EntityLivingBase target : nearby) {
            double dx = target.posX - entity.posX;
            double dz = target.posZ - entity.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001D) {
                dx = 1.0D;
                dist = 1.0D;
            }
            target.knockBack(entity, 0.9F, -dx / dist, -dz / dist);

            if (AbyssalCraftAPI.antimatter_potion != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 40, 0));
            } else {
                target.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 30, 0));
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    entity.posX, entity.posY + 0.2D, entity.posZ,
                    40, 0.6D, 0.2D, 0.6D, 0.08D);
            ws.spawnParticle(EnumParticleTypes.END_ROD,
                    entity.posX, entity.posY + 0.3D, entity.posZ,
                    15, 0.3D, 0.2D, 0.3D, 0.04D);
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.PLAYERS, 1.0F, 0.8F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.6F, 1.9F);

        setCooldown(entity, 200); // 10 秒冷却
        return true;
    }
}
