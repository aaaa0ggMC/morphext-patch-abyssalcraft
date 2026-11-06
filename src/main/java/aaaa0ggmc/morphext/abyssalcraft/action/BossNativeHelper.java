package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.follower.TimedSummons;
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

    /** M5：通用召唤物默认寿命（tick，30s）。落在建议 300-600t 区间上沿，配 cap≤4 防堆积。 */
    public static final int SUMMON_LIFESPAN_DEFAULT = 600;

    /** M5：Boss 亲卫 / 长期跟随类召唤物寿命（tick，60s）。 */
    public static final int SUMMON_LIFESPAN_GUARD = 1200;

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
            return findPlayerAcrossDims(entity.world, uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * M5：跨维玩家解析。根因：World#getPlayerEntityByUUID 只遍历本维度 playerEntities，
     * 主人换维度后解析为 null（黑洞/内爆豁免失效）。优先 PlayerList#getPlayerByUUID
     * （全维在线玩家列表，跨维有效）；拿不到（如纯客户端世界）再退回本维查找。
     */
    public static EntityPlayer findPlayerAcrossDims(net.minecraft.world.World world, java.util.UUID uuid) {
        if (world == null || uuid == null) return null;
        net.minecraft.server.MinecraftServer server = world.getMinecraftServer();
        if (server != null) {
            EntityPlayer player = server.getPlayerList().getPlayerByUUID(uuid);
            if (player != null) return player;
        }
        return world.getPlayerEntityByUUID(uuid);
    }

    public static EntityLivingBase getOwner(Entity entity) {
        return getOwnerPlayer(entity);
    }

    /**
     * S4：给召唤物打标记（同时打属主标签）。召唤物死亡不掉落战利品与经验
     * （见 SummonMinionGuard），并受 {@link #capSummons} 存量上限约束。
     * M5：同时挂持久寿命，默认 {@link #SUMMON_LIFESPAN_DEFAULT}。
     */
    public static void markSummon(Entity summon, EntityLivingBase owner) {
        markSummon(summon, owner, SUMMON_LIFESPAN_DEFAULT);
    }

    /**
     * M5：同 {@link #markSummon(Entity, EntityLivingBase)}，可指定寿命（tick）。
     * 寿命走核心通用召唤物时效机制 {@link TimedSummons#mark}：NBT {@code ulSummonUntil}
     * 世界总 tick 截止值，随存档持久（重登/重载寿命不重计），到期服务端自动清理
     * 并解除随从关系；玩家下线/重启后召唤物不再永久留存。
     */
    public static void markSummon(Entity summon, EntityLivingBase owner, int lifespanTicks) {
        if (summon == null) return;
        tagOwner(summon, owner);
        summon.getEntityData().setBoolean(TAG_SUMMON, true);
        if (summon instanceof EntityLivingBase) {
            TimedSummons.mark((EntityLivingBase) summon, lifespanTicks);
        } else {
            markSummonExpiry(summon, lifespanTicks);
        }
    }

    /**
     * M5：给非活体召唤物（黑洞/内爆等裸 {@link Entity}）挂与 {@link TimedSummons}
     * 同口径的持久寿命标记（同一 NBT 标签 {@code ulSummonUntil}、同一世界总 tick 时钟）。
     * 说明：{@link TimedSummons#mark} 只接受 EntityLivingBase，且到期清理挂在
     * LivingUpdateEvent 上，裸 Entity 不触发该钩子，故由实体 onUpdate 调
     * {@link #isSummonExpired} 自行判过期。
     */
    public static void markSummonExpiry(Entity summon, int lifespanTicks) {
        if (summon == null || summon.world == null || summon.world.isRemote) return;
        if (lifespanTicks <= 0) {
            summon.getEntityData().removeTag(TimedSummons.EXPIRE_TAG);
            return;
        }
        summon.getEntityData().setLong(TimedSummons.EXPIRE_TAG,
                summon.world.getTotalWorldTime() + lifespanTicks);
    }

    /** M5：召唤物持久寿命是否到期（服务端判定；未标记返回 false）。 */
    public static boolean isSummonExpired(Entity entity) {
        if (entity == null || entity.world == null || entity.world.isRemote) return false;
        if (!TimedSummons.isMarked(entity)) return false;
        return entity.world.getTotalWorldTime() >= entity.getEntityData().getLong(TimedSummons.EXPIRE_TAG);
    }

    public static boolean isSummon(Entity entity) {
        return entity != null && entity.getEntityData().hasKey(TAG_SUMMON)
                && entity.getEntityData().getBoolean(TAG_SUMMON);
    }

    /**
     * S4：同类召唤物存量上限——本次即将再召唤 {@code aboutToSpawn} 只前调用，
     * 保证"已有 + 本次"不超过 cap；超出时先移除 ticksExisted 最大的旧召唤物（替换式扩容）。
     * M5：计数改按 TimedSummons 持久寿命标记（{@link TimedSummons#isMarked}）识别召唤物，
     * 并尽力全维扫描（server.worlds 各维 loadedEntityList）。
     * 现实限制：未加载区块中的召唤物不在任何 loadedEntityList 里、无法精确计数，
     * 可能暂态超编；但 TTL 到期会自动清理孤儿召唤物，超编只是暂态，最终收敛回上限内。
     */
    public static void capSummons(net.minecraft.world.World world, EntityLivingBase owner,
                                  Class<? extends EntityLiving> type, int aboutToSpawn) {
        if (world == null || owner == null || type == null || aboutToSpawn <= 0) return;
        java.util.List<EntityLiving> existing = new java.util.ArrayList<>();
        net.minecraft.server.MinecraftServer server = world.getMinecraftServer();
        if (server != null) {
            for (net.minecraft.world.WorldServer ws : server.worlds) {
                collectSummons(ws, owner, type, existing);
            }
        } else {
            collectSummons(world, owner, type, existing);
        }
        int overflow = existing.size() + aboutToSpawn - MAX_SUMMONS_PER_TYPE;
        if (overflow <= 0) return;
        // 老的在前（ticksExisted 越大越老）
        existing.sort((a, b) -> Integer.compare(b.ticksExisted, a.ticksExisted));
        for (int i = 0; i < overflow && i < existing.size(); i++) {
            existing.get(i).setDead();
        }
    }

    private static void collectSummons(net.minecraft.world.World world, EntityLivingBase owner,
                                       Class<? extends EntityLiving> type, java.util.List<EntityLiving> out) {
        for (Entity e : world.loadedEntityList) {
            if (type.isInstance(e) && !e.isDead && e.isEntityAlive()
                    && TimedSummons.isMarked(e) && isOwnedBy(e, owner)) {
                out.add((EntityLiving) e);
            }
        }
    }

    private BossNativeHelper() { }
}
