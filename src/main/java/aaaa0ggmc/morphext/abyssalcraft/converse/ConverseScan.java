package aaaa0ggmc.morphext.abyssalcraft.converse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 一次客户端扫描的方面集合与可替换事实。不是网络包，不传给服务端。 */
public final class ConverseScan {
    private final Set<String> aspects = new LinkedHashSet<String>();
    private final Map<String, String> facts = new LinkedHashMap<String, String>();
    private final Map<String, String> noted = new LinkedHashMap<String, String>();

    public void add(String aspect) {
        if (aspect == null || aspect.isEmpty()) {
            return;
        }
        aspects.add(aspect.toLowerCase(Locale.ROOT));
    }

    /** 加标签，并记下这个标签对应的显示名（同标签只保留先写入的，调用方应按距离从近到远）。 */
    public void addNoted(String aspect, String name) {
        add(aspect);
        note(aspect, name);
    }

    public void note(String aspect, String name) {
        if (aspect == null || name == null || name.isEmpty()) {
            return;
        }
        String key = aspect.toLowerCase(Locale.ROOT);
        if (!noted.containsKey(key)) {
            noted.put(key, name);
        }
    }

    public void fact(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return;
        }
        if (!facts.containsKey(key)) {
            facts.put(key, value);
        }
    }

    public boolean has(String aspect) {
        return aspect != null && aspects.contains(aspect.toLowerCase(Locale.ROOT));
    }

    public boolean containsAll(Collection<String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        for (String aspect : required) {
            if (!has(aspect)) {
                return false;
            }
        }
        return true;
    }

    public String fact(String key) {
        return key == null ? null : facts.get(key);
    }

    public String noted(String aspect) {
        return aspect == null ? null : noted.get(aspect.toLowerCase(Locale.ROOT));
    }

    public Set<String> aspects() {
        return Collections.unmodifiableSet(aspects);
    }
}
