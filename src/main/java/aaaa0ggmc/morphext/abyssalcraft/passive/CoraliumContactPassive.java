package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
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
 * 珊瑚感染生物专属被动：珊瑚接触感染 (Coralium Contact Infection)
 * 1. 还击感染：被近战命中时，令攻击者感染珊瑚瘟疫（100t）并受 1.5 点珊瑚直伤；
 *    （"碰撞感染"分支因核心无 onCollide 分发点暂为死钩子，见方法内注 A7；文案已同步为还击感染）
 * 2. 珊瑚体质：完全免疫珊瑚瘟疫；
 * 3. 表现还原 EntityCoraliumSquid.collideWithEntity:42-47（碰撞即感染 cplague）
 *    与 EntityDragonMinion.attackEntitiesInList:475-486（接触使非珊瑚实体 cplague 200t）。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension IPassiveAbility 双重接口。
 */
public class CoraliumContactPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_coralium_contact";

    public CoraliumContactPassive() {
        super(ID, "morphext.ability.ac_coralium_contact.name", "morphext.ability.ac_coralium_contact.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        // 近战接触反感染（不免疫伤害，只做接触惩罚）
        if (source != null && source.getImmediateSource() instanceof EntityLivingBase
                && !source.isProjectile() && !source.isMagicDamage()) {
            EntityLivingBase attacker = (EntityLivingBase) source.getImmediateSource();
            if (attacker != player && !attacker.isOnSameTeam(player)) {
                infect(player, attacker);
            }
        }
        return false;
    }

    @Override
    public void onCollide(EntityPlayer player, EntityLivingBase other, MorphContext context) {
        // 注（A7）：核心目前没有任何 IPassiveAbility.onCollide 分发点，此分支是死钩子；
        // "感染"实际生效路径是上方 onAttacked 还击感染。保留实现等待核心补分发，勿删。
        if (player == null || other == null || player.world.isRemote || other.isOnSameTeam(player)) return;
        infect(player, other);
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        // ICoraliumEntity → 免珊瑚瘟疫
        return AbyssalCraftAPI.coralium_plague != null && effect.getPotion() == AbyssalCraftAPI.coralium_plague;
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

    private static void infect(EntityPlayer player, EntityLivingBase other) {
        if (player == null || other == null || player.world.isRemote) return;

        if (AbyssalCraftAPI.coralium_plague != null) {
            other.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 100, 0));
        }
        DamageSource coraliumSrc = AbyssalCraftAPI.coralium != null ? AbyssalCraftAPI.coralium :
                DamageSource.causePlayerDamage(player);
        other.attackEntityFrom(coraliumSrc, 1.5F);

        World world = player.world;
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    other.posX, other.posY + other.height * 0.5D, other.posZ,
                    8, 0.3D, 0.3D, 0.3D, 0.0D);
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    other.posX, other.posY + other.height * 0.3D, other.posZ,
                    4, 0.25D, 0.2D, 0.25D, 0.01D);
        }
        world.playSound(null, other.posX, other.posY, other.posZ,
                ACSounds.ghoul_hurt != null ? ACSounds.ghoul_hurt : SoundEvents.ENTITY_ZOMBIE_INFECT,
                SoundCategory.PLAYERS, 0.9F, 1.3F);
    }
}
