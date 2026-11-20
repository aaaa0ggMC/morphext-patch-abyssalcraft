package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** 准星：先找视线交到的实体，再记方块。不改世界。 */
public final class LookDetector implements ConverseDetector {
    @Override
    public String id() {
        return "look";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || player.world == null || !player.world.isRemote || scan == null) {
            return;
        }
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Entity looked = nearest(player, eye, look, ConverseAspects.LOOK_REACH);
        if (looked != null) {
            EntityTags.tag(scan, looked, true);
        }
        Vec3d end = eye.add(look.x * ConverseAspects.BLOCK_REACH, look.y * ConverseAspects.BLOCK_REACH,
                look.z * ConverseAspects.BLOCK_REACH);
        RayTraceResult hit = player.world.rayTraceBlocks(eye, end, true);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.getBlockPos() != null) {
            BlockTags.tag(scan, player.world, hit.getBlockPos());
        } else {
            scan.add(ConverseAspects.BLOCK_AIR);
        }
    }

    private static Entity nearest(EntityPlayer player, Vec3d eye, Vec3d look, double reach) {
        Vec3d end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        AxisAlignedBB box = player.getEntityBoundingBox()
                .expand(look.x * reach, look.y * reach, look.z * reach)
                .grow(1.0D);
        List<Entity> list = player.world.getEntitiesWithinAABBExcludingEntity(player, box);
        if (list == null || list.isEmpty()) {
            return null;
        }
        Entity best = null;
        double bestDist = reach;
        for (Entity entity : list) {
            if (entity == null || entity.isDead || entity == player) {
                continue;
            }
            if (!(entity instanceof EntityLivingBase) && !entity.canBeCollidedWith()) {
                continue;
            }
            AxisAlignedBB target = entity.getEntityBoundingBox().grow(0.3D);
            RayTraceResult intercept = target.calculateIntercept(eye, end);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }
            double dist = eye.distanceTo(intercept.hitVec);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }
}
