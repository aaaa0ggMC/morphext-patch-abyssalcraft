package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.common.entity.EntityAcidProjectile;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSlug;
import com.shinoow.abyssalcraft.common.entity.EntityDreadedCharge;
import com.shinoow.abyssalcraft.common.entity.EntityInkProjectile;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 深渊国度全部生物核心技能集。
 * 遵循安全与平衡设计：
 * 1. 前向安全距离发射，彻底杜绝 self-hit 自身反噬；
 * 2. 严禁无控摧毁玩家建筑与生态（不改变环境群系、爆炸不破坏方块）；
 * 3. 目标全面适配所有 EntityLivingBase，不仅针对玩家。
 */
public final class AbyssalActions {

    /** 次级修格斯 / 修格斯：喷射高腐蚀修格斯酸液 */
    public static class AcidSpit extends AbyssalCraftAction {
        public AcidSpit() { super(35); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityAcidProjectile projectile = new EntityAcidProjectile(world, entity);
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.4D);
            projectile.setPosition(safe.x, safe.y, safe.z);
            projectile.shoot(look.x, look.y, look.z, 1.4F, 1.0F);
            world.spawnEntity(projectile);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 1.0F, 0.8F);
            return true;
        }
    }

    /** 修格斯：粘液爆发（大范围减速与强酸压制） */
    public static class ShoggothOoze extends AbyssalCraftAction {
        public ShoggothOoze() { super(120); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 7.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 3));
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 1));
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                        entity.posX, entity.posY + 0.2D, entity.posZ,
                        80, 2.5D, 0.3D, 2.5D, 0.1D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SLIME_ATTACK, SoundCategory.PLAYERS, 1.5F, 0.5F);
            return true;
        }
    }

    /** 恐惧生物：发射恐惧爆裂弹（无方块破坏） */
    public static class DreadCharge extends AbyssalCraftAction {
        public DreadCharge() { super(50); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.4D);
            EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, look.x * 0.12D, look.y * 0.12D, look.z * 0.12D);
            charge.setPosition(safe.x, safe.y, safe.z);
            world.spawnEntity(charge);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.6F);
            return true;
        }
    }

    /** 次级恐惧兽：快速恐惧蛞蝓弹（高速单发穿透） */
    public static class DreadSlug extends AbyssalCraftAction {
        public DreadSlug() { super(35); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.4D);
            EntityDreadSlug slug = new EntityDreadSlug(world, entity);
            slug.setPosition(safe.x, safe.y, safe.z);
            slug.shoot(look.x, look.y, look.z, 1.6F, 1.0F);
            world.spawnEntity(slug);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0F, 0.5F);
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
                double spreadX = (world.rand.nextDouble() - 0.5D) * 0.04D;
                double spreadY = (world.rand.nextDouble() - 0.5D) * 0.04D;
                double spreadZ = (world.rand.nextDouble() - 0.5D) * 0.04D;
                Vec3d safe = getSafeSpawnPosition(entity, 1.4D + i * 0.35D);
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity,
                        (look.x + spreadX) * 0.12D, (look.y + spreadY) * 0.12D, (look.z + spreadZ) * 0.12D);
                charge.setPosition(safe.x, safe.y + (i * 0.1D), safe.z);
                world.spawnEntity(charge);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.5F);
            return true;
        }
    }

    /** 查加洛斯之拳 / 恐惧傀儡：恐惧震地重砸 */
    public static class DreadSlam extends AbyssalCraftAction {
        public DreadSlam() { super(90); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 12.0F);
                e.motionY += 0.45D;
                if (AbyssalCraftAPI.dread_plague != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 140, 0));
                }
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 1.5F, 0.5F);
            return true;
        }
    }

    /** 阿索拉 / 幽灵龙：喷吐幽灵龙息火球 */
    public static class SpectralFireball extends AbyssalCraftAction {
        public SpectralFireball() { super(120); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.6D);
            EntityWitherSkull skull = new EntityWitherSkull(world, entity, look.x * 0.15D, look.y * 0.15D, look.z * 0.15D);
            skull.setPosition(safe.x, safe.y, safe.z);
            skull.setInvulnerable(false);
            world.spawnEntity(skull);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.2F, 0.7F);
            return true;
        }
    }

    /** 食尸鬼家族：飞扑撕咬 */
    public static class GhoulPounce extends AbyssalCraftAction {
        public GhoulPounce() { super(70); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.motionX += look.x * 1.15D;
            entity.motionY += 0.36D;
            entity.motionZ += look.z * 1.15D;
            entity.velocityChanged = true;

            List<EntityLivingBase> list = findNearbyEntities(entity, 3.2D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 120, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.0F, 1.4F);
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
            Vec3d safe = getSafeSpawnPosition(entity, 1.4D);
            ink.setPosition(safe.x, safe.y, safe.z);
            ink.shoot(look.x, look.y, look.z, 1.2F, 1.0F);
            world.spawnEntity(ink);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
            return true;
        }
    }

    /** 遗民：尖啸战吼（范围声波震慑与群体弱化） */
    public static class RemnantScreech extends AbyssalCraftAction {
        public RemnantScreech() { super(160); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 9.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 5.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 120, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 140, 1));
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.NOTE,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                        30, 1.5D, 0.8D, 1.5D, 0.05D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 1.5F, 1.2F);
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

    /** 暗影巨兽 / 暗影泰坦 / 萨克索斯：暗影坍缩（强力引力吸附与范围重创） */
    public static class ShadowCollapse extends AbyssalCraftAction {
        public ShadowCollapse() { super(140); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 9.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                Vec3d diff = entity.getPositionVector().subtract(e.getPositionVector()).normalize();
                e.motionX += diff.x * 0.7D;
                e.motionY += 0.25D;
                e.motionZ += diff.z * 0.7D;
                e.attackEntityFrom(source, 14.0F);
                e.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
                e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.5F, 0.4F);
            return true;
        }
    }

    /** 反物质苦力怕：反物质内爆（非破坏性引力微型湮灭） */
    public static class AntiImplosion extends AbyssalCraftAction {
        public AntiImplosion() { super(100); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 7.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                Vec3d diff = entity.getPositionVector().subtract(e.getPositionVector()).normalize();
                e.motionX += diff.x * 0.9D;
                e.motionY += 0.25D;
                e.motionZ += diff.z * 0.9D;
                e.attackEntityFrom(source, 15.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 100, 0));
                }
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 1.8F);
            return true;
        }
    }

    /** 扎哈尔：终焉湮灭（奥穆索尔黑洞奇点冲击，不破坏方块） */
    public static class JzaharOblivion extends AbyssalCraftAction {
        public JzaharOblivion() { super(220); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 12.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 22.0F);
                if (AbyssalCraftAPI.shadow != null) {
                    e.attackEntityFrom(AbyssalCraftAPI.shadow, 8.0F);
                }
                e.addPotionEffect(new PotionEffect(MobEffects.WITHER, 160, 1));
                e.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 50, 0));
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 2.0F, 0.4F);
            return true;
        }
    }

    /** 恐惧生物：恐惧战吼（范围声波与恐惧震慑） */
    public static class DreadRoar extends AbyssalCraftAction {
        public DreadRoar() { super(120); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 8.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 6.0F);
                e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                e.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 120, 0));
                if (AbyssalCraftAPI.dread_plague != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 100, 0));
                }
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        40, 2.0D, 0.4D, 2.0D, 0.05D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0F, 1.4F);
            return true;
        }
    }

    /** 暗影生物：暗影闪烁步（向前暗影突进） */
    public static class ShadowStep extends AbyssalCraftAction {
        public ShadowStep() { super(70); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            double targetX = entity.posX + look.x * 7.0D;
            double targetY = entity.posY + look.y * 7.0D;
            double targetZ = entity.posZ + look.z * 7.0D;

            // 检查目标点安全地形
            net.minecraft.util.math.BlockPos targetPos = new net.minecraft.util.math.BlockPos(targetX, targetY, targetZ);
            if (!world.getBlockState(targetPos).getMaterial().isSolid()) {
                entity.setPositionAndUpdate(targetX, targetY, targetZ);
            } else {
                entity.setPositionAndUpdate(entity.posX + look.x * 3.5D, entity.posY, entity.posZ + look.z * 3.5D);
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        30, 0.5D, 0.5D, 0.5D, 0.1D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 0.8F);
            return true;
        }
    }

    /** 修格斯：腐蚀酸雾喷射 */
    public static class AcidMelt extends AbyssalCraftAction {
        public AcidMelt() { super(80); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 5.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 7.0F);
                e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 2));
                e.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 100, 1));
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        25, 1.5D, 0.3D, 1.5D, 0.05D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1.2F, 0.9F);
            return true;
        }
    }

    /** 反物质生物：反物质脉冲射击 */
    public static class AntimatterBlast extends AbyssalCraftAction {
        public AntimatterBlast() { super(90); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 10.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 80, 0));
                } else {
                    e.addPotionEffect(new PotionEffect(MobEffects.WITHER, 80, 0));
                }
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.6F);
            return true;
        }
    }

    private AbyssalActions() { }
}
