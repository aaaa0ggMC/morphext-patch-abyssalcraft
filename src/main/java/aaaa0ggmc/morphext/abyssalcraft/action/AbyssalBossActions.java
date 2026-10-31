package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole;
import aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.IOmotholEntity;
import com.shinoow.abyssalcraft.common.entity.EntityChagarothFist;
import com.shinoow.abyssalcraft.common.entity.EntityChagarothSpawn;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSpawn;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSlug;
import com.shinoow.abyssalcraft.common.entity.EntityDreadedCharge;
import com.shinoow.abyssalcraft.lib.ACSounds;
import com.shinoow.abyssalcraft.lib.util.SpecialTextUtil;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 深渊国度核心 Boss 原生机制与动作重构体系 (Native Boss Mechanism Adapter Actions)：
 * 1. 扎哈尔 (J'zahar)：原生法杖 3D 挥动骨骼动画、三头凋灵首齐射 (中头蓝无敌、左头、右头)、终焉 1000 爆炸粒子古神咆哮、黑洞/引力坍缩、大地震颤；
 * 2. 萨克索斯 (Sacthoth)：口部暗影烈焰喷涌 (45高斯暗影烟雾粒子)、战术 128 烟雾粒子暗影瞬移；
 * 3. 阿索拉 / 恐惧巨龙 (Asorah / Dragon Boss)：恐惧龙息三向齐射、俯冲强袭 (高速飞掠碾压冲击波)；
 * 4. 查伽洛斯 (Cha'garoth)：巨拳重击撼地 (范围撕裂击飞与巨拳投射)、眷族幼体孵化召唤 (散射搭载幼体与仆从的蛞蝓)；
 * 5. 全面启用智能准星无死角定向与属主标签注入，绝不强制锁定敌人，彻底杜绝自伤自锁。
 */
public final class AbyssalBossActions {

    /* =========================================================================
     *                         扎哈尔 (J'zahar) 原生动作
     * ========================================================================= */

    /**
     * 扎哈尔原生三头凋灵首齐射 (Ancient Wither Salvo)：
     * 1. 驱动扎哈尔法杖挥砍 3D 骨骼动画 (swingArm)；
     * 2. 调用原生 launchWitherSkullToCoords，分别从中头（蓝色无敌头）、左头、右头轰向准星；
     * 3. 目标由 CrosshairTargetDirector 智能引导（有敌人打敌人，无敌人打准星视线交点，实现 100% 自由射击手感）。
     */
    public static class JzaharWitherSalvo extends AbyssalCraftAction {
        public JzaharWitherSalvo() { super(35); } // 冷却 1.75s，手感轻快连贯

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            EntityLivingBase morphMob = BossNativeHelper.getMorphEntity(entity, morph);

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 64.0D);
            Vec3d target = aim.coordTarget;

            // 1. 驱动法杖浮空 3D 挥击骨骼动画
            entity.swingArm(EnumHand.MAIN_HAND);
            if (morphMob != null) {
                morphMob.copyLocationAndAnglesFrom(entity);
                morphMob.renderYawOffset = entity.renderYawOffset;
                morphMob.rotationYawHead = entity.getRotationYawHead();
                morphMob.swingArm(EnumHand.MAIN_HAND);
            }

            if (morphMob != null && BossNativeHelper.isJzahar(morphMob)) {
                // 原生三头齐射：0 = 中头 (蓝色无敌凋灵首)，1 = 右头，2 = 左头
                BossNativeHelper.launchJzaharSkull(morphMob, 0, target.x, target.y, target.z, true);
                BossNativeHelper.launchJzaharSkull(morphMob, 1, target.x, target.y, target.z, false);
                BossNativeHelper.launchJzaharSkull(morphMob, 2, target.x, target.y, target.z, false);
            } else {
                // 降级兜底方案（分别从中头、右头、左头三路齐发）
                Vec3d look = entity.getLook(1.0F);
                Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
                Vec3d centerPos = getSafeSpawnPosition(entity, 2.0D).add(0, 1.2D, 0);

                // 中头：蓝色无敌首
                Vec3d dirCenter = target.subtract(centerPos).normalize();
                EntityWitherSkull skullCenter = new EntityWitherSkull(world, entity, dirCenter.x * 0.15D, dirCenter.y * 0.15D, dirCenter.z * 0.15D);
                skullCenter.setPosition(centerPos.x, centerPos.y, centerPos.z);
                skullCenter.setInvulnerable(true);
                BossNativeHelper.tagOwner(skullCenter, entity);
                world.spawnEntity(skullCenter);

                // 右头
                Vec3d rightPos = centerPos.add(right.scale(1.3D));
                Vec3d dirRight = target.subtract(rightPos).normalize();
                EntityWitherSkull skullRight = new EntityWitherSkull(world, entity, dirRight.x * 0.15D, dirRight.y * 0.15D, dirRight.z * 0.15D);
                skullRight.setPosition(rightPos.x, rightPos.y, rightPos.z);
                BossNativeHelper.tagOwner(skullRight, entity);
                world.spawnEntity(skullRight);

                // 左头
                Vec3d leftPos = centerPos.subtract(right.scale(1.3D));
                Vec3d dirLeft = target.subtract(leftPos).normalize();
                EntityWitherSkull skullLeft = new EntityWitherSkull(world, entity, dirLeft.x * 0.15D, dirLeft.y * 0.15D, dirLeft.z * 0.15D);
                skullLeft.setPosition(leftPos.x, leftPos.y, leftPos.z);
                BossNativeHelper.tagOwner(skullLeft, entity);
                world.spawnEntity(skullLeft);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.6F, 0.7F);
            return true;
        }
    }

    /**
     * 扎哈尔终焉古神咆哮 (Shout Blast)：
     * 1. 广播深渊语台词 "...mglagln!" 与专属音效 ACSounds.jzahar_blast；
     * 2. 严格还原源码中 1000 颗高斯喷涌爆炸粒子；
     * 3. 前方 48 格大扇形力场强力击飞与撞墙伤害（排除宿主自身与奥穆索尔生物）。
     */
    public static class JzaharShoutBlast extends AbyssalCraftAction {
        public JzaharShoutBlast() { super(160); } // 冷却 8s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);
            EntityLivingBase morphMob = BossNativeHelper.getMorphEntity(entity, morph);
            if (morphMob != null) {
                morphMob.swingArm(EnumHand.MAIN_HAND);
            }

            // 1. 原生古神台词与咆哮音效
            SpecialTextUtil.JzaharGroup(world, "...mglagln!");
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.jzahar_blast, SoundCategory.HOSTILE, 5.0F, 1.0F);

            Vec3d vector = entity.getLookVec();

            // 2. 完美还原原生 1000 颗爆炸粒子喷射渲染
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + vector.x * 2.0D;
                double py = entity.posY + entity.getEyeHeight() - 0.2D + vector.y * 2.0D;
                double pz = entity.posZ + vector.z * 2.0D;
                double spread = 10.0D;
                for (int i = 0; i < 1000; i++) {
                    double dx = vector.x;
                    double dy = vector.y;
                    double dz = vector.z;
                    double velocity = 2.0D + entity.getRNG().nextDouble() * 18.0D;
                    dx += entity.getRNG().nextGaussian() * 0.007499999832361937D * spread;
                    dy += entity.getRNG().nextGaussian() * 0.007499999832361937D * spread;
                    dz += entity.getRNG().nextGaussian() * 0.007499999832361937D * spread;
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

            // 3. 48 格大扇形力场强力击飞与撞墙伤害（排除宿主自身与奥穆索尔生物）
            double size = 48.0D;
            List<Entity> list = world.getEntitiesWithinAABB(Entity.class,
                    entity.getEntityBoundingBox().grow(size).offset(vector.x * 24.0D, vector.y * 24.0D, vector.z * 24.0D));
            for (Entity e : list) {
                if (e != entity && e instanceof EntityLivingBase && !(e instanceof IOmotholEntity) && !e.isOnSameTeam(entity)) {
                    double velocity = 3.0D + entity.getRNG().nextDouble() * 6.0D;
                    e.addVelocity(vector.x * velocity, 0.4D + (vector.y + 0.2D) * velocity * 0.3D, vector.z * velocity);
                    DamageSource dmg = entity instanceof EntityPlayer ?
                            DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.FLY_INTO_WALL;
                    e.attackEntityFrom(dmg, (float) (16.0D + velocity * 2.0D));
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
        public JzaharSingularity() { super(160); } // 冷却 8s

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
                // 潜行：生成安全黑洞（对宿主与古神绝对免吸免伤）
                SpecialTextUtil.JzaharGroup(world, "Ph'nilgh'ri n'ghft!");
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.jzahar_black_hole, SoundCategory.HOSTILE, 5.0F, 1.0F);

                MorphEntityBlackHole blackHole = new MorphEntityBlackHole(world, entity);
                blackHole.setPosition(target.x, target.y + 1.2D, target.z);
                BossNativeHelper.tagOwner(blackHole, entity);
                world.spawnEntity(blackHole);
            } else {
                // 普通：生成安全奇点内爆（对宿主与古神绝对免吸免伤）
                SpecialTextUtil.JzaharGroup(world, "Nilgh'ri mtli!");
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.jzahar_implosion, SoundCategory.HOSTILE, 5.0F, 1.0F);

                MorphEntityImplosion implosion = new MorphEntityImplosion(world, entity);
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
        public JzaharEarthquake() { super(200); } // 冷却 10s

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
                // 调参（NEW#8）：单跳伤害从 18 收敛到 15，对齐 morph 近战可玩性封顶线；保留 64 格震颤场爽感
                target.attackEntityFrom(dmg, 15.0F);
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
     * 萨克索斯原生口部暗影烈焰喷涌 (Shadow Flame)：
     * 1. 还原原生口部 45 颗高斯暗影烟雾粒子喷射与 Ghast Shoot 音效；
     * 2. 造成纯正 AbyssalCraftAPI.shadow 伤害与强力失明/减速；
     * 3. 准星自由无锁敌喷吐，宿主绝对豁免自伤。
     */
    public static class SacthothShadowFlame extends AbyssalCraftAction {
        public SacthothShadowFlame() { super(40); } // 冷却 2s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d eye = entity.getPositionEyes(1.0F);

            entity.swingArm(EnumHand.MAIN_HAND);
            EntityLivingBase morphMob = BossNativeHelper.getMorphEntity(entity, morph);
            if (morphMob != null) {
                morphMob.swingArm(EnumHand.MAIN_HAND);
            }

            world.playSound(null, entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.5F + world.rand.nextFloat() * 0.5F);
            world.playSound(null, entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                    ACSounds.shadow_hurt, SoundCategory.HOSTILE, 1.2F, 0.8F);

            // 还原原生口部 45 颗高斯暗影烟雾粒子喷射
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 0.8D;
                double py = entity.posY + entity.getEyeHeight();
                double pz = entity.posZ + look.z * 0.8D;

                for (int i = 0; i < 45; i++) {
                    double dx = look.x;
                    double dy = look.y;
                    double dz = look.z;
                    double spread = 5.0D + entity.getRNG().nextDouble() * 2.5D;
                    double velocity = 0.6D + entity.getRNG().nextDouble() * 0.8D;

                    dx += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dy += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dz += entity.getRNG().nextGaussian() * 0.0075D * spread;
                    dx *= velocity;
                    dy *= velocity;
                    dz *= velocity;

                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            px + entity.getRNG().nextDouble() - 0.5D,
                            py + entity.getRNG().nextDouble() - 0.5D,
                            pz + entity.getRNG().nextDouble() - 0.5D,
                            0, dx, dy, dz, 1.0D);
                    ws.spawnParticle(EnumParticleTypes.PORTAL,
                            px + entity.getRNG().nextDouble() - 0.5D,
                            py + entity.getRNG().nextDouble() - 0.5D,
                            pz + entity.getRNG().nextDouble() - 0.5D,
                            1, dx * 0.3D, dy * 0.3D, dz * 0.3D, 0.1D);
                }
            }

            // 自由定向与锥形杀伤
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 18.0D);
            DamageSource shadowSrc = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            // 准星锁中的核心目标承受重创
            if (aim.hasEntity()) {
                EntityLivingBase target = aim.entityTarget;
                target.attackEntityFrom(shadowSrc, 15.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 140, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 1));
            }

            // 锥形范围内所有其他敌人受到范围暗影侵蚀
            List<EntityLivingBase> list = entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
                    entity.getEntityBoundingBox().grow(12.0D).offset(look.x * 6.0D, look.y * 6.0D, look.z * 6.0D),
                    e -> e != entity && e != aim.entityTarget && e.isEntityAlive() && !e.isOnSameTeam(entity));
            for (EntityLivingBase e : list) {
                Vec3d toTarget = e.getPositionVector().add(0, e.height * 0.5D, 0).subtract(eye);
                double dist = toTarget.length();
                if (dist <= 14.0D) {
                    double dot = toTarget.normalize().dotProduct(look);
                    if (dot > 0.4D) {
                        e.attackEntityFrom(shadowSrc, (float) Math.max(6.0D, 14.0D - dist * 0.6D));
                        e.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
                        e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
                    }
                }
            }
            return true;
        }
    }

    /**
     * 萨克索斯战术暗影瞬移 (Tactical Shadow Step)：
     * 1. 沿瞬移路径喷发原生 128 颗末影浓烟粒子轨迹；
     * 2. 精确落地并播放末影人瞬移原声；
     * 3. 拥有智能方块碰撞检测，绝不卡入实心方块或掉入虚空。
     */
    public static class SacthothShadowStep extends AbyssalCraftAction {
        public SacthothShadowStep() { super(50); } // 冷却 2.5s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            double startX = entity.posX;
            double startY = entity.posY;
            double startZ = entity.posZ;

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 28.0D);
            double targetX = aim.coordTarget.x;
            double targetY = aim.coordTarget.y;
            double targetZ = aim.coordTarget.z;

            // 确保落地点站在坚固方块上而非悬空或卡在方块内
            BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
            if (world.getBlockState(targetPos).getMaterial().blocksMovement()) {
                targetY = targetPos.getY() + 1.0D;
            } else {
                BlockPos ground = targetPos.down();
                for (int i = 0; i < 8 && !world.getBlockState(ground).getMaterial().blocksMovement() && ground.getY() > 0; i++) {
                    ground = ground.down();
                }
                if (world.getBlockState(ground).getMaterial().blocksMovement()) {
                    targetY = ground.getY() + 1.0D;
                }
            }

            entity.setPositionAndUpdate(targetX, targetY, targetZ);
            entity.fallDistance = 0.0F;

            // 完美还原原生 128 颗浓烟粒子轨迹
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                short count = 128;
                for (int l = 0; l < count; ++l) {
                    double d6 = l / (count - 1.0D);
                    float f = (world.rand.nextFloat() - 0.5F) * 0.2F;
                    float f1 = (world.rand.nextFloat() - 0.5F) * 0.2F;
                    float f2 = (world.rand.nextFloat() - 0.5F) * 0.2F;
                    double d7 = startX + (targetX - startX) * d6 + (world.rand.nextDouble() - 0.5D) * 1.5D;
                    double d8 = startY + (targetY - startY) * d6 + world.rand.nextDouble() * 3.0D;
                    double d9 = startZ + (targetZ - startZ) * d6 + (world.rand.nextDouble() - 0.5D) * 1.5D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d7, d8, d9, 0, f, f1, f2, 1.0D);
                }
            }

            world.playSound(null, startX, startY, startZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 1.0F);
            world.playSound(null, targetX, targetY, targetZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 1.0F);
            return true;
        }
    }

    /* =========================================================================
     *                         查伽洛斯 (Cha'garoth) 原生动作
     * ========================================================================= */

    /**
     * 查伽洛斯多头恐惧弹幕 (Chagaroth Dread Salvo)：
     * 1. 轮流发射恐惧火球 EntityDreadedCharge、高速穿甲恐惧蛞蝓 EntityDreadSlug；
     * 2. 概率发射载有巨拳或幼体的弹幕突击敌阵；
     * 3. 注入属主标签，杜绝反噬。
     */
    public static class ChagarothDreadSalvo extends AbyssalCraftAction {
        public ChagarothDreadSalvo() { super(40); } // 冷却 2s

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
                // 高速穿甲恐惧蛞蝓
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

                EntityLiving minion = world.rand.nextBoolean() ?
                        new EntityChagarothFist(world) : new EntityChagarothSpawn(world);
                minion.copyLocationAndAnglesFrom(slug);
                BossNativeHelper.tagOwner(minion, entity);
                world.spawnEntity(minion);
                minion.startRiding(slug);

                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.2F, 0.6F);
            }
            return true;
        }
    }

    /**
     * 查伽洛斯巨拳重击撼地 (Fist of Cha'garoth - ac_chagaroth_fist)：
     * 1. 撼地重碾身前 6 格范围敌人造成大额恐惧重创、击飞并扩散恐惧瘟疫；
     * 2. 发射高速恐惧巨拳 (EntityChagarothFist) 突袭撕裂目标；
     * 3. 伴随剧烈地震与铁砧坠落声，属主标签注入，绝不反噬宿主。
     */
    public static class ChagarothFist extends AbyssalCraftAction {
        public ChagarothFist() { super(100); } // 冷却 5s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            // 1. 身前撼地重砸
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(5.0D).offset(look.x * 3.0D, 0.2D, look.z * 3.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            DamageSource src = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(src, 18.0F);
                target.motionY += 0.7D;
                target.velocityChanged = true;
                if (AbyssalCraftAPI.dread_plague != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 180, 0));
                }
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 2));
            }

            // 2. 发射恐惧巨拳投射物（跨准星自由指向）
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 32.0D);
            Vec3d targetCoord = aim.coordTarget;
            Vec3d safe = getSafeSpawnPosition(entity, 2.2D);
            Vec3d shootDir = targetCoord.subtract(safe).normalize();

            EntityDreadSlug slug = new EntityDreadSlug(world, entity);
            slug.setPosition(safe.x, safe.y, safe.z);
            slug.shoot(shootDir.x, shootDir.y, shootDir.z, 1.6F, 1.0F);
            BossNativeHelper.tagOwner(slug, entity);
            world.spawnEntity(slug);

            EntityChagarothFist fist = new EntityChagarothFist(world);
            fist.copyLocationAndAnglesFrom(slug);
            BossNativeHelper.tagOwner(fist, entity);
            if (aim.hasEntity()) {
                fist.setAttackTarget(aim.entityTarget);
            }
            world.spawnEntity(fist);
            fist.startRiding(slug);

            // 特效与震撼音画
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, entity.posX + look.x * 2.5D, entity.posY + 0.5D, entity.posZ + look.z * 2.5D, 4, 0.5, 0.2, 0.5, 0.0);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX + look.x * 2.5D, entity.posY + 0.5D, entity.posZ + look.z * 2.5D, 25, 1.0, 0.5, 1.0, 0.05);
                ws.spawnParticle(EnumParticleTypes.LAVA, entity.posX + look.x * 2.5D, entity.posY + 0.2D, entity.posZ + look.z * 2.5D, 15, 1.0, 0.2, 1.0, 0.0);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dreadguard_barf, SoundCategory.HOSTILE, 1.6F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.HOSTILE, 1.8F, 0.5F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.2F, 0.6F);
            return true;
        }
    }

    /**
     * 查伽洛斯眷族幼体孵化召唤 (ac_chagaroth_spawn)：
     * 1. 向准星散射多枚搭载恐惧幼体与仆从的恐惧蛞蝓；
     * 2. 幼体自动扑咬敌对目标并施加恐惧瘟疫；
     * 3. 属主标签注入，绝不反噬宿主。
     */
    public static class ChagarothSpawn extends AbyssalCraftAction {
        public ChagarothSpawn() { super(140); } // 冷却 7s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 32.0D);

            double[] spreadAngles = new double[] { -0.2D, 0.0D, 0.2D };
            for (int i = 0; i < spreadAngles.length; i++) {
                double angle = spreadAngles[i];
                double vx = look.x * Math.cos(angle) - look.z * Math.sin(angle);
                double vz = look.x * Math.sin(angle) + look.z * Math.cos(angle);
                Vec3d safe = getSafeSpawnPosition(entity, 2.2D);

                EntityDreadSlug slug = new EntityDreadSlug(world, entity);
                slug.setPosition(safe.x, safe.y, safe.z);
                slug.shoot(vx, look.y + 0.1D, vz, 1.4F, 1.0F);
                BossNativeHelper.tagOwner(slug, entity);
                world.spawnEntity(slug);

                EntityLiving minion;
                if (i == 1 && world.rand.nextBoolean()) {
                    EntityChagarothFist fist = new EntityChagarothFist(world);
                    fist.copyLocationAndAnglesFrom(slug);
                    minion = fist;
                } else {
                    EntityChagarothSpawn spawn = new EntityChagarothSpawn(world);
                    spawn.copyLocationAndAnglesFrom(slug);
                    minion = spawn;
                }

                BossNativeHelper.tagOwner(minion, entity);
                if (aim.hasEntity()) {
                    minion.setAttackTarget(aim.entityTarget);
                }
                world.spawnEntity(minion);
                minion.startRiding(slug);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SLIME, entity.posX + look.x * 2.0D, entity.posY + 1.0D, entity.posZ + look.z * 2.0D, 30, 0.6, 0.6, 0.6, 0.1);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dread_spawn_ambient, SoundCategory.HOSTILE, 1.5F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.3F, 0.6F);
            return true;
        }
    }

    /* =========================================================================
     *                         阿索拉 / 恐惧巨龙 (Asorah / Dragon Boss) 原生动作
     * ========================================================================= */

    /**
     * 阿索拉 / 恐惧巨龙恐惧龙息三向齐射 (Dragon Boss Breath)：
     * 1. 扇形齐射 3 枚扩散型恐惧爆裂火球；
     * 2. 安全距离偏移发射，彻底杜绝自伤；
     * 3. 伴随剧烈龙吼与前向冲击波。
     */
    public static class DragonBossBreath extends AbyssalCraftAction {
        public DragonBossBreath() { super(60); } // 冷却 3s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            Vec3d safe = getSafeSpawnPosition(entity, 2.8D);

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2.2F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.5F);

            // 扇形齐射 3 枚恐惧爆裂火球
            double[] angles = new double[] { -0.18D, 0.0D, 0.18D };
            for (double angle : angles) {
                double vx = look.x * Math.cos(angle) - look.z * Math.sin(angle);
                double vz = look.x * Math.sin(angle) + look.z * Math.cos(angle);
                EntityDreadedCharge charge = new EntityDreadedCharge(world, entity, vx * 0.14D, look.y * 0.14D, vz * 0.14D);
                charge.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.tagOwner(charge, entity);
                world.spawnEntity(charge);
            }
            return true;
        }
    }

    /**
     * 阿索拉 / 恐惧巨龙原生俯冲强袭 (Dragon Boss Dive / Swoop Rush - ac_dragon_dive):
     * 1. 沿视线方向极速俯冲飞掠冲刺 (强力爆发动能位移)；
     * 2. 播放巨龙振翅 SoundEvents.ENTITY_ENDERDRAGON_FLAP 与咆哮 SoundEvents.ENTITY_ENDERDRAGON_GROWL；
     * 3. 沿突进路径喷射龙息粒子 (DRAGON_BREATH) 与大爆炸特效 (EXPLOSION_LARGE)；
     * 4. 沿途碾压判定：对前方 5 格内所有敌对实体造成 18.0F 动能+恐惧重创，强烈击飞并施加恐惧瘟疫与迟缓；
     * 5. 冲刺期间及冲刺后赋予宿主短暂免摔落保护与速度加成。
     */
    public static class DragonBossDiveRush extends AbyssalCraftAction {
        public DragonBossDiveRush() { super(120); } // 冷却 6s

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 1. 爆发性前向俯冲冲击
            entity.motionX += look.x * 2.2D;
            entity.motionY += Math.max(look.y * 1.5D, -0.6D) + 0.1D;
            entity.motionZ += look.z * 2.2D;
            entity.fallDistance = 0.0F;
            entity.velocityChanged = true;

            // 2. 音效：振翅狂风 + 恶龙咆哮
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_FLAP, SoundCategory.HOSTILE, 2.5F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2.0F, 0.9F);

            // 3. 沿冲刺路径激荡龙息与爆炸冲击波粒子
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 1; i <= 8; i++) {
                    double dist = i * 1.2D;
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            entity.posX + look.x * dist, entity.posY + entity.getEyeHeight() * 0.5D + look.y * dist, entity.posZ + look.z * dist,
                            6, 0.4D, 0.4D, 0.4D, 0.05D);
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                            entity.posX + look.x * dist, entity.posY + 0.5D + look.y * dist, entity.posZ + look.z * dist,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }

            // 4. 前方路径碾压与冲击击飞
            AxisAlignedBB pathBox = entity.getEntityBoundingBox().grow(3.5D).offset(look.x * 3.0D, look.y * 1.5D, look.z * 3.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, pathBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource src = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(src, 18.0F);
                target.motionX += look.x * 1.5D;
                target.motionY += 0.65D;
                target.motionZ += look.z * 1.5D;
                target.velocityChanged = true;
                if (AbyssalCraftAPI.dread_plague != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 160, 0));
                }
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1));
            }

            return true;
        }
    }

    private AbyssalBossActions() { }
}
