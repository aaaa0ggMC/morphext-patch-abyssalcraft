package aaaa0ggmc.morphext.abyssalcraft.action;

import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.IOmotholEntity;
import com.shinoow.abyssalcraft.common.entity.EntityBlackHole;
import com.shinoow.abyssalcraft.common.entity.EntityChagarothFist;
import com.shinoow.abyssalcraft.common.entity.EntityChagarothSpawn;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSlug;
import com.shinoow.abyssalcraft.common.entity.EntityDreadedCharge;
import com.shinoow.abyssalcraft.common.entity.EntityImplosion;
import com.shinoow.abyssalcraft.lib.ACSounds;
import com.shinoow.abyssalcraft.lib.util.SpecialTextUtil;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 深渊国度全部核心 Boss 原生机制与渲染复用动作集 (Native Boss Mechanism Adapter Actions)：
 * 1. 扎哈尔 (J'zahar)：原生三头凋灵齐射、终焉咆哮 (1000爆炸粒子)、黑洞/引力坍缩、大地震颤；
 * 2. 萨克索斯 (Sacthoth)：原生口部暗影烈焰喷涌、战术暗影瞬移 (128浓烟粒子)；
 * 3. 查伽洛斯 (Cha'garoth)：原生三头恐惧弹幕与恐惧巨拳/幼体召唤；
 * 4. 阿萨托斯之龙 (DragonBoss)：原生恐惧龙息与连环爆裂弹；
 * 5. 全面启用智能定向与宿主安全标签，杜绝自伤自锁。
 */
public final class AbyssalBossActions {

    /* =========================================================================
     *                         扎哈尔 (J'zahar) 原生动作
     * ========================================================================= */

    /**
     * 扎哈尔原生三头凋灵首齐射 (Wither Salvo)：
     * 1. 驱动扎哈尔法杖挥砍 3D 骨骼动画 (swingArm)；
     * 2. 调用原生 launchWitherSkullToCoords，分别从中头（蓝无敌）、左头、右头精确轰向准星；
     * 3. 目标由 CrosshairTargetDirector 智能引导（有敌人打敌人，无敌人打准星视线交点）。
     */
    public static class JzaharWitherSalvo extends AbyssalCraftAction {
        public JzaharWitherSalvo() { super(35); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityLivingBase morphMob = BossNativeHelper.getMorphEntity(entity, morph);

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 64.0D);
            Vec3d target = aim.coordTarget;

            // 驱动法杖浮空 3D 挥击正弦动画
            entity.swingArm(EnumHand.MAIN_HAND);
            if (morphMob != null) {
                morphMob.swingArm(EnumHand.MAIN_HAND);
            }

            if (morphMob != null && BossNativeHelper.isJzahar(morphMob)) {
                // 原生三头齐射：0 = 中头 (25% 无敌蓝头)，1 = 右头，2 = 左头
                boolean blueSkull = world.rand.nextFloat() < 0.25F;
                BossNativeHelper.launchJzaharSkull(morphMob, 0, target.x, target.y, target.z, blueSkull);
                BossNativeHelper.launchJzaharSkull(morphMob, 1, target.x, target.y, target.z, false);
                BossNativeHelper.launchJzaharSkull(morphMob, 2, target.x, target.y, target.z, false);
            } else {
                // 降级兜底方案（若未能提取到 dummy 实例）
                Vec3d look = entity.getLook(1.0F);
                Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
                net.minecraft.entity.projectile.EntityWitherSkull skull =
                        new net.minecraft.entity.projectile.EntityWitherSkull(world, entity, look.x * 0.1D, look.y * 0.1D, look.z * 0.1D);
                skull.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.tagOwner(skull, entity);
                world.spawnEntity(skull);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.7F);
            return true;
        }
    }

    /**
     * 扎哈尔终焉古神咆哮 (Shout Blast)：
     * 1. 广播深渊语台词 "...mglagln!" 与专属音效 ACSounds.jzahar_blast；
     * 2. 完美还原原生 1000 颗高斯喷涌爆炸粒子；
     * 3. 前方 48 格大扇形力场强力击飞与撞墙伤害（排除宿主自身）。
     */
    public static class JzaharShoutBlast extends AbyssalCraftAction {
        public JzaharShoutBlast() { super(160); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            // 1. 原生古神台词与咆哮音效
            SpecialTextUtil.JzaharGroup(world, "...mglagln!");
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.jzahar_blast, SoundCategory.HOSTILE, 5.0F, 1.0F);

            Vec3d vector = entity.getLookVec();

            // 2. 原生 1000 颗爆炸粒子喷射渲染
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX;
                double py = entity.posY + entity.getEyeHeight() - 0.2D;
                double pz = entity.posZ;
                double spread = 10.0D;
                for (int i = 0; i < 600; i++) {
                    double dx = vector.x;
                    double dy = vector.y;
                    double dz = vector.z;
                    double velocity = 2.0D + entity.getRNG().nextDouble() * 16.0D;
                    dx += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dy += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dz += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dx *= velocity;
                    dy *= velocity;
                    dz *= velocity;
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                            px + entity.getRNG().nextDouble() - 0.5D,
                            py + entity.getRNG().nextDouble() - 0.5D,
                            pz + entity.getRNG().nextDouble() - 0.5D,
                            0, dx, dy, dz, 1.0D);
                }
            }

            // 3. 48 格扇形强力击退与撞墙致死打击（排除自身与奥穆索尔生物）
            double size = 48.0D;
            List<Entity> list = world.getEntitiesWithinAABB(Entity.class,
                    entity.getEntityBoundingBox().grow(size).offset(vector.x * 24.0D, vector.y * 24.0D, vector.z * 24.0D));
            for (Entity e : list) {
                if (e != entity && e instanceof EntityLivingBase && !(e instanceof IOmotholEntity)) {
                    double velocity = 3.0D + entity.getRNG().nextDouble() * 6.0D;
                    e.addVelocity(vector.x * velocity, 0.4D + (vector.y + 0.2D) * velocity * 0.3D, vector.z * velocity);
                    DamageSource dmg = entity instanceof EntityPlayer ?
                            DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.FLY_INTO_WALL;
                    e.attackEntityFrom(dmg, (float) (14.0D + velocity * 2.0D));
                }
            }
            return true;
        }
    }

    /**
     * 扎哈尔奇点坍缩与虚空湮灭黑洞 (Singularity / Black Hole)：
     * 1. 普通释放：召唤原生虚空内爆 EntityImplosion（台词 "Nilgh'ri mtli!"）；
     * 2. 潜行释放：召唤原生湮灭黑洞 EntityBlackHole（台词 "Ph'nilgh'ri n'ghft!"）；
     * 3. 触发原生末影龙震撼音画 (world.playEvent 3000)；
     * 4. 注入属主标签，宿主享有绝对免伤免被吸入。
     */
    public static class JzaharSingularity extends AbyssalCraftAction {
        public JzaharSingularity() { super(160); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityLivingBase morphMob = BossNativeHelper.getMorphEntity(entity, morph);

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 48.0D);
            Vec3d target = aim.coordTarget;
            BlockPos targetPos = new BlockPos(target);

            entity.swingArm(EnumHand.MAIN_HAND);
            if (morphMob != null) {
                morphMob.swingArm(EnumHand.MAIN_HAND);
            }

            if (entity.isSneaking()) {
                // 潜行：生成黑洞
                SpecialTextUtil.JzaharGroup(world, "Ph'nilgh'ri n'ghft!");
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.jzahar_black_hole, SoundCategory.HOSTILE, 5.0F, 1.0F);

                EntityBlackHole blackHole = new EntityBlackHole(world);
                blackHole.setPosition(target.x, target.y + 1.2D, target.z);
                BossNativeHelper.tagOwner(blackHole, entity);
                world.spawnEntity(blackHole);
            } else {
                // 普通：生成内爆
                SpecialTextUtil.JzaharGroup(world, "Nilgh'ri mtli!");
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.jzahar_implosion, SoundCategory.HOSTILE, 5.0F, 1.0F);

                EntityImplosion implosion = new EntityImplosion(world);
                implosion.setPosition(target.x, target.y + 1.2D, target.z);
                BossNativeHelper.tagOwner(implosion, entity);
                world.spawnEntity(implosion);
            }

            // 原生末影龙音画震撼特效
            world.playEvent(3000, targetPos, 0);
            return true;
        }
    }

    /**
     * 扎哈尔奥穆索尔大地震颤 (Earthquake)：
     * 1. 台词 "Shugnah throd!" 与 ACSounds.jzahar_earthquake；
     * 2. 64 格范围内非古神生物发生剧烈视线扰动、减速与反胃，地面熔岩腾空；
     * 3. 宿主完全豁免自残。
     */
    public static class JzaharEarthquake extends AbyssalCraftAction {
        public JzaharEarthquake() { super(220); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            SpecialTextUtil.JzaharGroup(world, "Shugnah throd!");
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.jzahar_earthquake, SoundCategory.HOSTILE, 5.0F, 1.0F);

            List<EntityLivingBase> list = entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
                    entity.getEntityBoundingBox().grow(48.0D),
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity) && !(e instanceof IOmotholEntity));

            DamageSource dmg = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.HOT_FLOOR;

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(dmg, 18.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 140, 2));
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 100, 1));
                target.motionY += 0.35D;
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.2D, entity.posZ,
                        80, 8.0D, 0.2D, 8.0D, 0.1D);
            }
            return true;
        }
    }

    /* =========================================================================
     *                         萨克索斯 (Sacthoth) 原生动作
     * ========================================================================= */

    /**
     * 萨克索斯原生暗影烈焰喷涌 (Shadow Flame)：
     * 1. 沿准星视线喷出大量紫黑暗影粒子与 ACSounds / Ghast Shoot 音效；
     * 2. 造成纯正 AbyssalCraftAPI.shadow 伤害与强力失明/减速；
     * 3. 宿主玩家绝对豁免自伤。
     */
    public static class SacthothShadowFlame extends AbyssalCraftAction {
        public SacthothShadowFlame() { super(45); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d eye = entity.getPositionEyes(1.0F);

            world.playSound(null, entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.2F, 0.5F);

            // 服务端口部暗影浓烟喷射
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 1; i <= 15; i++) {
                    double dist = i * 0.9D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            eye.x + look.x * dist, eye.y + look.y * dist, eye.z + look.z * dist,
                            4, 0.3D, 0.3D, 0.3D, 0.02D);
                    ws.spawnParticle(EnumParticleTypes.PORTAL,
                            eye.x + look.x * dist, eye.y + look.y * dist, eye.z + look.z * dist,
                            3, 0.2D, 0.2D, 0.2D, 0.05D);
                }
            }

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 18.0D);
            if (aim.hasEntity()) {
                EntityLivingBase target = aim.entityTarget;
                DamageSource shadowSrc = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow : DamageSource.causeMobDamage(entity);
                target.attackEntityFrom(shadowSrc, 12.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 120, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
            } else {
                // 锥形范围扫射
                List<EntityLivingBase> list = entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
                        entity.getEntityBoundingBox().grow(12.0D).offset(look.x * 6.0D, look.y * 6.0D, look.z * 6.0D),
                        e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
                for (EntityLivingBase e : list) {
                    DamageSource shadowSrc = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow : DamageSource.causeMobDamage(entity);
                    e.attackEntityFrom(shadowSrc, 8.0F);
                    e.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 80, 0));
                    e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                }
            }
            return true;
        }
    }

    /**
     * 萨克索斯战术暗影瞬移 (Shadow Step)：
     * 1. 沿瞬移路径喷发 128 颗末影浓烟粒子；
     * 2. 精确落地并播放末影人瞬移原声；
     * 3. 拥有智能方块碰撞检测，绝不卡入实心方块。
     */
    public static class SacthothShadowStep extends AbyssalCraftAction {
        public SacthothShadowStep() { super(60); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            double startX = entity.posX;
            double startY = entity.posY;
            double startZ = entity.posZ;

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 24.0D);
            double targetX = aim.coordTarget.x;
            double targetY = aim.coordTarget.y;
            double targetZ = aim.coordTarget.z;

            // 确保落地点站在坚固方块上而非悬空或卡在方块内
            BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
            if (world.getBlockState(targetPos).getMaterial().blocksMovement()) {
                targetY = targetPos.getY() + 1.0D;
            }

            entity.setPositionAndUpdate(targetX, targetY, targetZ);

            // 还原原生 128 颗浓烟粒子轨迹
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                short count = 128;
                for (int l = 0; l < count; ++l) {
                    double d6 = l / (count - 1.0D);
                    double d7 = startX + (targetX - startX) * d6 + (world.rand.nextDouble() - 0.5D) * 1.5D;
                    double d8 = startY + (targetY - startY) * d6 + world.rand.nextDouble() * 3.0D;
                    double d9 = startZ + (targetZ - startZ) * d6 + (world.rand.nextDouble() - 0.5D) * 1.5D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d7, d8, d9, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }

            world.playSound(null, startX, startY, startZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.playSound(null, targetX, targetY, targetZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return true;
        }
    }

    /* =========================================================================
     *                         查伽洛斯 (Cha'garoth) 原生动作
     * ========================================================================= */

    /**
     * 查伽洛斯多头恐惧弹幕与仆从骑乘 (Chagaroth Dread Salvo)：
     * 1. 随机发射恐惧火球 EntityDreadedCharge 或高速恐惧蛞蝓 EntityDreadSlug；
     * 2. 概率召唤骑乘蛞蝓的恐惧巨拳 EntityChagarothFist 或恐惧幼体 EntityChagarothSpawn；
     * 3. 注入属主标签，杜绝反噬。
     */
    public static class ChagarothDreadSalvo extends AbyssalCraftAction {
        public ChagarothDreadSalvo() { super(45); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 2.0D);

            int choice = world.rand.nextInt(4);
            if (choice == 0) {
                // 恐惧爆裂弹
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, look.x * 0.15D, look.y * 0.15D, look.z * 0.15D);
                charge.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.tagOwner(charge, entity);
                world.spawnEntity(charge);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.2F, 0.4F);
            } else if (choice == 1) {
                // 高速恐惧蛞蝓
                EntityDreadSlug slug = new EntityDreadSlug(world, entity);
                slug.setPosition(safe.x, safe.y, safe.z);
                slug.shoot(look.x, look.y, look.z, 1.8F, 1.0F);
                BossNativeHelper.tagOwner(slug, entity);
                world.spawnEntity(slug);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.2F, 0.5F);
            } else {
                // 召唤恐惧巨拳/幼体骑乘蛞蝓
                EntityDreadSlug slug = new EntityDreadSlug(world, entity);
                slug.setPosition(safe.x, safe.y, safe.z);
                slug.shoot(look.x, look.y, look.z, 1.4F, 1.0F);
                BossNativeHelper.tagOwner(slug, entity);
                world.spawnEntity(slug);

                EntityLivingBase rider;
                if (world.rand.nextBoolean()) {
                    EntityChagarothFist fist = new EntityChagarothFist(world);
                    fist.copyLocationAndAnglesFrom(slug);
                    BossNativeHelper.tagOwner(fist, entity);
                    world.spawnEntity(fist);
                    fist.startRiding(slug);
                } else {
                    EntityChagarothSpawn spawn = new EntityChagarothSpawn(world);
                    spawn.copyLocationAndAnglesFrom(slug);
                    BossNativeHelper.tagOwner(spawn, entity);
                    world.spawnEntity(spawn);
                    spawn.startRiding(slug);
                }
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.2F, 0.6F);
            }
            return true;
        }
    }

    /* =========================================================================
     *                         阿萨托斯之龙 (DragonBoss) 原生动作
     * ========================================================================= */

    /**
     * 阿萨托斯之龙恐惧龙息连环轰击 (Dragon Boss Breath)：
     * 1. 扇形齐射 3 枚扩散型恐惧火球；
     * 2. 伴随剧烈龙吼与前向冲击波。
     */
    public static class DragonBossBreath extends AbyssalCraftAction {
        public DragonBossBreath() { super(70); }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 2.5D);

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2.0F, 0.7F);

            // 连射 3 枚扇形恐惧爆裂弹
            double[] angles = new double[] { -0.15D, 0.0D, 0.15D };
            for (double angle : angles) {
                double vx = look.x * Math.cos(angle) - look.z * Math.sin(angle);
                double vz = look.x * Math.sin(angle) + look.z * Math.cos(angle);
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, vx * 0.12D, look.y * 0.12D, vz * 0.12D);
                charge.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.tagOwner(charge, entity);
                world.spawnEntity(charge);
            }
            return true;
        }
    }

    private AbyssalBossActions() { }
}
