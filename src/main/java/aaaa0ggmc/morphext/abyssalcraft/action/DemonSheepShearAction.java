package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/**
 * 恶魔羊专属动作：恶魔修剪与地狱火环 (ID: ac_demon_sheep_shear)
 * 1. 手持剪刀 (Items.SHEARS)：修剪羊毛，消耗剪刀耐久，掉落恶魔羊毛、烈焰粉与地狱岩，引爆周身烈焰，冷却 12s；
 * 2. 未手持剪刀：向四周 5 格范围释放狂暴的地狱火环，重创、击退并点燃所有敌对目标，冷却 15s。
 */
public class DemonSheepShearAction extends AbyssalCraftAction {

    public DemonSheepShearAction() {
        super(240); // 默认基准冷却 12 秒
    }

    public static EnumHand findHeldShears(EntityLivingBase entity) {
        if (entity == null) return null;
        if (entity.getHeldItemMainhand().getItem() == Items.SHEARS) return EnumHand.MAIN_HAND;
        if (entity.getHeldItemOffhand().getItem() == Items.SHEARS) return EnumHand.OFF_HAND;
        return null;
    }

    @Override
    protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        EnumHand shearsHand = player != null ? findHeldShears(player) : null;

        // === 1. 剪刀修剪分支 ===
        if (shearsHand != null && player != null) {
            ItemStack shears = player.getHeldItem(shearsHand);
            shears.damageItem(1, player);

            Random rand = world.rand;
            // 掉落 1-2 烈焰粉
            ItemStack blazePowder = new ItemStack(Items.BLAZE_POWDER, 1 + rand.nextInt(2));
            entity.entityDropItem(blazePowder, 0.5F);

            // 掉落 1-2 地狱岩
            ItemStack netherrack = new ItemStack(Item.getItemFromBlock(Blocks.NETHERRACK), 1 + rand.nextInt(2));
            entity.entityDropItem(netherrack, 0.5F);

            // 掉落恶魔黑色羊毛
            ItemStack demonWool = new ItemStack(Blocks.WOOL, 1, 15);
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound display = new NBTTagCompound();
            String woolName = I18n.translateToLocal("item.morphext_patch_abyssalcraft.demon_wool.name");
            display.setString("Name", TextFormatting.RED + (woolName.isEmpty() ? "恶魔羊毛" : woolName));
            tag.setTag("display", display);
            demonWool.setTagCompound(tag);
            entity.entityDropItem(demonWool, 0.5F);

            // 引爆近身烈焰冲击
            List<EntityLivingBase> list = findNearbyEntities(entity, 4.0D);
            DamageSource ds = DamageSource.causePlayerDamage(player);
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(ds, 6.0F);
                target.setFire(5);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        45, 1.5D, 0.3D, 1.5D, 0.08D);
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        12, 1.0D, 0.2D, 1.0D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.2F, 1.0F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.8F);

            setCooldown(entity, 240); // 12 秒冷却
            return true;
        }

        // === 2. 未手持剪刀：释放地狱火环 ===
        List<EntityLivingBase> list = findNearbyEntities(entity, 5.0D);
        DamageSource ds = player != null ? DamageSource.causePlayerDamage(player) : DamageSource.causeMobDamage(entity);

        for (EntityLivingBase target : list) {
            double dx = target.posX - entity.posX;
            double dz = target.posZ - entity.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001D) {
                dx = 1.0D;
                dist = 1.0D;
            }
            target.attackEntityFrom(ds, 8.0F);
            target.setFire(6);
            target.knockBack(entity, 1.4F, -dx / dist, -dz / dist);
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    65, 2.5D, 0.4D, 2.5D, 0.1D);
            ws.spawnParticle(EnumParticleTypes.LAVA,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    20, 2.0D, 0.3D, 2.0D, 0.05D);
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.3F, 0.7F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0F, 0.9F);

        setCooldown(entity, 300); // 15 秒冷却
        return true;
    }
}
