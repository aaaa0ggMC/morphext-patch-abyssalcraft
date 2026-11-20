package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 周围实体。只读客户端世界，按距离从近到远打标签，最多细标 48 只。 */
public final class NearbyEntityDetector implements ConverseDetector {
    private static final int TAG_CAP = 48;

    @Override
    public String id() {
        return "nearby_entity";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || player.world == null || !player.world.isRemote || scan == null) {
            return;
        }
        double radius = ConverseAspects.NEARBY_RADIUS;
        AxisAlignedBB box = player.getEntityBoundingBox().grow(radius);
        List<Entity> found = player.world.getEntitiesWithinAABBExcludingEntity(player, box);
        if (found == null || found.isEmpty()) {
            scan.add(ConverseAspects.ENTITY_ALONE);
            scan.fact("count", "0");
            return;
        }
        double radiusSq = radius * radius;
        List<Entity> nearby = new ArrayList<Entity>();
        for (Entity entity : found) {
            if (!worth(player, entity, radiusSq)) {
                continue;
            }
            nearby.add(entity);
        }
        if (nearby.isEmpty()) {
            scan.add(ConverseAspects.ENTITY_ALONE);
            scan.fact("count", "0");
            return;
        }
        Collections.sort(nearby, new Comparator<Entity>() {
            @Override
            public int compare(Entity a, Entity b) {
                return Double.compare(player.getDistanceSq(a), player.getDistanceSq(b));
            }
        });
        int living = 0;
        for (Entity entity : nearby) {
            if (entity instanceof EntityLivingBase) {
                living++;
            }
        }
        scan.add(ConverseAspects.ENTITY_ANY);
        scan.fact("count", Integer.toString(living));
        if (living <= 0) {
            scan.add(ConverseAspects.ENTITY_ALONE);
        } else if (living == 1) {
            scan.add(ConverseAspects.ENTITY_COUNT_ONE);
        } else if (living <= 4) {
            scan.add(ConverseAspects.ENTITY_COUNT_FEW);
        } else {
            scan.add(ConverseAspects.ENTITY_COUNT_MANY);
        }
        int tagged = 0;
        for (Entity entity : nearby) {
            if (tagged >= TAG_CAP) {
                break;
            }
            EntityTags.tag(scan, entity, false);
            if (entity instanceof EntityLivingBase && scan.fact("entity") == null) {
                scan.fact("entity", Names.of(entity));
            }
            tagged++;
        }
    }

    private static boolean worth(EntityPlayer player, Entity entity, double radiusSq) {
        if (entity == null || entity == player || entity.isDead) {
            return false;
        }
        if (entity instanceof EntityXPOrb) {
            return false;
        }
        if (!(entity instanceof EntityLivingBase) && !(entity instanceof EntityItem)
                && !(entity instanceof EntityArrow) && !(entity instanceof EntityThrowable)
                && !(entity instanceof EntityFireball)) {
            return false;
        }
        return player.getDistanceSq(entity) <= radiusSq;
    }
}
