package aaaa0ggmc.morphext.abyssalcraft.converse;

import aaaa0ggmc.morphext.abyssalcraft.MorphExtAbyssalCraft;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 本体低语目录。运行时 {@link #register(ConverseLine)}，或任意已安装模组的
 * {@code assets/<modid>/morphext/converse/*.json}（{@code index.json} 只作开发环境清单）。
 * 同 id 先注册的保留。占位符填不上的句子由选句器跳过。
 */
public final class ConverseCatalog {
    private static final List<ConverseLine> LINES = new ArrayList<ConverseLine>();
    private static final Set<String> IDS = new LinkedHashSet<String>();
    private static final Set<String> SOURCES = new LinkedHashSet<String>();
    private static File configDirectory;
    private static boolean loaded;

    private ConverseCatalog() {
    }

    public static void setConfigDirectory(File dir) {
        configDirectory = dir;
    }

    public static void register(ConverseLine line) {
        if (line == null || line.id == null || line.id.isEmpty() || line.zh.isEmpty()) {
            return;
        }
        synchronized (ConverseCatalog.class) {
            if (!IDS.add(line.id)) {
                return;
            }
            LINES.add(line);
        }
    }

    /** 客户端初始化时调用。重复调用无副作用。 */
    public static void load() {
        synchronized (ConverseCatalog.class) {
            if (loaded) {
                return;
            }
            loaded = true;
        }
        try {
            loadEmbedded();
            loadConfig();
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] catalog load failed", t);
        }
        MorphExtAbyssalCraft.LOGGER.info("[jzahar-converse] catalog lines={}", size());
    }

    public static int size() {
        synchronized (ConverseCatalog.class) {
            return LINES.size();
        }
    }

    public static List<ConverseLine> snapshot() {
        synchronized (ConverseCatalog.class) {
            return Collections.unmodifiableList(new ArrayList<ConverseLine>(LINES));
        }
    }

    private static void loadEmbedded() {
        if (Loader.instance() == null || Loader.instance().getActiveModList() == null) {
            loadOwnClasspath();
            return;
        }
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (mod == null || mod.getModId() == null) {
                continue;
            }
            String modId = mod.getModId().toLowerCase(Locale.ROOT);
            String prefix = "assets/" + modId + "/morphext/converse/";
            File source = mod.getSource();
            boolean scanned = false;
            if (source != null && source.isFile()) {
                scanned = loadZip(source, prefix);
            } else if (source != null && source.isDirectory()) {
                scanned = loadDirectory(new File(source, prefix), prefix);
            }
            if (!scanned) {
                loadIndex(prefix);
            }
        }
    }

    private static void loadOwnClasspath() {
        loadIndex("assets/" + MorphExtAbyssalCraft.MODID + "/morphext/converse/");
    }

    private static boolean loadZip(File jar, String prefix) {
        ZipFile zip = null;
        boolean any = false;
        try {
            zip = new ZipFile(jar);
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith(prefix) || !name.endsWith(".json") || name.endsWith("/index.json")) {
                    continue;
                }
                InputStream in = zip.getInputStream(entry);
                try {
                    if (read(in, "jar:" + jar.getName() + "!" + name)) {
                        any = true;
                    }
                } finally {
                    in.close();
                }
            }
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.debug("[jzahar-converse] skip jar {}: {}", jar.getName(), t.toString());
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return any;
    }

    private static boolean loadDirectory(File dir, String prefix) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        boolean any = false;
        for (File file : files) {
            if (file.isDirectory()) {
                if (loadDirectory(file, prefix + file.getName() + "/")) {
                    any = true;
                }
                continue;
            }
            if (!file.getName().endsWith(".json") || "index.json".equals(file.getName())) {
                continue;
            }
            try (InputStream in = new java.io.FileInputStream(file)) {
                if (read(in, file.getPath())) {
                    any = true;
                }
            } catch (Throwable t) {
                MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] bad file {}", file.getName(), t);
            }
        }
        return any;
    }

    private static void loadIndex(String prefix) {
        String indexPath = prefix + "index.json";
        InputStream index = ConverseCatalog.class.getClassLoader().getResourceAsStream(indexPath);
        if (index == null) {
            return;
        }
        try (Reader reader = new InputStreamReader(index, StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            JsonArray files = null;
            if (root.isJsonArray()) {
                files = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("files")) {
                files = root.getAsJsonObject().getAsJsonArray("files");
            }
            if (files == null) {
                return;
            }
            for (JsonElement element : files) {
                if (element == null || !element.isJsonPrimitive()) {
                    continue;
                }
                String name = element.getAsString();
                if (name == null || name.isEmpty() || name.contains("..") || !name.endsWith(".json")) {
                    continue;
                }
                String path = prefix + name;
                InputStream in = ConverseCatalog.class.getClassLoader().getResourceAsStream(path);
                if (in == null) {
                    MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] missing {}", path);
                    continue;
                }
                try {
                    read(in, "classpath:" + path);
                } finally {
                    in.close();
                }
            }
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] bad index {}", indexPath, t);
        }
    }

    private static void loadConfig() {
        if (configDirectory == null) {
            return;
        }
        File dir = new File(new File(configDirectory, "morphext"), "converse");
        loadDirectory(dir, "config/morphext/converse/");
    }

    private static boolean read(InputStream in, String source) {
        if (in == null || source == null) {
            return false;
        }
        synchronized (ConverseCatalog.class) {
            if (!SOURCES.add(source)) {
                return false;
            }
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            JsonArray lines = extract(root);
            if (lines == null) {
                MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] {} has no lines", source);
                return false;
            }
            int added = 0;
            int index = 0;
            for (JsonElement element : lines) {
                index++;
                ConverseLine line = parseLine(element, source, index);
                int before = size();
                register(line);
                if (size() > before) {
                    added++;
                }
            }
            return added > 0;
        } catch (Throwable t) {
            MorphExtAbyssalCraft.LOGGER.warn("[jzahar-converse] failed to read {}", source, t);
            return false;
        }
    }

    private static JsonArray extract(JsonElement root) {
        if (root == null || root.isJsonNull()) {
            return null;
        }
        if (root.isJsonArray()) {
            return root.getAsJsonArray();
        }
        if (!root.isJsonObject()) {
            return null;
        }
        JsonObject object = root.getAsJsonObject();
        if (object.has("lines") && object.get("lines").isJsonArray()) {
            return object.getAsJsonArray("lines");
        }
        return null;
    }

    private static ConverseLine parseLine(JsonElement element, String source, int index) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String zh = text(object, "zh");
        if (zh.isEmpty()) {
            zh = text(object, "text");
        }
        if (zh.isEmpty()) {
            return null;
        }
        String en = text(object, "en");
        String id = text(object, "id");
        if (id.isEmpty()) {
            id = source + "#" + index;
        }
        List<String> aspects = new ArrayList<String>();
        if (object.has("aspects") && object.get("aspects").isJsonArray()) {
            for (JsonElement aspect : object.getAsJsonArray("aspects")) {
                if (aspect != null && aspect.isJsonPrimitive()) {
                    aspects.add(aspect.getAsString());
                }
            }
        } else if (object.has("aspect") && object.get("aspect").isJsonPrimitive()) {
            aspects.add(object.get("aspect").getAsString());
        }
        int weight = 1;
        if (object.has("weight") && object.get("weight").isJsonPrimitive()) {
            try {
                weight = object.get("weight").getAsInt();
            } catch (Throwable ignored) {
                weight = 1;
            }
        }
        return new ConverseLine(id, aspects, zh, en, weight);
    }

    private static String text(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        String value = object.get(key).getAsString();
        return value == null ? "" : value;
    }
}
