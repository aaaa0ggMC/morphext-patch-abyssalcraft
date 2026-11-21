package aaaa0ggmc.morphext.abyssalcraft.converse;

import net.minecraft.util.text.translation.I18n;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从已命中的句子里随机抽一个方面，或综合多个方面。
 * 占位符对不上事实就跳过，避免把 {@code {entity}} 原样印出来。
 */
public final class ConversePicker {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)}");
    private static final Random RANDOM = new Random();
    private static final Deque<String> RECENT = new ArrayDeque<String>();
    private static final int RECENT_CAP = 8;

    private ConversePicker() {
    }

    public static String pick(ConverseScan scan) {
        if (!ready()) {
            ConverseCatalog.load();
        }
        boolean zh = isZh();
        if (scan == null) {
            return prefix(zh) + hardcoded(zh);
        }
        List<ConverseLine> matched = new ArrayList<ConverseLine>();
        List<ConverseLine> fallback = new ArrayList<ConverseLine>();
        for (ConverseLine line : ConverseCatalog.snapshot()) {
            if (line.aspects.isEmpty()) {
                fallback.add(line);
            } else if (scan.containsAll(line.aspects)) {
                matched.add(line);
            }
        }
        String text = null;
        if (!matched.isEmpty()) {
            List<ConverseLine> multi = multi(matched);
            boolean combine = !multi.isEmpty() && RANDOM.nextFloat() < 0.34F;
            List<ConverseLine> pool = combine ? multi : linesFor(matched, chooseAspect(scan, matched));
            if (pool.isEmpty()) {
                pool = matched;
            }
            text = tryPick(pool, scan, zh);
            if (text == null) {
                text = tryPick(matched, scan, zh);
            }
        }
        if (text == null) {
            text = tryPick(fallback, scan, zh);
        }
        return text == null ? prefix(zh) + hardcoded(zh) : text;
    }

    private static boolean ready() {
        return ConverseCatalog.size() > 0;
    }

    private static List<ConverseLine> multi(List<ConverseLine> matched) {
        List<ConverseLine> multi = new ArrayList<ConverseLine>();
        for (ConverseLine line : matched) {
            if (line.aspects.size() >= 2) {
                multi.add(line);
            }
        }
        return multi;
    }

    private static List<ConverseLine> linesFor(List<ConverseLine> matched, String aspect) {
        if (aspect == null) {
            return matched;
        }
        List<ConverseLine> pool = new ArrayList<ConverseLine>();
        for (ConverseLine line : matched) {
            if (line.aspects.contains(aspect)) {
                pool.add(line);
            }
        }
        return pool;
    }

    private static String chooseAspect(ConverseScan scan, List<ConverseLine> matched) {
        int total = 0;
        List<String> aspects = new ArrayList<String>();
        List<Integer> weights = new ArrayList<Integer>();
        for (String aspect : scan.aspects()) {
            if (!mentioned(matched, aspect)) {
                continue;
            }
            int weight = weightOf(aspect);
            aspects.add(aspect);
            weights.add(weight);
            total += weight;
        }
        if (total <= 0 || aspects.isEmpty()) {
            return null;
        }
        int roll = RANDOM.nextInt(total);
        int acc = 0;
        for (int i = 0; i < aspects.size(); i++) {
            acc += weights.get(i);
            if (roll < acc) {
                return aspects.get(i);
            }
        }
        return aspects.get(aspects.size() - 1);
    }

    private static boolean mentioned(List<ConverseLine> matched, String aspect) {
        for (ConverseLine line : matched) {
            if (line.aspects.contains(aspect)) {
                return true;
            }
        }
        return false;
    }

    static int weightOf(String aspect) {
        if (aspect == null) {
            return 1;
        }
        if (ConverseAspects.ENTITY_LOOKED.equals(aspect) || ConverseAspects.ENTITY_BOSS.equals(aspect)
                || ConverseAspects.ENTITY_PLAYER.equals(aspect) || ConverseAspects.ITEM_NECRONOMICON.equals(aspect)
                || ConverseAspects.ENTITY_SHOGGOTH.equals(aspect) || ConverseAspects.DIM_DARK_REALM.equals(aspect)
                || ConverseAspects.DIM_OMOTHOL.equals(aspect) || ConverseAspects.ENTITY_DRAGON.equals(aspect)) {
            return 6;
        }
        if (ConverseAspects.ENTITY_ANY.equals(aspect) || ConverseAspects.BLOCK_ANY.equals(aspect)
                || ConverseAspects.ITEM_ANY.equals(aspect) || ConverseAspects.BLOCK_AIR.equals(aspect)
                || ConverseAspects.ITEM_EMPTY.equals(aspect) || ConverseAspects.WORLD_CLEAR.equals(aspect)
                || ConverseAspects.WORLD_DAY.equals(aspect) || ConverseAspects.SELF_HEALTHY.equals(aspect)
                || aspect.startsWith("entity:count")) {
            return 1;
        }
        if (aspect.startsWith("dim:") || aspect.startsWith("world:") || aspect.startsWith("biome:")) {
            return 2;
        }
        return 3;
    }

    private static String tryPick(List<ConverseLine> pool, ConverseScan scan, boolean zh) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        List<ConverseLine> fresh = new ArrayList<ConverseLine>();
        for (ConverseLine line : pool) {
            if (!RECENT.contains(line.id)) {
                fresh.add(line);
            }
        }
        List<ConverseLine> use = new ArrayList<ConverseLine>(fresh.isEmpty() ? pool : fresh);
        int attempts = Math.min(12, use.size());
        for (int i = 0; i < attempts; i++) {
            ConverseLine line = weighted(use);
            if (line == null) {
                return null;
            }
            String filled = fill(line, scan, zh);
            use.remove(line);
            if (filled != null) {
                remember(line.id);
                return prefix(zh) + filled;
            }
        }
        return null;
    }

    private static ConverseLine weighted(List<ConverseLine> pool) {
        int total = 0;
        for (ConverseLine line : pool) {
            total += line.weight;
        }
        if (total <= 0) {
            return null;
        }
        int roll = RANDOM.nextInt(total);
        int acc = 0;
        for (ConverseLine line : pool) {
            acc += line.weight;
            if (roll < acc) {
                return line;
            }
        }
        return pool.get(pool.size() - 1);
    }

    static String fill(ConverseLine line, ConverseScan scan, boolean zh) {
        String raw = zh ? line.zh : line.en;
        if (raw == null || raw.isEmpty()) {
            raw = line.zh;
        }
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(raw);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = resolve(matcher.group(1), line, scan);
            if (value == null || value.isEmpty()) {
                return null;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(buffer);
        String filled = buffer.toString();
        return filled.isEmpty() ? null : filled;
    }

    private static String resolve(String token, ConverseLine line, ConverseScan scan) {
        if (token == null) {
            return null;
        }
        String key = token.toLowerCase(java.util.Locale.ROOT);
        if ("entity".equals(key)) {
            return entityName(line, scan);
        }
        if ("looked".equals(key)) {
            return first(scan.fact("looked"), scan.noted(ConverseAspects.ENTITY_LOOKED));
        }
        if ("player".equals(key)) {
            return first(scan.fact("player"), scan.noted(ConverseAspects.ENTITY_PLAYER));
        }
        if ("hostile".equals(key)) {
            return first(scan.fact("hostile"), scan.noted(ConverseAspects.ENTITY_HOSTILE));
        }
        if ("animal".equals(key)) {
            return first(scan.fact("animal"), scan.noted(ConverseAspects.ENTITY_ANIMAL));
        }
        if ("boss".equals(key)) {
            return first(scan.fact("boss"), scan.noted(ConverseAspects.ENTITY_BOSS));
        }
        if ("block".equals(key)) {
            return blockName(line, scan);
        }
        if ("item".equals(key)) {
            return first(scan.fact("item"), scan.noted(ConverseAspects.ITEM_ANY));
        }
        if ("biome".equals(key)) {
            return scan.fact("biome");
        }
        if ("dimension".equals(key)) {
            return scan.fact("dimension");
        }
        if ("count".equals(key)) {
            return scan.fact("count");
        }
        return null;
    }

    private static String entityName(ConverseLine line, ConverseScan scan) {
        if (line.aspects.contains(ConverseAspects.ENTITY_LOOKED)) {
            String looked = first(scan.noted(ConverseAspects.ENTITY_LOOKED), scan.fact("looked"));
            if (looked != null) {
                return looked;
            }
        }
        for (String aspect : line.aspects) {
            if (aspect.startsWith("entity:") && !aspect.startsWith("entity:count")
                    && !ConverseAspects.ENTITY_ANY.equals(aspect)
                    && !ConverseAspects.ENTITY_ALONE.equals(aspect)) {
                String noted = scan.noted(aspect);
                if (noted != null && !noted.isEmpty()) {
                    return noted;
                }
            }
        }
        return scan.fact("entity");
    }

    private static String blockName(ConverseLine line, ConverseScan scan) {
        for (String aspect : line.aspects) {
            if (aspect.startsWith("block:") && !ConverseAspects.BLOCK_AIR.equals(aspect)
                    && !ConverseAspects.BLOCK_ANY.equals(aspect)) {
                String noted = scan.noted(aspect);
                if (noted != null && !noted.isEmpty()) {
                    return noted;
                }
            }
        }
        return first(scan.fact("block"), scan.noted(ConverseAspects.BLOCK_ANY));
    }

    private static String first(String a, String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return null;
    }

    private static void remember(String id) {
        if (id == null) {
            return;
        }
        RECENT.remove(id);
        RECENT.addLast(id);
        while (RECENT.size() > RECENT_CAP) {
            RECENT.removeFirst();
        }
    }

    private static boolean isZh() {
        try {
            String marker = I18n.translateToLocal("morphext.converse.lang");
            if ("en".equals(marker)) {
                return false;
            }
            if ("zh".equals(marker)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static String prefix(boolean zh) {
        return zh ? "§5[扎哈尔] §f" : "§5[J'zahar] §f";
    }

    private static String hardcoded(boolean zh) {
        return zh
                ? "群星之外唯余死寂。深渊暂未垂听。"
                : "Beyond the stars lies only dead silence. The Abyss does not answer yet.";
    }
}
