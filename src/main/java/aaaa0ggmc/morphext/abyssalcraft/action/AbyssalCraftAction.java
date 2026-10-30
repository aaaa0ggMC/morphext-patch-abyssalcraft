package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * 深渊国度主动能力基类。
 * 面向任意 EntityLivingBase（无论是玩家变身还是非玩家形态），彻底消除硬编码仅支持 Player 的缺陷。
 */
public abstract class AbyssalCraftAction implements IAction {
    private final int cooldownTicks;

    public AbyssalCraftAction(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public void execute(EntityLivingBase entity, AbstractMorph morph) {
        if (entity == null || entity.world == null) {
            return;
        }

        // 仅在服务端执行实际能力逻辑
        if (!entity.world.isRemote) {
            boolean success = executeServer(entity, morph);
            if (success && cooldownTicks > 0) {
                applyCooldown(entity, cooldownTicks);
            }
        }
    }

    /**
     * 服务端具体能力逻辑。
     * @return 是否成功释放（成功则进入冷却）
     */
    protected abstract boolean executeServer(EntityLivingBase entity, AbstractMorph morph);

    /**
     * 为实体赋予 Metamorph Max 的 MOB_ABILITY_COOLDOWN 药水冷却效果（如存在）
     */
    protected void applyCooldown(EntityLivingBase entity, int ticks) {
        try {
            Class<?> clazz = Class.forName("jones.metamorphmax.init.PotionInit");
            Potion potion = (Potion) clazz.getField("MOB_ABILITY_COOLDOWN").get(null);
            if (potion != null) {
                entity.addPotionEffect(new PotionEffect(potion, ticks, 0, false, false));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 通用射线实体检测（面向所有存活的目标 EntityLivingBase）
     */
    protected EntityLivingBase findTargetInCrosshairs(EntityLivingBase entity, double maxDistance) {
        World world = entity.world;
        Vec3d eyes = entity.getPositionEyes(1.0F);
        Vec3d look = entity.getLook(1.0F);
        Vec3d reach = eyes.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        AxisAlignedBB box = entity.getEntityBoundingBox().expand(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance).grow(1.0D);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box, e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

        EntityLivingBase closest = null;
        double minDistance = maxDistance;

        for (EntityLivingBase target : list) {
            AxisAlignedBB targetBox = target.getEntityBoundingBox().grow(0.3D);
            RayTraceResult intercept = targetBox.calculateIntercept(eyes, reach);
            if (intercept != null) {
                double dist = eyes.distanceTo(intercept.hitVec);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = target;
                }
            }
        }

        return closest;
    }

    /**
     * 获取施放者周围范围内的所有敌对/可选目标
     */
    protected List<EntityLivingBase> findNearbyEntities(EntityLivingBase entity, double radius) {
        AxisAlignedBB box = entity.getEntityBoundingBox().grow(radius);
        return entity.world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
    }
}
