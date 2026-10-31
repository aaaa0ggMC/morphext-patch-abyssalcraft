package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.EntityUtil;
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
 * 反物质生物专属被动：反物质猎杀本能 (Antimatter Predator Instinct)
 * 1. 猎杀本能：对一切非反物质生物的近战命中追加 2 点反物质直伤与短暂迟缓
 *    （还原 EntityAntiPlayer 的仇恨一切非 anti 生物目标 Task 与各 anti 实体的猎杀目标表）；
 * 2. 反物质体质：免疫反物质药水（IAntiEntity 语义）。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension IPassiveAbility 双重接口。
 */
public class AntiPredatorPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_anti_predator";

    public AntiPredatorPassive() {
        super(ID, "morphext.ability.ac_anti_predator.name", "morphext.ability.ac_anti_predator.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onAttack(EntityPlayer player, EntityLivingBase target, DamageSource source, float amount, MorphContext context) {
        if (player == null || target == null || player.world.isRemote || target.isOnSameTeam(player)) return;
        if (EntityUtil.isEntityAnti(target)) return; // 反物质同族互不猎杀

        if (AbyssalCraftAPI.antimatter != null) {
            target.attackEntityFrom(AbyssalCraftAPI.antimatter, 2.0F);
        }
        if (AbyssalCraftAPI.antimatter_potion != null) {
            target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 40, 0));
        } else {
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20, 0));
        }

        World world = player.world;
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                    target.posX, target.posY + target.height * 0.5D, target.posZ,
                    8, 0.3D, 0.3D, 0.3D, 0.1D);
        }
        world.playSound(null, target.posX, target.posY, target.posZ,
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.5F, 1.8F);
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        // IAntiEntity → 免反物质药水
        return AbyssalCraftAPI.antimatter_potion != null && effect.getPotion() == AbyssalCraftAPI.antimatter_potion;
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
}
