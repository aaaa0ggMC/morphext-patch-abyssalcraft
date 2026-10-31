package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 恶魔牛专属动作：熔岩挤奶与地狱之火 (ID: ac_demon_cow_milk)
 * 1. 手持空桶 (Items.BUCKET)：消耗空桶，给予熔岩桶 (Items.LAVA_BUCKET)，播放熔岩与挤奶音效，冷却 10s；
 * 2. 手持打火石 (Items.FLINT_AND_STEEL)：消耗打火石耐久，进入恶魔地狱火充能姿态，点燃自身并在周围激荡烈焰重创敌人，冷却 20s；
 * 3. 未手持指定物品：发动地狱烈焰冲顶，向上轰飞并点燃前方范围敌人，冷却 15s。
 */
public class DemonCowMilkAction extends AbyssalCraftAction {

    public DemonCowMilkAction() {
        super(200); // 默认基准冷却 10 秒
    }

    @Override
    protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        EnumHand bucketHand = player != null ? findHeldItem(player, Items.BUCKET) : null;
        EnumHand flintHand = player != null ? findHeldItem(player, Items.FLINT_AND_STEEL) : null;

        // === 1. 挤奶分支：盛装熔岩桶 ===
        if (bucketHand != null && player != null) {
            ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
            consumeAndGive(player, bucketHand, lavaBucket);

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_COW_MILK, SoundCategory.PLAYERS, 1.0F, 0.8F);

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.6D, entity.posZ,
                        15, 0.3D, 0.2D, 0.3D, 0.05D);
            }

            setCooldown(entity, 200); // 10 秒冷却
            return true;
        }

        // === 2. 打火石分支：恶魔地狱火充能姿态 ===
        if (flintHand != null && player != null) {
            ItemStack flintStack = player.getHeldItem(flintHand);
            flintStack.damageItem(1, player);

            // 点燃自身（恶魔牛天生完全免疫火焰伤害）
            entity.setFire(12);
            entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 240, 0));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 240, 0));

            // 点燃身周敌人并造成火焰伤害
            List<EntityLivingBase> nearby = findNearbyEntities(entity, 3.5D);
            DamageSource fireDs = DamageSource.causePlayerDamage(player);
            for (EntityLivingBase t : nearby) {
                t.setFire(8);
                t.attackEntityFrom(fireDs, 5.0F);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        50, 1.5D, 0.4D, 1.5D, 0.08D);
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        15, 1.0D, 0.3D, 1.0D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1.2F, 0.9F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.7F);

            setCooldown(entity, 400); // 20 秒冷却
            return true;
        }

        // === 3. 未手持空桶/打火石：地狱烈焰冲顶 ===
        Vec3d look = entity.getLook(1.0F);
        Vec3d front = entity.getPositionEyes(1.0F).add(look.x * 2.5D, look.y * 2.5D, look.z * 2.5D);
        AxisAlignedBB box = new AxisAlignedBB(
                front.x - 2.5D, front.y - 1.5D, front.z - 2.5D,
                front.x + 2.5D, front.y + 2.0D, front.z + 2.5D
        );
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

        DamageSource ds = player != null ? DamageSource.causePlayerDamage(player) : DamageSource.causeMobDamage(entity);
        for (EntityLivingBase target : list) {
            target.attackEntityFrom(ds, 8.0F);
            target.setFire(6);
            target.motionY += 0.5D;
            target.motionX += look.x * 0.7D;
            target.motionZ += look.z * 0.7D;
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    front.x, front.y, front.z,
                    60, 1.2D, 1.0D, 1.2D, 0.1D);
            ws.spawnParticle(EnumParticleTypes.LAVA,
                    front.x, front.y, front.z,
                    20, 0.8D, 0.6D, 0.8D, 0.05D);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    front.x, front.y, front.z,
                    15, 1.0D, 0.5D, 1.0D, 0.02D);
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.2F, 0.8F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_COW_HURT, SoundCategory.PLAYERS, 1.0F, 0.6F);

        setCooldown(entity, 300); // 15 秒冷却
        return true;
    }
}
