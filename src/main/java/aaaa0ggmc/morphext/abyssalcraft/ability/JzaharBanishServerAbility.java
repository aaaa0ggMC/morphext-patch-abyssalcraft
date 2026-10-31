package aaaa0ggmc.morphext.abyssalcraft.ability;

import aaaa0ggmc.morphext.ability.ActivationResult;
import aaaa0ggmc.morphext.ability.PlayerAbilityStore;
import aaaa0ggmc.morphext.ability.ServerAbilityService;
import aaaa0ggmc.morphext.integration.metamorph.MorphAbilityDefinition;
import aaaa0ggmc.morphext.integration.metamorph.MorphJsonConfig;
import com.google.gson.JsonObject;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 扎哈尔古神遣返法则的服务端执行器（AbyssalCraft patch 专属）。
 *
 * <p>核心 morphext 只提供通用的 {@link ServerAbilityService} 与
 * {@link PlayerAbilityStore}；本类通过更具体的前缀 {@code morphext:jzahar} 注册，
 * 不会污染核心的通用命名空间。</p>
 */
public final class JzaharBanishServerAbility implements ServerAbilityService.Handler {
    public static final String ID = "morphext:jzahar_banish";
    public static final String TAG = "morphext_jzahar_banish";

    @Override
    public boolean isAvailable(EntityPlayerMP player, String id) {
        if (!ID.equals(id)) {
            return false;
        }
        IMorphing morphing = Morphing.get(player);
        if (morphing == null || !morphing.isMorphed()) {
            return false;
        }
        AbstractMorph current = morphing.getCurrentMorph();
        if (current == null || current.name == null) {
            return false;
        }
        // 数据驱动：当前形态的 morph_abilities.json 必须显式声明该能力。
        MorphAbilityDefinition def = MorphJsonConfig.get(current.name);
        return def != null && def.abilities != null && def.abilities.contains(ID);
    }

    @Override
    public ActivationResult activate(EntityPlayerMP player, String id) {
        if (!ID.equals(id) || !isAvailable(player, id)) {
            return ActivationResult.unavailable();
        }
        JsonObject state = PlayerAbilityStore.get(player).getState(ID);
        boolean current = !state.has("enabled") || !state.get("enabled").isJsonPrimitive()
                || state.get("enabled").getAsBoolean();
        boolean next = !current;
        state.addProperty("enabled", next);
        PlayerAbilityStore.update(player, data -> data.putState(ID, state));
        player.getEntityData().setBoolean(TAG, next);

        // 同步当前变身 dummy 实体
        IMorphing morphing = Morphing.get(player);
        if (morphing != null && morphing.isMorphed()) {
            AbstractMorph morph = morphing.getCurrentMorph();
            if (morph instanceof EntityMorph) {
                EntityLivingBase dummy = ((EntityMorph) morph).getEntity(player.world);
                if (dummy != null) {
                    dummy.getEntityData().setBoolean(TAG, next);
                }
            }
        }

        TextComponentTranslation msg = next
                ? new TextComponentTranslation("morphext.jzahar.banish.enabled")
                : new TextComponentTranslation("morphext.jzahar.banish.disabled");
        player.sendStatusMessage(msg, true);

        float pitch = next ? 1.4F : 0.6F;
        net.minecraft.util.SoundEvent sound = next
                ? net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_SPAWN
                : net.minecraft.init.SoundEvents.BLOCK_LEVER_CLICK;
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound,
                net.minecraft.util.SoundCategory.PLAYERS, 0.7F, pitch);
        return ActivationResult.success(0);
    }

    @Override
    public void onRevoked(EntityPlayerMP player, String id) {
        if (ID.equals(id) && player != null) {
            player.getEntityData().removeTag(TAG);
        }
    }

    @Override
    public void onDataWiped(EntityPlayerMP player, String id) {
        onRevoked(player, id);
    }
}
