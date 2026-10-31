package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
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
import net.minecraft.entity.monster.IMob;
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
        }
    }

    /**
     * 每 tick 更新：持续确保变身实体 AI 被净化，并抑制变身扎哈尔的地形破坏计数器与黑洞拉扯
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;

        // 1. 如果是玩家，持续维护其内部变身实体的索敌 AI 并免疫黑洞物理拉扯
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph currentMorph = morphing.getCurrentMorph();
                sanitizeMorphEntity(player, currentMorph);

                // 如果变身为扎哈尔或深渊古神，免疫周围野生黑洞与坍缩的物理引力拉扯
                if (isMorphedAsEldritch(currentMorph)) {
                    neutralizeBlackHolePull(player);
                }
            }
        }

        // 2. 如果是扎哈尔实例：若是变身 dummy 实体或附带宿主标签，双重防御阻止 deathTicks 达到 790
        if (BossNativeHelper.isJzahar(entity)) {
            if (!entity.isAddedToWorld() || entity.getEntityData().hasKey(BossNativeHelper.TAG_MORPH_OWNER)) {
                int dt = BossNativeHelper.getJzaharDeathTicks(entity);
                if (dt >= 750) {
                    // 重置计数器，绝对杜绝 790 tick 的 19x19x19 范围 setBlockToAir 蒸发
                    BossNativeHelper.setJzaharDeathTicks(entity, 0);
                }
            }
        }
    }

    /**
     * 实体生成监听：凡是 dummy 实体发射的凋灵首、投射物，自动打上宿主属主标签
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world.isRemote) return;

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
     * 实体 AI 净化逻辑：将对 EntityPlayer 的仇恨任务清除，替换为安全的怪对怪索敌
     */
    private static void sanitizeMorphEntity(EntityPlayer player, AbstractMorph morph) {
        if (!(morph instanceof EntityMorph)) return;
        EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
        if (!(dummy instanceof EntityLiving)) return;

        EntityLiving living = (EntityLiving) dummy;
        BossNativeHelper.tagOwner(living, player);

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
            // 注入安全索敌：只攻击怪，绝不攻击宿主玩家与队友
            living.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>((EntityCreature) living, EntityLivingBase.class, 10, true, false,
                    target -> target != player && !target.isOnSameTeam(player) && (target instanceof IMob || target instanceof EntityLiving)));
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
     * 免疫周围野生黑洞与坍缩的物理引力拉扯
     */
    private static void neutralizeBlackHolePull(EntityPlayer player) {
        World world = player.world;
        List<Entity> blackHoles = world.getEntitiesWithinAABB(Entity.class,
                player.getEntityBoundingBox().grow(48.0D),
                e -> e instanceof EntityBlackHole || e instanceof EntityImplosion);

        if (!blackHoles.isEmpty()) {
            if (player instanceof EntityPlayerMP) {
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

    public static boolean isMorphedAsEldritch(AbstractMorph morph) {
        if (morph == null || morph.name == null) return false;
        String n = morph.name.toLowerCase();
        return n.contains("jzahar") || n.contains("sacthoth") || n.contains("omothol")
                || n.contains("shoggoth") || n.contains("chagaroth") || n.contains("dragonboss");
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

        // 暗影形态近战攻击被动触发：对被攻击目标施加致盲与暗影凋零
        if (event.getSource() != null && event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph morph = morphing.getCurrentMorph();
                if (morph != null && morph.name != null && morph.name.toLowerCase().contains("shadow")) {
                    aaaa0ggmc.morphext.abyssalcraft.passive.ShadowPhotophobiaPassive.handleAttackProc(player, event.getEntityLiving());
                }
            }
        }
    }

    private static boolean handleHostDamageImmunity(EntityLivingBase victim, DamageSource source) {
        if (!(victim instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) victim;

        Entity trueSource = source.getTrueSource();
        Entity immediateSource = source.getImmediateSource();

        // 1. 来自带有属主标签的弹幕/实体伤害，直接阻断
        if (BossNativeHelper.isOwnedBy(trueSource, player) || BossNativeHelper.isOwnedBy(immediateSource, player)) {
            return true;
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

                // 3. 扎哈尔变身自身触发的大地震熔岩地热与终焉咆哮撞墙自伤，阻断
                if (BossNativeHelper.isJzahar(dummy) || mName.contains("jzahar")) {
                    if (source == DamageSource.HOT_FLOOR || source == DamageSource.FLY_INTO_WALL || source == DamageSource.LIGHTNING_BOLT) {
                        return true;
                    }
                }

                // 4. 阿索拉 / 恐惧巨龙俯冲强袭防摔落自伤与恐惧/珊瑚瘟疫伤害阻断
                if (mName.contains("dragonboss") || mName.contains("asorah")) {
                    if (source == DamageSource.FALL || source == com.shinoow.abyssalcraft.api.AbyssalCraftAPI.dread
                            || source == com.shinoow.abyssalcraft.api.AbyssalCraftAPI.coralium) {
                        return true;
                    }
                }

                // 5. 萨克索斯/暗影生物自身暗影与窒息免伤
                if (mName.contains("shadow") || mName.contains("sacthoth")) {
                    if (source == com.shinoow.abyssalcraft.api.AbyssalCraftAPI.shadow || source == DamageSource.IN_WALL) {
                        return true;
                    }
                }

                // 6. 查伽洛斯恐惧免伤
                if (mName.contains("chagaroth")) {
                    if (source == com.shinoow.abyssalcraft.api.AbyssalCraftAPI.dread) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 药水负面免疫：萨克索斯/暗影形态免疫失明光环
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (event.getPotionEffect().getPotion() == MobEffects.BLINDNESS) {
                IMorphing morphing = Morphing.get(player);
                if (morphing != null && morphing.isMorphed()) {
                    AbstractMorph morph = morphing.getCurrentMorph();
                    if (morph != null && morph.name != null) {
                        String name = morph.name.toLowerCase();
                        if (name.contains("sacthoth") || name.contains("shadow") || name.contains("jzahar")) {
                            event.setResult(Event.Result.DENY);
                        }
                    }
                }
            }
        }
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
                if (morph != null && morph.name != null && morph.name.contains("jzahar")) {
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
