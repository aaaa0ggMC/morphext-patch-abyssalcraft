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
 * 反物质生物专属被动：反物质湮灭反应与虚空免疫 (Antimatter Annihilation & Void Immunity)
 * 1. 物理接触湮灭反应 (Contact Annihilation)：反物质生物接触正物质实体时发生微型物质湮灭反应，造成真实反物质直伤与击退；
 * 2. 命中湮灭充能 (Hit Annihilation Proc)：攻击命中（含弹射物，trueSource 为玩家）附带额外反物质直伤并施加反物质药水侵蚀；
 *    （原"近战限定"注释失真：被动分发按 trueSource 触发，弹射物命中同样生效；S2 后只走被动单通道，数值为单倍）
 * 3. 反物质绝对免疫 (Antimatter Immunity)：完全免疫反物质伤害 (DamageSource antimatter) 与反物质药水负面。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension 现代 IPassiveAbility 双重接口。
 */
public class AntimatterAnnihilationPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_antimatter_annihilation";

    public AntimatterAnnihilationPassive() {
        super(ID, "morphext.ability.ac_antimatter_annihilation.name", "morphext.ability.ac_antimatter_annihilation.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleAntimatterTick(player);
    }

    @Override
    public void onCollide(EntityPlayer player, EntityLivingBase target, MorphContext context) {
        // 注（A7）：核心目前没有任何 IPassiveAbility.onCollide 分发点，此分支是死钩子；
        // "接触湮灭"实际生效路径是 MorphBossAdaptHandler.onLivingUpdate 的名字匹配碰撞补偿。
        // 保留实现等待核心补分发（补上后自动生效），勿删。
        handleCollisionAnnihilation(player, target);
    }

    @Override
    public void onAttack(EntityPlayer player, EntityLivingBase target, DamageSource source, float amount, MorphContext context) {
        handleAttackProc(player, target);
    }

    @Override
    public boolean onAttacked(EntityPlayer player, DamageSource source, float amount, MorphContext context) {
        if (source == AbyssalCraftAPI.antimatter || "antimatter".equalsIgnoreCase(source.getDamageType())) {
            return true; // 绝对免疫反物质伤害
        }
        return false;
    }

    @Override
    public boolean onPotionApplicable(EntityPlayer player, PotionEffect effect, MorphContext context) {
        if (AbyssalCraftAPI.antimatter_potion != null && effect.getPotion() == AbyssalCraftAPI.antimatter_potion) {
            return true; // 绝对免疫反物质药水效果
        }
        return false;
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleAntimatterTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            if (AbyssalCraftAPI.antimatter_potion != null) {
                ((EntityPlayer) entity).removePotionEffect(AbyssalCraftAPI.antimatter_potion);
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

    public static void handleAntimatterTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        World world = player.world;
        if (player.ticksExisted % 30 == 0 && world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX, player.posY + player.height * 0.5D, player.posZ,
                    3, 0.25D, 0.35D, 0.25D, 0.05D);
        }

        // 持续确保反物质药水不作用于自身
        if (AbyssalCraftAPI.antimatter_potion != null && player.isPotionActive(AbyssalCraftAPI.antimatter_potion)) {
            player.removePotionEffect(AbyssalCraftAPI.antimatter_potion);
        }
    }

    /**
     * 实体碰撞时触发的微型反物质湮灭反应
     */
    public static void handleCollisionAnnihilation(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null || target.world.isRemote || target == player || target.isOnSameTeam(player)) return;

        // 若目标非反物质生物，接触瞬间触发微型物质湮灭反应！
        if (!EntityUtil.isEntityAnti(target) && target.hurtResistantTime <= 5) {
            DamageSource antiSrc = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : DamageSource.causePlayerDamage(player);
            target.attackEntityFrom(antiSrc, 6.0F);

            if (AbyssalCraftAPI.antimatter_potion != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 80, 0));
            }

            // 湮灭冲击波反冲击退
            double dx = target.posX - player.posX;
            double dz = target.posZ - player.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001D) {
                dx = 1.0D;
                dist = 1.0D;
            }
            target.knockBack(player, 0.8F, -dx / dist, -dz / dist);
            target.velocityChanged = true;

            World world = target.world;
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        25, 0.3D, 0.3D, 0.3D, 0.1D);
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        3, 0.2D, 0.2D, 0.2D, 0.05D);
            }

            world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 2.0F);
            world.playSound(null, target.posX, target.posY, target.posZ,
                    ACSounds.antiplayer_hurt != null ? ACSounds.antiplayer_hurt : SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.PLAYERS, 1.0F, 1.4F);
        }
    }

    /**
     * 近战攻击触发的额外反物质湮灭直伤
     */
    public static void handleAttackProc(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null || target.world.isRemote || target.isOnSameTeam(player)) return;

        if (!EntityUtil.isEntityAnti(target)) {
            DamageSource antiSrc = AbyssalCraftAPI.antimatter != null ? AbyssalCraftAPI.antimatter : DamageSource.causePlayerDamage(player);
            target.attackEntityFrom(antiSrc, 4.0F);

            if (AbyssalCraftAPI.antimatter_potion != null) {
                target.addPotionEffect(new PotionEffect(AbyssalCraftAPI.antimatter_potion, 100, 0));
            }

            World world = target.world;
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        15, 0.25D, 0.25D, 0.25D, 0.05D);
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        target.posX, target.posY + target.height * 0.5D, target.posZ,
                        8, 0.2D, 0.2D, 0.2D, 0.02D);
            }

            world.playSound(null, target.posX, target.posY, target.posZ,
                    ACSounds.antiplayer_hurt != null ? ACSounds.antiplayer_hurt : SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.PLAYERS, 1.2F, 1.1F);
        }
    }
}
