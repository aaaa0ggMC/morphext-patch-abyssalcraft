package aaaa0ggmc.morphext.abyssalcraft.converse.detect;

import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseAspects;
import aaaa0ggmc.morphext.abyssalcraft.converse.ConverseScan;
import com.shinoow.abyssalcraft.api.entity.EntityUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

/** 把一只实体写成方面标签。调用方应按距离从近到远，笔记只保留先写入的名字。 */
final class EntityTags {
    private EntityTags() {
    }

    static void tag(ConverseScan scan, Entity entity, boolean looked) {
        if (scan == null || entity == null || entity.isDead) {
            return;
        }
        String name = Names.of(entity);
        if (name.isEmpty()) {
            name = "?";
        }
        String simple = entity.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String qualified = entity.getClass().getName().toLowerCase(Locale.ROOT);
        ResourceLocation key = null;
        try {
            key = EntityList.getKey(entity);
        } catch (Throwable ignored) {
        }
        String path = key == null || key.getPath() == null ? "" : key.getPath().toLowerCase(Locale.ROOT);
        String blob = simple + " " + path;
        if (key != null) {
            ConverseAspects.named(scan, "entity", key, name);
        }
        if (looked) {
            scan.addNoted(ConverseAspects.ENTITY_LOOKED, name);
            scan.fact("looked", name);
        }
        if (entity instanceof EntityPlayer) {
            scan.addNoted(ConverseAspects.ENTITY_PLAYER, name);
            scan.fact("player", name);
        }
        if (entity instanceof EntityVillager || blob.contains("villager")) {
            scan.addNoted(ConverseAspects.ENTITY_VILLAGER, name);
        }
        if (entity instanceof EntityZombie || blob.contains("zombie")) {
            scan.addNoted(ConverseAspects.ENTITY_ZOMBIE, name);
            scan.addNoted(ConverseAspects.ENTITY_UNDEAD, name);
        }
        if (entity instanceof EntitySkeleton || blob.contains("skeleton")) {
            scan.addNoted(ConverseAspects.ENTITY_SKELETON, name);
            scan.addNoted(ConverseAspects.ENTITY_UNDEAD, name);
        }
        if (blob.contains("ghoul") || blob.contains("wither") || blob.contains("undead")) {
            scan.addNoted(ConverseAspects.ENTITY_UNDEAD, name);
        }
        if (entity instanceof EntityCreeper || blob.contains("creeper")) {
            scan.addNoted(ConverseAspects.ENTITY_CREEPER, name);
        }
        if (entity instanceof EntitySpider || blob.contains("spider")) {
            scan.addNoted(ConverseAspects.ENTITY_SPIDER, name);
        }
        if (entity instanceof EntityEnderman || blob.contains("enderman") || blob.contains("endermite")) {
            scan.addNoted(ConverseAspects.ENTITY_ENDERMAN, name);
        }
        if (entity instanceof EntityWitch || blob.contains("witch")) {
            scan.addNoted(ConverseAspects.ENTITY_WITCH, name);
        }
        if (entity instanceof EntitySlime || blob.contains("slime") || blob.contains("magma_cube")) {
            scan.addNoted(ConverseAspects.ENTITY_SLIME, name);
        }
        if (entity instanceof EntityWolf || blob.contains("wolf")) {
            scan.addNoted(ConverseAspects.ENTITY_WOLF, name);
        }
        if (blob.contains("golem")) {
            scan.addNoted(ConverseAspects.ENTITY_GOLEM, name);
        }
        if (entity instanceof EntityAnimal || entity instanceof EntityAmbientCreature
                || (entity instanceof EntityWaterMob && !(entity instanceof IMob))) {
            scan.addNoted(ConverseAspects.ENTITY_ANIMAL, name);
            scan.fact("animal", name);
        }
        if (entity instanceof IMob || entity instanceof EntityMob) {
            scan.addNoted(ConverseAspects.ENTITY_HOSTILE, name);
            scan.fact("hostile", name);
        }
        boolean minion = blob.contains("minion") || blob.contains("remnant") || blob.contains("offspring")
                || blob.contains("fist") || path.contains("spawn");
        if (minion) {
            scan.addNoted(ConverseAspects.ENTITY_MINION, name);
        }
        boolean namedBoss = blob.contains("jzahar") || blob.contains("sacthoth")
                || (blob.contains("chagaroth") && !blob.contains("fist") && !blob.contains("spawn"))
                || blob.contains("dragonboss") || blob.contains("ender_dragon")
                || entity instanceof EntityWither || entity instanceof EntityDragon;
        if (!minion && (namedBoss || blob.contains("boss"))) {
            scan.addNoted(ConverseAspects.ENTITY_BOSS, name);
            scan.fact("boss", name);
        }
        boolean abyssal = qualified.contains("abyssalcraft")
                || (key != null && "abyssalcraft".equals(key.getNamespace()));
        if (abyssal) {
            scan.addNoted(ConverseAspects.ENTITY_ABYSSAL, name);
        }
        if (entity instanceof EntityLivingBase) {
            tagAbyssalFamily(scan, (EntityLivingBase) entity, blob, name);
        }
        if (blob.contains("shadow")) {
            scan.addNoted(ConverseAspects.ENTITY_SHADOW, name);
        }
        if (blob.contains("shoggoth")) {
            scan.addNoted(ConverseAspects.ENTITY_SHOGGOTH, name);
        }
        if (blob.contains("omothol") || blob.contains("jzahar") || blob.contains("gatekeeper") || blob.contains("remnant")) {
            scan.addNoted(ConverseAspects.ENTITY_OMOTHOL, name);
        }
        if (blob.contains("demon")) {
            scan.addNoted(ConverseAspects.ENTITY_DEMON, name);
        }
        if (blob.contains("dragon") || entity instanceof EntityDragon) {
            scan.addNoted(ConverseAspects.ENTITY_DRAGON, name);
        }
        if (blob.contains("ghoul")) {
            scan.addNoted(ConverseAspects.ENTITY_GHOUL, name);
        }
        if (entity instanceof EntityItem) {
            scan.addNoted(ConverseAspects.ENTITY_ITEM, name);
        }
        if (entity instanceof EntityArrow || entity instanceof EntityThrowable || entity instanceof EntityFireball
                || blob.contains("projectile") || blob.contains("arrow") || blob.contains("charge")
                || blob.contains("fireball") || blob.contains("skull")) {
            scan.addNoted(ConverseAspects.ENTITY_PROJECTILE, name);
        }
    }

    private static void tagAbyssalFamily(ConverseScan scan, EntityLivingBase living, String blob, String name) {
        try {
            if (EntityUtil.isEntityCoralium(living) || blob.contains("coralium")) {
                scan.addNoted(ConverseAspects.ENTITY_CORALIUM, name);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (EntityUtil.isEntityDread(living) || blob.contains("dread")) {
                scan.addNoted(ConverseAspects.ENTITY_DREAD, name);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (EntityUtil.isEntityAnti(living) || blob.contains("antimatter") || blob.contains("anti_")) {
                scan.addNoted(ConverseAspects.ENTITY_ANTIMATTER, name);
            }
        } catch (Throwable ignored) {
        }
    }
}
