package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.common.items.armor.ItemEthaxiumArmor;
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
 * 修格斯专属被动：高腐蚀强酸体质与远古原生免疫 (Shoggoth Caustic Physiology & Eldritch Immunity)
 * 1. 腐蚀性酸液与环境免疫：完全免疫修格斯酸液 (DamageSource acid)、仙人掌 (cactus)、窒息 (inWall)、溺水 (drown)；
 * 2. 弹射物凝胶跳弹抗性 (Projectile Bounce Immunity)：免疫一切弹射物伤害，遭受箭矢或飞弹打击时软体反弹并播放史莱姆弹跳音效；
 * 3. 远古原生瘟疫免疫：免疫珊瑚瘟疫 (coralium_plague)、恐惧瘟疫 (dread_plague)、反物质与中毒；
 * 4. 水下两栖机能：完全水下呼吸且在水中获得额外滑行游动推进力；
 * 5. 近战腐蚀酸触：近战命中敌人造成严重粘滞迟缓 III、虚弱，并直接腐蚀损耗目标防具耐久。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension 现代 IPassiveAbility 双重接口。
 */
public class ShoggothAcidPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_shoggoth_acid";

    public ShoggothAcidPassive() {
        super(ID, "morphext.ability.ac_shoggoth_acid.name", "morphext.ability.ac_shoggoth_acid.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleShoggothTick(player);
    }

    @Override
    public void onAttack(EntityPlayer player, EntityLivingBase target, DamageSource source, float amount, MorphContext context) {
        handleAttackProc(player, target);
    }

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        // 1. 弹射物软体跳弹免伤 (100% 还原 EntityShoggothBase.attackEntityFrom 机制)
        if (source.isProjectile()) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_SLIME_JUMP, SoundCategory.PLAYERS, 1.0F,
                    ((player.world.rand.nextFloat() - player.world.rand.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(EnumParticleTypes.SLIME,
                        player.posX, player.posY + player.height * 0.5D, player.posZ,
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
            }
            return true;
        }

        // 2. 强酸、窒息、仙人掌、溺水免疫
        if (source == AbyssalCraftAPI.acid || "acid".equalsIgnoreCase(source.getDamageType())
                || "shoggoth_acid".equalsIgnoreCase(source.getDamageType())
                || source == DamageSource.CACTUS || source == DamageSource.IN_WALL || source == DamageSource.DROWN) {
            return true;
        }

        return false;
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        // 3. 远古原生古神免疫：珊瑚瘟疫、恐惧瘟疫、反物质药水、普通中毒
        if (effect.getPotion() == MobEffects.POISON
                || (AbyssalCraftAPI.coralium_plague != null && effect.getPotion() == AbyssalCraftAPI.coralium_plague)
                || (AbyssalCraftAPI.dread_plague != null && effect.getPotion() == AbyssalCraftAPI.dread_plague)
                || (AbyssalCraftAPI.antimatter_potion != null && effect.getPotion() == AbyssalCraftAPI.antimatter_potion)) {
            return true;
        }
        return false;
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleShoggothTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            EntityPlayer player = (EntityPlayer) entity;
            player.setAir(300);
            if (AbyssalCraftAPI.coralium_plague != null) player.removePotionEffect(AbyssalCraftAPI.coralium_plague);
            if (AbyssalCraftAPI.dread_plague != null) player.removePotionEffect(AbyssalCraftAPI.dread_plague);
            if (AbyssalCraftAPI.antimatter_potion != null) player.removePotionEffect(AbyssalCraftAPI.antimatter_potion);
            player.removePotionEffect(MobEffects.POISON);
        }
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        // 解除变身清除残留
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    public static void handleShoggothTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        // 水中环境：防止窒息溺水，并提供原生滑行游动推力
        if (player.isInWater()) {
            player.setAir(300);
            player.motionX += player.getLookVec().x * 0.012D;
            player.motionY += player.getLookVec().y * 0.012D;
            player.motionZ += player.getLookVec().z * 0.012D;
        }

        // 周身微量滴落粘液粒子
        if (player.ticksExisted % 30 == 0 && player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(EnumParticleTypes.SLIME,
                    player.posX, player.posY + 0.2D, player.posZ,
                    3, 0.35D, 0.1D, 0.35D, 0.01D);
        }
    }

    public static void handleAttackProc(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null || target.world.isRemote || target.isOnSameTeam(player)) return;

        // 强酸粘液减速 III (4秒) 与虚弱 I (4秒)
        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2));
        target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0));

        // 腐蚀目标护甲耐久 3 点（伊萨希姆 Ethaxium 护甲免疫）
        for (ItemStack armor : target.getArmorInventoryList()) {
            if (!armor.isEmpty() && !(armor.getItem() instanceof ItemEthaxiumArmor)) {
                armor.damageItem(3, target);
            }
        }

        World world = target.world;
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SLIME,
                    target.posX, target.posY + target.height * 0.5D, target.posZ,
                    15, 0.3D, 0.3D, 0.3D, 0.05D);
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    target.posX, target.posY + target.height * 0.5D, target.posZ,
                    8, 0.2D, 0.2D, 0.2D, 0.0D);
        }

        world.playSound(null, target.posX, target.posY, target.posZ,
                ACSounds.shoggoth_consume != null ? ACSounds.shoggoth_consume : SoundEvents.ENTITY_SLIME_ATTACK,
                SoundCategory.PLAYERS, 1.2F, 0.9F);
    }
}
