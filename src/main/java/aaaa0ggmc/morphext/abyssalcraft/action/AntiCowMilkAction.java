package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.item.ACItems;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 反物质牛专属动作：反物质挤奶与脉冲 (ID: ac_anti_cow_milk)
 * 1. 手持空桶 (Items.BUCKET)：消耗空桶，给予液态反物质桶 (ACItems.liquid_antimatter_bucket_stack)，播放挤奶音效，冷却 10s；
 * 2. 未手持空桶：释放反物质反冲脉冲，击退击飞周围 4 格内生物并施加反物质效果，冷却 15s。
 */
public class AntiCowMilkAction extends AbyssalCraftAction {

    public AntiCowMilkAction() {
        super(200); // 默认基准冷却 10 秒
    }

    @Override
    protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        EnumHand hand = player != null ? findHeldItem(player, Items.BUCKET) : null;

        if (hand != null && player != null) {
            // === 挤奶分支：获取液态反物质桶 ===
            ItemStack antimatterBucket = ACItems.liquid_antimatter_bucket_stack != null ?
                    ACItems.liquid_antimatter_bucket_stack.copy() : new ItemStack(Items.BUCKET);
            consumeAndGive(player, hand, antimatterBucket);

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_COW_MILK, SoundCategory.PLAYERS, 1.0F, 1.0F);

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 0.6D, entity.posZ,
                        25, 0.4D, 0.3D, 0.4D, 0.05D);
            }

            setCooldown(entity, 200); // 10 秒冷却
            return true;
        }

        // === 未手持空桶分支：反物质反冲脉冲 ===
        List<EntityLivingBase> list = findNearbyEntities(entity, 4.0D);
        DamageSource ds = player != null ? DamageSource.causePlayerDamage(player) : DamageSource.causeMobDamage(entity);

        for (EntityLivingBase target : list) {
            double dx = target.posX - entity.posX;
            double dz = target.posZ - entity.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001D) {
                dx = 1.0D;
                dist = 1.0D;
            }
            target.knockBack(entity, 1.8F, -dx / dist, -dz / dist);
            target.motionY += 0.3D;
            target.attackEntityFrom(ds, 6.0F);

            if (AbyssalCraftAPI.antimatter_potion != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 80, 0));
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    60, 2.0D, 0.4D, 2.0D, 0.1D);
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    8, 1.0D, 0.2D, 1.0D, 0.05D);
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.9F, 1.7F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_COW_HURT, SoundCategory.PLAYERS, 1.0F, 1.3F);

        setCooldown(entity, 300); // 15 秒冷却
        return true;
    }
}
