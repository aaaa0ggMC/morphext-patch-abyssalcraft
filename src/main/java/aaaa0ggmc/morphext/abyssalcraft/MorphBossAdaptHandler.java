package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
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
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;

/**
 * 深渊生物与 Boss 动态切面适配核心处理器 (Boss Dynamic Adaptation & Immunity Layer)：
 * 1. 净化变身实体的 AI 索敌机制，彻底消除锁定宿主自身的死穴；
 * 2. 宿主绝对免伤护盾（免疫自身弹幕、黑洞、坍缩、地震落石及咆哮撞墙自伤）；
 * 3. 萨克索斯/暗影生物绝对免疫失明光环（视野清晰无黑屏）；
 * 4. 扎哈尔变身死亡拦截：彻底阻断 19x19x19 地形蒸发与暗黑领域放逐，延续第四面墙人设发送幽默临死感言并自然重生；
 * 5. 阻止黑洞将宿主吸入随机异次元维度；
 * 6. 零侵入世界：对自然刷出的原生 Boss 100% 保持原有 AI 与数值，不产生任何副作用。
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
     * 变身完成事件：立即净化内部实体的索敌 AI，防止锁定宿主
     */
    @SubscribeEvent
    public static void onMorphPost(MorphEvent.Post event) {
        if (event.player != null && !event.player.world.isRemote) {
            sanitizeMorphEntity(event.player, event.morph);
        }
    }

    /**
     * 每 tick 更新：持续确保变身实体 AI 被净化，并抑制变身扎哈尔的地形破坏计数器
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;

        // 1. 如果是玩家，持续维护其内部变身实体的索敌 AI
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.ticksExisted % 20 == 0) {
                IMorphing morphing = Morphing.get(player);
                if (morphing != null && morphing.isMorphed()) {
                    sanitizeMorphEntity(player, morphing.getCurrentMorph());
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
     * 实体 AI 净化逻辑：将对 EntityPlayer 的仇恨任务清除，替换为安全的怪对怪索敌
     */
    private static void sanitizeMorphEntity(EntityPlayer player, AbstractMorph morph) {
        if (!(morph instanceof EntityMorph)) return;
        EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
        if (!(dummy instanceof EntityLiving)) return;

        EntityLiving living = (EntityLiving) dummy;
        BossNativeHelper.tagOwner(living, player);

        // 清除任何硬编码锁定 EntityPlayer 的索敌任务
        boolean cleaned = false;
        Iterator<EntityAITasks.EntityAITaskEntry> it = living.targetTasks.taskEntries.iterator();
        while (it.hasNext()) {
            EntityAITasks.EntityAITaskEntry entry = it.next();
            if (entry.action instanceof EntityAINearestAttackableTarget) {
                it.remove();
                cleaned = true;
            }
        }

        if (cleaned && living instanceof EntityCreature) {
            // 注入安全索敌：只攻击怪，绝不攻击宿主玩家与队友
            living.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>((EntityCreature) living, EntityLivingBase.class, 10, true, false,
                    target -> target != player && !target.isOnSameTeam(player) && (target instanceof IMob || target instanceof EntityLiving)));
        }

        // 若当前目标为宿主玩家，立刻解除
        if (living.getAttackTarget() == player) {
            living.setAttackTarget(null);
        }
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

                // 3. 扎哈尔变身自身触发的大地震熔岩地热与终焉咆哮撞墙自伤，阻断
                if (BossNativeHelper.isJzahar(dummy) || (morph.name != null && morph.name.contains("jzahar"))) {
                    if (source == DamageSource.HOT_FLOOR || source == DamageSource.FLY_INTO_WALL || source == DamageSource.LIGHTNING_BOLT) {
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
