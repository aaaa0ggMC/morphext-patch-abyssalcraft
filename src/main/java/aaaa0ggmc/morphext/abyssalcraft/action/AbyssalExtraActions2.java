package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.integration.metamorph.vanilla.ServerActionScheduler;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSpawn;
import com.shinoow.abyssalcraft.common.entity.EntityDreadguard;
import com.shinoow.abyssalcraft.common.entity.anti.EntityAntiZombie;
import com.shinoow.abyssalcraft.lib.ACSounds;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityTippedArrow;
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
 * 深渊国度生物能力增补技能集（morph-abilities-audit E2 工作包）。
 * 与 {@link AbyssalActions} / {@link AbyssalExtraActions} 同构：服务端权威执行
 * （execute 内 isRemote 短路由基类保证）、ICooldownAction 精准冷却（6-25s，Boss 级 30s）、
 * 前向安全发射与属主标签防自伤、单发伤害 ≤8（Boss 级 ≤15）、全程无方块破坏。
 *
 * 本批动作全部以 AbyssalCraft 实体源码中"尚未表达的真实行为"为准绳，
 * 趣味演绎为辅；各动作注释标注源码依据。
 */
public final class AbyssalExtraActions2 {

    /* =====================================================================
     *  食尸鬼双爪连击 (ac_ghoul_rake) —— GhoulBase 双臂挥击 + DreadedGhoul 加成
     *  源码依据：EntityGhoulBase.attackEntityAsMob（swingArm 双臂）与
     *  EntityDreadedGhoul.getBonusDamage:1.5（双爪撕裂加成）
     * ===================================================================== */

    /** 食尸鬼家族：双爪连击 (ID: ac_ghoul_rake, 冷却 8s) */
    public static class GhoulRakeAction extends AbyssalCraftAction {
        public GhoulRakeAction() {
            super(160); // 冷却 8s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);
            entity.swingArm(EnumHand.OFF_HAND);

            AxisAlignedBB rakeBox = entity.getEntityBoundingBox().grow(2.2D, 1.1D, 2.2D)
                    .offset(look.x * 1.6D, 0.2D, look.z * 1.6D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, rakeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            for (EntityLivingBase target : list) {
                // 双臂两段撕裂（每段 4 点，单发不超 8）
                target.attackEntityFrom(source, 4.0F);
                target.attackEntityFrom(source, 4.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 60, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 40, 0));
                target.knockBack(entity, 0.5F, target.posX - entity.posX, target.posZ - entity.posZ);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.8D;
                double py = entity.posY + entity.getEyeHeight() * 0.6D;
                double pz = entity.posZ + look.z * 1.8D;
                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, px, py, pz, 8, 0.9D, 0.5D, 0.9D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.CRIT, px, py, pz, 20, 0.8D, 0.5D, 0.8D, 0.15D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.ghoul_hurt != null ? ACSounds.ghoul_hurt : SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD,
                    SoundCategory.HOSTILE, 1.1F, 0.8F);
            return true;
        }
    }

    /* =====================================================================
     *  燃尸自焚抓 (ac_ghoul_immolate) —— 深渊食尸鬼/深渊僵尸的点燃特性
     *  源码依据：EntityDepthsGhoul.onAttacking / EntityAbyssalZombie.attackEntityAsMob
     *  —— 自身燃烧且徒手时点燃目标
     * ===================================================================== */

    /** 深渊食尸鬼 / 深渊僵尸：燃尸自焚抓 (ID: ac_ghoul_immolate, 冷却 9s) */
    public static class GhoulImmolateAction extends AbyssalCraftAction {
        public GhoulImmolateAction() {
            super(180); // 冷却 9s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            // 自身燃烧（深渊食尸鬼火焰形态表现；不伤自身，宿主自带火免时仅视觉）
            entity.setFire(8);

            AxisAlignedBB grabBox = entity.getEntityBoundingBox().grow(2.2D, 1.1D, 2.2D)
                    .offset(look.x * 1.6D, 0.2D, look.z * 1.6D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, grabBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity)).setFireDamage();

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 4.0F);
                target.setFire(5); // 燃烧徒手点燃（源码 2×difficulty 秒）
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX, entity.posY + 0.6D, entity.posZ,
                        35, 0.6D, 0.8D, 0.6D, 0.04D);
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.8D, entity.posZ,
                        10, 0.5D, 0.5D, 0.5D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 0.9F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.ghoul_normal_ambient != null ? ACSounds.ghoul_normal_ambient : SoundEvents.ENTITY_ZOMBIE_AMBIENT,
                    SoundCategory.HOSTILE, 1.3F, 0.6F);
            return true;
        }
    }

    /* =====================================================================
     *  恐惧瘟疫领域 (ac_dread_contagion) —— 恐惧军团的瘟疫传播面
     *  源码依据：EntityDreadling/DreadSpawn/GreaterDreadSpawn/LesserDreadbeast/
     *  Dreadguard 命中上 dplague 100t 的感染链 + EntityChagaroth:463-468 群系扩散
     * ===================================================================== */

    /** 恐惧军团：恐惧瘟疫领域 (ID: ac_dread_contagion, 冷却 15s) */
    public static class DreadContagionAction extends AbyssalCraftAction {
        public DreadContagionAction() {
            super(300); // 冷却 15s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            final World world = entity.world;
            final CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 16.0D);
            final Vec3d anchor = aim.coordTarget;

            world.playSound(null, anchor.x, anchor.y, anchor.z,
                    ACSounds.dreadguard_barf != null ? ACSounds.dreadguard_barf : SoundEvents.ENTITY_WITHER_SHOOT,
                    SoundCategory.HOSTILE, 1.0F, 0.5F);

            // 8 秒瘟疫领域：每秒对 4 格内敌人造成 1 点恐惧伤害 + 恐惧瘟疫 + 迟缓
            ServerActionScheduler.scheduleTicker(160, remaining -> {
                if (remaining % 20 != 0 || !(world instanceof WorldServer)) {
                    return;
                }
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        anchor.x, anchor.y + 0.6D, anchor.z,
                        12, 3.4D, 1.0D, 3.4D, 0.02D);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        anchor.x, anchor.y + 0.4D, anchor.z,
                        8, 3.0D, 0.8D, 3.0D, 0.02D);

                AxisAlignedBB zone = new AxisAlignedBB(
                        anchor.x - 4.0D, anchor.y - 3.0D, anchor.z - 4.0D,
                        anchor.x + 4.0D, anchor.y + 3.0D, anchor.z + 4.0D);
                List<EntityLivingBase> victims = world.getEntitiesWithinAABB(EntityLivingBase.class, zone,
                        e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
                DamageSource source = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread :
                        (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                                : DamageSource.causeMobDamage(entity));
                for (EntityLivingBase victim : victims) {
                    victim.attackEntityFrom(source, 1.0F);
                    if (AbyssalCraftAPI.dread_plague != null) {
                        victim.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 100, 0));
                    }
                    victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 0));
                }
            }, () -> {});
            return true;
        }
    }

    /* =====================================================================
     *  龙翼风暴 (ac_dragon_gale) —— 深渊之龙躯体碰撞击飞
     *  源码依据：EntityDragonBoss.collideWithEntities:471-486（径向速度击飞）与
     *  attackEntitiesInList:489-503（10 点物理撞击）
     * ===================================================================== */

    /** 深渊之龙 / 幽魂龙：龙翼风暴 (ID: ac_dragon_gale, 冷却 10s) */
    public static class DragonGaleAction extends AbyssalCraftAction {
        public DragonGaleAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            AxisAlignedBB galeBox = entity.getEntityBoundingBox().grow(6.0D, 2.5D, 6.0D)
                    .offset(look.x * 5.0D, 0.5D, look.z * 5.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, galeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 6.0F);
                // 径向击飞（还原 collideWithEntities 的速度矢量公式）
                double dx = target.posX - entity.posX;
                double dz = target.posZ - entity.posZ;
                double d4 = Math.max(0.5D, dx * dx + dz * dz);
                target.addVelocity(dx / d4 * 2.0D, 0.25D, dz / d4 * 2.0D);
                target.velocityChanged = true;
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, 0));
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CLOUD,
                        entity.posX + look.x * 3.0D, entity.posY + 1.2D, entity.posZ + look.z * 3.0D,
                        40, 2.5D, 1.0D, 2.5D, 0.12D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX + look.x * 3.0D, entity.posY + 0.5D, entity.posZ + look.z * 3.0D,
                        25, 2.0D, 0.8D, 2.0D, 0.03D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 1.2F, 0.7F);
            return true;
        }
    }

    /* =====================================================================
     *  恐惧军团召唤 (ac_chagaroth_legion) —— 查伽洛斯周期召唤波次
     *  源码依据：EntityChagaroth.java:541-580（600t DreadSpawn/ChagarothSpawn、
     *  2400t Dreadguard 分级出兵）
     * ===================================================================== */

    /** 查伽洛斯：恐惧军团召唤 (ID: ac_chagaroth_legion, 冷却 30s) */
    public static class ChagarothLegionAction extends AbyssalCraftAction {
        public ChagarothLegionAction() {
            super(600); // 冷却 30s（Boss 级上限）
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 32.0D);

            // 1 名恐惧卫士 + 2 只恐惧幼体（还原分级出兵）
            EntityDreadguard guard = new EntityDreadguard(world);
            Vec3d safe = getSafeSpawnPosition(entity, 2.4D);
            guard.setPosition(safe.x, safe.y, safe.z);
            BossNativeHelper.tagOwner(guard, entity);
            if (aim.hasEntity()) {
                guard.setAttackTarget(aim.entityTarget);
            }
            world.spawnEntity(guard);

            for (int i = 0; i < 2; i++) {
                Vec3d pos = getSafeSpawnPosition(entity, 2.0D + i * 1.2D);
                EntityDreadSpawn spawn = new EntityDreadSpawn(world);
                spawn.setPosition(pos.x, pos.y, pos.z);
                BossNativeHelper.tagOwner(spawn, entity);
                if (aim.hasEntity()) {
                    spawn.setAttackTarget(aim.entityTarget);
                }
                world.spawnEntity(spawn);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        50, 1.8D, 1.0D, 1.8D, 0.03D);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        30, 1.5D, 0.8D, 1.5D, 0.04D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.1F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dread_spawn_ambient != null ? ACSounds.dread_spawn_ambient : SoundEvents.ENTITY_SLIME_SQUISH,
                    SoundCategory.HOSTILE, 1.4F, 0.7F);
            return true;
        }
    }

    /* =====================================================================
     *  暗影重压 (ac_shadow_crush) —— 暗影巨兽的压迫式近战重击
     *  源码依据：EntityShadowTitan（jar 反编译）纯近战肉盾 15 伤 / EntitySacthoth
     *  近战重压（本动作取 ≤8 平衡值，仅保留压迫感表现）
     * ===================================================================== */

    /** 暗影巨兽 / 萨克索斯：暗影重压 (ID: ac_shadow_crush, 冷却 10s) */
    public static class ShadowCrushAction extends AbyssalCraftAction {
        public ShadowCrushAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            List<EntityLivingBase> list = findNearbyEntities(entity, 4.5D);
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                            : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                // 压迫击飞：挑空 + 迟缓
                target.motionY = Math.max(target.motionY, 0.35D);
                target.velocityChanged = true;
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 0.3D, entity.posZ,
                        50, 2.6D, 0.8D, 2.6D, 0.03D);
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        25, 2.2D, 0.6D, 2.2D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.HOSTILE, 1.5F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shadow_hurt != null ? ACSounds.shadow_hurt : SoundEvents.ENTITY_WITHER_HURT,
                    SoundCategory.HOSTILE, 1.1F, 0.7F);
            return true;
        }
    }

    /* =====================================================================
     *  反物质增援 (ac_anti_reinforce) —— EntityAntiZombie 召唤增援
     *  源码依据：EntityAntiZombie.java:75,86,277（spawnReinforcements 属性 +
     *  Leader zombie bonus）
     * ===================================================================== */

    /** 反物质僵尸：反物质增援 (ID: ac_anti_reinforce, 冷却 20s) */
    public static class AntiReinforceAction extends AbyssalCraftAction {
        public AntiReinforceAction() {
            super(400); // 冷却 20s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 24.0D);

            for (int i = 0; i < 2; i++) {
                Vec3d safe = getSafeSpawnPosition(entity, 1.8D + i * 0.9D);
                EntityAntiZombie reinforcement = new EntityAntiZombie(world);
                reinforcement.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.tagOwner(reinforcement, entity);
                if (aim.hasEntity()) {
                    reinforcement.setAttackTarget(aim.entityTarget);
                }
                world.spawnEntity(reinforcement);
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        60, 1.6D, 1.0D, 1.6D, 0.15D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.4F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 0.8F, 1.6F);
            return true;
        }
    }

    /* =====================================================================
     *  反物质火箭 (ac_anti_flame_arrow) —— EntityAntiSkeleton 火矢
     *  源码依据：EntityAntiSkeleton.attackEntityWithRangedAttack:216
     *  （entityarrow.setFire(100) 燃烧箭）
     * ===================================================================== */

    /** 反物质骷髅：反物质火箭 (ID: ac_anti_flame_arrow, 冷却 10s) */
    public static class AntiFlameArrowAction extends AbyssalCraftAction {
        public AntiFlameArrowAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            for (int i = -1; i <= 1; i++) {
                Vec3d safe = getSafeSpawnPosition(entity, 1.6D);
                EntityTippedArrow arrow = new EntityTippedArrow(world, entity);
                arrow.setPosition(safe.x, safe.y, safe.z);
                arrow.shoot(look.x + i * 0.08D, look.y + 0.04D, look.z + i * 0.04D, 2.0F, 2.0F);
                arrow.setDamage(4.0D);
                arrow.setFire(100); // 火矢（源码 setFire(100)）
                BossNativeHelper.tagOwner(arrow, entity);
                world.spawnEntity(arrow);
            }

            if (world instanceof WorldServer) {
                Vec3d safe = getSafeSpawnPosition(entity, 1.6D);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                        safe.x, safe.y, safe.z, 15, 0.3D, 0.3D, 0.3D, 0.03D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.9F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7F, 1.2F);
            return true;
        }
    }

    /* =====================================================================
     *  反物质过载 (ac_anti_overcharge) —— EntityAntiCreeper 雷击充能
     *  源码依据：EntityAntiCreeper.java:287-293（雷击充能 ×2 强化湮灭脉冲）
     * ===================================================================== */

    /** 反物质苦力怕：反物质过载 (ID: ac_anti_overcharge, 冷却 20s) */
    public static class AntiOverchargeAction extends AbyssalCraftAction {
        public AntiOverchargeAction() {
            super(400); // 冷却 20s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;

            // 充能姿态：短时强化（纯增益，无直接伤害）
            entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 160, 0, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 160, 0, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 160, 1, false, true));

            // 微型斥力脉冲（仅击退）
            for (EntityLivingBase target : findNearbyEntities(entity, 4.0D)) {
                target.knockBack(entity, 1.0F, target.posX - entity.posX, target.posZ - entity.posZ);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.REDSTONE,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        40, 0.8D, 1.0D, 0.8D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 0.8D, entity.posZ,
                        35, 0.7D, 0.9D, 0.7D, 0.12D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.0F, 1.4F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.PLAYERS, 1.1F, 1.1F);
            return true;
        }
    }

    /* =====================================================================
     *  湮灭冲撞 (ac_anti_matter_rage) —— 反物质动物的 ODB 湮灭冲撞
     *  源码依据：EntityAntiCow/EntityAntiPig.collideWithEntity（撞上正物质同类
     *  即湮灭爆炸）——本动作取可控冲撞形态（趣味演绎，无爆炸无方块破坏）
     * ===================================================================== */

    /** 反物质动物 / 反物质深渊僵尸：湮灭冲撞 (ID: ac_anti_matter_rage, 冷却 10s) */
    public static class AntimatterRageAction extends AbyssalCraftAction {
        public AntimatterRageAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            // 前冲
            entity.motionX += look.x * 1.3D;
            entity.motionZ += look.z * 1.3D;
            entity.velocityChanged = true;

            AxisAlignedBB chargeBox = entity.getEntityBoundingBox().grow(3.2D, 1.2D, 3.2D)
                    .offset(look.x * 2.0D, 0.2D, look.z * 2.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, chargeBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                            : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 6.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 60, 0));
                }
                target.knockBack(entity, 1.1F, look.x, look.z);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.5D;
                double pz = entity.posZ + look.z * 1.5D;
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        px, entity.posY + 0.8D, pz, 30, 0.7D, 0.5D, 0.7D, 0.12D);
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        px, entity.posY + 0.6D, pz, 10, 0.5D, 0.4D, 0.5D, 0.05D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.6F, 1.6F);
            return true;
        }
    }

    /* =====================================================================
     *  恶魔狂暴 (ac_demon_frenzy) —— 恶魔/邪恶动物的狂暴面
     *  源码依据：EntityEvilAnimal/EntityDemonAnimal 激进近战 AI + 火焰免疫 +
     *  EntityDemonAnimal.canBurn 火径机制（整合包默认关闭，取爆发形态表现）
     * ===================================================================== */

    /** 恶魔动物 / 邪恶动物：恶魔狂暴 (ID: ac_demon_frenzy, 冷却 20s) */
    public static class DemonFrenzyAction extends AbyssalCraftAction {
        public DemonFrenzyAction() {
            super(400); // 冷却 20s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;

            // 狂暴姿态（短时增益）
            entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 160, 0, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 160, 0, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 160, 0, false, true));

            // 恶念火环余波（低伤点燃）
            DamageSource fireSrc = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) :
                    DamageSource.causeMobDamage(entity)).setFireDamage();
            for (EntityLivingBase target : findNearbyEntities(entity, 4.0D)) {
                target.attackEntityFrom(fireSrc, 2.0F);
                target.setFire(3);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double px = entity.posX + Math.cos(rad) * 1.8D;
                    double pz = entity.posZ + Math.sin(rad) * 1.8D;
                    ws.spawnParticle(EnumParticleTypes.FLAME, px, entity.posY + 0.4D, pz,
                            3, 0.1D, 0.3D, 0.1D, 0.01D);
                }
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.6D, entity.posZ,
                        12, 0.6D, 0.5D, 0.6D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.2F, 0.9F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.9F, 1.1F);
            return true;
        }
    }

    /* =====================================================================
     *  残影相位闪烁 (ac_remnant_blink) —— 奥穆索遗民的相位闪烁
     *  源码依据：EntityRemnant.enrage 灵体尖啸与奥穆索居民的异界位移特性
     *  （趣味演绎：短距相位闪烁 + 短暂隐身，非源码瞬移）
     * ===================================================================== */

    /** 奥穆索遗民：残影相位闪烁 (ID: ac_remnant_blink, 冷却 12s) */
    public static class RemnantBlinkAction extends AbyssalCraftAction {
        public RemnantBlinkAction() {
            super(240); // 冷却 12s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            double oldX = entity.posX;
            double oldY = entity.posY;
            double oldZ = entity.posZ;

            // 向准星方向相位滑行（≤12 格）
            Vec3d look = entity.getLook(1.0F);
            double tx = entity.posX + look.x * 12.0D;
            double ty = entity.posY + look.y * 6.0D;
            double tz = entity.posZ + look.z * 12.0D;

            // 安全落点：向下探测可站立位置，窒息则退回原位
            if (world.isBlockLoaded(new BlockPos(tx, ty, tz))) {
                BlockPos landing = new BlockPos(tx, ty, tz);
                for (int dy = 0; dy < 6; dy++) {
                    if (world.getBlockState(landing.down()).getMaterial().isSolid()) {
                        ty = landing.getY();
                        break;
                    }
                    landing = landing.down();
                }
                if (world.getBlockState(new BlockPos(tx, ty, tz)).getMaterial().blocksMovement()
                        || world.getBlockState(new BlockPos(tx, ty + 1, tz)).getMaterial().blocksMovement()) {
                    tx = oldX;
                    ty = oldY;
                    tz = oldZ;
                }
            }

            entity.setPositionAndUpdate(tx, ty, tz);
            entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 40, 0, false, false));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 0, false, true));

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL, oldX, oldY + 1.0D, oldZ, 40, 0.6D, 0.9D, 0.6D, 0.2D);
                ws.spawnParticle(EnumParticleTypes.PORTAL, tx, ty + 1.0D, tz, 40, 0.6D, 0.9D, 0.6D, 0.2D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, tx, ty + 0.5D, tz, 20, 0.5D, 0.6D, 0.5D, 0.02D);
            }

            world.playSound(null, oldX, oldY, oldZ,
                    ACSounds.remnant_scream != null ? ACSounds.remnant_scream : SoundEvents.ENTITY_GHAST_SCREAM,
                    SoundCategory.HOSTILE, 0.9F, 1.4F);
            world.playSound(null, tx, ty, tz,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 0.8F, 1.6F);
            return true;
        }
    }

    /* =====================================================================
     *  奥穆索震颤场 (ac_jzahar_tremor_field) —— J'zahar 地震余波
     *  源码依据：EntityJzahar.java:462-480（地震余波：地面单位随机扰动 +
     *  缓慢 III + 反胃 + hotFloor 1 伤）
     * ===================================================================== */

    /** 耆神 J'zahar：奥穆索震颤场 (ID: ac_jzahar_tremor_field, 冷却 30s) */
    public static class JzaharTremorFieldAction extends AbyssalCraftAction {
        public JzaharTremorFieldAction() {
            super(600); // 冷却 30s（Boss 级上限）
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            final World world = entity.world;
            final CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 32.0D);
            final Vec3d anchor = aim.coordTarget;

            world.playSound(null, anchor.x, anchor.y, anchor.z,
                    ACSounds.jzahar_earthquake != null ? ACSounds.jzahar_earthquake : SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.HOSTILE, 1.4F, 0.6F);

            // 8 秒震颤场：每秒扰动 8 格内地面单位
            ServerActionScheduler.scheduleTicker(160, remaining -> {
                if (remaining % 20 != 0) {
                    return;
                }
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.CLOUD,
                            anchor.x, anchor.y + 0.4D, anchor.z,
                            25, 5.0D, 0.5D, 5.0D, 0.06D);
                    ws.spawnParticle(EnumParticleTypes.LAVA,
                            anchor.x, anchor.y + 0.3D, anchor.z,
                            8, 4.5D, 0.3D, 4.5D, 0.01D);
                }

                AxisAlignedBB zone = new AxisAlignedBB(
                        anchor.x - 8.0D, anchor.y - 3.0D, anchor.z - 8.0D,
                        anchor.x + 8.0D, anchor.y + 4.0D, anchor.z + 8.0D);
                List<EntityLivingBase> victims = world.getEntitiesWithinAABB(EntityLivingBase.class, zone,
                        e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity) && e.onGround);
                for (EntityLivingBase victim : victims) {
                    // 随机扰动（源码 motionX/Y/Z += rand）
                    victim.motionX += world.rand.nextFloat() * 0.1D - 0.05D;
                    victim.motionZ += world.rand.nextFloat() * 0.1D - 0.05D;
                    victim.velocityChanged = true;
                    victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20, 2));
                    if (world.rand.nextInt(5) == 0) {
                        victim.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 40, 2));
                        victim.motionY = Math.max(victim.motionY, 0.35D);
                        victim.attackEntityFrom(DamageSource.HOT_FLOOR, 1.0F);
                    }
                }
            }, () -> {});
            return true;
        }
    }

    /* =====================================================================
     *  古神咩啸 (ac_shub_bleat) —— 修格斯之嗣的羊叫震慑
     *  源码依据：EntityShubOffspring.getAmbientSound = ENTITY_SHEEP_AMBIENT、
     *  getSoundVolume 3.0（巨声羊叫）+ 古神之嗣的威压（趣味演绎）
     * ===================================================================== */

    /** 修格斯之嗣：古神咩啸 (ID: ac_shub_bleat, 冷却 12s) */
    public static class ShubBleatAction extends AbyssalCraftAction {
        public ShubBleatAction() {
            super(240); // 冷却 12s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            List<EntityLivingBase> list = findNearbyEntities(entity, 8.0D);
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = AbyssalCraftAPI.shadow != null ? AbyssalCraftAPI.shadow :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                            : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 5.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 80, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0));
                target.knockBack(entity, 0.6F, target.posX - entity.posX, target.posZ - entity.posZ);
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                        40, 2.4D, 1.2D, 2.4D, 0.0D);
            }

            // 巨声羊叫（源码 getSoundVolume 3.0）
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_SHEEP_AMBIENT, SoundCategory.HOSTILE, 3.0F, 0.6F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 0.8F, 1.2F);
            return true;
        }
    }

    /* =====================================================================
     *  门卫双臂重锤 (ac_gatekeeper_crush) —— EntityGatekeeperMinion 双臂挥击
     *  源码依据：EntityGatekeeperMinion.attackEntityAsMob:76-83
     *  （swingArm 双臂连击 + 18 伤本体；取 ≤8 平衡值）
     * ===================================================================== */

    /** 奥穆索门卫：双臂重锤 (ID: ac_gatekeeper_crush, 冷却 9s) */
    public static class GatekeeperCrushAction extends AbyssalCraftAction {
        public GatekeeperCrushAction() {
            super(180); // 冷却 9s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);
            entity.swingArm(EnumHand.OFF_HAND);

            AxisAlignedBB crushBox = entity.getEntityBoundingBox().grow(2.4D, 1.2D, 2.4D)
                    .offset(look.x * 1.7D, 0.2D, look.z * 1.7D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, crushBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 2));
                target.knockBack(entity, 1.2F, look.x, look.z);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.8D;
                double pz = entity.posZ + look.z * 1.8D;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC, px, entity.posY + 0.8D, pz,
                        25, 0.7D, 0.5D, 0.7D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 0.3D, pz,
                        15, 0.8D, 0.4D, 0.8D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shadow_death != null ? ACSounds.shadow_death : SoundEvents.ENTITY_IRONGOLEM_ATTACK,
                    SoundCategory.HOSTILE, 1.3F, 0.7F);
            return true;
        }
    }

    /* =====================================================================
     *  修格斯重压 (ac_shoggoth_crush) —— 大型修格斯的巨躯碾压
     *  源码依据：EntityShoggoth.isBig 近战附效 + EntityGreaterShoggoth
     *  getArmorAcidDamage:12（酸蚀护甲）——本动作取 ≤8 平衡值
     * ===================================================================== */

    /** 修格斯家族：修格斯重压 (ID: ac_shoggoth_crush, 冷却 10s) */
    public static class ShoggothCrushAction extends AbyssalCraftAction {
        public ShoggothCrushAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            entity.swingArm(EnumHand.MAIN_HAND);

            List<EntityLivingBase> list = findNearbyEntities(entity, 5.0D);
            if (list.isEmpty()) {
                return false;
            }

            DamageSource source = AbyssalCraftAPI.acid != null ? AbyssalCraftAPI.acid :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                            : DamageSource.causeMobDamage(entity));

            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                // 酸蚀护甲（源码每次 8-12 点腐蚀 → 虚弱表征）
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
                target.motionY = Math.max(target.motionY, 0.3D);
                target.velocityChanged = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SLIME,
                        entity.posX, entity.posY + 0.3D, entity.posZ,
                        45, 2.8D, 0.8D, 2.8D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        25, 2.4D, 0.7D, 2.4D, 0.03D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shoggoth_step != null ? ACSounds.shoggoth_step : SoundEvents.ENTITY_SLIME_ATTACK,
                    SoundCategory.HOSTILE, 1.6F, 0.5F);
            return true;
        }
    }

    private AbyssalExtraActions2() {
    }
}
