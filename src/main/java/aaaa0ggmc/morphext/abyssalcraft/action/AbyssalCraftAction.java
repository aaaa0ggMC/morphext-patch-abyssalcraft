package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
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
 * 深渊国度主动能力通用基类。
 * 1. 彻底解耦硬编码面向 EntityPlayer 的缺陷，全面面向通用存活实体 EntityLivingBase；
 * 2. 实现 ICooldownAction，支持原生精准冷却；
 * 3. 安全距离与实体过滤，防止任何 self-hit 或友军误伤；
 * 4. 严禁无控世界破坏，保护玩家建筑与地形。
 */
public abstract class AbyssalCraftAction implements IAction, ICooldownAction {
    private final int cooldownTicks;

    public AbyssalCraftAction(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public int getCooldownTicks(EntityLivingBase entity) {
        return this.cooldownTicks;
    }

    @Override
    public void execute(EntityLivingBase entity, AbstractMorph morph) {
        if (entity == null || entity.world == null) {
            return;
        }

        if (!entity.world.isRemote) {
            executeServer(entity, morph);
        }
    }

    protected abstract boolean executeServer(EntityLivingBase entity, AbstractMorph morph);

    /**
     * 计算安全的弹射物生成坐标（朝视线方向前向偏移，避免与释放者 AABB 重叠碰撞导致自伤）
     */
    protected Vec3d getSafeSpawnPosition(EntityLivingBase entity, double forwardOffset) {
        Vec3d look = entity.getLook(1.0F);
        return new Vec3d(
                entity.posX + look.x * forwardOffset,
                entity.posY + entity.getEyeHeight() - 0.1D + look.y * forwardOffset,
                entity.posZ + look.z * forwardOffset
        );
    }

    /**
     * 通用射线实体检测（面向所有存活的敌对/非同队 EntityLivingBase，不仅针对 Player）
     */
    protected EntityLivingBase findTargetInCrosshairs(EntityLivingBase entity, double maxDistance) {
        World world = entity.world;
        Vec3d eyes = entity.getPositionEyes(1.0F);
        Vec3d look = entity.getLook(1.0F);
        Vec3d reach = eyes.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        AxisAlignedBB box = entity.getEntityBoundingBox().expand(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance).grow(1.0D);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

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
     * 获取施放者周围范围内的所有存活敌对目标（排除施法者自身和友军）
     */
    protected List<EntityLivingBase> findNearbyEntities(EntityLivingBase entity, double radius) {
        AxisAlignedBB box = entity.getEntityBoundingBox().grow(radius);
        return entity.world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));
    }
}
