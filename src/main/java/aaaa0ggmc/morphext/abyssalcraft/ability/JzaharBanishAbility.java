package aaaa0ggmc.morphext.abyssalcraft.ability;

import aaaa0ggmc.morphext.ability.Ability;
import aaaa0ggmc.morphext.ability.AbilityStateStore;
import aaaa0ggmc.morphext.ability.ExecutionSide;
import aaaa0ggmc.morphext.ability.IStatefulAbility;
import com.google.gson.JsonObject;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 扎哈尔 (J'zahar) 古神遣返光环状态能力 (Boss Banishment State Ability)。
 *
 * <p>该能力属于 AbyssalCraft 适配层，由本 patch 模组注册；核心 morphext 不包含任何
 * J'zahar 专属判断。形态定义（morph_abilities.json）通过 {@code morphext:jzahar_banish}
 * 声明它，核心只在变身期间做通用的数据驱动授予。</p>
 *
 * 状态语义：
 *  - [遣返: ON]: 开启（默认）。扎哈尔维持至高排他法则，周围 64 格内任何 Boss 生物将受到古神遣返驱逐。
 *  - [遣返: OFF]: 关闭。扎哈尔收敛遣返光环，允许宿主以扎哈尔形态与其他首领生物共存、同台决斗！
 */
public class JzaharBanishAbility extends Ability implements IStatefulAbility {
    public static final String ID = "morphext:jzahar_banish";
    public static final String TAG = "morphext_jzahar_banish";

    public JzaharBanishAbility() {
        super(ID, "Boss Banish [ON/OFF]");
        // 末影之眼作为虚空放逐与法则观测的代表图标
        this.icon(new ItemStack(Items.ENDER_EYE));
    }

    /**
     * 这是扎哈尔变身专属的开关，不是所有玩家天生拥有的能力。
     * 变身期间由 morph_abilities.json 声明并临时授予；通过能力卷轴永久解锁后才会脱离形态常驻。
     */
    @Override
    public boolean isInnate() {
        return false;
    }

    @Override
    public ExecutionSide getExecutionSide() {
        return ExecutionSide.SERVER;
    }

    public static boolean isEnabled() {
        JsonObject state = AbilityStateStore.get(ID);
        if (state.has("enabled") && state.get("enabled").isJsonPrimitive()) {
            return state.get("enabled").getAsBoolean();
        }
        return true; // 默认开启 (true)
    }

    public static boolean isEnabled(JsonObject state) {
        if (state != null && state.has("enabled") && state.get("enabled").isJsonPrimitive()) {
            return state.get("enabled").getAsBoolean();
        }
        return true; // 默认开启 (true)
    }

    @Override
    public String getDisplayName() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            try {
                if (I18n.canTranslate("morphext.ability.jzahar_banish.name")) {
                    return I18n.translateToLocal("morphext.ability.jzahar_banish.name");
                }
            } catch (Throwable ignored) {
            }
        }
        return "古神遣返法则";
    }

    @Override
    public String getStateLabel(EntityPlayer player) {
        boolean on = isEnabled();
        if (player != null && player.getEntityData().hasKey(TAG)) {
            on = player.getEntityData().getBoolean(TAG);
        }
        if (on) {
            return TextFormatting.GREEN + "[" + I18n.translateToLocal("gui.morphext.state.banish.on") + "]" + TextFormatting.RESET;
        } else {
            return TextFormatting.GRAY + "[" + I18n.translateToLocal("gui.morphext.state.banish.off") + "]" + TextFormatting.RESET;
        }
    }

    @Override
    public boolean isActive(EntityPlayer player) {
        if (player != null && player.getEntityData().hasKey(TAG)) {
            return player.getEntityData().getBoolean(TAG);
        }
        return isEnabled();
    }

    @Override
    public void onActivate(EntityPlayer player) {
        if (player != null && player.world.isRemote) {
            aaaa0ggmc.morphext.client.ClientAbilityExecutor.activate(this);
        }
    }

    @Override
    public JsonObject saveState() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", isEnabled());
        return json;
    }

    @Override
    public void loadState(JsonObject state) {
    }

    @Override
    public String getDescription() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            try {
                if (I18n.canTranslate("morphext.ability.jzahar_banish.desc")) {
                    return I18n.translateToLocal("morphext.ability.jzahar_banish.desc");
                }
            } catch (Throwable ignored) {
            }
        }
        return "切换扎哈尔对周围 64 格内首领生物 (Boss) 的古神遣返法则状态 [开/关]：\n"
                + "· [遣返: ON]（默认）：自动遣返并驱逐周围末影龙、凋灵、深渊古神等全部 Boss；\n"
                + "· [遣返: OFF]：收敛光环，允许扎哈尔与其他首领 Boss 并存、同台决斗。";
    }
}
