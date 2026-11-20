package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

/** 客户端显示名。失败时退回注册名，不抛到检测器外面。 */
final class Names {
    private Names() {
    }

    static String of(Entity entity) {
        if (entity == null) {
            return "";
        }
        try {
            ITextComponent display = entity.getDisplayName();
            if (display != null) {
                String text = display.getUnformattedText();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String name = entity.getName();
            return name == null ? "" : name;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
