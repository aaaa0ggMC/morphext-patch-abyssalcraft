package aaaa0ggmc.morphext.abyssalcraft.action;

import aaaa0ggmc.morphext.integration.metamorph.ICooldownAction;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 深渊国度主动能力通用基类。
 * 1. 彻底解耦硬编码面向 EntityPlayer 的缺陷，全面面向通用存活实体 EntityLivingBase；
 * 2. 实现 ICooldownAction，支持原生精准动态冷却；
 * 3. 安全距离与实体过滤，防止任何 self-hit 或友军误伤；
 * 4. 严禁无控世界破坏，保护玩家建筑与地形。
 */
public abstract class AbyssalCraftAction implements IAction, ICooldownAction {
    /** A15：空挥/未命中（executeServer 返回 false）时退还的短档冷却（1s 手感档）。 */
    private static final int REFUND_COOLDOWN_TICKS = 20;

    private final int cooldownTicks;
    /**
     * 分支动态冷却（A10 语义修正）：executeServer 内 setCooldown 写入"本次执行的分支冷却档"，
     * getCooldownTicks 读取即消费（读一次即移除），execute 开始时也会清空残留。
     * 预留冷却查询（ServerAbilityService 在 execute 前调用）因此永远拿到基准值，
     * 不再把上一次分支的档位带到下一次（旧实现只增不减、跨分支互相污染）。
     * 实际冷却仍由 ServerAbilityStore 时间戳权威强制，最终生效档 = max(基准, 本次分支档)。
     */
    protected final Map<UUID, Integer> dynamicCooldowns = new ConcurrentHashMap<>();

    public AbyssalCraftAction(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public int getCooldownTicks(EntityLivingBase entity) {
        if (entity != null) {
            Integer cd = dynamicCooldowns.remove(entity.getUniqueID());
            if (cd != null) {
                return cd;
            }
        }
        return this.cooldownTicks;
    }

    protected void setCooldown(EntityLivingBase entity, int ticks) {
        if (entity != null) {
            dynamicCooldowns.put(entity.getUniqueID(), ticks);
        }
    }

    /**
     * 辅助方法：检查实体主手或副手是否持有指定物品
     */
    public static EnumHand findHeldItem(EntityLivingBase entity, Item item) {
        if (entity == null || item == null) return null;
        if (entity.getHeldItemMainhand().getItem() == item) {
            return EnumHand.MAIN_HAND;
        }
        if (entity.getHeldItemOffhand().getItem() == item) {
            return EnumHand.OFF_HAND;
        }
        return null;
    }

    /**
     * 辅助方法：消耗手持容器并给予产物（如挤奶、盛装熔岩）
     */
    protected boolean consumeAndGive(EntityPlayer player, EnumHand hand, ItemStack resultStack) {
        ItemStack held = player.getHeldItem(hand);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        if (held.isEmpty()) {
            player.setHeldItem(hand, resultStack.copy());
        } else if (!player.inventory.addItemStackToInventory(resultStack.copy())) {
            player.dropItem(resultStack.copy(), false);
        }
        return true;
    }

    @Override
    public void execute(EntityLivingBase entity, AbstractMorph morph) {
        if (entity == null || entity.world == null) {
            return;
        }

        if (!entity.world.isRemote) {
            // A10：清掉上次执行可能残留的分支冷却档，防异常中断后串档
            dynamicCooldowns.remove(entity.getUniqueID());
            entity.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
            boolean success = executeServer(entity, morph);
            if (!success) {
                // A15：空挥/未命中不白吃完整冷却，按 1s 短档退还
                refundCooldown(entity, morph);
            }
        }
    }

    protected abstract boolean executeServer(EntityLivingBase entity, AbstractMorph morph);

    /**
     * A15 空挥冷却退还：executeServer 返回 false（无命中/无效果）时，把
     * ServerAbilityService 已预留的完整冷却改写为 1s 短档。
     * 冷却存档键为 "metamorph:granted:&lt;morph名&gt;:&lt;动作注册名&gt;"
     * （与 MetamorphServerAbilities.activate 的键一致），动作注册名按注册表身份反查；
     * 反查失败则放弃退还（保留完整冷却），绝不影响玩法。
     */
    private void refundCooldown(EntityLivingBase entity, AbstractMorph morph) {
        if (!(entity instanceof net.minecraft.entity.player.EntityPlayerMP) || morph == null || morph.name == null) {
            return;
        }
        try {
            String actionName = null;
            for (Map.Entry<String, IAction> entry : mchorse.metamorph.api.MorphManager.INSTANCE.actions.entrySet()) {
                if (entry.getValue() == this) {
                    actionName = entry.getKey();
                    break;
                }
            }
            if (actionName == null) {
                return;
            }
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) entity;
            String id = "metamorph:granted:" + morph.name + ":" + actionName;
            long now = aaaa0ggmc.morphext.ability.PlayerAbilityStore.now(mp);
            aaaa0ggmc.morphext.ability.PlayerAbilityStore.update(mp,
                    data -> data.setCooldown(id, now, REFUND_COOLDOWN_TICKS));
        } catch (Throwable ignored) {
            // 退还失败不破坏玩法：保留完整冷却
        }
    }

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
