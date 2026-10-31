package aaaa0ggmc.morphext.abyssalcraft.proxy;

import aaaa0ggmc.morphext.client.hand.GenericHandRegistry;
import aaaa0ggmc.morphext.client.hand.HandPreset;
import aaaa0ggmc.morphext.client.render.GenericMorphRenderRegistry;
import aaaa0ggmc.morphext.abyssalcraft.SafeCleansingRitualHandler;
import com.shinoow.abyssalcraft.common.entity.EntityDragonBoss;
import com.shinoow.abyssalcraft.common.entity.EntityDragonMinion;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityBlackHole.class,
                com.shinoow.abyssalcraft.client.render.entity.RenderBlackHole::new
        );
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                aaaa0ggmc.morphext.abyssalcraft.entity.MorphEntityImplosion.class,
                com.shinoow.abyssalcraft.client.render.entity.RenderImplosion::new
        );
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        aaaa0ggmc.morphext.ability.AbilityRegistry.register(
                new aaaa0ggmc.morphext.abyssalcraft.ability.JzaharBanishAbility());
        SafeCleansingRitualHandler.install();
        aaaa0ggmc.morphext.variant.MorphCreativeInjector.registerProvider(new aaaa0ggmc.morphext.abyssalcraft.variant.AbyssalCraftVariantProvider());
        registerHandPresets();
        registerRenderAdjustments();
    }

    private void registerRenderAdjustments() {
        try {
            // 深渊巨龙 (Asorah / DragonBoss) 与巨龙仆从 (DragonMinion) 模型几何朝向与原版巨龙一致，
            // 但未被 Metamorph 识别为 EntityDragon，变身状态下刚好反转 180°。
            // 正常打 Boss 完全正常，仅变身时反向。在此通过 GenericMorphRenderRegistry DI 接口注册变身 180° 翻转修复：
            GenericMorphRenderRegistry.registerRotate180("abyssalcraft:dragonboss");
            GenericMorphRenderRegistry.registerRotate180("abyssalcraft.dragonboss");
            GenericMorphRenderRegistry.registerRotate180("abyssalcraft:dragonminion");
            GenericMorphRenderRegistry.registerRotate180("abyssalcraft.dragonminion");
            GenericMorphRenderRegistry.registerRotate180(EntityDragonBoss.class);
            GenericMorphRenderRegistry.registerRotate180(EntityDragonMinion.class);
        } catch (Throwable t) {
            org.apache.logging.log4j.LogManager.getLogger("morphext-patch-abyssalcraft")
                .error("[morphext-patch-abyssalcraft] Failed to register dragon render adjustments", t);
        }
    }

    private static void reg(String morphId, HandPreset preset) {
        GenericHandRegistry.registerMorph(morphId, preset);
        if (morphId.contains(":")) {
            GenericHandRegistry.registerMorph(morphId.replaceFirst(":", "."), preset);
        } else if (morphId.contains(".")) {
            GenericHandRegistry.registerMorph(morphId.replaceFirst("\\.", ":"), preset);
        }
    }

    private void registerHandPresets() {
        try {
            // ==========================================
            // 1. 模型类预设 (Model Class Presets)
            // ==========================================
            GenericHandRegistry.registerModel("ModelJzahar", new HandPreset("staff1", "abyssalnomicon"));
            GenericHandRegistry.registerModel("ModelGhoul", new HandPreset("rarm1", "larm1"));
            GenericHandRegistry.registerModel("ModelLesserShoggoth", new HandPreset("rArm01a", "lArm01a"));
            GenericHandRegistry.registerModel("ModelShadowMonster", new HandPreset("Rarm1", "Larm1"));
            GenericHandRegistry.registerModel("ModelShadowBeast", new HandPreset("rarm1", "larm1"));
            GenericHandRegistry.registerModel("ModelShadowCreature", new HandPreset("RightArm2", "LeftArm2"));
            // Sacthoth (解剖学右手在 negative X，源码命名反转为 leftarm1)
            GenericHandRegistry.registerModel("ModelSacthoth", new HandPreset("leftarm1", "rightarm1"));
            GenericHandRegistry.registerModel("ModelSkeletonGoliath", new HandPreset("rightarm", "leftarm"));
            GenericHandRegistry.registerModel("ModelGatekeeperMinion", new HandPreset("rightarm1", "leftarm1"));
            GenericHandRegistry.registerModel("ModelRemnant", new HandPreset("rightarm", "leftarm"));
            GenericHandRegistry.registerModel("ModelRemnantTrader", new HandPreset("villagerArms0", "villagerArms1"));
            GenericHandRegistry.registerModel("ModelDreadling", new HandPreset("rightarm", "leftarm"));
            GenericHandRegistry.registerModel("ModelDreadSpawn", new HandPreset("arm", "thing"));
            GenericHandRegistry.registerModel("ModelDreadguard", new HandPreset("rightarm", "leftarm"));
            GenericHandRegistry.registerModel("ModelChagarothFist", new HandPreset("arm1", "arm1"));
            GenericHandRegistry.registerModel("ModelChagaroth", new HandPreset("walltentacle5", "walltentacle1"));
            GenericHandRegistry.registerModel("ModelChagarothSpawn", new HandPreset("rightleg1", "leftleg1"));
            GenericHandRegistry.registerModel("ModelShubOffspring", new HandPreset("rLeg01a", "lLeg01a"));
            GenericHandRegistry.registerModel("ModelDragonBoss", new HandPreset("frontLeg", "frontLeg"));
            GenericHandRegistry.registerModel("ModelDragonMinion", new HandPreset("frontLeg", "frontLeg"));
            GenericHandRegistry.registerModel("ModelAntiBat", new HandPreset("batRightWing", "batLeftWing"));
            GenericHandRegistry.registerModel("ModelEvilSheep2", new HandPreset("leg3", "leg4"));
            GenericHandRegistry.registerModel("ModelDemonSheep2", new HandPreset("leg3", "leg4"));

            // ==========================================
            // 2. 形态别名绑定 (支持 modid:name 与 modid.name 双向映射)
            // ==========================================
            // J'zahar (古神扎哈尔: 右手法杖，左手死灵之书)
            HandPreset jzaharPreset = new HandPreset("staff1", "abyssalnomicon");
            reg("abyssalcraft:jzahar", jzaharPreset);
            reg("abyssalcraft.jzahar", jzaharPreset);

            // Ghouls (深渊/暗影/反物质/恐惧/奥穆索食尸鬼)
            HandPreset ghoulPreset = new HandPreset("rarm1", "larm1");
            reg("abyssalcraft:depthsghoul", ghoulPreset);
            reg("abyssalcraft:depths_ghoul", ghoulPreset);
            reg("abyssalcraft.depthsghoul", ghoulPreset);
            reg("abyssalcraft.depths_ghoul", ghoulPreset);
            reg("abyssalcraft:ghoul", ghoulPreset);
            reg("abyssalcraft.ghoul", ghoulPreset);
            reg("abyssalcraft:dreadedghoul", ghoulPreset);
            reg("abyssalcraft:dreaded_ghoul", ghoulPreset);
            reg("abyssalcraft.dreadedghoul", ghoulPreset);
            reg("abyssalcraft.dreaded_ghoul", ghoulPreset);
            reg("abyssalcraft:shadowghoul", ghoulPreset);
            reg("abyssalcraft:shadow_ghoul", ghoulPreset);
            reg("abyssalcraft.shadowghoul", ghoulPreset);
            reg("abyssalcraft.shadow_ghoul", ghoulPreset);
            reg("abyssalcraft:antighoul", ghoulPreset);
            reg("abyssalcraft:anti_ghoul", ghoulPreset);
            reg("abyssalcraft.antighoul", ghoulPreset);
            reg("abyssalcraft.anti_ghoul", ghoulPreset);
            reg("abyssalcraft:omotholghoul", ghoulPreset);
            reg("abyssalcraft:omothol_ghoul", ghoulPreset);
            reg("abyssalcraft.omotholghoul", ghoulPreset);
            reg("abyssalcraft.omothol_ghoul", ghoulPreset);

            // Shoggoths (修格斯家族)
            HandPreset shoggothPreset = new HandPreset("rArm01a", "lArm01a");
            reg("abyssalcraft:lessershoggoth", shoggothPreset);
            reg("abyssalcraft:lesser_shoggoth", shoggothPreset);
            reg("abyssalcraft.lessershoggoth", shoggothPreset);
            reg("abyssalcraft.lesser_shoggoth", shoggothPreset);
            reg("abyssalcraft:shoggoth", shoggothPreset);
            reg("abyssalcraft.shoggoth", shoggothPreset);
            reg("abyssalcraft:greatershoggoth", shoggothPreset);
            reg("abyssalcraft:greater_shoggoth", shoggothPreset);
            reg("abyssalcraft.greatershoggoth", shoggothPreset);
            reg("abyssalcraft.greater_shoggoth", shoggothPreset);

            // Shadow Monster (暗影怪物)
            HandPreset shadowMonsterPreset = new HandPreset("Rarm1", "Larm1");
            reg("abyssalcraft:shadowmonster", shadowMonsterPreset);
            reg("abyssalcraft:shadow_monster", shadowMonsterPreset);
            reg("abyssalcraft.shadowmonster", shadowMonsterPreset);
            reg("abyssalcraft.shadow_monster", shadowMonsterPreset);

            // Shadow Beast & Shadow Titan (暗影巨兽/泰坦)
            HandPreset shadowBeastPreset = new HandPreset("rarm1", "larm1");
            reg("abyssalcraft:shadowbeast", shadowBeastPreset);
            reg("abyssalcraft:shadow_beast", shadowBeastPreset);
            reg("abyssalcraft.shadowbeast", shadowBeastPreset);
            reg("abyssalcraft.shadow_beast", shadowBeastPreset);
            reg("abyssalcraft:shadowtitan", shadowBeastPreset);
            reg("abyssalcraft.shadowtitan", shadowBeastPreset);

            // Shadow Creature (暗影生物)
            HandPreset shadowCreaturePreset = new HandPreset("RightArm2", "LeftArm2");
            reg("abyssalcraft:shadowcreature", shadowCreaturePreset);
            reg("abyssalcraft:shadow_creature", shadowCreaturePreset);
            reg("abyssalcraft.shadowcreature", shadowCreaturePreset);
            reg("abyssalcraft.shadow_creature", shadowCreaturePreset);

            // Sacthoth (萨克索斯 / 暗影首领: 解剖右手为负X leftarm1)
            HandPreset sacthothPreset = new HandPreset("leftarm1", "rightarm1");
            reg("abyssalcraft:shadowboss", sacthothPreset);
            reg("abyssalcraft.shadowboss", sacthothPreset);
            reg("abyssalcraft:sacthoth", sacthothPreset);
            reg("abyssalcraft.sacthoth", sacthothPreset);

            // Skeleton Goliath (歌利亚巨型骷髅)
            HandPreset gskeletonPreset = new HandPreset("rightarm", "leftarm");
            reg("abyssalcraft:gskeleton", gskeletonPreset);
            reg("abyssalcraft.gskeleton", gskeletonPreset);
            reg("abyssalcraft:skeleton_goliath", gskeletonPreset);
            reg("abyssalcraft.skeleton_goliath", gskeletonPreset);

            // Gatekeeper Minion / J'zahar Minion (守门人/扎哈尔仆从)
            HandPreset minionPreset = new HandPreset("rightarm1", "leftarm1");
            reg("abyssalcraft:jzaharminion", minionPreset);
            reg("abyssalcraft.jzaharminion", minionPreset);
            reg("abyssalcraft:gatekeeper_minion", minionPreset);
            reg("abyssalcraft.gatekeeper_minion", minionPreset);

            // Remnants (奥穆索遗民)
            HandPreset remnantPreset = new HandPreset("rightarm", "leftarm");
            reg("abyssalcraft:remnant", remnantPreset);
            reg("abyssalcraft.remnant", remnantPreset);
            HandPreset remnantTraderPreset = new HandPreset("villagerArms0", "villagerArms1");
            reg("abyssalcraft:remnanttrader", remnantTraderPreset);
            reg("abyssalcraft.remnanttrader", remnantTraderPreset);

            // Dreadlings & Dread Spawns (恐惧仆从与恐惧幼体)
            HandPreset dreadlingPreset = new HandPreset("rightarm", "leftarm");
            reg("abyssalcraft:dreadling", dreadlingPreset);
            reg("abyssalcraft.dreadling", dreadlingPreset);
            reg("abyssalcraft:dreadguard", dreadlingPreset);
            reg("abyssalcraft.dreadguard", dreadlingPreset);

            HandPreset dreadSpawnPreset = new HandPreset("arm", "thing");
            reg("abyssalcraft:dreadspawn", dreadSpawnPreset);
            reg("abyssalcraft.dreadspawn", dreadSpawnPreset);
            reg("abyssalcraft:greaterdreadspawn", dreadSpawnPreset);
            reg("abyssalcraft.greaterdreadspawn", dreadSpawnPreset);

            // Golem constructs (深渊傀儡 / 恐惧傀儡)
            HandPreset golemPreset = new HandPreset("ironGolemRightArm", "ironGolemLeftArm");
            reg("abyssalcraft:dreadgolem", golemPreset);
            reg("abyssalcraft.dreadgolem", golemPreset);
            reg("abyssalcraft:abygolem", golemPreset);
            reg("abyssalcraft.abygolem", golemPreset);

            // Cha'garoth & Parts (查加洛斯之拳与触手)
            HandPreset fistPreset = new HandPreset("arm1", "arm1");
            reg("abyssalcraft:chagarothfist", fistPreset);
            reg("abyssalcraft.chagarothfist", fistPreset);

            HandPreset chagarothPreset = new HandPreset("walltentacle5", "walltentacle1");
            reg("abyssalcraft:chagaroth", chagarothPreset);
            reg("abyssalcraft.chagaroth", chagarothPreset);

            HandPreset chagarothSpawnPreset = new HandPreset("rightleg1", "leftleg1");
            reg("abyssalcraft:chagarothspawn", chagarothSpawnPreset);
            reg("abyssalcraft.chagarothspawn", chagarothSpawnPreset);
            reg("abyssalcraft:lesserdreadbeast", chagarothSpawnPreset);
            reg("abyssalcraft.lesserdreadbeast", chagarothSpawnPreset);

            // Shub-Niggurath Offspring (黑山羊幼崽)
            HandPreset shubPreset = new HandPreset("rLeg01a", "lLeg01a");
            reg("abyssalcraft:shuboffspring", shubPreset);
            reg("abyssalcraft.shuboffspring", shubPreset);

            // Dragon Boss & Dragon Minion (恐惧之龙阿索拉与飞龙仆从)
            HandPreset dragonPreset = new HandPreset("frontLeg", "frontLeg");
            reg("abyssalcraft:dragonboss", dragonPreset);
            reg("abyssalcraft.dragonboss", dragonPreset);
            reg("abyssalcraft:dragonminion", dragonPreset);
            reg("abyssalcraft.dragonminion", dragonPreset);

            // Anti-Bat (反物质蝙蝠双翼)
            HandPreset batPreset = new HandPreset("batRightWing", "batLeftWing");
            reg("abyssalcraft:antibat", batPreset);
            reg("abyssalcraft.antibat", batPreset);

            // Evil / Demon / Anti 四足动物 (前肢绑定)
            HandPreset quadPreset = new HandPreset("leg3", "leg4");
            reg("abyssalcraft:evilsheep", quadPreset);
            reg("abyssalcraft:demonsheep", quadPreset);
            reg("abyssalcraft:evilpig", quadPreset);
            reg("abyssalcraft:demonpig", quadPreset);
            reg("abyssalcraft:anti_pig", quadPreset);
            reg("abyssalcraft:antipig", quadPreset);
            reg("abyssalcraft:anticow", quadPreset);
            reg("abyssalcraft:anti_cow", quadPreset);
            reg("abyssalcraft:demoncow", quadPreset);
            reg("abyssalcraft:evilcow", quadPreset);
            reg("abyssalcraft:anticreeper", quadPreset);
            reg("abyssalcraft:anti_creeper", quadPreset);

            // Anti-Spider (前节肢)
            HandPreset spiderPreset = new HandPreset("leg1", "leg2");
            reg("abyssalcraft:antispider", spiderPreset);
            reg("abyssalcraft:anti_spider", spiderPreset);

            // Anti / Demon / Evil Chicken (双翼)
            HandPreset chickenPreset = new HandPreset("rightWing", "leftWing");
            reg("abyssalcraft:antichicken", chickenPreset);
            reg("abyssalcraft:demonchicken", chickenPreset);
            reg("abyssalcraft:evilchicken", chickenPreset);

            org.apache.logging.log4j.LogManager.getLogger("morphext-abyssalcraft").info("[morphext-abyssalcraft] Successfully registered AbyssalCraft 3D model bone bindings into GenericHandRegistry.");
        } catch (Throwable t) {
            org.apache.logging.log4j.LogManager.getLogger("morphext-abyssalcraft").warn("Failed to register AC hand presets", t);
        }
    }
}
