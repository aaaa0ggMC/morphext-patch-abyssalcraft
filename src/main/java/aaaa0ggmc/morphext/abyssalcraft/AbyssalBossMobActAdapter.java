package aaaa0ggmc.morphext.abyssalcraft;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
import aaaa0ggmc.morphext.integration.metamorph.SafeMobActAction;
import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 深渊国度全部 Boss 与特殊生物的原生 Mob Act 适配器 (Abyssal Boss Native Mob Act Adapter):
 * 当玩家变身为扎哈尔、萨克索斯、查伽洛斯等深渊 Boss 时，按键触发通用 mob_act 将自动适配触发该生物的原生标志性攻击：
 * 1. 扎哈尔 (J'zahar)：驱动原生三头凋灵齐射 (中头概率蓝头)，挥动浮空法杖；
 * 2. 萨克索斯 (Sacthoth)：驱动原生口部暗影烈焰喷涌；
 * 3. 查伽洛斯 (Cha'garoth)：三头发射高速恐惧蛞蝓；
 * 4. 阿萨托斯之龙 (DragonBoss)：发射恐惧龙息爆裂弹；
 * 5. 修格斯 (Shoggoth)：喷吐强酸粘液弹；
 * 6. 恐惧生物 (Dread)：发射恐惧充能弹；
 * 7. 珊瑚生物 (Coralium)：喷吐剧毒珊瑚墨汁；
 * 8. 反物质生物 (Antimatter)：发射反物质脉冲。
 */
public class AbyssalBossMobActAdapter implements SafeMobActAction.IMobActAdapter {

    @Override
    public boolean handleMobAct(EntityLivingBase player, AbstractMorph morph, EntityLiving dummy, @Nullable EntityLivingBase targetEnemy, Vec3d lookTarget) {
        if (morph == null || morph.name == null) return false;
        String name = morph.name.toLowerCase();
        World world = player.world;

        Vec3d aimCoord = targetEnemy != null ?
                new Vec3d(targetEnemy.posX, targetEnemy.posY + targetEnemy.getEyeHeight() * 0.5D, targetEnemy.posZ) : lookTarget;

        // 1. 扎哈尔 (J'zahar) 原生三头凋灵首齐射
        if (name.contains("jzahar") || BossNativeHelper.isJzahar(dummy)) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            boolean blueSkull = world.rand.nextFloat() < 0.25F;
            BossNativeHelper.launchJzaharSkull(dummy, 0, aimCoord.x, aimCoord.y, aimCoord.z, blueSkull);
            BossNativeHelper.launchJzaharSkull(dummy, 1, aimCoord.x, aimCoord.y, aimCoord.z, false);
            BossNativeHelper.launchJzaharSkull(dummy, 2, aimCoord.x, aimCoord.y, aimCoord.z, false);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5F, 0.7F);
            return true;
        }

        // 2. 萨克索斯 (Sacthoth) 原生口部暗影烈焰
        if (name.contains("sacthoth") || BossNativeHelper.isSacthoth(dummy)) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction flame = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_sacthoth_shadow_flame");
            if (flame != null) {
                flame.execute(player, morph);
                return true;
            }
        }

        // 3. 查伽洛斯 (Cha'garoth) 原生恐惧弹幕
        if (name.contains("chagaroth")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction salvo = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_chagaroth_dread_salvo");
            if (salvo != null) {
                salvo.execute(player, morph);
                return true;
            }
        }

        // 4. 阿萨托斯之龙 (DragonBoss) 原生恐惧龙息
        if (name.contains("dragonboss")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction breath = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_dragon_boss_breath");
            if (breath != null) {
                breath.execute(player, morph);
                return true;
            }
        }

        // 5. 修格斯系列 (Shoggoth) 原生强酸喷吐
        if (name.contains("shoggoth")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction spit = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_acid_spit");
            if (spit != null) {
                spit.execute(player, morph);
                return true;
            }
        }

        // 6. 恐惧生物 (Dread) 原生恐惧充能弹
        if (name.contains("dread")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction charge = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_dread_charge");
            if (charge != null) {
                charge.execute(player, morph);
                return true;
            }
        }

        // 7. 珊瑚生物 (Coralium) 原生珊瑚墨汁
        if (name.contains("coralium")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction squirt = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_coralium_squirt");
            if (squirt != null) {
                squirt.execute(player, morph);
                return true;
            }
        }

        // 8. 反物质生物 (Antimatter) 原生反物质脉冲
        if (name.contains("antimatter") || name.contains("anti")) {
            player.swingArm(EnumHand.MAIN_HAND);
            dummy.swingArm(EnumHand.MAIN_HAND);
            mchorse.metamorph.api.abilities.IAction pulse = mchorse.metamorph.api.MorphManager.INSTANCE.actions.get("ac_anti_matter_blast");
            if (pulse != null) {
                pulse.execute(player, morph);
                return true;
            }
        }

        return false;
    }
}
