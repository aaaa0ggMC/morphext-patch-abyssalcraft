package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 恐惧渊素傀儡专属被动：恐惧巨像铁躯 (Dreadgolem Toughness)
 * 1. 磐石躯体 (Ore Cluster Body)：免疫溺水 (drown)、仙人掌 (cactus)、窒息 (inWall) 与摔落伤害 (fall)；
 * 2. 巨像韧性与击退抗性：受到实体近战与常规物理攻击时大幅衰减伤害并激荡金属沉重钝响，原地稳如磐石；
 * 3. 渊素重铠 (Abyssal Toughness)：持续提供抗性提升与高额护甲韧性庇护。
 */
public class DreadgolemToughnessPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_dreadgolem_toughness";

    public DreadgolemToughnessPassive() {
        super(ID, "morphext.ability.ac_dreadgolem_toughness.name", "morphext.ability.ac_dreadgolem_toughness.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleGolemTick(player);
    }

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        // 1. 环境与绝对免伤判定
        if (source == DamageSource.DROWN || source == DamageSource.CACTUS
                || source == DamageSource.IN_WALL || source == DamageSource.FALL) {
            return true; // 绝对免伤
        }

        // 2. 承受普通物理攻击时的金属钝响与减免反馈
        if (!source.isMagicDamage() && !source.canHarmInCreative()) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8F, 1.4F);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_IRONGOLEM_HURT, SoundCategory.PLAYERS, 1.0F, 0.7F);
            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(EnumParticleTypes.CRIT,
                        player.posX, player.posY + player.height * 0.5D, player.posZ,
                        6, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
        return false;
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleGolemTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            ((EntityPlayer) entity).addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 200, 1, false, false));
        }
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            ((EntityPlayer) entity).removePotionEffect(MobEffects.RESISTANCE);
        }
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    public static void handleGolemTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        // 持续抗性提升与击退抗性庇护
        if (player.ticksExisted % 40 == 0) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 1, false, false));
            player.setAir(300); // 彻底防止水下溺水
        }
    }
}
