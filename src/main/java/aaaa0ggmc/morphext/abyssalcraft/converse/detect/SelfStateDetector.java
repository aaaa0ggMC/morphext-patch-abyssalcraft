package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetector;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/** 宿主自身状态。夜视是扎哈尔常态，不单独当成「身上有药水」。 */
public final class SelfStateDetector implements ConverseDetector {
    @Override
    public String id() {
        return "self";
    }

    @Override
    public void detect(EntityPlayer player, ConverseScan scan) {
        if (player == null || scan == null) {
            return;
        }
        if (player.isSneaking()) {
            scan.add(ConverseAspects.SELF_SNEAKING);
        }
        if (player.isSprinting()) {
            scan.add(ConverseAspects.SELF_SPRINTING);
        }
        if (player.capabilities != null && player.capabilities.isFlying) {
            scan.add(ConverseAspects.SELF_FLYING);
        } else if (player.isElytraFlying()) {
            scan.add(ConverseAspects.SELF_FLYING);
        }
        float max = player.getMaxHealth();
        float ratio = max <= 0.0F ? 1.0F : player.getHealth() / max;
        if (ratio <= 0.20F) {
            scan.add(ConverseAspects.SELF_CRITICAL);
            scan.add(ConverseAspects.SELF_LOW_HEALTH);
        } else if (ratio <= 0.40F) {
            scan.add(ConverseAspects.SELF_LOW_HEALTH);
        } else if (ratio >= 0.85F) {
            scan.add(ConverseAspects.SELF_HEALTHY);
        }
        if (player.isBurning()) {
            scan.add(ConverseAspects.SELF_ON_FIRE);
        }
        if (player.isInWater()) {
            scan.add(ConverseAspects.SELF_IN_WATER);
        }
        if (player.isInLava()) {
            scan.add(ConverseAspects.SELF_IN_LAVA);
        }
        if (player.hurtTime > 0) {
            scan.add(ConverseAspects.SELF_HURT);
        }
        if (player.getFoodStats() != null && player.getFoodStats().getFoodLevel() <= 6) {
            scan.add(ConverseAspects.SELF_HUNGRY);
        }
        if (hasUnusualPotion(player)) {
            scan.add(ConverseAspects.SELF_POTION);
        }
    }

    private static boolean hasUnusualPotion(EntityPlayer player) {
        if (player.getActivePotionEffects() == null) {
            return false;
        }
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect == null || effect.getPotion() == null) {
                continue;
            }
            ResourceLocation id = effect.getPotion().getRegistryName();
            if (id != null && "night_vision".equals(id.getPath())) {
                continue;
            }
            return true;
        }
        return false;
    }
}
