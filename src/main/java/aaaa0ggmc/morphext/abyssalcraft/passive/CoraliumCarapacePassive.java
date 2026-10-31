package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.api.AbyssalCraftAPI;
import com.shinoow.abyssalcraft.api.entity.EntityUtil;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * 深渊僵尸专属被动：珊瑚结晶角质硬化 (Coralium Carapace)
 * 1. 结晶角质 (Carapace Armor & Toughness)：全身覆有坚硬珊瑚角质，常驻提供 +4 点护甲与 +4 点韧性护甲；
 * 2. 珊瑚刺反弹 (Coralium Thorns)：受到物理打击时有 25% 几率反弹珊瑚刺刺伤攻击者，附加珊瑚瘟疫与流血；
 * 3. 珊瑚体质免疫 (Coralium Immunity)：免疫珊瑚瘟疫药水负面。
 */
public class CoraliumCarapacePassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_coralium_carapace";

    private static final UUID CARAPACE_ARMOR_UUID = UUID.fromString("6a17b120-f584-48e2-9d33-4318c5e62ab1");
    private static final UUID CARAPACE_TOUGHNESS_UUID = UUID.fromString("7b28c231-0695-49f3-ae44-5429d6f73bc2");

    private static final AttributeModifier ARMOR_MODIFIER =
            new AttributeModifier(CARAPACE_ARMOR_UUID, "Coralium Carapace Armor", 4.0D, 0).setSaved(false);
    private static final AttributeModifier TOUGHNESS_MODIFIER =
            new AttributeModifier(CARAPACE_TOUGHNESS_UUID, "Coralium Carapace Toughness", 4.0D, 0).setSaved(false);

    public CoraliumCarapacePassive() {
        super(ID, "morphext.ability.ac_coralium_carapace.name", "morphext.ability.ac_coralium_carapace.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onApplied(EntityPlayer player, MorphContext context) {
        applyModifiers(player);
    }

    @Override
    public void onRemoved(EntityPlayer player, MorphContext context) {
        removeModifiers(player);
    }

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        if (player != null && !player.world.isRemote) {
            applyModifiers(player);
            // 确保自身不被珊瑚瘟疫侵害
            if (AbyssalCraftAPI.coralium_plague != null && player.isPotionActive(AbyssalCraftAPI.coralium_plague)) {
                player.removePotionEffect(AbyssalCraftAPI.coralium_plague);
            }
        }
    }

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        handleCarapaceThorns(player, source, amount);
        return false; // 不直接取消伤害，通过常驻 +4 护甲与 +4 韧性实现防御减免
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        if (AbyssalCraftAPI.coralium_plague != null && effect.getPotion() == AbyssalCraftAPI.coralium_plague) {
            return true; // 免疫珊瑚瘟疫
        }
        return false;
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            applyModifiers((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            applyModifiers((EntityPlayer) entity);
        }
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            removeModifiers((EntityPlayer) entity);
        }
    }

    // ==========================================
    //               属性修饰符管理
    // ==========================================

    public static void applyModifiers(EntityPlayer player) {
        if (player == null) return;
        applyModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_MODIFIER);
        applyModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER);
    }

    public static void removeModifiers(EntityPlayer player) {
        if (player == null) return;
        removeModifier(player, SharedMonsterAttributes.ARMOR, ARMOR_MODIFIER);
        removeModifier(player, SharedMonsterAttributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER);
    }

    private static void applyModifier(EntityLivingBase entity, IAttribute attribute, AttributeModifier modifier) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        if (instance != null && !instance.hasModifier(modifier)) {
            instance.applyModifier(modifier);
        }
    }

    private static void removeModifier(EntityLivingBase entity, IAttribute attribute, AttributeModifier modifier) {
        IAttributeInstance instance = entity.getEntityAttribute(attribute);
        if (instance != null && instance.hasModifier(modifier)) {
            instance.removeModifier(modifier);
        }
    }

    // ==========================================
    //               珊瑚刺反弹逻辑
    // ==========================================

    public static void handleCarapaceThorns(EntityPlayer player, DamageSource source, float amount) {
        if (player == null || player.world.isRemote || source == null) return;

        // 仅在受到物理伤害时有 25% 几率反弹
        if (source.isMagicDamage() || source.isFireDamage() || source.isExplosion()) return;

        World world = player.world;
        if (world.rand.nextFloat() >= 0.25F) return;

        Entity attackerEntity = source.getTrueSource();
        if (attackerEntity instanceof EntityLivingBase && attackerEntity != player && !attackerEntity.isOnSameTeam(player)) {
            EntityLivingBase attacker = (EntityLivingBase) attackerEntity;

            // 反弹伤害：基于承受伤害 50% 或至少 4 点珊瑚伤害
            float thornDamage = Math.max(4.0F, amount * 0.5F);
            DamageSource thornSrc = AbyssalCraftAPI.coralium != null ?
                    AbyssalCraftAPI.coralium :
                    DamageSource.causeThornsDamage(player);

            attacker.attackEntityFrom(thornSrc, thornDamage);

            // 施加珊瑚瘟疫
            if (AbyssalCraftAPI.coralium_plague != null && !EntityUtil.isEntityCoralium(attacker)) {
                attacker.addPotionEffect(new PotionEffect(AbyssalCraftAPI.coralium_plague, 100, 0));
            }

            // 音效反馈：水晶碰撞音效与荆棘音效
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 1.4F);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENCHANT_THORNS_HIT, SoundCategory.PLAYERS, 1.0F, 1.0F);

            // 粒子反馈：青色珊瑚结晶粒子在攻击者身上爆发
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        attacker.posX, attacker.posY + attacker.height * 0.5D, attacker.posZ,
                        15, 0.35D, 0.35D, 0.35D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                        player.posX, player.posY + player.height * 0.5D, player.posZ,
                        10, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
    }
}
