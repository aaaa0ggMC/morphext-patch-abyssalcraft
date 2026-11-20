package aaaa0ggmc.morphext.abyssalcraft.converse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 一条可带 § 格式的本体低语。aspects 为空 = 无条件兜底。 */
public final class ConverseLine {
    public final String id;
    public final List<String> aspects;
    /** 简体；也是无英文时的回退。 */
    public final String zh;
    public final String en;
    /** 与 {@link #zh} 相同，留给只读 text 的旧调用。 */
    public final String text;
    public final int weight;

    public ConverseLine(String id, List<String> aspects, String text, int weight) {
        this(id, aspects, text, text, weight);
    }

    public ConverseLine(String id, List<String> aspects, String zh, String en, int weight) {
        this.id = id;
        List<String> copy = new ArrayList<String>();
        if (aspects != null) {
            for (String aspect : aspects) {
                if (aspect == null || aspect.isEmpty()) {
                    continue;
                }
                copy.add(aspect.toLowerCase(Locale.ROOT));
            }
        }
        this.aspects = Collections.unmodifiableList(copy);
        this.zh = zh == null ? "" : zh;
        this.en = en == null || en.isEmpty() ? this.zh : en;
        this.text = this.zh;
        this.weight = weight < 1 ? 1 : Math.min(weight, 8);
    }
}
