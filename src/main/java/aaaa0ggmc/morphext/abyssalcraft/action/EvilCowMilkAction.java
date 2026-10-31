package aaaa0ggmc.morphext.abyssalcraft.action;

import mchorse.metamorph.api.morphs.AbstractMorph;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * 邪恶牛专属动作：诅咒挤奶与恶念冲撞 (ID: ac_evil_cow_milk)
 * 1. 手持空桶 (Items.BUCKET)：消耗空桶，给予深渊侵蚀的诅咒牛奶 (饮用清除药水状态但附加反胃与虚弱效果)，冷却 10s；
 * 2. 未手持空桶：发动恶念冲撞，向前猛烈突进，重创并虚弱沿途敌人，冷却 15s。
 */
public class EvilCowMilkAction extends AbyssalCraftAction {

    public EvilCowMilkAction() {
        super(200); // 默认基准冷却 10 秒
    }

    /**
     * 构建被深渊侵蚀的牛奶物品堆
     */
    public static ItemStack createCursedMilkStack() {
        ItemStack stack = new ItemStack(Items.MILK_BUCKET);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("CursedMilk", true);

        NBTTagCompound display = new NBTTagCompound();
        String name = I18n.translateToLocal("item.morphext_patch_abyssalcraft.cursed_milk.name");
        display.setString("Name", TextFormatting.DARK_PURPLE + (name.isEmpty() ? "被深渊侵蚀的牛奶" : name));

        NBTTagList lore = new NBTTagList();
        String desc = I18n.translateToLocal("item.morphext_patch_abyssalcraft.cursed_milk.desc");
        lore.appendTag(new NBTTagString(TextFormatting.GRAY + (desc.isEmpty() ? "清除药水状态，但深渊恶念将侵蚀心智..." : desc)));
        display.setTag("Lore", lore);

        tag.setTag("display", display);
        stack.setTagCompound(tag);
        return stack;
    }

    /**
     * 监听玩家或生物饮用完诅咒牛奶：原版牛奶清除药水后，立即施加深渊诅咒反胃与虚弱
     */
    @SubscribeEvent
    public static void onCursedMilkConsumed(LivingEntityUseItemEvent.Finish event) {
        ItemStack item = event.getItem();
        if (!item.isEmpty() && item.getItem() == Items.MILK_BUCKET) {
            if (item.hasTagCompound() && item.getTagCompound().getBoolean("CursedMilk")) {
                EntityLivingBase entity = event.getEntityLiving();
                if (entity != null && !entity.world.isRemote) {
                    entity.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 240, 0));
                    entity.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 300, 1));
                    entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                            SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.9F, 0.6F);
                }
            }
        }
    }

    @Override
    protected boolean executeServer(EntityLivingBase entity, AbstractMorph morph) {
        World world = entity.world;
        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        EnumHand hand = player != null ? findHeldItem(player, Items.BUCKET) : null;

        // === 1. 挤奶分支：深渊侵蚀牛奶 ===
        if (hand != null && player != null) {
            ItemStack cursedMilk = createCursedMilkStack();
            consumeAndGive(player, hand, cursedMilk);

            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.ENTITY_COW_MILK, SoundCategory.PLAYERS, 1.0F, 0.8F);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 0.8F, 0.6F);

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB,
                        entity.posX, entity.posY + 0.6D, entity.posZ,
                        25, 0.3D, 0.2D, 0.3D, 0.05D);
            }

            setCooldown(entity, 200); // 10 秒冷却
            return true;
        }

        // === 2. 未手持空桶：恶念冲撞 ===
        Vec3d look = entity.getLook(1.0F);
        entity.motionX += look.x * 1.3D;
        entity.motionY += 0.25D;
        entity.motionZ += look.z * 1.3D;
        entity.velocityChanged = true;

        AxisAlignedBB rushBox = entity.getEntityBoundingBox().grow(2.5D, 1.0D, 2.5D);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, rushBox,
                e -> e != entity && e.isEntityAlive() && !e.isOnSameTeam(entity));

        DamageSource ds = player != null ? DamageSource.causePlayerDamage(player) : DamageSource.causeMobDamage(entity);
        for (EntityLivingBase target : list) {
            target.attackEntityFrom(ds, 9.0F);
            target.knockBack(entity, 1.5F, -look.x, -look.z);
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2));
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 1));
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    35, 1.0D, 0.4D, 1.0D, 0.05D);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    15, 0.8D, 0.3D, 0.8D, 0.02D);
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_COW_HURT, SoundCategory.PLAYERS, 1.2F, 0.6F);
        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 0.9F, 0.8F);

        setCooldown(entity, 300); // 15 秒冷却
        return true;
    }
}
