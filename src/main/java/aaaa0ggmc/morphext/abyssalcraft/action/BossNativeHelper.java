package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import net.minecraft.entity.Entity;
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

    public static boolean isOwnedBy(Entity entity, EntityPlayer player) {
        if (entity == null || player == null) return false;
        if (!entity.getEntityData().hasKey(TAG_MORPH_OWNER)) return false;
        String uuidStr = entity.getEntityData().getString(TAG_MORPH_OWNER);
        return player.getUniqueID().toString().equals(uuidStr);
    }

    private BossNativeHelper() { }
}
