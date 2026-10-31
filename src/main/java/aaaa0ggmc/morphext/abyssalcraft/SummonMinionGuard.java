package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * S4：召唤物刷宝抑制（Summon Loot Suppression）。
 * morph 动作召唤出的是带完整战利品表的真实 AC 生物，玩家反复召唤再亲手击杀
 * 即可无限刷物品/经验并造成实体堆积。本处理器对打过 {@link BossNativeHelper#TAG_SUMMON}
 * 标记的召唤物统一取消战利品掉落与经验掉落；野生同种生物完全不受影响。
 * 召唤存量上限见 {@link BossNativeHelper#capSummons}（同宿主同类型 ≤4，超出替换旧的）。
 */
@Mod.EventBusSubscriber(modid = MorphExtAbyssalCraft.MODID)
public final class SummonMinionGuard {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && BossNativeHelper.isSummon(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && BossNativeHelper.isSummon(event.getEntityLiving())) {
            event.setDroppedExperience(0);
        }
    }

    private SummonMinionGuard() {
    }
}
