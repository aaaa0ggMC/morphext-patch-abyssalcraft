package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.EntityUtil;
import com.shinoow.abyssalcraft.lib.ACSounds;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 恐惧生物专属被动：恐惧瘟疫传染链与恐惧炽焰抗性 (Dread Plague & Dread Ember Immunity)
 * 1. 恐惧瘟疫传染链 (Dread Plague Transmission)：攻击命中（含弹射物，trueSource 为玩家）必然施加恐惧瘟疫 (100 ticks / 5秒) 与真实恐惧直伤，非恐惧生物被攻击引发恐惧烈焰粒子；
 *    （原"近战限定"注释失真：被动分发按 trueSource 触发，弹射物命中同样生效；S2 后只走被动单通道，数值为单倍）
 * 2. 恐惧体质免疫 (Dread Immunity)：完全免疫恐惧伤害 (DamageSource dread)、火焰熔岩伤害与恐惧瘟疫负面状态；
 * 3. 恐惧微粒：周身间歇性散发恐惧熔火微粒与余烬。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension 现代 IPassiveAbility 双重接口。
 */
public class DreadPlaguePassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_dread_plague";

    public DreadPlaguePassive() {
        super(ID, "morphext.ability.ac_dread_plague.name", "morphext.ability.ac_dread_plague.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleDreadTick(player);
    }

    @Override
    public void onAttack(EntityPlayer player, EntityLivingBase target, DamageSource source, float amount, MorphContext context) {
        handleAttackProc(player, target);
    }

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        if (source == AbyssalCraftAPI.dread || "dread".equalsIgnoreCase(source.getDamageType()) || source.isFireDamage()) {
            return true; // 免疫恐惧伤害与火焰
        }
        return false;
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        if (AbyssalCraftAPI.dread_plague != null && effect.getPotion() == AbyssalCraftAPI.dread_plague) {
            return true; // 免疫恐惧瘟疫
        }
        return false;
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleDreadTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            // 变身瞬间驱散已有恐惧瘟疫
            if (AbyssalCraftAPI.dread_plague != null) {
                ((EntityPlayer) entity).removePotionEffect(AbyssalCraftAPI.dread_plague);
            }
        }
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        // 解除变身清除残留
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    public static void handleDreadTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        World world = player.world;
        if (player.ticksExisted % 40 == 0 && world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME,
                    player.posX, player.posY + player.height * 0.5D, player.posZ,
                    2, 0.25D, 0.35D, 0.25D, 0.02D);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA,
                    player.posX, player.posY + player.height * 0.3D, player.posZ,
                    1, 0.2D, 0.1D, 0.2D, 0.0D);
        }

        // 持续确保恐惧瘟疫不作用于自身
        if (AbyssalCraftAPI.dread_plague != null && player.isPotionActive(AbyssalCraftAPI.dread_plague)) {
            player.removePotionEffect(AbyssalCraftAPI.dread_plague);
        }
    }

    public static void handleAttackProc(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null || target.world.isRemote || target.isOnSameTeam(player)) return;

        // 若目标非恐惧生物，施加恐惧瘟疫与恐惧灼伤
        if (!EntityUtil.isEntityDread(target)) {
            if (AbyssalCraftAPI.dread_plague != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.dread_plague, 100, 0));
            }
            DamageSource dreadSrc = AbyssalCraftAPI.dread != null ? AbyssalCraftAPI.dread : DamageSource.causePlayerDamage(player);
            target.attackEntityFrom(dreadSrc, 2.5F);

            World world = target.world;
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.LAVA,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        3, 0.25D, 0.25D, 0.25D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        4, 0.2D, 0.2D, 0.2D, 0.02D);
            }

            world.playSound(null, target.posX, target.posY, target.posZ,
                    ACSounds.dreadguard_ambient != null ? ACSounds.dreadguard_ambient : SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                    SoundCategory.PLAYERS, 1.2F, 0.8F);
        }
    }
}
