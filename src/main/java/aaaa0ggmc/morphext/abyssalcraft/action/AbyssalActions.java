package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.common.entity.EntityAcidProjectile;
import com.shinoow.abyssalcraft.common.entity.EntityDreadedCharge;
import com.shinoow.abyssalcraft.common.entity.EntityInkProjectile;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * 深渊国度全部生物核心技能集。
 */
public final class AbyssalActions {

    /** 次级修格斯：喷射高腐蚀修格斯酸液 */
    public static class AcidSpit extends AbyssalCraftAction {
        public AcidSpit() { super(30); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityAcidProjectile projectile = new EntityAcidProjectile(world, entity);
            Vec3d look = entity.getLook(1.0F);
            projectile.shoot(look.x, look.y, look.z, 1.4F, 1.0F);
            world.spawnEntity(projectile);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 1.0F, 0.8F);
            return true;
        }
    }

    /** 大型恐惧之体 / 恐惧仆从：发射恐惧爆裂弹 */
    public static class DreadCharge extends AbyssalCraftAction {
        public DreadCharge() { super(50); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, look.x * 0.1D, look.y * 0.1D, look.z * 0.1D);
            charge.posX = entity.posX + look.x * 1.2D;
            charge.posY = entity.posY + entity.getEyeHeight();
            charge.posZ = entity.posZ + look.z * 1.2D;
            world.spawnEntity(charge);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.6F);
            return true;
        }
    }

    /** 恐惧卫士：连发 3 发恐惧弹幕 */
    public static class DreadBarrage extends AbyssalCraftAction {
        public DreadBarrage() { super(80); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            for (int i = 0; i < 3; i++) {
                double spreadX = (world.rand.nextDouble() - 0.5D) * 0.05D;
                double spreadY = (world.rand.nextDouble() - 0.5D) * 0.05D;
                double spreadZ = (world.rand.nextDouble() - 0.5D) * 0.05D;
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity,
                        (look.x + spreadX) * 0.12D, (look.y + spreadY) * 0.12D, (look.z + spreadZ) * 0.12D);
                charge.posX = entity.posX + look.x * (1.2D + i * 0.3D);
                charge.posY = entity.posY + entity.getEyeHeight() + (i * 0.1D);
                charge.posZ = entity.posZ + look.z * (1.2D + i * 0.3D);
                world.spawnEntity(charge);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.5F);
            return true;
        }
    }

    /** 恐惧傀儡：恐惧震地重砸 */
    public static class DreadSlam extends AbyssalCraftAction {
        public DreadSlam() { super(100); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 12.0F);
                e.motionY += 0.45D;
                if (AbyssalCraftAPI.dread_plague != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 160, 0));
                }
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 1.5F, 0.5F);
            return true;
        }
    }

    /** 珊瑚鱿鱼：喷射剧毒珊瑚墨汁飞弹与迷雾 */
    public static class CoraliumSquirt extends AbyssalCraftAction {
        public CoraliumSquirt() { super(40); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityInkProjectile ink = new EntityInkProjectile(world, entity);
            Vec3d look = entity.getLook(1.0F);
            ink.shoot(look.x, look.y, look.z, 1.2F, 1.0F);
            world.spawnEntity(ink);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
            return true;
        }
    }

    /** 暗影生物：遁入阴影帷幕（隐匿与疾跑） */
    public static class ShadowVeil extends AbyssalCraftAction {
        public ShadowVeil() { super(120); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 160, 0, false, false));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 160, 1, false, false));
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.5F);
            return true;
        }
    }

    /** 暗影巨兽 / 暗影泰坦：暗影坍缩（强力引力吸附与范围重创） */
    public static class ShadowCollapse extends AbyssalCraftAction {
        public ShadowCollapse() { super(140); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 9.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                Vec3d diff = entity.getPositionVector().subtract(e.getPositionVector()).normalize();
                e.motionX += diff.x * 0.8D;
                e.motionY += 0.25D;
                e.motionZ += diff.z * 0.8D;
                e.attackEntityFrom(source, 14.0F);
                e.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
                e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.5F, 0.4F);
            return true;
        }
    }

    /** 反物质苦力怕：反物质内爆（反向吸附与微型湮灭） */
    public static class AntiImplosion extends AbyssalCraftAction {
        public AntiImplosion() { super(100); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 7.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                Vec3d diff = entity.getPositionVector().subtract(e.getPositionVector()).normalize();
                e.motionX += diff.x * 1.1D;
                e.motionY += 0.3D;
                e.motionZ += diff.z * 1.1D;
                e.attackEntityFrom(source, 16.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 120, 0));
                }
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 1.8F);
            return true;
        }
    }

    /** 扎哈尔：终焉湮灭（奥穆托尔黑洞冲击） */
    public static class JzaharOblivion extends AbyssalCraftAction {
        public JzaharOblivion() { super(160); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 12.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 24.0F);
                if (AbyssalCraftAPI.shadow != null) {
                    e.attackEntityFrom(AbyssalCraftAPI.shadow, 10.0F);
                }
                e.addPotionEffect(new PotionEffect(MobEffects.WITHER, 200, 1));
                e.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 60, 0));
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 2.0F, 0.4F);
            return true;
        }
    }

    private AbyssalActions() { }
}
