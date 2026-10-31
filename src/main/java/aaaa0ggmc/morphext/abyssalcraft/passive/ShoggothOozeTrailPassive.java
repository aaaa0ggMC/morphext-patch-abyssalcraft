package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 修格斯家族专属被动：软泥拖曳 (Shoggoth Ooze Trail)
 * 1. 软泥拖曳：移动时周身不断渗出腐蚀性软泥微粒，2.5 格内贴身敌人被软泥缠滞（缓慢 I 2s，每秒刷新）；
 * 2. 软泥接触（死分支，见方法内注 A7）：与实体碰撞时同样缠滞对方——核心暂无 onCollide 分发点，
 *    实际由上方 2.5 格光环覆盖贴身缠滞；
 * 3. 表现还原 EntityShoggothBase.spawnOoze:206-230（修格斯脚下持续生成 shoggoth_ooze）的生态特性。
 *
 * 兼备 Metamorph 原生 IAbility 与 MorphExtension IPassiveAbility 双重接口。
 */
public class ShoggothOozeTrailPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_shoggoth_ooze_trail";

    public ShoggothOozeTrailPassive() {
        super(ID, "morphext.ability.ac_shoggoth_ooze_trail.name", "morphext.ability.ac_shoggoth_ooze_trail.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handleOozeTick(player);
    }

    @Override
    public void onCollide(EntityPlayer player, EntityLivingBase other, MorphContext context) {
        // 注（A7）：核心目前没有任何 IPassiveAbility.onCollide 分发点，此分支是死钩子；
        // "缠滞"实际生效路径是 onUpdate 的 2.5 格贴身光环。保留实现等待核心补分发，勿删。
        if (player == null || other == null || player.world.isRemote || other.isOnSameTeam(player)) return;
        other.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 0));
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handleOozeTick((EntityPlayer) entity);
        }
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

    public static void handleOozeTick(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        World world = player.world;
        if (player.ticksExisted % 20 != 0) return;

        // 软泥微粒（spawnOoze 的视觉还原）
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                    player.posX, player.posY + 0.15D, player.posZ,
                    4, 0.45D, 0.15D, 0.45D, 0.0D);
        }

        // 贴身缠滞
        for (EntityLivingBase other : world.getEntitiesWithinAABB(EntityLivingBase.class,
                player.getEntityBoundingBox().grow(2.5D, 1.0D, 2.5D),
                e -> e != player && e.isEntityAlive() && !e.isOnSameTeam(player))) {
            other.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 0));
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME,
                        other.posX, other.posY + 0.1D, other.posZ,
                        3, 0.3D, 0.1D, 0.3D, 0.0D);
            }
        }
    }
}
