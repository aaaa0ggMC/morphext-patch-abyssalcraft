package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.EntityUtil;
import com.shinoow.abyssalcraft.common.entity.EntityAcidProjectile;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSlug;
import com.shinoow.abyssalcraft.common.entity.EntityDreadedCharge;
import com.shinoow.abyssalcraft.common.entity.EntityInkProjectile;
import com.shinoow.abyssalcraft.lib.ACSounds;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 深渊国度全部生物核心技能集。
 * 遵循统一主动被动能力架构 (UAPA) 与安全机制：
 * 1. 前向安全距离发射 (>= 1.8D) 并打上属主标签，彻底杜绝 self-hit 自身反噬；
 * 2. 严禁无控摧毁玩家建筑与生态（不改变环境群系、爆炸不破坏方块）；
 * 3. 目标全面适配所有 EntityLivingBase，智能面向准星与敌人判定；
 * 4. 完整还原暗影瞬步、巨爪横扫、恐惧撼地猛击、反物质爆发与珊瑚墨汁等机制。
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
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            projectile.setPosition(safe.x, safe.y, safe.z);
            projectile.shoot(look.x, look.y, look.z, 1.4F, 1.0F);
            BossNativeHelper.tagOwner(projectile, entity);
            world.spawnEntity(projectile);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shoggoth_shoot != null ? ACSounds.shoggoth_shoot : SoundEvents.ENTITY_SLIME_SQUISH,
                    SoundCategory.PLAYERS, 1.0F, 0.8F);
            return true;
        }
    }

    /** 修格斯：粘液爆发（大范围减速与强酸压制） */
    public static class ShoggothOoze extends AbyssalCraftAction {
        public ShoggothOoze() { super(100); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = AbyssalCraftAPI.acid != null ? AbyssalCraftAPI.acid : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 7.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 2)); // 5秒 缓慢 III
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 0)); // 5秒 虚弱 I
            }
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                        entity.posX, entity.posY + 0.2D, entity.posZ,
                        100, 2.8D, 0.4D, 2.8D, 0.1D);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        30, 2.0D, 0.3D, 2.0D, 0.0D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shoggoth_consume != null ? ACSounds.shoggoth_consume : SoundEvents.ENTITY_SLIME_ATTACK,
                    SoundCategory.PLAYERS, 1.5F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SLIME_ATTACK, SoundCategory.PLAYERS, 1.2F, 0.6F);
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
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, look.x * 0.12D, look.y * 0.12D, look.z * 0.12D);
            charge.setPosition(safe.x, safe.y, safe.z);
            BossNativeHelper.tagOwner(charge, entity);
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
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            EntityDreadSlug slug = new EntityDreadSlug(world, entity);
            slug.setPosition(safe.x, safe.y, safe.z);
            slug.shoot(look.x, look.y, look.z, 1.6F, 1.0F);
            BossNativeHelper.tagOwner(slug, entity);
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
                Vec3d safe = getSafeSpawnPosition(entity, 1.8D + i * 0.35D);
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity,
                        (look.x + spreadX) * 0.12D, (look.y + spreadY) * 0.12D, (look.z + spreadZ) * 0.12D);
                charge.setPosition(safe.x, safe.y + (i * 0.1D), safe.z);
                BossNativeHelper.tagOwner(charge, entity);
                world.spawnEntity(charge);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.5F);
            return true;
        }
    }

    /** 恐惧守卫：重型战刃撼地猛击 (ID: ac_dread_slam) - 震碎地面激起冲击波与恐惧粒子，击飞身前 5 格敌人 */
    public static class DreadSlamAction extends AbyssalCraftAction {
        public DreadSlamAction() { super(120); } // 冷却 6s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            entity.swingArm(EnumHand.MAIN_HAND);

            // 身前 5 格判定盒
            AxisAlignedBB slamBox = entity.getEntityBoundingBox().grow(4.0D).offset(look.x * 2.5D, 0.2D, look.z * 2.5D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, slamBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 14.0F);
                e.motionY += 0.75D;
                e.motionX += look.x * 0.85D;
                e.motionZ += look.z * 0.85D;
                e.velocityChanged = true;
                if (AbyssalCraftAPI.dread_plague != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 140, 0));
                }
            }

            // 震地同心圆碎屑与恐惧烈焰粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double cx = entity.posX + look.x * 2.5D;
                double cz = entity.posZ + look.z * 2.5D;
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double px = cx + Math.cos(rad) * 4.5D;
                    double pz = cz + Math.sin(rad) * 4.5D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 0.2D, pz, 2, 0.1, 0.2, 0.1, 0.05);
                    ws.spawnParticle(EnumParticleTypes.LAVA, px, entity.posY + 0.2D, pz, 2, 0.0, 0.0, 0.0, 0.0);
                    ws.spawnParticle(EnumParticleTypes.FLAME, px, entity.posY + 0.2D, pz, 1, 0.02, 0.05, 0.02, 0.02);
                }
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, entity.posY + 0.2D, cz, 4, 0.8, 0.2, 0.8, 0.0);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dreadguard_barf != null ? ACSounds.dreadguard_barf : SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 1.8F, 0.5F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 0.6F);
            return true;
        }
    }
    public static class DreadSlam extends DreadSlamAction {}

    /**
     * 恐惧守卫：战刃单体重斩 (ID: ac_dread_katana, 冷却 4s)。
     * 与 DreadSlamAction（AoE 撼地）分化（NEW#2）：聚焦单体的沉重纵斩，
     * 对准星/近身单一目标造成高额恐惧直伤、重击退与恐惧瘟疫，附带护甲撕裂表现。
     */
    public static class DreadKatanaAction extends AbyssalCraftAction {
        public DreadKatanaAction() { super(80); } // 冷却 4s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 单体选取：准星直击优先，其次近身 3.5 格最近敌人
            EntityLivingBase target = findTargetInCrosshairs(entity, 4.5D);
            if (target == null) {
                AxisAlignedBB near = entity.getEntityBoundingBox().grow(3.5D).offset(look.x * 1.2D, 0.0D, look.z * 1.2D);
                List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, near,
                        e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
                double best = 3.5D * 3.5D;
                for (EntityLivingBase candidate : list) {
                    double distSq = candidate.getDistanceSq(entity);
                    if (distSq < best) {
                        best = distSq;
                        target = candidate;
                    }
                }
            }
            if (target == null) {
                return false;
            }

            entity.swingArm(EnumHand.MAIN_HAND);

            DamageSource source;
            if (AbyssalCraftAPI.dread != null) {
                source = AbyssalCraftAPI.dread; // AC 恐惧伤害源自带破甲
            } else {
                source = (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                        : DamageSource.causeMobDamage(entity)).setDamageBypassesArmor();
            }
            // 重斩：单体重创 + 破甲撕裂 + 恐惧瘟疫（A13：16→15，对齐 morph 可玩性封顶线）
            target.attackEntityFrom(source, 15.0F);
            if (AbyssalCraftAPI.dread_plague != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 140, 0));
            }
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
            target.knockBack(entity, 1.2F, -look.x, -look.z);
            target.velocityChanged = true;

            // 纵斩刀光与骨屑
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = target.posX;
                double py = target.posY + target.height * 0.6D;
                double pz = target.posZ;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC, px, py, pz, 30, 0.35D, 0.6D, 0.35D, 0.12D);
                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, py, pz, 2, 0.1D, 0.1D, 0.1D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.LAVA, px, target.posY + 0.1D, pz, 8, 0.3D, 0.1D, 0.3D, 0.0D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 1.2F);
            return true;
        }
    }

    /** 奥穆索遗民 / 阿索拉：喷吐幽魂虚空火球 (ID: ac_spectral_fireball) - 向准星安全发射一颗幽灵火球，命中引发幽冥爆炸 (冷却 5s) */
    public static class SpectralFireball extends AbyssalCraftAction {
        public SpectralFireball() { super(100); } // 冷却 5s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            EntityWitherSkull skull = new EntityWitherSkull(world, entity, look.x * 0.16D, look.y * 0.16D, look.z * 0.16D);
            skull.setPosition(safe.x, safe.y, safe.z);
            skull.setInvulnerable(false);
            BossNativeHelper.tagOwner(skull, entity);
            world.spawnEntity(skull);

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, safe.x, safe.y, safe.z, 8, 0.2D, 0.2D, 0.2D, 0.02D);
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, safe.x, safe.y, safe.z, 12, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.2F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.8F);
            return true;
        }
    }

    /**
     * 深渊食尸鬼：水下高速伏击 (ID: ac_ghoul_underwater_ambush, 冷却 8s)
     * 在水中释放时爆发 2.5D 水流推进力高速冲刺并隐匿，出水扑袭身前 3.5 格目标造成 12 点物理破甲重创。
     */
    public static class GhoulUnderwaterAmbushAction extends AbyssalCraftAction {
        public GhoulUnderwaterAmbushAction() { super(160); } // 冷却 8s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            boolean inWater = entity.isInWater();

            if (inWater) {
                // 水中爆发 2.5D 水流推进力高速冲刺与水下隐匿
                double propel = 2.5D;
                entity.motionX = look.x * propel;
                entity.motionY = look.y * (propel * 0.75D);
                entity.motionZ = look.z * propel;
                entity.velocityChanged = true;

                entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 100, 0, false, false));
                entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1, false, false));

                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.WATER_BUBBLE, entity.posX, entity.posY + 0.5D, entity.posZ, 50, 0.8D, 0.4D, 0.8D, 0.2D);
                    ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX, entity.posY + 0.5D, entity.posZ, 40, 0.8D, 0.4D, 0.8D, 0.15D);
                    ws.spawnParticle(EnumParticleTypes.WATER_WAKE, entity.posX, entity.posY + 0.5D, entity.posZ, 30, 0.5D, 0.2D, 0.5D, 0.1D);
                }

                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_PLAYER_SWIM, SoundCategory.PLAYERS, 1.5F, 0.6F);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1.2F, 0.8F);
            } else {
                // 陆地突进
                entity.motionX += look.x * 1.5D;
                entity.motionY += 0.42D;
                entity.motionZ += look.z * 1.5D;
                entity.velocityChanged = true;

                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, entity.posX, entity.posY + 0.2D, entity.posZ, 15, 0.4D, 0.1D, 0.4D, 0.05D);
                }

                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.7F);
            }

            // 扑袭身前 3.5 格目标造成 12 点物理破甲重创
            AxisAlignedBB strikeBox = entity.getEntityBoundingBox().grow(2.5D, 1.6D, 2.5D).offset(look.x * 2.2D, look.y * 0.8D, look.z * 2.2D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, strikeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource armorPierce = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) :
                    DamageSource.causeMobDamage(entity)).setDamageBypassesArmor();

            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(armorPierce, 12.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                if (AbyssalCraftAPI.coralium_plague != null && !EntityUtil.isEntityCoralium(target)) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 100, 0));
                }
                hit = true;
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.ghoul_normal_ambient, SoundCategory.HOSTILE, 1.4F, 0.8F);
            if (hit) {
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5F, 0.7F);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.2F, 1.5F);
            }

            return true;
        }
    }
    public static class GhoulUnderwaterAmbush extends GhoulUnderwaterAmbushAction {}

    /**
     * 深渊食尸鬼：珊瑚利爪感染 (ID: ac_coralium_claws, 冷却 4s)
     * 挥舞带珊瑚侵蚀的腐爪，命中敌人施加 120 ticks 珊瑚瘟疫 (cplague) 与虚弱。
     */
    public static class CoraliumClawsAction extends AbyssalCraftAction {
        public CoraliumClawsAction() { super(80); } // 冷却 4s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            entity.swingArm(EnumHand.MAIN_HAND);

            // 身前 3.5 格扇形抓击
            AxisAlignedBB strikeBox = entity.getEntityBoundingBox().grow(2.5D, 1.2D, 2.5D).offset(look.x * 1.8D, 0.2D, look.z * 1.8D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, strikeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = AbyssalCraftAPI.coralium != null ?
                    AbyssalCraftAPI.coralium :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 9.0F);
                if (AbyssalCraftAPI.coralium_plague != null && !EntityUtil.isEntityCoralium(target)) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 120, 0)); // 120 ticks 珊瑚瘟疫
                }
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 120, 0)); // 120 ticks 虚弱
                target.knockBack(entity, 0.6F, -look.x, -look.z);
                hit = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.8D;
                double py = entity.posY + entity.getEyeHeight() * 0.7D;
                double pz = entity.posZ + look.z * 1.8D;
                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC, px, py, pz, 15, 0.4D, 0.3D, 0.4D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, px, py, pz, 15, 0.4D, 0.2D, 0.4D, 0.1D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0F, 1.6F);
            if (hit) {
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.ghoul_normal_ambient, SoundCategory.HOSTILE, 1.2F, 1.0F);
            }

            return true;
        }
    }
    public static class CoraliumClaws extends CoraliumClawsAction {}

    /**
     * 食尸鬼家族：飞扑撕咬 (ID: ac_ghoul_pounce) - 扑向目标，造成撕咬撕裂伤害。
     * 疫种按宿主分流（NEW#9）：珊瑚感染型（深渊食尸鬼/深渊僵尸）施加珊瑚瘟疫，
     * 恐惧军团型（dread/chagaroth 系）施加恐惧瘟疫，其余宿主只留撕咬（不再串味播疫）。
     */
    public static class GhoulPounceAction extends AbyssalCraftAction {
        public GhoulPounceAction() { super(70); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            String morphName = morph != null && morph.name != null ? morph.name.toLowerCase(java.util.Locale.ROOT) : "";

            // 扑跃突进
            entity.motionX += look.x * 1.35D;
            entity.motionY += 0.40D;
            entity.motionZ += look.z * 1.35D;
            entity.velocityChanged = true;

            // 判定盒沿着位移向量向前延伸平移至落点中心，防止扑中却判定失效
            AxisAlignedBB hitBox = entity.getEntityBoundingBox().offset(look.x * 1.6D, 0.2D, look.z * 1.6D).grow(2.5D, 1.5D, 2.5D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, hitBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            boolean dreadPlague = morphName.contains("dread") || morphName.contains("chagaroth");
            boolean coraliumPlague = !dreadPlague
                    && (morphName.contains("depths") || (morphName.contains("abyssalzombie") && !morphName.contains("anti")));
            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 10.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 140, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                if (coraliumPlague && AbyssalCraftAPI.coralium_plague != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 140, 0));
                } else if (dreadPlague && AbyssalCraftAPI.dread_plague != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 140, 0));
                }
                hit = true;
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.ghoul_normal_ambient, SoundCategory.HOSTILE, 1.2F, 0.9F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1.0F, 1.4F);
            if (hit) {
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.4F, 1.2F);
            }
            return true;
        }
    }
    public static class GhoulPounce extends GhoulPounceAction {}

    /**
     * 珊瑚鱿鱼：珊瑚瘟疫风暴喷射 (ID: ac_coralium_ink_storm, 冷却 12s)
     * 喷射大范围致命深蓝珊瑚墨汁迷雾（半径 7 格），沿途敌人失明 6 秒并染上珊瑚瘟疫，自身喷气反冲后撤加速。
     */
    public static class CoraliumInkStormAction extends AbyssalCraftAction {
        public CoraliumInkStormAction() { super(240); } // 冷却 12s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 1. 周围 7 格大范围深蓝珊瑚墨汁风暴
            List<EntityLivingBase> list = findNearbyEntities(entity, 7.0D);
            DamageSource source = AbyssalCraftAPI.coralium != null ?
                    AbyssalCraftAPI.coralium :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 10.0F);
                if (AbyssalCraftAPI.coralium_plague != null && !EntityUtil.isEntityCoralium(target)) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 160, 0)); // 珊瑚瘟疫
                }
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 120, 0)); // 失明 6 秒 (120 ticks)
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 1));  // 缓慢 II 6 秒
            }

            // 2. 前向齐射 3 枚扩散墨汁飞弹
            for (int i = -1; i <= 1; i++) {
                Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
                EntityInkProjectile ink = new EntityInkProjectile(world, entity);
                ink.setPosition(safe.x, safe.y + (i == 0 ? 0.0D : 0.15D), safe.z);
                double spreadX = look.x + (i * -look.z * 0.22D);
                double spreadZ = look.z + (i * look.x * 0.22D);
                ink.shoot(spreadX, look.y + 0.08D, spreadZ, 1.4F, 2.0F);
                BossNativeHelper.tagOwner(ink, entity);
                world.spawnEntity(ink);
            }

            // 3. 喷气反冲后撤加速
            entity.motionX -= look.x * 1.35D;
            entity.motionZ -= look.z * 1.35D;
            entity.motionY += 0.35D;
            entity.velocityChanged = true;
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 80, 1, false, false));

            // 4. 深蓝墨汁迷雾粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + 0.5D, entity.posZ, 80, 3.5D, 1.2D, 3.5D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX, entity.posY + 0.5D, entity.posZ, 100, 3.0D, 1.0D, 3.0D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.WATER_BUBBLE, entity.posX, entity.posY + 0.5D, entity.posZ, 60, 2.5D, 1.0D, 2.5D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.SPELL_MOB, entity.posX, entity.posY + 0.5D, entity.posZ, 50, 3.0D, 1.0D, 3.0D, 0.0D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SQUID_AMBIENT, SoundCategory.PLAYERS, 2.0F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.5F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_SWIM, SoundCategory.PLAYERS, 1.2F, 0.8F);

            return true;
        }
    }
    public static class CoraliumInkStorm extends CoraliumInkStormAction {}

    /** 珊瑚鱿鱼：致命珊瑚感染墨汁雾 (ID: ac_coralium_ink) - 喷射墨汁雾与飞弹，周围敌人染上珊瑚瘟疫与致盲迟缓 */
    public static class CoraliumInkAction extends AbyssalCraftAction {
        public CoraliumInkAction() { super(300); } // 冷却 15s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 1. 周围 6 格墨汁迷雾感染
            List<EntityLivingBase> list = findNearbyEntities(entity, 6.0D);
            DamageSource source = AbyssalCraftAPI.coralium != null ? AbyssalCraftAPI.coralium : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                if (AbyssalCraftAPI.coralium_plague != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 160, 0));
                }
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
            }

            // 2. 前向发射墨汁弹 (安全距离 1.8D)
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            EntityInkProjectile ink = new EntityInkProjectile(world, entity);
            ink.setPosition(safe.x, safe.y, safe.z);
            ink.shoot(look.x, look.y, look.z, 1.3F, 1.0F);
            BossNativeHelper.tagOwner(ink, entity);
            world.spawnEntity(ink);

            // 3. 鱿鱼瞬时反冲位移
            entity.motionX -= look.x * 0.7D;
            entity.motionZ -= look.z * 0.7D;
            entity.velocityChanged = true;

            // 4. 粒子与音效
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX, entity.posY + 0.5D, entity.posZ, 80, 2.0D, 1.0D, 2.0D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.SPELL_MOB, entity.posX, entity.posY + 0.5D, entity.posZ, 40, 1.5D, 0.8D, 1.5D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + 0.5D, entity.posZ, 30, 1.5D, 0.5D, 1.5D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.5F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.2F, 0.6F);
            return true;
        }
    }

    /** 珊瑚鱿鱼：喷射剧毒珊瑚墨汁飞弹与迷雾 (旧版兼容) */
    public static class CoraliumSquirt extends AbyssalCraftAction {
        public CoraliumSquirt() { super(40); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityInkProjectile ink = new EntityInkProjectile(world, entity);
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
            ink.setPosition(safe.x, safe.y, safe.z);
            ink.shoot(look.x, look.y, look.z, 1.2F, 1.0F);
            BossNativeHelper.tagOwner(ink, entity);
            world.spawnEntity(ink);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
            return true;
        }
    }

    /** 奥穆索遗民：奥穆索穿脑尖叫 (ID: ac_remnant_screech, 冷却 10s) - 发出异界穿脑尖啸，使周围 12 格敌人陷入严重反胃与缓慢 III，并伴随异界空间粒子 */
    public static class RemnantScreech extends AbyssalCraftAction {
        public RemnantScreech() { super(200); } // 冷却 10s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 12.0D);
            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity).setMagicDamage() :
                    DamageSource.causeMobDamage(entity).setMagicDamage();

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 160, 0)); // 严重反胃 8秒
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 2)); // 缓慢 III (amplifier 2) 6秒
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 160, 1)); // 虚弱 II 8秒
                target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 160, 1));
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + entity.getEyeHeight() * 0.5D, entity.posZ,
                        80, 2.5D, 1.2D, 2.5D, 0.2D);
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        entity.posX, entity.posY + entity.getEyeHeight() * 0.5D, entity.posZ,
                        40, 2.0D, 1.0D, 2.0D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                        30, 1.8D, 0.8D, 1.8D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.NOTE,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                        20, 1.5D, 0.8D, 1.5D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.remnant_scream != null ? ACSounds.remnant_scream : SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 3.0F, 1.0F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.2F, 1.2F);
            return true;
        }
    }

    /**
     * 恶魔动物：地狱火环冲撞 (ID: ac_demon_fire_charge, 冷却 8s)
     * 浑身燃起地狱火冲锋 8 格，点燃沿途敌人并留下火径。
     */
    public static class DemonFireChargeAction extends AbyssalCraftAction {
        public DemonFireChargeAction() { super(160); } // 冷却 8s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 浑身燃起地狱火
            entity.setFire(3);

            // 冲锋推进
            entity.motionX += look.x * 1.6D;
            entity.motionZ += look.z * 1.6D;
            entity.motionY += 0.22D;
            entity.velocityChanged = true;

            // 冲锋前方 8 格判定盒
            AxisAlignedBB chargeBox = entity.getEntityBoundingBox().expand(look.x * 8.0D, 1.0D, look.z * 8.0D).grow(1.5D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, chargeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource fireSrc = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) :
                    DamageSource.causeMobDamage(entity)).setFireDamage();

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(fireSrc, 10.0F);
                target.setFire(6); // 点燃沿途敌人 6 秒
                target.knockBack(entity, 1.3F, -look.x, -look.z);
            }

            // 留下地狱火径与火环粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 16; i++) {
                    double dist = (i / 16.0D) * 8.0D;
                    double px = entity.posX + look.x * dist;
                    double py = entity.posY + 0.1D;
                    double pz = entity.posZ + look.z * dist;
                    ws.spawnParticle(EnumParticleTypes.FLAME, px, py, pz, 4, 0.3D, 0.2D, 0.3D, 0.05D);
                    ws.spawnParticle(EnumParticleTypes.LAVA, px, py, pz, 1, 0.2D, 0.1D, 0.2D, 0.0D);
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 2, 0.2D, 0.2D, 0.2D, 0.02D);
                }
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double px = entity.posX + Math.cos(rad) * 2.0D;
                    double pz = entity.posZ + Math.sin(rad) * 2.0D;
                    ws.spawnParticle(EnumParticleTypes.FLAME, px, entity.posY + 0.3D, pz, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                }
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1.2F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.4F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_FIREBALL_EPLD, SoundCategory.PLAYERS, 1.0F, 1.4F);

            return true;
        }
    }
    public static class DemonFireCharge extends DemonFireChargeAction {}

    /** 暗影生物：潜行暗影披风与隐匿 (ID: ac_shadow_veil) - 隐入阴影帷幕，潜行时获得强化匿踪与暗影护甲 */
    public static class ShadowVeil extends AbyssalCraftAction {
        public ShadowVeil() { super(100); } // 冷却 5s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            boolean sneaking = entity.isSneaking();
            int duration = sneaking ? 240 : 160;

            entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, duration, 0, false, false));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, duration, sneaking ? 2 : 1, false, false));
            entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, duration, 0, false, false));
            if (sneaking) {
                entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 240, 1, false, false));
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ, 25, 0.5D, 0.5D, 0.5D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.PORTAL, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ, 40, 0.6D, 0.6D, 0.6D, 0.1D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shadow_death, SoundCategory.HOSTILE, 1.0F, 0.9F);
            return true;
        }
    }

    /** 暗影生物：暗影巨爪挥击 (ID: ac_shadow_slash) - 横扫身前 4 格敌人造成高额暗影撕裂伤害 */
    public static class ShadowSlashAction extends AbyssalCraftAction {
        public ShadowSlashAction() { super(60); } // 冷却 3s，手感轻快犀利

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d eye = entity.getPositionEyes(1.0F);

            entity.swingArm(EnumHand.MAIN_HAND);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shadow_hurt, SoundCategory.PLAYERS, 1.4F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.6F);

            // 身前 4 格横扫判定盒
            AxisAlignedBB sweepBox = entity.getEntityBoundingBox().grow(3.0D).offset(look.x * 2.0D, look.y * 1.0D, look.z * 2.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, sweepBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource shadowSrc = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                Vec3d toTarget = target.getPositionVector().add(0, target.height * 0.5D, 0).subtract(eye);
                double dist = toTarget.length();
                if (dist <= 4.2D) {
                    double dot = toTarget.normalize().dotProduct(look);
                    if (dot > 0.25D || dist < 2.0D) {
                        // A13：16→15，对齐 morph 可玩性封顶线
                        target.attackEntityFrom(shadowSrc, 15.0F);
                        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
                        target.knockBack(entity, 0.8F, -look.x, -look.z);
                    }
                }
            }

            // 扇形暗影爪光与烟雾粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int angle = -45; angle <= 45; angle += 15) {
                    double rad = Math.toRadians(entity.rotationYaw + 90 + angle);
                    double px = entity.posX + Math.cos(rad) * 2.5D;
                    double pz = entity.posZ + Math.sin(rad) * 2.5D;
                    double py = entity.posY + entity.getEyeHeight() - 0.2D;
                    ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 2, 0.1, 0.1, 0.1, 0.02);
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
                }
            }

            return true;
        }
    }

    /** 暗影生物：融入暗影瞬步 (ID: ac_shadow_step) - 融入暗影并向前或准星敌人背后瞬步 16 格，播放暗影粒子与咆哮，命中施加致盲 4 秒与暗影凋零 */
    public static class ShadowStepAction extends AbyssalCraftAction {
        public ShadowStepAction() { super(80); } // 冷却 4s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            double startX = entity.posX;
            double startY = entity.posY;
            double startZ = entity.posZ;

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 16.0D);
            double targetX, targetY, targetZ;

            if (aim.hasEntity()) {
                EntityLivingBase targetMob = aim.entityTarget;
                Vec3d targetLook = targetMob.getLook(1.0F);
                // 瞬移至准星敌人背后 1.5 格
                targetX = targetMob.posX - targetLook.x * 1.5D;
                targetY = targetMob.posY;
                targetZ = targetMob.posZ - targetLook.z * 1.5D;
            } else {
                Vec3d look = entity.getLook(1.0F);
                if (aim.coordTarget != null && aim.coordTarget.distanceTo(new Vec3d(startX, startY, startZ)) < 15.5D) {
                    targetX = aim.coordTarget.x - look.x * 0.8D;
                    targetY = aim.coordTarget.y;
                    targetZ = aim.coordTarget.z - look.z * 0.8D;
                } else {
                    targetX = entity.posX + look.x * 16.0D;
                    targetY = entity.posY + look.y * 16.0D;
                    targetZ = entity.posZ + look.z * 16.0D;
                }
            }

            // 检查方块防卡实心方块
            BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
            if (world.getBlockState(targetPos).getMaterial().blocksMovement()) {
                targetY = targetPos.getY() + 1.0D;
            }

            entity.setPositionAndUpdate(targetX, targetY, targetZ);

            // 轨迹与落点暗影粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 30; i++) {
                    double progress = i / 30.0D;
                    double px = startX + (targetX - startX) * progress;
                    double py = startY + (targetY - startY) * progress + entity.getEyeHeight() * 0.5D;
                    double pz = startZ + (targetZ - startZ) * progress;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                    ws.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 1, 0.05, 0.05, 0.05, 0.05);
                }
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, targetX, targetY + 0.5D, targetZ, 40, 0.8, 0.5, 0.8, 0.05);
            }

            // 音效：末影传送 + 暗影吼叫
            world.playSound(null, startX, startY, startZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 0.8F);
            world.playSound(null, targetX, targetY, targetZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 0.8F);
            world.playSound(null, targetX, targetY, targetZ, ACSounds.shadow_death, SoundCategory.HOSTILE, 1.2F, 0.6F);

            // 命中判定与负面效果：对落点 4 格内敌人造成暗影伤害并施加致盲 4 秒与暗影凋零 4 秒
            AxisAlignedBB area = entity.getEntityBoundingBox().grow(3.5D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            DamageSource shadowSrc = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(shadowSrc, 12.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 80, 0)); // 致盲 4 秒
                target.addPotionEffect(new PotionEffect(MobEffects.WITHER, 80, 1));    // 暗影凋零 4 秒
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
            }

            return true;
        }
    }
    public static class ShadowStep extends ShadowStepAction {}

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

    /** 反物质苦力怕：反物质湮灭脉冲 (ID: ac_anti_creeper_fuse) - 高能反物质聚变脉冲，全域 10 格无方块破坏且宿主 100% 免伤 */
    public static class AntiCreeperAnnihilationAction extends AbyssalCraftAction {
        public AntiCreeperAnnihilationAction() { super(200); } // 冷却 10s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;

            // 范围 10 格，排除自身与队友，宿主绝对豁免
            List<EntityLivingBase> list = findNearbyEntities(entity, 10.0D);
            DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase e : list) {
                // A13：单发 26 超 E2 红线（Boss 级 ≤15），收敛到 15
                e.attackEntityFrom(source, 15.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 200, 1));
                }
                Vec3d diff = e.getPositionVector().subtract(entity.getPositionVector()).normalize();
                e.motionX += diff.x * 1.4D;
                e.motionY += 0.55D;
                e.motionZ += diff.z * 1.4D;
                e.velocityChanged = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, entity.posX, entity.posY + 1.0D, entity.posZ, 4, 1.0D, 0.5D, 1.0D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.PORTAL, entity.posX, entity.posY + 1.0D, entity.posZ, 200, 4.5D, 2.5D, 4.5D, 0.25D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + 0.5D, entity.posZ, 80, 3.5D, 1.0D, 3.5D, 0.08D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0F, 0.5F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS, 1.8F, 1.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.antiplayer_hurt != null ? ACSounds.antiplayer_hurt : SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.4F, 0.7F);
            return true;
        }
    }

    /** 反物质玩家：全能型反物质武器挥砍与反冲位移 (ID: ac_anti_player_slash) */
    public static class AntiPlayerSlashAction extends AbyssalCraftAction {
        public AntiPlayerSlashAction() { super(60); } // 冷却 3s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            if (entity.isSneaking()) {
                // === 潜行模式：反物质反冲规避 (Antimatter Recoil Disengage) ===
                entity.motionX -= look.x * 1.4D;
                entity.motionZ -= look.z * 1.4D;
                entity.motionY += 0.25D;
                entity.velocityChanged = true;

                entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 1, false, false));
                entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 60, 0, false, false));

                // 驱退近身目标
                List<EntityLivingBase> list = findNearbyEntities(entity, 3.5D);
                DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
                for (EntityLivingBase e : list) {
                    e.attackEntityFrom(source, 8.0F);
                    if (AbyssalCraftAPI.antimatter_potion != null) {
                        e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 60, 0));
                    }
                    e.knockBack(entity, 1.2F, look.x, look.z);
                }

                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.PORTAL, entity.posX, entity.posY + 0.5D, entity.posZ, 40, 0.5D, 0.5D, 0.5D, 0.1D);
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX, entity.posY + 0.5D, entity.posZ, 4, 0.2D, 0.2D, 0.2D, 0.05D);
                }

                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 1.2F);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.antiplayer_hurt != null ? ACSounds.antiplayer_hurt : SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.2F, 1.5F);
                return true;
            }

            // === 正常模式：反物质次元斩击 (Antimatter Dimensional Slash) ===
            entity.swingArm(EnumHand.MAIN_HAND);
            entity.motionX += look.x * 1.2D;
            entity.motionZ += look.z * 1.2D;
            entity.velocityChanged = true;

            Vec3d eye = entity.getPositionEyes(1.0F);
            AxisAlignedBB sweepBox = entity.getEntityBoundingBox().grow(3.5D).offset(look.x * 2.0D, look.y * 0.5D, look.z * 2.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, sweepBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase target : list) {
                Vec3d toTarget = target.getPositionVector().add(0, target.height * 0.5D, 0).subtract(eye);
                double dist = toTarget.length();
                if (dist <= 5.0D) {
                    double dot = toTarget.normalize().dotProduct(look);
                    if (dot > 0.2D || dist < 2.0D) {
                        // A13：16→15，对齐 morph 可玩性封顶线
                        target.attackEntityFrom(source, 15.0F);
                        if (AbyssalCraftAPI.antimatter_potion != null) {
                            target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 120, 0));
                        }
                        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
                        target.knockBack(entity, 1.0F, -look.x, -look.z);
                    }
                }
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int angle = -45; angle <= 45; angle += 15) {
                    double rad = Math.toRadians(entity.rotationYaw + 90 + angle);
                    double px = entity.posX + Math.cos(rad) * 2.5D;
                    double pz = entity.posZ + Math.sin(rad) * 2.5D;
                    double py = entity.posY + entity.getEyeHeight() - 0.2D;
                    ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    ws.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 3, 0.1, 0.1, 0.1, 0.05);
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
                }
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.antiplayer_hurt != null ? ACSounds.antiplayer_hurt : SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.3F, 1.0F);
            return true;
        }
    }

    /** 反物质生物：反物质内爆 (旧版兼容) */
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

    /** 反物质生物：凝聚反物质微粒爆发 (ID: ac_antimatter_burst) - 对周围实体施加反物质状态与湮灭冲击 */
    public static class AntimatterBurstAction extends AbyssalCraftAction {
        public AntimatterBurstAction() { super(300); } // 冷却 15s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            List<EntityLivingBase> list = findNearbyEntities(entity, 7.0D);
            DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase e : list) {
                e.attackEntityFrom(source, 14.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 160, 0));
                }
                Vec3d diff = e.getPositionVector().subtract(entity.getPositionVector()).normalize();
                e.motionX += diff.x * 0.85D;
                e.motionY += 0.35D;
                e.motionZ += diff.z * 0.85D;
                e.velocityChanged = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL, entity.posX, entity.posY + 1.0D, entity.posZ, 120, 2.5D, 1.0D, 2.5D, 0.15D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + 0.5D, entity.posZ, 40, 2.0D, 0.5D, 2.0D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, entity.posX, entity.posY + 1.0D, entity.posZ, 5, 1.5D, 0.5D, 1.5D, 0.0D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.antiplayer_hurt, SoundCategory.HOSTILE, 1.4F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5F, 1.6F);
            return true;
        }
    }

    /**
     * 扎哈尔：终焉湮灭 (ID: ac_jzahar_oblivion)。
     * 真·终焉 AOE（NEW#4 绑定修复的目标类）：与 ac_jzahar_singularity（召唤内爆/黑洞实体）分化，
     * 以自身为中心引爆终焉湮灭力场——12 格内敌对生物承受湮灭直伤 + 暗影追伤 + 凋零 + 浮空，
     * 并被引力拽向中心；不破坏任何方块。
     */
    public static class JzaharOblivion extends AbyssalCraftAction {
        public JzaharOblivion() { super(220); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            List<EntityLivingBase> list = findNearbyEntities(entity, 12.0D);
            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                // A13：湮灭直伤 22 超 E2 Boss 红线（≤15），收敛到 15；暗影追伤 8 保留（单发 ≤8）
                e.attackEntityFrom(source, 15.0F);
                if (AbyssalCraftAPI.shadow != null) {
                    e.attackEntityFrom(AbyssalCraftAPI.shadow, 8.0F);
                }
                e.addPotionEffect(new PotionEffect(MobEffects.WITHER, 160, 1));
                e.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 50, 0));
                // 终焉引力：向湮灭中心拖拽
                Vec3d pull = entity.getPositionVector().subtract(e.getPositionVector()).normalize();
                e.motionX += pull.x * 0.8D;
                e.motionY += 0.3D;
                e.motionZ += pull.z * 0.8D;
                e.velocityChanged = true;
            }

            // 终焉湮灭力场视觉：中心奇点坍缩 + 扩散湮灭环
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                        entity.posX, entity.posY + 1.2D, entity.posZ,
                        3, 1.5D, 0.8D, 1.5D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        entity.posX, entity.posY + 0.8D, entity.posZ,
                        120, 5.5D, 1.6D, 5.5D, 0.04D);
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        200, 5.0D, 2.0D, 5.0D, 0.2D);
                for (int angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    double px = entity.posX + Math.cos(rad) * 6.0D;
                    double pz = entity.posZ + Math.sin(rad) * 6.0D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 0.3D, pz,
                            2, 0.1D, 0.3D, 0.1D, 0.02D);
                }
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 2.0F, 0.4F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.jzahar_black_hole, SoundCategory.HOSTILE, 3.0F, 1.0F);
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

    /** 反物质生物：反物质脉冲射击 (Antimatter Blast) - 自由贯穿光束重构版 */
    public static class AntimatterBlast extends AbyssalCraftAction {
        public AntimatterBlast() { super(90); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d eyes = entity.getPositionEyes(1.0F);
            double maxDist = 24.0D;
            Vec3d end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

            // 检查方块阻挡
            RayTraceResult blockHit = world.rayTraceBlocks(eyes, end, false, true, false);
            Vec3d impactPoint = blockHit != null ? blockHit.hitVec : end;

            // 沿光束检测所有贯穿的实体
            AxisAlignedBB beamBox = new AxisAlignedBB(eyes.x, eyes.y, eyes.z, impactPoint.x, impactPoint.y, impactPoint.z).grow(1.2D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, beamBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);
            for (EntityLivingBase e : list) {
                AxisAlignedBB aabb = e.getEntityBoundingBox().grow(0.4D);
                RayTraceResult intercept = aabb.calculateIntercept(eyes, impactPoint);
                if (intercept != null || e.getDistanceSq(eyes.x, eyes.y, eyes.z) < 9.0D) {
                    e.attackEntityFrom(source, 12.0F);
                    if (AbyssalCraftAPI.antimatter_potion != null) {
                        e.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 100, 0));
                    } else {
                        e.addPotionEffect(new PotionEffect(MobEffects.WITHER, 100, 1));
                    }
                }
            }

            // 服务端绘制反物质暗能光束粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double dist = eyes.distanceTo(impactPoint);
                int steps = Math.max(1, (int) (dist / 0.5D));
                Vec3d dir = impactPoint.subtract(eyes).normalize();
                for (int i = 0; i <= steps; i++) {
                    Vec3d p = eyes.add(dir.x * i * 0.5D, dir.y * i * 0.5D, dir.z * i * 0.5D);
                    ws.spawnParticle(EnumParticleTypes.PORTAL, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.05);
                    ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, impactPoint.x, impactPoint.y, impactPoint.z, 5, 0.2, 0.2, 0.2, 0.05);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.8F);
            return true;
        }
    }

    /**
     * 远古骷髅歌利亚 / 渊素傀儡：巨灵撼地重踏 (ID: ac_goliath_smash, 冷却 5s)
     * 4.5米远古巨像践踏大地，撼碎地面引发 6 格冲击波，震飞并重创周围所有敌人。
     */
    public static class GoliathSmashAction extends AbyssalCraftAction {
        public GoliathSmashAction() { super(100); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            entity.swingArm(EnumHand.MAIN_HAND);

            // 6 格地面震波判定盒
            AxisAlignedBB slamBox = entity.getEntityBoundingBox().grow(5.5D, 2.0D, 5.5D).offset(look.x * 1.5D, 0.0D, look.z * 1.5D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, slamBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            for (EntityLivingBase e : list) {
                // A13：16→15，对齐 morph 可玩性封顶线
                e.attackEntityFrom(source, 15.0F);
                e.motionY += 0.65D;
                e.motionX += (e.posX - entity.posX) * 0.4D;
                e.motionZ += (e.posZ - entity.posZ) * 0.4D;
                e.velocityChanged = true;
                e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2));
            }

            // 震地冲击波骨屑与巨石粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double cx = entity.posX + look.x * 1.2D;
                double cz = entity.posZ + look.z * 1.2D;
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double px = cx + Math.cos(rad) * 4.0D;
                    double pz = cz + Math.sin(rad) * 4.0D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 0.2D, pz, 3, 0.1, 0.2, 0.1, 0.04);
                    ws.spawnParticle(EnumParticleTypes.CRIT, px, entity.posY + 0.3D, pz, 4, 0.1, 0.1, 0.1, 0.05);
                }
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, entity.posY + 0.3D, cz, 3, 0.6, 0.2, 0.6, 0.0);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.HOSTILE, 1.8F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.2F, 0.5F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SKELETON_HURT, SoundCategory.HOSTILE, 1.5F, 0.5F);

            return true;
        }
    }

    /**
     * 反物质蜘蛛：反物质跃击与虚空织网 (ID: ac_anti_spider_leap, 冷却 3.5s)
     * 极速向准星腾空飞扑，落地在周围激荡反物质丝网微粒，使目标深陷强力迟缓与反物质虚弱。
     */
    public static class AntiSpiderLeapAction extends AbyssalCraftAction {
        public AntiSpiderLeapAction() { super(70); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 跃起突进
            entity.motionX += look.x * 1.5D;
            entity.motionY += 0.45D;
            entity.motionZ += look.z * 1.5D;
            entity.velocityChanged = true;

            // 身前判定盒
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(3.0D, 1.5D, 3.0D).offset(look.x * 2.2D, 0.2D, look.z * 2.2D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = AbyssalCraftAPI.antimatter != null ?
                    AbyssalCraftAPI.antimatter :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 10.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 100, 0));
                }
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2));
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 1));
                target.knockBack(entity, 0.8F, -look.x, -look.z);
                hit = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 2.0D;
                double py = entity.posY + 0.3D;
                double pz = entity.posZ + look.z * 2.0D;
                ws.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 40, 1.2D, 0.5D, 1.2D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.END_ROD, px, py, pz, 15, 0.8D, 0.3D, 0.8D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1.3F, 1.2F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.8F, 1.8F);
            if (hit) {
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 1.2F, 1.4F);
            }

            return true;
        }
    }

    /**
     * 反物质骷髅：反物质虚空箭雨 (ID: ac_anti_skeleton_arrow, 冷却 2s)
     * 拉开虚空骨弓，向准星高速射出反物质侵蚀箭矢，命中引发虚空湮灭冲击。
     */
    public static class AntiSkeletonArrowAction extends AbyssalCraftAction {
        public AntiSkeletonArrowAction() { super(40); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 1.8D);

            net.minecraft.entity.projectile.EntityTippedArrow arrow = new net.minecraft.entity.projectile.EntityTippedArrow(world, entity);
            arrow.setPosition(safe.x, safe.y, safe.z);
            arrow.shoot(look.x, look.y + 0.05D, look.z, 2.2F, 1.0F);
            arrow.setDamage(8.0D);
            if (AbyssalCraftAPI.antimatter_potion != null) {
                arrow.addEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 100, 0));
            } else {
                arrow.addEffect(new PotionEffect(MobEffects.WITHER, 80, 1));
            }
            BossNativeHelper.tagOwner(arrow, entity);
            world.spawnEntity(arrow);

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL, safe.x, safe.y, safe.z, 15, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.2F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.6F, 2.0F);

            return true;
        }
    }

    /**
     * 邪恶动物：恶念冲顶与凶暴突进 (ID: ac_evil_charge, 冷却 4s)
     * 凶狠向前冲刺撞击前方敌人，造成强力击退并发出恶念尖啸。
     */
    public static class EvilChargeAction extends AbyssalCraftAction {
        public EvilChargeAction() { super(80); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            entity.motionX += look.x * 1.4D;
            entity.motionZ += look.z * 1.4D;
            entity.motionY += 0.22D;
            entity.velocityChanged = true;

            AxisAlignedBB box = entity.getEntityBoundingBox().grow(2.5D, 1.2D, 2.5D).offset(look.x * 1.8D, 0.2D, look.z * 1.8D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 10.0F);
                target.knockBack(entity, 1.4F, -look.x, -look.z);
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 60, 0));
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.5D;
                double pz = entity.posZ + look.z * 1.5D;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 0.3D, pz, 15, 0.4D, 0.2D, 0.4D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY, px, entity.posY + 0.5D, pz, 5, 0.3D, 0.2D, 0.3D, 0.0D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 1.0F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 1.2F, 0.7F);

            return true;
        }
    }

    private AbyssalActions() { }
}
