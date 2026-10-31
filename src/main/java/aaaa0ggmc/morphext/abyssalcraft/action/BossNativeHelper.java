package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 深渊 Boss 原生反射与桥接助手：
 * 1. 动态调用生物原生私有攻击与弹幕方法，保证 100% 还原原生表现力；
 * 2. 完全解耦第三方可选类库（如 IceAndFire 的 IBlacklistedFromStatues），杜绝 NoClassDefFoundError；
 * 3. 注入属主标签，便于全局免伤切面识别；
 * 4. 安全反射，带有完整的异常防御与降级保护。
 */
public final class BossNativeHelper {

    public static final String TAG_MORPH_OWNER = "MorphOwnerUUID";

    /** S4：morph 动作召唤物标记（用于掉落/经验抑制与存量上限识别）。 */
    public static final String TAG_SUMMON = "MorphExtSummon";

    /** S4：同一宿主、同一召唤物类型的场上存活上限（超出替换旧的，防刷宝与实体堆积）。 */
    public static final int MAX_SUMMONS_PER_TYPE = 4;

    private static MethodHandle jzaharLaunchSkullMH;
    private static MethodHandle sacthothTeleportMH;
    private static Field jzaharDeathTicksField;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            Class<?> jzaharClass = Class.forName("com.shinoow.abyssalcraft.common.entity.EntityJzahar");
            Method mSkull = jzaharClass.getDeclaredMethod("launchWitherSkullToCoords",
                    int.class, double.class, double.class, double.class, boolean.class);
            mSkull.setAccessible(true);
            jzaharLaunchSkullMH = lookup.unreflect(mSkull);

            jzaharDeathTicksField = jzaharClass.getDeclaredField("deathTicks");
            jzaharDeathTicksField.setAccessible(true);

            Class<?> sacthothClass = Class.forName("com.shinoow.abyssalcraft.common.entity.EntitySacthoth");
            Method mTeleport = sacthothClass.getDeclaredMethod("teleportTo",
                    double.class, double.class, double.class);
            mTeleport.setAccessible(true);
            sacthothTeleportMH = lookup.unreflect(mTeleport);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static EntityLivingBase getMorphEntity(EntityLivingBase entity, AbstractMorph morph) {
        if (morph instanceof EntityMorph) {
            return ((EntityMorph) morph).getEntity(entity.world);
        }
        return null;
    }

    public static boolean isJzahar(EntityLivingBase entity) {
        return entity != null && entity.getClass().getName().endsWith("EntityJzahar");
    }

    public static boolean isSacthoth(EntityLivingBase entity) {
        return entity != null && entity.getClass().getName().endsWith("EntitySacthoth");
    }

    public static boolean launchJzaharSkull(EntityLivingBase jzahar, int head, double x, double y, double z, boolean invulnerable) {
        if (jzahar == null || jzaharLaunchSkullMH == null) return false;
        try {
            jzaharLaunchSkullMH.invoke(jzahar, head, x, y, z, invulnerable);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean teleportSacthoth(EntityLivingBase sacthoth, double x, double y, double z) {
        if (sacthoth == null || sacthothTeleportMH == null) return false;
        try {
            return (boolean) sacthothTeleportMH.invoke(sacthoth, x, y, z);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static int getJzaharDeathTicks(EntityLivingBase jzahar) {
        if (jzahar == null || jzaharDeathTicksField == null) return 0;
        try {
            return jzaharDeathTicksField.getInt(jzahar);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void setJzaharDeathTicks(EntityLivingBase jzahar, int ticks) {
        if (jzahar == null || jzaharDeathTicksField == null) return;
        try {
            jzaharDeathTicksField.setInt(jzahar, ticks);
        } catch (Throwable ignored) {
        }
    }

    public static void tagOwner(Entity entity, EntityLivingBase owner) {
        if (entity != null && owner != null) {
            entity.getEntityData().setString(TAG_MORPH_OWNER, owner.getUniqueID().toString());
        }
    }

    public static boolean isOwnedBy(Entity entity, EntityLivingBase owner) {
        if (entity == null || owner == null) return false;
        if (!entity.getEntityData().hasKey(TAG_MORPH_OWNER)) return false;
        String uuidStr = entity.getEntityData().getString(TAG_MORPH_OWNER);
        return owner.getUniqueID().toString().equals(uuidStr);
    }

    public static boolean isOwnedBy(Entity entity, EntityPlayer player) {
        return isOwnedBy(entity, (EntityLivingBase) player);
    }

    /** A16：实体是否被"别人"召唤/标记（有属主标签但属主不是 owner）。无主实体返回 false。 */
    public static boolean isOwnedByOther(Entity entity, EntityLivingBase owner) {
        if (entity == null || owner == null) return false;
        if (!entity.getEntityData().hasKey(TAG_MORPH_OWNER)) return false;
        String uuidStr = entity.getEntityData().getString(TAG_MORPH_OWNER);
        return !owner.getUniqueID().toString().equals(uuidStr);
    }

    public static EntityPlayer getOwnerPlayer(Entity entity) {
        if (entity == null || entity.world == null) return null;
        if (!entity.getEntityData().hasKey(TAG_MORPH_OWNER)) return null;
        try {
            java.util.UUID uuid = java.util.UUID.fromString(entity.getEntityData().getString(TAG_MORPH_OWNER));
            return entity.world.getPlayerEntityByUUID(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static EntityLivingBase getOwner(Entity entity) {
        return getOwnerPlayer(entity);
    }

    /**
     * S4：给召唤物打标记（同时打属主标签）。召唤物死亡不掉落战利品与经验
     * （见 SummonMinionGuard），并受 {@link #capSummons} 存量上限约束。
     */
    public static void markSummon(Entity summon, EntityLivingBase owner) {
        if (summon == null) return;
        tagOwner(summon, owner);
        summon.getEntityData().setBoolean(TAG_SUMMON, true);
    }

    public static boolean isSummon(Entity entity) {
        return entity != null && entity.getEntityData().hasKey(TAG_SUMMON)
                && entity.getEntityData().getBoolean(TAG_SUMMON);
    }

    /**
     * S4：同类召唤物存量上限——本次即将再召唤 {@code aboutToSpawn} 只前调用，
     * 保证"已有 + 本次"不超过 cap；超出时先移除 ticksExisted 最大的旧召唤物（替换式扩容）。
     */
    public static void capSummons(net.minecraft.world.World world, EntityLivingBase owner,
                                  Class<? extends EntityLiving> type, int aboutToSpawn) {
        if (world == null || owner == null || type == null || aboutToSpawn <= 0) return;
        java.util.List<EntityLiving> existing = new java.util.ArrayList<>();
        for (Entity e : world.loadedEntityList) {
            if (type.isInstance(e) && !e.isDead && e.isEntityAlive() && isSummon(e) && isOwnedBy(e, owner)) {
                existing.add((EntityLiving) e);
            }
        }
        int overflow = existing.size() + aboutToSpawn - MAX_SUMMONS_PER_TYPE;
        if (overflow <= 0) return;
        // 老的在前（ticksExisted 越大越老）
        existing.sort((a, b) -> Integer.compare(b.ticksExisted, a.ticksExisted));
        for (int i = 0; i < overflow && i < existing.size(); i++) {
            existing.get(i).setDead();
        }
    }

    private BossNativeHelper() { }
}
