package aaaa0ggmc.morphext.abyssalcraft.ability;

import aaaa0ggmc.morphext.ability.Ability;
import aaaa0ggmc.morphext.ability.ActivationResult;
import aaaa0ggmc.morphext.ability.ExecutionSide;
import aaaa0ggmc.morphext.abyssalcraft.MorphExtAbyssalCraft;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseCatalog;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseDetectors;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConversePicker;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import com.shinoow.abyssalcraft.api.item.ACItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 和本体交谈。检测与显示都在客户端：{@link ExecutionSide#CLIENT} 不会发能力请求包，
 * 因此服务端不会为这句话去扫周围实体或朝向。
 *
 * <p>id 不用 {@code morphext:jzahar*}。那个前缀已经被古神遣返的服务端 handler 占住，
 * 最长前缀匹配会把别的 id 吞进去。</p>
 */
public class JzaharConverseAbility extends Ability {
    public static final String ID = "morphext:ac_converse";
    private static final int COOLDOWN_TICKS = 30;
    private static boolean announced;

    public JzaharConverseAbility() {
        super(ID, "和本体交谈");
        this.icon(icon());
    }

    @Override
    public boolean isInnate() {
        return false;
    }

    @Override
    public ExecutionSide getExecutionSide() {
        return ExecutionSide.CLIENT;
    }

    @Override
    public boolean isStateToggleable() {
        return false;
    }

    @Override
    public int getCooldownTicks(EntityPlayer player) {
        return COOLDOWN_TICKS;
    }

    @Override
    public String getDisplayName() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            try {
                if (I18n.canTranslate("morphext.ability.ac_converse.name")) {
                    return I18n.translateToLocal("morphext.ability.ac_converse.name");
                }
            } catch (Throwable ignored) {
            }
        }
        return "和本体交谈";
    }

    @Override
    public String getDescription() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            try {
                if (I18n.canTranslate("morphext.ability.ac_converse.desc")) {
                    return I18n.translateToLocal("morphext.ability.ac_converse.desc");
                }
            } catch (Throwable ignored) {
            }
        }
        return "倾听扎哈尔本体的低语。它会看你周围的生灵、视线里的方块、手里的器物和所在的界域，说一句只有你听得见的话。";
    }

    @Override
    public ActivationResult activate(EntityPlayer player) {
        if (player == null || player.world == null || !player.world.isRemote) {
            return ActivationResult.success(0);
        }
        ConverseDetectors.installDefaults();
        ConverseScan scan = ConverseDetectors.scan(player);
        String line = ConversePicker.pick(scan);
        if (line == null || line.isEmpty()) {
            return new ActivationResult(ActivationResult.Status.FAILED, 0, "");
        }
        player.sendMessage(new TextComponentString(line));
        if (!announced) {
            announced = true;
            MorphExtAbyssalCraft.LOGGER.info("[jzahar-converse] spoke aspects={} lines={}",
                    scan.aspects().size(), ConverseCatalog.size());
        }
        return ActivationResult.success(getCooldownTicks(player));
    }

    private static ItemStack icon() {
        try {
            if (ACItems.necronomicon != null) {
                return new ItemStack(ACItems.necronomicon);
            }
        } catch (Throwable ignored) {
        }
        return new ItemStack(Items.BOOK);
    }
}
