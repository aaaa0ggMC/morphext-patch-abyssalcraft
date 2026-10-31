package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.lib.ACSounds;
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
 * 暗影生物专属被动：暗影相位 (Shadow Phase)
 * 1. 相位闪烁：受到近战攻击时 20% 概率化影闪避（完全免伤），暗影微粒四散并获得短暂加速；
 * 2. 表现还原 EntitySacthoth 的受伤规避与 IN_WALL 传送脱困（:209-231、:247-253）
 *    以及暗影生物的 ParticleUtil.spawnShadowParticles 粒子消散特性（趣味演绎为相位闪避）。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension IPassiveAbility 双重接口。
 */
public class ShadowPhasePassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_shadow_phase";

    public ShadowPhasePassive() {
        super(ID, "morphext.ability.ac_shadow_phase.name", "morphext.ability.ac_shadow_phase.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        return tryPhase(player, source);
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    private static boolean tryPhase(EntityPlayer player, DamageSource source) {
        if (player == null || player.world == null || player.world.isRemote || source == null) return false;

        // 仅近战接触可相位闪避（弹射物/环境/魔法伤害不触发）
        if (!(source.getImmediateSource() instanceof EntityLivingBase)
                || source.isProjectile() || source.isMagicDamage()) {
            return false;
        }
        if (player.world.rand.nextInt(5) != 0) {
            return false;
        }

        World world = player.world;
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + player.height * 0.5D, player.posZ,
                    25, 0.5D, 0.7D, 0.5D, 0.02D);
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX, player.posY + player.height * 0.5D, player.posZ,
                    15, 0.4D, 0.6D, 0.4D, 0.1D);
        }
        world.playSound(null, player.posX, player.posY, player.posZ,
                ACSounds.shadow_hurt != null ? ACSounds.shadow_hurt : SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 0.9F, 1.3F);

        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 40, 0, false, false));
        return true; // 相位闪避，免疫本次伤害
    }
}
