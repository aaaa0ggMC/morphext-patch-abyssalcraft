package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.AbyssalCraftAction;
import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
import aaaa0ggmc.morphext.faction.FactionEnmity;
import aaaa0ggmc.morphext.faction.FactionGraphEngine;
import aaaa0ggmc.morphext.faction.FactionManager;
import com.shinoow.abyssalcraft.common.entity.EntityBlackHole;
import com.shinoow.abyssalcraft.common.entity.EntityImplosion;
import com.shinoow.abyssalcraft.lib.ACSounds;
import com.shinoow.abyssalcraft.lib.util.SpecialTextUtil;
import mchorse.metamorph.api.events.MorphEvent;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 深渊生物与 Boss 动态切面适配核心处理器 (Boss Dynamic Adaptation & Immunity Layer)：
 * 1. 净化变身实体的 AI 索敌机制，彻底消除锁定宿主自身的死穴；
 * 2. 拦截 LivingSetAttackTargetEvent，杜绝 Metamorph 强制将 dummy 实体目标重定向至宿主玩家；
 * 3. 宿主绝对免伤护盾（免疫自身弹幕、黑洞引力与致死伤害、内爆、地震熔岩及咆哮撞墙自伤）；
 * 4. 萨克索斯/暗影生物绝对免疫失明光环（视野清晰无黑屏）；
 * 5. 扎哈尔变身死亡拦截：彻底阻断 19x19x19 地形蒸发与暗黑领域放逐，延续第四面墙人设发送幽默临死感言并自然重生；
 * 6. 阻止黑洞将宿主吸入随机异次元维度与物理位移牵引；
 * 7. 零侵入世界：对自然刷出的原生 Boss 100% 保持原有 AI 与数值，不产生任何副作用。
 */
@Mod.EventBusSubscriber(modid = MorphExtAbyssalCraft.MODID)
public final class MorphBossAdaptHandler {

    private static final String[] JZAHAR_FOURTH_WALL_DEATH_QUOTES = new String[] {
            "Segmentation Fault (core dumped)... 这具凡人的代码容器竟无法承载我的神力！待我重构内存堆栈，稍后再会。",
            "NullPointerException: Player entity memory corrupted... 别以为这样就能注销我的线程，蝼蚁。待我重新载入！",
            "Whoops, process terminated with exit code 0! 谁写的垃圾回收机制？！等玩家重构完毕，我会再次降临！",
            "Haha, you think 0 HP can unbind me from this memory address? See you at respawn point, mortal!"
    };

    /**
     * 核心切面拦截：切断变身 dummy 实体对宿主玩家或自身设为攻击目标的死穴！
     * 彻底阻断 Metamorph 的 onLivingSetAttackTarget 强制把目标设为 player。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingSetAttackTarget(LivingSetAttackTargetEvent event) {
        EntityLivingBase source = event.getEntityLiving();
        EntityLivingBase target = event.getTarget();
        if (source == null || target == null || source.world.isRemote) return;

        EntityPlayer owner = findMorphOwner(source);
        if (owner != null) {
            // 如果试图把目标设为宿主玩家、dummy 自身、或队友：立即置空切断！
            if (target == owner || target == source || target.isOnSameTeam(owner)) {
                if (source instanceof EntityLiving) {
                    ((EntityLiving) source).setAttackTarget(null);
                }
            }
        }
    }

    /**
     * 变身完成事件：立即净化内部实体的索敌 AI，防止锁定宿主
     */
    @SubscribeEvent
    public static void onMorphPost(MorphEvent.Post event) {
        if (event.player != null && !event.player.world.isRemote) {
            sanitizeMorphEntity(event.player, event.morph);
            // S5：解除变身时清零 ac_shoggoth_feast 饱食层数，防止跨形态残留
            if (event.isDemorphing()) {
                aaaa0ggmc.morphext.abyssalcraft.action.AbyssalExtraActions.ShoggothFeastAction.clearFeastStacks(event.player.getUniqueID());
            }
        }
    }

    /**
     * S5：登出时清零 ac_shoggoth_feast 饱食层数（UUID 级静态 Map 防泄漏）。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            aaaa0ggmc.morphext.abyssalcraft.action.AbyssalExtraActions.ShoggothFeastAction.clearFeastStacks(event.player.getUniqueID());
        }
    }

    public static final String ABILITY_ID_JZAHAR_BANISH = "morphext:jzahar_banish";
    public static final String TAG_JZAHAR_BANISH = "morphext_jzahar_banish";

    /**
     * 判断扎哈尔古神遣返光环是否处于开启状态。
     * 默认开启 (true)，完全忠实原版 AbyssalCraft 扎哈尔排他法则。
     */
    public static boolean isJzaharBanishAuraEnabled(EntityPlayer player) {
        if (player == null) return true;
        // 1. 优先读取 player.getEntityData()，快速且支持任意端
        if (player.getEntityData().hasKey(TAG_JZAHAR_BANISH)) {
            return player.getEntityData().getBoolean(TAG_JZAHAR_BANISH);
        }
        // 2. 服务端检查 PlayerAbilityStore
        if (player instanceof EntityPlayerMP) {
            com.google.gson.JsonObject state = aaaa0ggmc.morphext.ability.PlayerAbilityStore.get((EntityPlayerMP) player).getState(ABILITY_ID_JZAHAR_BANISH);
            if (state.has("enabled") && state.get("enabled").isJsonPrimitive()) {
                return state.get("enabled").getAsBoolean();
            }
        }
        // 3. 客户端检查 AbilityStateStore
        if (player.world.isRemote) {
            com.google.gson.JsonObject state = aaaa0ggmc.morphext.ability.AbilityStateStore.get(ABILITY_ID_JZAHAR_BANISH);
            if (state.has("enabled") && state.get("enabled").isJsonPrimitive()) {
                return state.get("enabled").getAsBoolean();
            }
        }
        // 4. 默认开启 (true)
        return true;
    }

    /**
     * 设置扎哈尔古神遣返光环的开启/关闭状态。
     */
    public static void setJzaharBanishAuraEnabled(EntityPlayer player, boolean enabled) {
        if (player == null) return;
        player.getEntityData().setBoolean(TAG_JZAHAR_BANISH, enabled);

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP mp = (EntityPlayerMP) player;
            com.google.gson.JsonObject state = aaaa0ggmc.morphext.ability.PlayerAbilityStore.get(mp).getState(ABILITY_ID_JZAHAR_BANISH);
            state.addProperty("enabled", enabled);
            aaaa0ggmc.morphext.ability.PlayerAbilityStore.update(mp, data -> data.putState(ABILITY_ID_JZAHAR_BANISH, state));
            aaaa0ggmc.morphext.network.AbilityNetwork.syncPlayerIfChanged(mp);
        }

        // 同步更新当前变身 dummy 实体上的 NBT，确保无论通过 owner 还是直接通过 entityData 均能读取
        IMorphing morphing = Morphing.get(player);
        if (morphing != null && morphing.isMorphed()) {
            AbstractMorph morph = morphing.getCurrentMorph();
            if (morph instanceof EntityMorph) {
                EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
                if (dummy != null) {
                    dummy.getEntityData().setBoolean(TAG_JZAHAR_BANISH, enabled);
                }
            }
        }
    }

    /**
     * 每 tick 更新：持续确保变身实体 AI 被净化，并抑制变身扎哈尔的地形破坏计数器与黑洞拉扯
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null || entity.world == null) return;

        // 0. 扎哈尔变身 dummy 实例的遣返拦截与粒子防御（双端通用）
        if (BossNativeHelper.isJzahar(entity)) {
            boolean isMorphDummy = !entity.isAddedToWorld() || entity.getEntityData().hasKey(BossNativeHelper.TAG_MORPH_OWNER);
            if (isMorphDummy) {
                int dt = BossNativeHelper.getJzaharDeathTicks(entity);
                if (dt >= 750) {
                    // 重置计数器，绝对杜绝 790 tick 的 19x19x19 范围 setBlockToAir 蒸发
                    BossNativeHelper.setJzaharDeathTicks(entity, 0);
                }

                EntityPlayer owner = findMorphOwner(entity);
                boolean banishEnabled = true;
                if (owner != null) {
                    banishEnabled = isJzaharBanishAuraEnabled(owner);
                } else if (entity.getEntityData().hasKey(TAG_JZAHAR_BANISH)) {
                    banishEnabled = entity.getEntityData().getBoolean(TAG_JZAHAR_BANISH);
                }

                if (!banishEnabled) {
                    // 玩家主动关闭了遣返光环：阻断 dummy Jzahar 的 onLivingUpdate 执行，
                    // 彻底跳过 64 格内全部 Boss 的扫描与 removeEntity(entity) 遣返逻辑！
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // 1. 如果是玩家，免疫黑洞与坍缩的物理引力拉扯（双端执行，避免客户端被强行牵引！）
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph currentMorph = morphing.getCurrentMorph();
                // 扎哈尔或深渊古神形态：双端免疫一切黑洞引力拉扯
                if (isMorphedAsEldritch(player)) {
                    neutralizeBlackHolePull(player);
                }

                if (!entity.world.isRemote) {
                    // 仅对 AbyssalCraft 形态同步属主标记与遣返开关，避免每 tick 对全模组变身反复删加 AI（AI 净化已在 onMorphPost 完成）
                    if (currentMorph != null && currentMorph.name != null && currentMorph.name.toLowerCase(java.util.Locale.ROOT).startsWith("abyssalcraft")) {
                        syncAbyssalMorphTags(player, currentMorph);
                    }

                    // 反物质形态实体碰撞湮灭检测。
                    // 注（A7）：IPassiveAbility.onCollide 目前核心无任何分发点（死钩子），
                    // 这条按阵营解析的补偿路径是"接触湮灭"唯一实际生效的实现，
                    // 阵营解析口径改动会失效；若核心后续补上 onCollide 分发，可退役此段。
                    // FVP-28：原 morph.name.contains("anti") 子串选形态 → 改问形态阵营
                    // （resolveEntityFactions = 已激活 faction_* 被动 ∪ morph.name 映射），
                    // 覆盖 anti* 全家族且不误伤其它形态。
                    if (isMorphedInFaction(player, "antimatter")) {
                        List<EntityLivingBase> colliding = player.world.getEntitiesWithinAABB(EntityLivingBase.class,
                                player.getEntityBoundingBox().grow(0.15D),
                                e -> AbyssalCraftAction.isHostileTarget(player, e));
                        for (EntityLivingBase target : colliding) {
                            aaaa0ggmc.morphext.abyssalcraft.passive.AntimatterAnnihilationPassive.handleCollisionAnnihilation(player, target);
                        }
                    }
                }
            }
        }

        if (entity.world.isRemote) return;
    }

    /**
     * 实体生成监听：
     * 1. 凡是 Jzahar AI 或变身释放的 EntityBlackHole，无缝替换为免疫宿主与队友的 MorphEntityBlackHole；
     * 2. 凡是 EntityImplosion，无缝替换为安全的 MorphEntityImplosion；
     * 3. 凡是 dummy 实体发射的凋灵首、投射物，自动打上宿主属主标签。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world.isRemote) return;

        // 核心防御 1：变身扎哈尔 AI 释放的黑洞实体替换为安全黑洞；若场上有古神形态玩家，野生黑洞同样替换以豁免牵引
        if (entity.getClass() == EntityBlackHole.class) {
            EntityBlackHole blackHole = (EntityBlackHole) entity;
            EntityPlayer owner = findMorphOwner(blackHole.shootingEntity);
            if (owner == null) {
                owner = findMorphedJzaharOwner(entity.world);
            }
            if (owner != null || hasEldritchMorphedPlayer(entity.world)) {
                event.setCanceled(true);
                aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole safeHole =
                        new aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole(event.getWorld(), owner);
                safeHole.copyLocationAndAnglesFrom(blackHole);
                safeHole.motionX = blackHole.motionX;
                safeHole.motionY = blackHole.motionY;
                safeHole.motionZ = blackHole.motionZ;
                event.getWorld().spawnEntity(safeHole);
                return;
            }
        }

        // 核心防御 2：变身扎哈尔 AI 释放的奇点内爆实体替换为安全内爆
        if (entity.getClass() == EntityImplosion.class) {
            EntityImplosion implosion = (EntityImplosion) entity;
            EntityPlayer owner = findMorphOwner(implosion.shootingEntity);
            if (owner == null) {
                owner = findMorphedJzaharOwner(entity.world);
            }
            if (owner != null) {
                event.setCanceled(true);
                aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion safeImplosion =
                        new aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion(event.getWorld(), owner);
                safeImplosion.copyLocationAndAnglesFrom(implosion);
                safeImplosion.motionX = implosion.motionX;
                safeImplosion.motionY = implosion.motionY;
                safeImplosion.motionZ = implosion.motionZ;
                event.getWorld().spawnEntity(safeImplosion);
                return;
            }
        }

        if (entity instanceof EntityWitherSkull) {
            EntityWitherSkull skull = (EntityWitherSkull) entity;
            if (skull.shootingEntity != null) {
                EntityPlayer owner = findMorphOwner(skull.shootingEntity);
                if (owner != null) {
                    BossNativeHelper.tagOwner(skull, owner);
                }
            }
        }
    }

    /**
     * 每 tick 轻量同步：仅维持属主标签、Jzahar 遣返开关与清零误锁宿主，不再删加 AI 任务。
     */
    private static void syncAbyssalMorphTags(EntityPlayer player, AbstractMorph morph) {
        if (!(morph instanceof EntityMorph)) return;
        EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
        if (!(dummy instanceof EntityLiving)) return;
        EntityLiving living = (EntityLiving) dummy;
        BossNativeHelper.tagOwner(living, player);
        if (BossNativeHelper.isJzahar(living)) {
            living.getEntityData().setBoolean(TAG_JZAHAR_BANISH, isJzaharBanishAuraEnabled(player));
        }
        if (living.getAttackTarget() == player || living.getAttackTarget() == living) {
            living.setAttackTarget(null);
        }
        if (living.getRevengeTarget() == player || living.getRevengeTarget() == living) {
            living.setRevengeTarget(null);
        }
    }

    /**
     * 实体 AI 净化逻辑：将对 EntityPlayer 的仇恨任务清除，替换为安全的怪对怪索敌（仅在 MorphEvent.Post 调用）
     */
    private static void sanitizeMorphEntity(EntityPlayer player, AbstractMorph morph) {
        if (!(morph instanceof EntityMorph)) return;
        if (morph.name == null || !morph.name.toLowerCase(java.util.Locale.ROOT).startsWith("abyssalcraft")) return;
        EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
        if (!(dummy instanceof EntityLiving)) return;

        EntityLiving living = (EntityLiving) dummy;
        BossNativeHelper.tagOwner(living, player);
        if (BossNativeHelper.isJzahar(living)) {
            living.getEntityData().setBoolean(TAG_JZAHAR_BANISH, isJzaharBanishAuraEnabled(player));
        }

        // 1. 清除任何以 EntityPlayer 为目标的索敌任务并重置执行中任务
        boolean cleaned = false;
        List<net.minecraft.entity.ai.EntityAIBase> toRemoveTarget = new java.util.ArrayList<>();
        for (EntityAITasks.EntityAITaskEntry entry : living.targetTasks.taskEntries) {
            if (entry.action instanceof EntityAINearestAttackableTarget || entry.action instanceof EntityAIHurtByTarget) {
                toRemoveTarget.add(entry.action);
                cleaned = true;
            }
        }
        for (net.minecraft.entity.ai.EntityAIBase action : toRemoveTarget) {
            living.targetTasks.removeTask(action);
        }

        // 2. 清除 tasks 中的 EntityAIWatchClosest(EntityPlayer.class)
        List<net.minecraft.entity.ai.EntityAIBase> toRemoveTasks = new java.util.ArrayList<>();
        for (EntityAITasks.EntityAITaskEntry entry : living.tasks.taskEntries) {
            if (entry.action instanceof EntityAIWatchClosest) {
                toRemoveTasks.add(entry.action);
            }
        }
        for (net.minecraft.entity.ai.EntityAIBase action : toRemoveTasks) {
            living.tasks.removeTask(action);
        }

        if (cleaned && living instanceof EntityCreature) {
            // 注入安全索敌：只攻击敌人，绝不攻击宿主玩家、队友与同阵营生物
            // FVP-29：原谓词 `target instanceof IMob || target instanceof EntityLiving`
            // （IMob ⊂ EntityLiving）等价于「任何非同队 EntityLiving」→ 打盟友与主人的宠物。
            // 改问阵营图：FactionEnmity.isEnemy 内建主人/同主豁免 + MEMBER/RELATIVE/LORD 层级，
            // 未适配生物回退 MODERATE → true，行为不退化为「谁都不打」。
            living.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>((EntityCreature) living, EntityLivingBase.class, 10, true, false,
                    target -> target != player && !target.isOnSameTeam(player) && FactionEnmity.isEnemy(living, target)));
        }

        // 3. 实时强制清除宿主与自身仇恨目标
        if (living.getAttackTarget() == player || living.getAttackTarget() == living) {
            living.setAttackTarget(null);
        }
        if (living.getRevengeTarget() == player || living.getRevengeTarget() == living) {
            living.setRevengeTarget(null);
        }
    }

    /**
     * 免疫周围黑洞与坍缩的物理引力拉扯（安全黑洞 MorphEntityBlackHole 本身已豁免宿主，此处仅需阻断服务端发包抖动）
     */
    private static void neutralizeBlackHolePull(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            World world = player.world;
            List<Entity> blackHoles = world.getEntitiesWithinAABB(Entity.class,
                    player.getEntityBoundingBox().grow(48.0D),
                    e -> e instanceof EntityBlackHole || e instanceof EntityImplosion);

            if (!blackHoles.isEmpty()) {
                // 阻断服务端向客户端发送 SPacketEntityVelocity 造成的画面强行位移抖动
                ((EntityPlayerMP) player).velocityChanged = false;
            }
        }
    }

    /**
     * 反查某个实体是否为某个玩家变身的 dummy 实体
     */
    public static EntityPlayer findMorphOwner(EntityLivingBase entity) {
        if (entity == null || entity.world == null) return null;
        EntityPlayer owner = BossNativeHelper.getOwnerPlayer(entity);
        if (owner != null) return owner;

        for (EntityPlayer player : entity.world.playerEntities) {
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph morph = morphing.getCurrentMorph();
                if (morph instanceof EntityMorph) {
                    EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
                    if (dummy == entity) {
                        BossNativeHelper.tagOwner(entity, player);
                        return player;
                    }
                }
            }
        }
        return null;
    }

    /** 查找世界中任意处于扎哈尔形态的玩家（用于无属主标签的派生黑洞/内爆归属兜底）。 */
    private static EntityPlayer findMorphedJzaharOwner(World world) {
        if (world == null) return null;
        for (EntityPlayer p : world.playerEntities) {
            IMorphing morphing = Morphing.get(p);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph m = morphing.getCurrentMorph();
                if (m != null && m.name != null && m.name.toLowerCase(java.util.Locale.ROOT).contains("jzahar")) {
                    return p;
                }
            }
        }
        return null;
    }

    /** 检查世界中是否存在任意处于深渊古神阵营形态的玩家。 */
    private static boolean hasEldritchMorphedPlayer(World world) {
        if (world == null) return false;
        for (EntityPlayer p : world.playerEntities) {
            if (isMorphedAsEldritch(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * FVP-28：古神形态名单改问阵营图，删掉 morph.name 六子串硬判
     * （jzahar/sacthoth/omothol/shoggoth/chagaroth/dragonboss）。
     *
     * <p>口径 = 玩家当前阵营集合（已激活 {@code faction_*} 被动 ∪ morph.name 的
     * {@code entity_mappings}）是否落在 omothol / shadow / shoggoth / dread 四族。
     * 覆盖面对照：jzahar(omothol,shadow)、jzaharminion/remnant/omotholghoul(omothol)、
     * shoggoth 全族(shoggoth)、chagaroth 全族与 dragonboss(dread，经
     * {@code abyssalcraft:dragonboss → [dread, abyssal]} 映射)、
     * shadow 全族(shadow)——与旧六子串等价；新增 demon* 恶魔动物（本包数据里带
     * {@code faction_dread}）也会被计入，属数据自身的归属，不是漏判。</p>
     */
    public static boolean isMorphedAsEldritch(EntityPlayer player) {
        return isMorphedInFaction(player, "omothol", "shadow", "shoggoth", "dread");
    }

    /**
     * FVP-28：玩家当前形态是否属于给定阵营（单一真值 = 阵营图解析，不看 morph.name 子串）。
     */
    public static boolean isMorphedInFaction(EntityPlayer player, String... factionIds) {
        if (player == null || factionIds == null || factionIds.length == 0) return false;
        try {
            Set<String> factions = FactionGraphEngine.getInstance().resolveEntityFactions(player);
            if (factions == null || factions.isEmpty()) return false;
            for (String id : factionIds) {
                if (id != null && factions.contains(id.toLowerCase(java.util.Locale.ROOT))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // 解析失败按「不是该阵营」处理，不影响其它切面
        }
        return false;
    }

    /**
     * 形态基础名精确匹配：剥掉 "modid:" / "modid." 命名空间前缀后做全等比较，
     * 避免 contains 子串误伤（A17：如 abygolem 不等于 dreadgolem，不得共享其免疫）。
     */
    private static boolean isMorphNamed(String lowerMorphName, String baseName) {
        if (lowerMorphName == null) return false;
        String base = lowerMorphName;
        int cut = Math.max(base.lastIndexOf(':'), base.lastIndexOf('.'));
        if (cut >= 0) {
            base = base.substring(cut + 1);
        }
        return base.equals(baseName);
    }

    /**
     * 宿主绝对免伤与自伤免疫（HIGHEST 优先级最高防御）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (handleHostDamageImmunity(event.getEntityLiving(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (handleHostDamageImmunity(event.getEntityLiving(), event.getSource())) {
            event.setCanceled(true);
            return;
        }

        // 注（S2 修复）：原先这里还有一套按 morph.name.contains("shadow"/"dread"/"anti"/"shoggoth")
        // 的 handleAttackProc 手动分发，与 MorphAbilityManager.onLivingHurt → IPassiveAbility.onAttack
        // 的被动分发（ac_photophobia / ac_dread_plague / ac_antimatter_annihilation / ac_shoggoth_acid）
        // 对同一击双通道重复触发，导致 dread 2.5→5、anti 4→8、甲蚀 3→6 等数值翻倍。
        // 现已删除名字分发，统一走被动注册表单通道（单倍数值）；名字匹配还会给
        // 未挂载对应被动的形态（如 antighoul）强行附加伤害，属越权行为，一并消除。
    }

    private static boolean handleHostDamageImmunity(EntityLivingBase victim, DamageSource source) {
        if (!(victim instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) victim;

        Entity trueSource = source.getTrueSource();
        Entity immediateSource = source.getImmediateSource();

        // 1. 来自带有属主标签的弹幕/实体伤害：只拦定向打击（近战/弹幕直击主人）。
        //    波及不豁免（2026-11-04 拍板）：召唤物自爆/爆炸与范围效果溅射到主人照常结算——
        //    爆炸就是爆炸，波及谁谁掉血；判据与阵营门同一真值（FactionManager.isProtectionApplicable）。
        if (BossNativeHelper.isOwnedBy(trueSource, player) || BossNativeHelper.isOwnedBy(immediateSource, player)) {
            Entity actor = trueSource != null ? trueSource : immediateSource;
            if (FactionManager.isProtectionApplicable(actor, player, source)) {
                return true;
            }
        }

        IMorphing morphing = Morphing.get(player);
        if (morphing != null && morphing.isMorphed()) {
            AbstractMorph morph = morphing.getCurrentMorph();
            if (morph instanceof EntityMorph) {
                EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
                // 2. 来自变身内部 dummy 实体的直接伤害，阻断
                if (trueSource == dummy || immediateSource == dummy) {
                    return true;
                }

                String mName = morph.name != null ? morph.name.toLowerCase() : "";

                // 3. 扎哈尔变身自身触发的大地震熔岩地热与终焉咆哮撞墙自伤 + 第四面墙秒杀，阻断
                if (BossNativeHelper.isJzahar(dummy) || mName.contains("jzahar")) {
                    if (source == DamageSource.HOT_FLOOR || source == DamageSource.FLY_INTO_WALL || source == DamageSource.LIGHTNING_BOLT) {
                        return true;
                    }
                    // 第四面墙秒杀（伤害绝对值 + 不可格挡）由 AbyssalCraft patch 负责拦截
                    if (source.isDamageAbsolute() && source.isUnblockable()) {
                        return true;
                    }
                }

                // 4. 修格斯弹射物跳弹反馈音效（伤害免疫本身已交由核心 MorphImmunityHandler + morph_abilities.json 接管）
                if (mName.contains("shoggoth") && source.isProjectile()) {
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            net.minecraft.init.SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
        return false;
    }

    /**
     * 黑洞防误吸：防止玩家被自己释放的黑洞传送至异次元维度
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTravelDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph morph = morphing.getCurrentMorph();
                if (morph != null && morph.name != null && morph.name.toLowerCase(java.util.Locale.ROOT).contains("jzahar")) {
                    // 若周围 12 格内存在黑洞，拦截次元传送
                    boolean nearBlackHole = !player.world.getEntitiesWithinAABB(Entity.class,
                            player.getEntityBoundingBox().grow(12.0D),
                            e -> e.getClass().getName().endsWith("EntityBlackHole")).isEmpty();
                    if (nearBlackHole) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    /**
     * 扎哈尔变身死亡拦截与打破第四面墙感言：
     * 阻断 19x19x19 范围地形破坏，发送专属幽默感言，让玩家自然重生。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().world.isRemote) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph morph = morphing.getCurrentMorph();
                if (morph != null && morph.name != null && morph.name.toLowerCase().contains("jzahar")) {
                    // 1. 重置内部扎哈尔 dummy 实体的 deathTicks，消除 790 tick 破坏
                    if (morph instanceof EntityMorph) {
                        EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
                        if (dummy != null && BossNativeHelper.isJzahar(dummy)) {
                            BossNativeHelper.setJzaharDeathTicks(dummy, 0);
                        }
                    }

                    // 2. 播放原生古神音画特效
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            ACSounds.jzahar_blast, SoundCategory.HOSTILE, 5.0F, 0.8F);
                    if (player.world instanceof WorldServer) {
                        ((WorldServer) player.world).spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                                player.posX, player.posY + 1.5D, player.posZ, 4, 0.8D, 0.8D, 0.8D, 0.05D);
                    }

                    // 3. 打破第四面墙的 Jzahar 专属临死感言广播
                    String quote = JZAHAR_FOURTH_WALL_DEATH_QUOTES[player.world.rand.nextInt(JZAHAR_FOURTH_WALL_DEATH_QUOTES.length)];
                    SpecialTextUtil.JzaharGroup(player.world, quote);
                }
            }
        }
    }
}
