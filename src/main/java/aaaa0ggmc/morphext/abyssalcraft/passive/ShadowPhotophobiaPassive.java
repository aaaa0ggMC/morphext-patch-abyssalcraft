package aaaa0ggmc.morphext.abyssalcraft.passive;

import aaaa0ggmc.morphext.ability.AbstractPassiveAbility;
import aaaa0ggmc.morphext.ability.MorphContext;
import com.shinoow.abyssalcraft.lib.ACSounds;
import mchorse.metamorph.api.abilities.IAbility;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 暗影生物专属被动：暗影体质、畏光与失明附魔 (Shadow Photophobia & Shadow Affliction)
 * 1. 畏光 (Photophobia)：在强光/直射日光下 (光照 >= 12 且见光)，遭受畏光虚弱与迟缓，身体散发暗影轻烟；
 * 2. 暗影亲和 (Shadow Affinity)：身处黑暗与幽闭环境中 (光照 <= 7)，获得持续夜视与抗性提升；
 * 3. 失明附魔 (Blindness Infliction)：近战攻击目标必然施加致盲 (4秒) 与暗影凋零 (4秒)，并伴随原生暗影音效。
 *
 * 同时兼备 Metamorph 原生 IAbility 与 MorphExtension 现代 IPassiveAbility 双重接口，确保跨架构完美兼容。
 */
public class ShadowPhotophobiaPassive extends AbstractPassiveAbility implements IAbility {

    public static final String ID = "ac_photophobia";

    public ShadowPhotophobiaPassive() {
        super(ID, "morphext.ability.ac_photophobia.name", "morphext.ability.ac_photophobia.desc", ItemStack.EMPTY);
    }

    // ==========================================
    //       MorphExtension IPassiveAbility 实现
    // ==========================================

    @Override
    public void onUpdate(EntityPlayer player, MorphContext context) {
        handlePhotophobiaTick(player);
    }

    @Override
    public void onAttack(EntityPlayer player, EntityLivingBase target, DamageSource source, float amount, MorphContext context) {
        handleAttackProc(player, target);
    }

    // ==========================================
    //       Metamorph 原生 IAbility 桥接实现
    // ==========================================

    @Override
    public void update(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            handlePhotophobiaTick((EntityPlayer) entity);
        }
    }

    @Override
    public void onMorph(EntityLivingBase entity) {
        // 变身瞬间赋予初始暗影夜视
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            ((EntityPlayer) entity).addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 200, 0, false, false));
        }
    }

    @Override
    public void onDemorph(EntityLivingBase entity) {
        // 解除变身清除畏光产生的负面状态
        if (entity instanceof EntityPlayer && !entity.world.isRemote) {
            ((EntityPlayer) entity).removePotionEffect(MobEffects.WEAKNESS);
            ((EntityPlayer) entity).removePotionEffect(MobEffects.SLOWNESS);
        }
    }

    // ==========================================
    //               核心逻辑封装
    // ==========================================

    public static void handlePhotophobiaTick(EntityPlayer player) {
        if (player == null || player.world.isRemote || player.isCreative() || player.isSpectator()) return;

        World world = player.world;
        if (player.ticksExisted % 30 == 0) {
            BlockPos pos = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            int light = world.getLight(pos);

            // 1. 畏光判定：直接暴露在白天的阳光下或环境亮度极高 (>= 12)
            boolean inSunlight = world.isDaytime() && world.canSeeSky(pos) && world.getLightFor(EnumSkyBlock.SKY, pos) > 10;
            if (inSunlight || light >= 12) {
                player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, false));
                player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, false));

                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                            player.posX, player.posY + player.height * 0.5D, player.posZ,
                            3, 0.25D, 0.35D, 0.25D, 0.02D);
                }
            } else if (light <= 7) {
                // 2. 暗影庇护判定：昏暗无光环境获得夜视与抗性
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 260, 0, false, false));
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 60, 0, false, false));
            }
        }
    }

    public static void handleAttackProc(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null || target.world.isRemote) return;

        // 3. 近战失明附魔与暗影侵蚀：命中施加致盲 4 秒与暗影凋零 4 秒
        target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 80, 0));
        target.addPotionEffect(new PotionEffect(MobEffects.WITHER, 80, 1));

        World world = target.world;
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    target.posX, target.posY + target.height * 0.5D, target.posZ,
                    10, 0.35D, 0.35D, 0.35D, 0.02D);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.PORTAL,
                    target.posX, target.posY + target.height * 0.5D, target.posZ,
                    8, 0.25D, 0.25D, 0.25D, 0.05D);
        }

        world.playSound(null, target.posX, target.posY, target.posZ,
                ACSounds.shadow_hurt, SoundCategory.PLAYERS, 1.2F, 0.9F);
    }
}
