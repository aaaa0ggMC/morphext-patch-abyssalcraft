package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.integration.metamorph.vanilla.ServerActionScheduler;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.common.entity.EntityAcidProjectile;
import com.shinoow.abyssalcraft.common.entity.EntityDreadSpawn;
import com.shinoow.abyssalcraft.common.entity.EntityRemnant;
import com.shinoow.abyssalcraft.lib.ACSounds;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 深渊国度生物扩展技能集（morph-abilities-audit NEW 需求落地）。
 * 与 {@link AbyssalActions} 同构：服务端权威执行（execute 内 isRemote 短路由基类保证）、
 * ICooldownAction 精准冷却、前向安全发射与属主标签防自伤、爆炸与破坏均不伤方块，
 * 仅 ac_anti_door_break 按原生 EntityAIBreakDoor 语义受 mobGriefing 约束破坏木质方块。
 */
public final class AbyssalExtraActions {

    /* =====================================================================
     *  恐惧呕吐引导 (ac_dread_barf) —— EntityDreadguard 呕吐技还原
     *  源码依据：EntityDreadguard.java:179-208（60t 引导、自身定身、
     *  对瞄准点 2 格范围 1/3 概率造成 dread (4.5-距离) 伤害 + dplague 100t）
     * ===================================================================== */

    /** 恐惧卫士 / 查伽洛斯：恐惧呕吐引导 (ID: ac_dread_barf, 冷却 10s) */
    public static class DreadBarfAction extends AbyssalCraftAction {
        public DreadBarfAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            final World world = entity.world;
            final CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 12.0D);
            final Vec3d aimPoint = aim.coordTarget;

            // 引导起点表现：定身 + 起手干呕声
            entity.swingArm(EnumHand.MAIN_HAND);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dreadguard_barf != null ? ACSounds.dreadguard_barf : SoundEvents.ENTITY_WITHER_SHOOT,
                    SoundCategory.HOSTILE, 0.8F, 0.5F);

            // 60t（3s）原地引导：零化水平移动（还原源码 motionX/Z *= 0），每 5t 干呕声与粒子
            ServerActionScheduler.scheduleTicker(60, remaining -> {
                // A6：解除变身/切换形态后立即中止引导，不再对已解除变身的玩家定身
                if (entity.isEntityAlive() && entity.world == world && isStillMorphedAs(entity, morph)) {
                    // A6：击退/拉扯刚注入（velocityChanged）或水平速度超步行档的 tick 放过，
                    // 不再附赠 3 秒击退免疫；其余 tick 照旧定身
                    double hSpeedSq = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
                    if (!entity.velocityChanged && hSpeedSq <= 0.1024D) {
                        entity.motionX *= 0.0D;
                        entity.motionZ *= 0.0D;
                    }
                    // 定身即收起疾跑标志，消除"疾跑 FOV 保持而人不动"的观感错位
                    if (entity.isSprinting()) {
                        entity.setSprinting(false);
                    }
                    if (remaining % 10 == 0) {
                        world.playSound(null, entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                                ACSounds.dreadguard_barf != null ? ACSounds.dreadguard_barf : SoundEvents.ENTITY_WITHER_SHOOT,
                                SoundCategory.HOSTILE, 0.7F + world.rand.nextFloat(), world.rand.nextFloat() * 0.5F + 0.2F);
                    }
                    if (world instanceof WorldServer) {
                        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                                entity.posX, entity.posY + entity.getEyeHeight() * 0.6D, entity.posZ,
                                6, 0.35D, 0.25D, 0.35D, 0.02D);
                    }
                }
            }, () -> {
                // A6：引导完成前已解除变身/死亡/换维度则取消喷吐
                if (!entity.isEntityAlive() || entity.world != world || !isStillMorphedAs(entity, morph)) {
                    return;
                }
                // 引导完成：对瞄准点 2 格范围喷吐恐惧胆汁
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.SLIME,
                            aimPoint.x, aimPoint.y + 0.5D, aimPoint.z,
                            60, 1.6D, 0.8D, 1.6D, 0.05D);
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            aimPoint.x, aimPoint.y + 0.5D, aimPoint.z,
                            40, 1.6D, 0.8D, 1.6D, 0.03D);
                }
                world.playSound(null, aimPoint.x, aimPoint.y, aimPoint.z,
                        ACSounds.dreadguard_barf != null ? ACSounds.dreadguard_barf : SoundEvents.ENTITY_SLIME_ATTACK,
                        SoundCategory.HOSTILE, 1.6F, 0.6F);

                AxisAlignedBB splash = new AxisAlignedBB(
                        aimPoint.x - 2.0D, aimPoint.y - 2.0D, aimPoint.z - 2.0D,
                        aimPoint.x + 2.0D, aimPoint.y + 2.0D, aimPoint.z + 2.0D);
                List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, splash,
                        e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

                DamageSource source = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread :
                        (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                                : DamageSource.causeMobDamage(entity));

                for (EntityLivingBase target : list) {
                    boolean mainTarget = aim.hasEntity() && target == aim.entityTarget;
                    if (mainTarget || world.rand.nextInt(3) == 0) {
                        float damage = (float) Math.max(1.0D, 4.5D - entity.getDistance(target));
                        if (target.attackEntityFrom(source, damage) && AbyssalCraftAPI.dread_plague != null) {
                            target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 100, 0));
                        }
                    }
                }
            });
            return true;
        }

        /**
         * A6：引导是否仍应继续——玩家已解除变身或切换形态即终止
         * （非玩家施法实体无变身概念，恒为真）。
         */
        private static boolean isStillMorphedAs(EntityLivingBase entity, AbstractMorph morph) {
            if (!(entity instanceof EntityPlayer)) {
                return true;
            }
            IMorphing morphing = Morphing.get((EntityPlayer) entity);
            if (morphing == null || !morphing.isMorphed()) {
                return false;
            }
            AbstractMorph current = morphing.getCurrentMorph();
            return morph != null && current != null && morph.name != null && morph.name.equals(current.name);
        }
    }

    /* =====================================================================
     *  奥穆索之触 (ac_omothol_touch) —— EntityOmotholGhoul 近战附效还原
     *  源码依据：EntityOmotholGhoul.onAttacking:83-90（缓慢 III 100t + 失明 20t + 夜视 20t）
     * ===================================================================== */

    /** 奥穆索食尸鬼：奥穆索之触 (ID: ac_omothol_touch, 冷却 3s) */
    public static class OmotholTouchAction extends AbyssalCraftAction {
        public OmotholTouchAction() {
            super(60); // 冷却 3s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            // 身前 3.5 格触碰判定
            AxisAlignedBB touchBox = entity.getEntityBoundingBox().grow(2.5D, 1.2D, 2.5D)
                    .offset(look.x * 1.8D, 0.2D, look.z * 1.8D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, touchBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 6.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 2));  // 缓慢 III 5s
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 20, 0)); // 失明 1s
                hit = true;
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.6D;
                double py = entity.posY + entity.getEyeHeight() * 0.7D;
                double pz = entity.posZ + look.z * 1.6D;
                ws.spawnParticle(EnumParticleTypes.SPELL_MOB, px, py, pz, 30, 0.5D, 0.35D, 0.5D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 25, 0.45D, 0.3D, 0.45D, 0.1D);
                // 夜视粒子表现：宿主周身幽蓝微光（还原 onAttacking 附带的夜视 20t）
                ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                        20, 0.5D, 0.5D, 0.5D, 0.0D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.ghoul_normal_ambient != null ? ACSounds.ghoul_normal_ambient : SoundEvents.ENTITY_ZOMBIE_AMBIENT,
                    SoundCategory.HOSTILE, 1.3F, 0.7F);
            if (hit) {
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.9F, 1.3F);
            }
            return true;
        }
    }

    /* =====================================================================
     *  失明黑幕光环 (ac_sacthoth_dark_aura) —— EntitySacthoth 30 格失明黑幕
     *  源码依据：EntitySacthoth.java:405-429（30 格内玩家持续失明 40t，约 2s 刷新）
     * ===================================================================== */

    /** 萨克索斯：暗夜失明黑幕 (ID: ac_sacthoth_dark_aura, 冷却 8s) */
    public static class SacthothDarkAuraAction extends AbyssalCraftAction {
        public SacthothDarkAuraAction() {
            // A12：原生"30 格持续光环"被改造成 2s 冷却主动后可按键连打全屏失明，
            // 数值放大过头；收敛为 8s 冷却 / 16 格 / 失明 2s
            super(160); // 冷却 8s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            List<EntityLivingBase> list = findNearbyEntities(entity, 16.0D);

            for (EntityLivingBase target : list) {
                target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 40, 0)); // 失明 2s
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double px = entity.posX + Math.cos(rad) * 8.0D;
                    double pz = entity.posZ + Math.sin(rad) * 8.0D;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, entity.posY + 1.0D, pz,
                            4, 0.4D, 1.2D, 0.4D, 0.02D);
                }
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        40, 6.0D, 1.5D, 6.0D, 0.02D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.shadow_death != null ? ACSounds.shadow_death : SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                    SoundCategory.HOSTILE, 1.2F, 0.6F);
            return true;
        }
    }

    /* =====================================================================
     *  反物质破门强袭 (ac_anti_door_break) —— EntityAntiZombie 破门还原
     *  源码依据：EntityAntiZombie.java:55-58,62（EntityAIBreakDoor + setBreakDoors）
     *  破坏严格受 mobGriefing 约束（与原版 EntityAIBreakDoor 同款门槛），只伤木质方块。
     * ===================================================================== */

    /** 反物质僵尸：反物质破门强袭 (ID: ac_anti_door_break, 冷却 5s) */
    public static class AntiDoorBreakAction extends AbyssalCraftAction {
        public AntiDoorBreakAction() {
            super(100); // 冷却 5s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);

            // 蓄力撞击前冲
            entity.swingArm(EnumHand.MAIN_HAND);
            entity.motionX += look.x * 1.2D;
            entity.motionZ += look.z * 1.2D;
            entity.velocityChanged = true;

            // 身前 2 格、3 宽 × 2 高的木质方块破坏（不伤石制建筑）
            boolean griefing = ForgeEventFactory.getMobGriefingEvent(world, entity);
            int broken = 0;
            if (griefing) {
                Vec3d base = entity.getPositionVector().add(look.x * 1.5D, 0.0D, look.z * 1.5D);
                for (int dx = -1; dx <= 1 && broken < 6; dx++) {
                    for (int dy = 0; dy <= 1 && broken < 6; dy++) {
                        for (int dz = -1; dz <= 1 && broken < 6; dz++) {
                            BlockPos pos = new BlockPos(
                                    base.x + dx + look.x * 0.8D,
                                    base.y + dy,
                                    base.z + dz + look.z * 0.8D);
                            IBlockState state = world.getBlockState(pos);
                            if (state.getMaterial() != Material.WOOD || state.getBlock().isAir(state, world, pos)) {
                                continue;
                            }
                            if (!state.getBlock().canEntityDestroy(state, world, pos, entity)) {
                                continue;
                            }
                            if (!ForgeEventFactory.onEntityDestroyBlock(entity, pos, state)) {
                                continue;
                            }
                            world.destroyBlock(pos, false); // 不掉落，避免刷物资
                            broken++;
                        }
                    }
                }
            }

            // 击退门后目标
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(3.5D, 1.5D, 3.5D).offset(look.x * 2.0D, 0.2D, look.z * 2.0D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
            DamageSource source = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter :
                    (entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) entity)
                            : DamageSource.causeMobDamage(entity));
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, broken > 0 ? 10.0F : 6.0F);
                if (AbyssalCraftAPI.antimatter_potion != null) {
                    target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 80, 0));
                }
                target.knockBack(entity, 1.2F, look.x, look.z);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                double px = entity.posX + look.x * 1.8D;
                double pz = entity.posZ + look.z * 1.8D;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, px, entity.posY + 0.8D, pz,
                        6, 0.5D, 0.4D, 0.5D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK, px, entity.posY + 0.8D, pz,
                        30, 0.8D, 0.8D, 0.8D, 0.1D, Block.getIdFromBlock(Blocks.PLANKS));
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.PLAYERS, 1.5F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
            return true;
        }
    }

    /* =====================================================================
     *  恶念蜕变 (ac_evil_demonize) —— EntityEvilAnimal 死亡恶魔化还原
     *  源码依据：EntityEvilAnimal.onDeath:42-58（Demon Animals Spawn on Death=true）
     * ===================================================================== */

    /** 邪恶动物：恶念蜕变 (ID: ac_evil_demonize, 冷却 10s) */
    public static class EvilDemonizeAction extends AbyssalCraftAction {
        public EvilDemonizeAction() {
            super(200); // 冷却 10s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;

            // 濒死恶念蜕变为恶魔形态：回血 + 点燃周身 + 短时力量
            entity.heal(8.0F);
            entity.setFire(6); // 邪恶/恶魔动物火焰免疫
            entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 120, 1, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 0, false, true));
            entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 120, 1, false, true));

            List<EntityLivingBase> list = findNearbyEntities(entity, 4.5D);
            DamageSource fireSrc = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) :
                    DamageSource.causeMobDamage(entity)).setFireDamage();
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(fireSrc, 6.0F);
                target.setFire(5);
                target.knockBack(entity, 1.0F, target.posX - entity.posX, target.posZ - entity.posZ);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        60, 1.2D, 0.8D, 1.2D, 0.06D);
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        entity.posX, entity.posY + 0.8D, entity.posZ,
                        20, 0.8D, 0.6D, 0.8D, 0.02D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        30, 1.0D, 0.8D, 1.0D, 0.03D);
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1.4F, 0.7F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.1F, 0.8F);
            return true;
        }
    }

    /* =====================================================================
     *  扭曲剪切 (ac_evil_animal_shear) —— EntityEvilAnimal IShearable 还原
     *  源码依据：EntityEvilAnimal.onSheared:63-80 + 各子类 getShearingDrop
     * ===================================================================== */

    /** 邪恶动物：扭曲剪切 (ID: ac_evil_animal_shear, 冷却 12s/9s) */
    public static class EvilAnimalShearAction extends AbyssalCraftAction {
        public EvilAnimalShearAction() {
            // A10：基准取分支最低档 9s（空手分支）；剪刀分支 executeServer 内
            // setCooldown(240) 抬到 12s。分支冷却互不串档（动态表读取即消费）。
            super(180); // 基准冷却 9s
        }

        private static EnumHand findHeldShears(EntityLivingBase entity) {
            if (entity == null) return null;
            if (entity.getHeldItemMainhand().getItem() == Items.SHEARS) return EnumHand.MAIN_HAND;
            if (entity.getHeldItemOffhand().getItem() == Items.SHEARS) return EnumHand.OFF_HAND;
            return null;
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            String morphName = morph != null && morph.name != null ? morph.name.toLowerCase(java.util.Locale.ROOT) : "";
            EnumHand shearsHand = findHeldShears(entity);

            // === 1. 剪刀分支：掉落生肉/羊毛并当场恶魔化 ===
            if (shearsHand != null) {
                if (entity instanceof EntityPlayer) {
                    ItemStack shears = ((EntityPlayer) entity).getHeldItem(shearsHand);
                    shears.damageItem(1, (EntityPlayer) entity);
                }

                if (morphName.contains("chicken")) {
                    entity.entityDropItem(new ItemStack(Items.FEATHER, 2 + world.rand.nextInt(2)), 0.5F);
                } else if (morphName.contains("cow")) {
                    entity.entityDropItem(new ItemStack(Items.LEATHER, 2), 0.5F);
                    entity.entityDropItem(new ItemStack(Items.BEEF, 2 + world.rand.nextInt(2)), 0.5F);
                } else if (morphName.contains("pig")) {
                    entity.entityDropItem(new ItemStack(Items.PORKCHOP, 2 + world.rand.nextInt(2)), 0.5F);
                } else if (morphName.contains("sheep")) {
                    entity.entityDropItem(new ItemStack(Items.MUTTON, 2 + world.rand.nextInt(2)), 0.5F);
                    entity.entityDropItem(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1 + world.rand.nextInt(2)), 0.5F);
                } else {
                    entity.entityDropItem(new ItemStack(Items.ROTTEN_FLESH, 2 + world.rand.nextInt(2)), 0.5F);
                }

                // 当场恶魔化：恶念火环爆发
                List<EntityLivingBase> list = findNearbyEntities(entity, 4.0D);
                DamageSource ds = (entity instanceof EntityPlayer ?
                        DamageSource.causePlayerDamage((EntityPlayer) entity) :
                        DamageSource.causeMobDamage(entity)).setFireDamage();
                for (EntityLivingBase target : list) {
                    target.attackEntityFrom(ds, 6.0F);
                    target.setFire(5);
                }

                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                            entity.posX, entity.posY + 0.5D, entity.posZ,
                            45, 1.5D, 0.3D, 1.5D, 0.08D);
                }
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.2F, 1.0F);
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.8F);

                setCooldown(entity, 240); // 12s
                return true;
            }

            // === 2. 空手分支：恶念火环（恶魔化余波） ===
            List<EntityLivingBase> list = findNearbyEntities(entity, 4.5D);
            DamageSource ds = (entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) :
                    DamageSource.causeMobDamage(entity)).setFireDamage();
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(ds, 5.0F);
                target.setFire(4);
                target.knockBack(entity, 0.9F, target.posX - entity.posX, target.posZ - entity.posZ);
            }
            entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 60, 0, false, true));

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        35, 1.8D, 0.4D, 1.8D, 0.07D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.1F, 0.9F);

            setCooldown(entity, 180); // 9s
            return true;
        }
    }

    /* =====================================================================
     *  修格斯吞噬成长 (ac_shoggoth_feast) —— EntityShoggothBase.feed 饱食进化链
     *  源码依据：EntityShoggothBase.feed:175-185 + 各子类 performFoodAction
     * ===================================================================== */

    /** 修格斯家族：吞噬成长 (ID: ac_shoggoth_feast, 冷却 4s) */
    public static class ShoggothFeastAction extends AbyssalCraftAction {
        /** 宿主 UUID → 饱食层数（击倒积累，满 3 层触发进化喷吐） */
        private static final Map<UUID, Integer> FEAST_STACKS = new ConcurrentHashMap<>();

        public ShoggothFeastAction() {
            super(80); // 冷却 4s
        }

        /** S5：解除变身/登出时清零饱食层数，防 UUID 级静态 Map 泄漏与跨形态残留。 */
        public static void clearFeastStacks(UUID id) {
            if (id != null) {
                FEAST_STACKS.remove(id);
            }
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            entity.swingArm(EnumHand.MAIN_HAND);

            // 吞噬撕咬身前 3.5 格目标
            AxisAlignedBB biteBox = entity.getEntityBoundingBox().grow(2.5D, 1.2D, 2.5D)
                    .offset(look.x * 1.8D, 0.2D, look.z * 1.8D);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, biteBox,
                    e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

            DamageSource source = entity instanceof EntityPlayer ?
                    DamageSource.causePlayerDamage((EntityPlayer) entity) : DamageSource.causeMobDamage(entity);

            boolean hit = false;
            for (EntityLivingBase target : list) {
                target.attackEntityFrom(source, 8.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 100, 1));
                hit = true;
            }
            if (!hit) {
                return false;
            }

            // S5：治疗按动作级一口价封顶 6 点，不再按命中目标数线性放大
            //（多目标不再叠治疗，饱食层数仍按次积累，避免畜群旁近似不死）
            entity.heal(6.0F);

            // 饱食积累
            UUID id = entity.getUniqueID();
            int stacks = FEAST_STACKS.getOrDefault(id, 0) + 1;
            if (stacks >= 3) {
                // 满值进化：喷吐载崽酸弹 + 短暂体型与伤害提升（力量/吸收）
                FEAST_STACKS.put(id, 0);
                for (int i = -1; i <= 1; i++) {
                    Vec3d safe = getSafeSpawnPosition(entity, 1.8D);
                    EntityAcidProjectile projectile = new EntityAcidProjectile(world, entity);
                    projectile.setPosition(safe.x, safe.y, safe.z);
                    projectile.shoot(look.x, look.y + 0.12D + i * 0.06D, look.z, 1.4F, 1.5F);
                    BossNativeHelper.tagOwner(projectile, entity);
                    world.spawnEntity(projectile);
                }
                entity.heal(8.0F);
                entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 120, 1, false, true));
                entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 120, 1, false, true));
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                            entity.posX, entity.posY + 1.0D, entity.posZ,
                            60, 1.2D, 0.8D, 1.2D, 0.05D);
                }
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        ACSounds.shoggoth_consume != null ? ACSounds.shoggoth_consume : SoundEvents.ENTITY_SLIME_ATTACK,
                        SoundCategory.HOSTILE, 1.5F, 0.7F);
            } else {
                FEAST_STACKS.put(id, stacks);
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                            entity.posX, entity.posY + 0.5D, entity.posZ,
                            15, 0.6D, 0.4D, 0.6D, 0.05D);
                }
            }

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.3F, 0.8F);
            return true;
        }
    }

    /* =====================================================================
     *  分裂 / 合体（EntityDreadSpawn 聚合与 EntityGreaterDreadSpawn 死亡裂体）
     *  源码依据：EntityDreadSpawn.java:134-146、EntityGreaterDreadSpawn.java:150-184
     * ===================================================================== */

    /** 次级恐惧兽 / 恐惧兽：裂体召唤 (ID: ac_dread_split, 冷却 15s) */
    public static class DreadSplitAction extends AbyssalCraftAction {
        public DreadSplitAction() {
            super(300); // 冷却 15s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            Vec3d look = entity.getLook(1.0F);
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 24.0D);

            // S4：同类召唤物存量上限（同宿主同类型 ≤4，超出替换旧的）+ 召唤物免掉落/免经验
            BossNativeHelper.capSummons(world, entity, EntityDreadSpawn.class, 2);
            for (int i = 0; i < 2; i++) {
                Vec3d safe = getSafeSpawnPosition(entity, 1.8D + i * 0.6D);
                EntityDreadSpawn spawn = new EntityDreadSpawn(world);
                spawn.setPosition(safe.x, safe.y, safe.z);
                BossNativeHelper.markSummon(spawn, entity);
                if (aim.hasEntity()) {
                    spawn.setAttackTarget(aim.entityTarget);
                }
                world.spawnEntity(spawn);
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX + look.x * 1.5D, entity.posY + 0.5D, entity.posZ + look.z * 1.5D,
                        30, 0.8D, 0.5D, 0.8D, 0.03D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dread_spawn_ambient != null ? ACSounds.dread_spawn_ambient : SoundEvents.ENTITY_SLIME_SQUISH,
                    SoundCategory.HOSTILE, 1.4F, 0.7F);
            return true;
        }
    }

    /** 恐惧幼体：聚合同化 (ID: ac_dread_merge, 冷却 12s) —— 吞并周围无主/己方召唤的恐惧幼体壮大自身 */
    public static class DreadMergeAction extends AbyssalCraftAction {
        public DreadMergeAction() {
            super(240); // 冷却 12s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;

            // A16：只吞无主野生或自己召唤的幼体，不拆其他玩家的召唤编队
            List<EntityDreadSpawn> candidates = world.getEntitiesWithinAABB(EntityDreadSpawn.class,
                    entity.getEntityBoundingBox().grow(8.0D),
                    e -> e != entity && e.isEntityAlive() && !BossNativeHelper.isOwnedByOther(e, entity));
            if (candidates.isEmpty()) {
                return false;
            }

            int merged = 0;
            for (EntityDreadSpawn spawn : candidates) {
                if (merged >= 3) {
                    break;
                }
                spawn.setDead();
                merged++;
            }

            entity.heal(2.0F * merged);
            entity.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 1, false, true));
            if (merged >= 2) {
                entity.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 0, false, true));
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        entity.posX, entity.posY + 0.5D, entity.posZ,
                        30 + merged * 15, 1.2D, 0.8D, 1.2D, 0.03D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.dread_spawn_ambient != null ? ACSounds.dread_spawn_ambient : SoundEvents.ENTITY_SLIME_SQUISH,
                    SoundCategory.HOSTILE, 1.5F, 0.6F);
            return true;
        }
    }

    /* =====================================================================
     *  残影唤起 (ac_remnant_rally) —— EntityGatekeeperMinion 受击唤起 Remnant
     *  源码依据：EntityGatekeeperMinion.java:101-113（唤起 16 格内 Remnant 围攻攻击者）
     * ===================================================================== */

    /** 奥穆索门卫：残影唤起 (ID: ac_remnant_rally, 冷却 15s) */
    public static class RemnantRallyAction extends AbyssalCraftAction {
        public RemnantRallyAction() {
            super(300); // 冷却 15s
        }

        @Override
        protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
            World world = entity.world;
            CrosshairTargetDirector.AimResult aim = CrosshairTargetDirector.resolveTarget(entity, 24.0D);
            EntityLivingBase foe = aim.hasEntity() ? aim.entityTarget : null;
            if (foe == null) {
                // 无准星敌人时取最近敌对目标
                List<EntityLivingBase> nearby = findNearbyEntities(entity, 16.0D);
                if (!nearby.isEmpty()) {
                    foe = nearby.get(0);
                }
            }

            // 1. 号召 16 格内已存在的残影围攻
            List<EntityRemnant> remnants = world.getEntitiesWithinAABB(EntityRemnant.class,
                    entity.getEntityBoundingBox().grow(16.0D),
                    e -> e != entity && e.isEntityAlive());
            int rallied = 0;
            if (foe != null) {
                for (EntityRemnant remnant : remnants) {
                    if (rallied >= 6) {
                        break;
                    }
                    remnant.setAttackTarget(foe);
                    rallied++;
                }
            }

            // 2. 附近无残影时唤起 2 道残影仆从
            if (rallied == 0) {
                // S4：同类召唤物存量上限 + 召唤物免掉落/免经验（只影响新唤起的仆从，不改野生残影）
                BossNativeHelper.capSummons(world, entity, EntityRemnant.class, 2);
                for (int i = 0; i < 2; i++) {
                    Vec3d safe = getSafeSpawnPosition(entity, 1.8D + i * 0.7D);
                    EntityRemnant echo = new EntityRemnant(world);
                    echo.setPosition(safe.x, safe.y, safe.z);
                    BossNativeHelper.markSummon(echo, entity);
                    if (foe != null) {
                        echo.setAttackTarget(foe);
                    }
                    world.spawnEntity(echo);
                    rallied++;
                }
            }

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        80, 3.0D, 1.2D, 3.0D, 0.15D);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        entity.posX, entity.posY + 1.0D, entity.posZ,
                        30, 2.0D, 1.0D, 2.0D, 0.05D);
            }
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    ACSounds.remnant_scream != null ? ACSounds.remnant_scream : SoundEvents.ENTITY_GHAST_SCREAM,
                    SoundCategory.HOSTILE, 1.8F, 0.8F);
            return true;
        }
    }

    private AbyssalExtraActions() {
    }
}
