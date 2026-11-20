package aaaa0ggmc.morphext.abyssalcraft.converse;

/**
 * 「和本体交谈」的方面标签。检测器只往扫描结果里加这些标签；
 * 文案按标签匹配，不在检测器里写死句子。
 *
 * <p>检测与选句都在客户端。新检测实现 {@link ConverseDetector} 后调用
 * {@link ConverseDetectors#register(ConverseDetector)}；新句子调用
 * {@link ConverseCatalog#register(ConverseLine)}，或丢进任意已安装模组的
 * {@code assets/<modid>/morphext/converse/*.json}。</p>
 *
 * <p>占位符（事实缺失的句子会被跳过）：
 * {@code {entity}} {@code {looked}} {@code {player}} {@code {hostile}}
 * {@code {animal}} {@code {boss}} {@code {block}} {@code {item}}
 * {@code {biome}} {@code {dimension}} {@code {count}}。
 * {@code {entity}} 会优先用本句命中的那个实体名，而不是最近的随便一只。</p>
 */
public final class ConverseAspects {
    public static final double NEARBY_RADIUS = 16.0D;
    public static final double LOOK_REACH = 16.0D;
    public static final double BLOCK_REACH = 12.0D;

    public static final String ENTITY_ALONE = "entity:alone";
    public static final String ENTITY_ANY = "entity:any";
    public static final String ENTITY_COUNT_ONE = "entity:count:one";
    public static final String ENTITY_COUNT_FEW = "entity:count:few";
    public static final String ENTITY_COUNT_MANY = "entity:count:many";
    public static final String ENTITY_PLAYER = "entity:player";
    public static final String ENTITY_VILLAGER = "entity:villager";
    public static final String ENTITY_ZOMBIE = "entity:zombie";
    public static final String ENTITY_SKELETON = "entity:skeleton";
    public static final String ENTITY_CREEPER = "entity:creeper";
    public static final String ENTITY_SPIDER = "entity:spider";
    public static final String ENTITY_ENDERMAN = "entity:enderman";
    public static final String ENTITY_WITCH = "entity:witch";
    public static final String ENTITY_SLIME = "entity:slime";
    public static final String ENTITY_ANIMAL = "entity:animal";
    public static final String ENTITY_WOLF = "entity:wolf";
    public static final String ENTITY_GOLEM = "entity:golem";
    public static final String ENTITY_HOSTILE = "entity:hostile";
    public static final String ENTITY_UNDEAD = "entity:undead";
    public static final String ENTITY_BOSS = "entity:boss";
    public static final String ENTITY_ABYSSAL = "entity:abyssal";
    public static final String ENTITY_CORALIUM = "entity:coralium";
    public static final String ENTITY_DREAD = "entity:dread";
    public static final String ENTITY_SHADOW = "entity:shadow";
    public static final String ENTITY_SHOGGOTH = "entity:shoggoth";
    public static final String ENTITY_OMOTHOL = "entity:omothol";
    public static final String ENTITY_DEMON = "entity:demon";
    public static final String ENTITY_ANTIMATTER = "entity:antimatter";
    public static final String ENTITY_DRAGON = "entity:dragon";
    public static final String ENTITY_GHOUL = "entity:ghoul";
    public static final String ENTITY_MINION = "entity:minion";
    public static final String ENTITY_ITEM = "entity:item";
    public static final String ENTITY_PROJECTILE = "entity:projectile";
    public static final String ENTITY_LOOKED = "entity:looked";

    public static final String BLOCK_AIR = "block:air";
    public static final String BLOCK_ANY = "block:any";
    public static final String BLOCK_LIQUID = "block:liquid";
    public static final String BLOCK_WATER = "block:water";
    public static final String BLOCK_LAVA = "block:lava";
    public static final String BLOCK_FIRE = "block:fire";
    public static final String BLOCK_ORE = "block:ore";
    public static final String BLOCK_PLANT = "block:plant";
    public static final String BLOCK_WOOD = "block:wood";
    public static final String BLOCK_STONE = "block:stone";
    public static final String BLOCK_DIRT = "block:dirt";
    public static final String BLOCK_SAND = "block:sand";
    public static final String BLOCK_GLASS = "block:glass";
    public static final String BLOCK_CONTAINER = "block:container";
    public static final String BLOCK_ABYSSAL = "block:abyssal";
    public static final String BLOCK_CORALIUM = "block:coralium";
    public static final String BLOCK_DARKSTONE = "block:darkstone";
    public static final String BLOCK_DREADSTONE = "block:dreadstone";
    public static final String BLOCK_ABYSSALNITE = "block:abyssalnite";

    public static final String ITEM_EMPTY = "item:empty";
    public static final String ITEM_ANY = "item:any";
    public static final String ITEM_SWORD = "item:sword";
    public static final String ITEM_BOW = "item:bow";
    public static final String ITEM_TOOL = "item:tool";
    public static final String ITEM_PICKAXE = "item:pickaxe";
    public static final String ITEM_AXE = "item:axe";
    public static final String ITEM_FOOD = "item:food";
    public static final String ITEM_ARMOR = "item:armor";
    public static final String ITEM_POTION = "item:potion";
    public static final String ITEM_BOOK = "item:book";
    public static final String ITEM_ABYSSAL = "item:abyssal";
    public static final String ITEM_NECRONOMICON = "item:necronomicon";
    public static final String ITEM_OFFHAND = "item:offhand";

    public static final String SELF_SNEAKING = "self:sneaking";
    public static final String SELF_SPRINTING = "self:sprinting";
    public static final String SELF_FLYING = "self:flying";
    public static final String SELF_LOW_HEALTH = "self:low_health";
    public static final String SELF_CRITICAL = "self:critical";
    public static final String SELF_HEALTHY = "self:healthy";
    public static final String SELF_ON_FIRE = "self:on_fire";
    public static final String SELF_IN_WATER = "self:in_water";
    public static final String SELF_IN_LAVA = "self:in_lava";
    public static final String SELF_POTION = "self:potion";
    public static final String SELF_HURT = "self:hurt";
    public static final String SELF_HUNGRY = "self:hungry";

    public static final String WORLD_DAY = "world:day";
    public static final String WORLD_NIGHT = "world:night";
    public static final String WORLD_DAWN = "world:dawn";
    public static final String WORLD_DUSK = "world:dusk";
    public static final String WORLD_RAIN = "world:rain";
    public static final String WORLD_THUNDER = "world:thunder";
    public static final String WORLD_CLEAR = "world:clear";

    public static final String BIOME_OCEAN = "biome:ocean";
    public static final String BIOME_FOREST = "biome:forest";
    public static final String BIOME_DESERT = "biome:desert";
    public static final String BIOME_MOUNTAIN = "biome:mountain";
    public static final String BIOME_SWAMP = "biome:swamp";
    public static final String BIOME_HELL = "biome:hell";
    public static final String BIOME_COLD = "biome:cold";
    public static final String BIOME_JUNGLE = "biome:jungle";
    public static final String BIOME_MESA = "biome:mesa";
    public static final String BIOME_PLAINS = "biome:plains";
    public static final String BIOME_RIVER = "biome:river";
    public static final String BIOME_BEACH = "biome:beach";
    public static final String BIOME_MUSHROOM = "biome:mushroom";
    public static final String BIOME_VOID = "biome:void";
    public static final String BIOME_END = "biome:end";
    public static final String BIOME_ABYSSAL = "biome:abyssal";
    public static final String BIOME_DREAD = "biome:dread";
    public static final String BIOME_OMOTHOL = "biome:omothol";

    public static final String DIM_OVERWORLD = "dim:overworld";
    public static final String DIM_NETHER = "dim:nether";
    public static final String DIM_END = "dim:end";
    public static final String DIM_ABYSSAL_WASTELAND = "dim:abyssal_wasteland";
    public static final String DIM_DREADLANDS = "dim:dreadlands";
    public static final String DIM_OMOTHOL = "dim:omothol";
    public static final String DIM_DARK_REALM = "dim:dark_realm";
    public static final String DIM_OTHER = "dim:other";

    private ConverseAspects() {
    }

    /** {@code prefix:named:modid:path} 与 {@code prefix:path}。path 已是小写注册名。 */
    public static void named(ConverseScan scan, String prefix, net.minecraft.util.ResourceLocation id, String note) {
        if (scan == null || prefix == null || id == null || id.getPath() == null || id.getPath().isEmpty()) {
            return;
        }
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        String namespace = id.getNamespace() == null ? "minecraft" : id.getNamespace().toLowerCase(java.util.Locale.ROOT);
        scan.addNoted(prefix + ":" + path, note);
        scan.addNoted(prefix + ":named:" + namespace + ":" + path, note);
    }
}
