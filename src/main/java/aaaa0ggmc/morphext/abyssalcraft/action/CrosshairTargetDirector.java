package aaaa0ggmc.morphext.abyssalcraft.action;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * 准星目标定向器（智能锁敌与视线空间交点引导）：
 * 1. 准星对准敌对生物时：定向至该目标生物；
 * 2. 准星对准方块或空气时：定向至视线延伸的光线交点坐标；
 * 3. 彻底杜绝锁定宿主自身或友军。
 *
 * <p><b>FVP-27</b>：实体候选过滤由 {@code !isOnSameTeam}（scoreboard 队伍）改为
 * {@link AbyssalCraftAction#isHostileTarget}（= 存活 + 非同队 + {@code FactionEnmity.isEnemy}）。
 * 几何引导逻辑（射线/AABB 拦截/最近命中）完全不变；未锁到敌人时仍旧退化为方块交点
 * 与视线末端，自由射击手感不受影响。</p>
 */
public final class CrosshairTargetDirector {

    public static class AimResult {
        public final EntityLivingBase entityTarget;
        public final Vec3d coordTarget;

        public AimResult(EntityLivingBase entityTarget, Vec3d coordTarget) {
            this.entityTarget = entityTarget;
            this.coordTarget = coordTarget;
        }

        public boolean hasEntity() {
            return entityTarget != null;
        }
    }

    public static AimResult resolveTarget(EntityLivingBase entity, double maxDist) {
        World world = entity.world;
        Vec3d eye = entity.getPositionEyes(1.0F);
        Vec3d look = entity.getLook(1.0F);
        Vec3d reach = eye.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

        // 1. 射线检测周围存活实体（排除施法者自身、友军与同阵营生物）
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(look.x * maxDist, look.y * maxDist, look.z * maxDist).grow(1.5D);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                e -> AbyssalCraftAction.isHostileTarget(entity, e));

        EntityLivingBase closestEntity = null;
        double minDistance = maxDist;

        for (EntityLivingBase target : list) {
            AxisAlignedBB targetBox = target.getEntityBoundingBox().grow(0.35D);
            RayTraceResult intercept = targetBox.calculateIntercept(eye, reach);
            if (intercept != null) {
                double dist = eye.distanceTo(intercept.hitVec);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestEntity = target;
                }
            }
        }

        if (closestEntity != null) {
            Vec3d targetCenter = new Vec3d(closestEntity.posX, closestEntity.posY + closestEntity.getEyeHeight() * 0.5D, closestEntity.posZ);
            return new AimResult(closestEntity, targetCenter);
        }

        // 2. 射线检测方块碰撞
        RayTraceResult blockHit = world.rayTraceBlocks(eye, reach, false, false, true);
        if (blockHit != null && blockHit.hitVec != null) {
            return new AimResult(null, blockHit.hitVec);
        }

        // 3. 自由指向视线末端空间坐标
        return new AimResult(null, reach);
    }

    private CrosshairTargetDirector() { }
}
