package aaaa0ggmc.morphext.abyssalcraft.entity;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
import com.shinoow.abyssalcraft.api.entity.IOmotholEntity;
import com.shinoow.abyssalcraft.common.blocks.BlockRitualAltar;
import com.shinoow.abyssalcraft.common.blocks.BlockRitualPedestal;
import com.shinoow.abyssalcraft.common.entity.EntityBlackHole;
import com.shinoow.abyssalcraft.common.entity.EntityImplosion;
import com.shinoow.abyssalcraft.init.InitHandler;
import com.shinoow.abyssalcraft.lib.ACConfig;
import com.shinoow.abyssalcraft.lib.ACLib;
import com.shinoow.abyssalcraft.lib.world.TeleporterDarkRealm;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.capabilities.morphing.IMorphing;
import mchorse.metamorph.capabilities.morphing.Morphing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;

/**
 * 变身扎哈尔安全湮灭黑洞 (Morph Safe EntityBlackHole):
 * 1. 完美继承并还原原生 EntityBlackHole 的 3D 旋转模型、引力吸附与 FallingBlock 撕裂特效；
 * 2. 核心防御：施法者宿主玩家、队友、深渊古神变身玩家享有 100% 引力免疫、伤害免疫与异次元放逐豁免；
 * 3. 彻底根除发射黑洞时宿主被强行吸入暴毙或流放暗黑领域的致命死穴。
 */
public class MorphEntityBlackHole extends EntityBlackHole {

    private UUID ownerUUID;
    private double pullSpeed = 0.05D;

    public MorphEntityBlackHole(World worldIn) {
        super(worldIn);
    }

    public MorphEntityBlackHole(World worldIn, EntityLivingBase caster) {
        super(worldIn);
        if (caster != null) {
            this.ownerUUID = caster.getUniqueID();
            BossNativeHelper.tagOwner(this, caster);
        }
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        if (ticksExisted > 300) {
            setDead();
            return;
        }

        EntityLivingBase owner = getOwner();

        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().grow(48.0D))) {
            // 关键安全判定：豁免宿主、队友与深渊古神！
            if (isExempt(entity, owner)) continue;

            if (entity.isEntityAlive() && entity instanceof EntityLivingBase) {
                if (entity.posY < posY) {
                    entity.motionY += 0.025F;
                }
                double d1 = 7.0D;
                double d2 = posX + (rand.nextDouble() * 8.0D - 4.0D) - entity.posX;
                double d3 = posY + (rand.nextDouble() * 8.0D - 4.0D) - entity.posY;
                double d4 = posZ + (rand.nextDouble() * 8.0D - 4.0D) - entity.posZ;
                double d5 = d2 * d2 + d3 * d3 + d4 * d4;
                if (d5 > 0.001D) {
                    entity.addVelocity(d2 / d5 * d1 * pullSpeed, d3 / d5 * d1 * pullSpeed, d4 / d5 * d1 * pullSpeed);
                }

                if (ACConfig.hardcoreMode && entity.getDistanceSq(this) <= 36.0D) {
                    entity.attackEntityFrom(DamageSource.OUT_OF_WORLD, 4.0F);
                }

                if (entity.getDistanceSq(this) <= 9.0D && !world.isRemote) {
                    banishHostile((EntityLivingBase) entity);
                }
            } else if (!(entity instanceof EntityBlackHole) && !(entity instanceof EntityImplosion)) {
                // 原生非生物物体（方块碎片等）引力拉扯
                double d1 = 7.0D;
                double d2 = posX + (rand.nextDouble() * 4.0D - 2.0D) - entity.posX;
                double d3 = posY + (rand.nextDouble() * 4.0D - 2.0D) - entity.posY;
                double d4 = posZ + (rand.nextDouble() * 4.0D - 2.0D) - entity.posZ;
                double d5 = d2 * d2 + d3 * d3 + d4 * d4;
                if (d5 > 0.001D) {
                    entity.setPosition(entity.posX, entity.posY, entity.posZ);
                    entity.addVelocity(d2 / d5 * d1, d3 / d5 * d1, d4 / d5 * d1);
                }
                world.setEntityState(entity, (byte) 2);
                if (entity.getDistanceSq(this) <= 4.0D) {
                    entity.setDead();
                }
            }
        }

        pullSpeed += 0.0005D;

        if (!world.isRemote) {
            updateBlockSucking();
        }
    }

    private void updateBlockSucking() {
        int i = MathHelper.floor(posY);
        int i1 = MathHelper.floor(posX);
        int j1 = MathHelper.floor(posZ);
        for (int l1 = -6; l1 <= 6; l1++) {
            for (int i2 = -6; i2 <= 6; i2++) {
                for (int j = -6; j <= 6; j++) {
                    int j2 = i1 + l1;
                    int k = i + j;
                    int l = j1 + i2;
                    BlockPos pos = new BlockPos(j2, k, l);
                    IBlockState state = world.getBlockState(pos);
                    if (!state.getBlock().isAir(state, world, pos)
                            && rand.nextInt(10) == 0
                            && world.isAreaLoaded(getPosition().add(-32, -32, -32), getPosition().add(32, 32, 32))
                            && state.getBlock().getExplosionResistance(world, pos, this, null) < 600000
                            && !(state.getBlock() instanceof BlockRitualAltar)
                            && !(state.getBlock() instanceof BlockRitualPedestal)) {
                        if (state.getMaterial().isLiquid()) {
                            world.setBlockToAir(new BlockPos(j2, k, l));
                        } else {
                            world.spawnEntity(new EntityFallingBlock(world, j2, k + 0.5D, l, state));
                        }
                    }
                }
            }
        }
    }

    private void banishHostile(EntityLivingBase livingBase) {
        int id = InitHandler.INSTANCE.getRandomDimension(livingBase.dimension, rand);

        if (livingBase.timeUntilPortal <= 0) {
            if (livingBase instanceof EntityPlayerMP) {
                EntityPlayerMP thePlayer = (EntityPlayerMP) livingBase;
                thePlayer.timeUntilPortal = ACConfig.portalCooldown;
                if (ForgeHooks.onTravelToDimension(thePlayer, id)) {
                    thePlayer.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 255));
                    thePlayer.setPosition(rand.nextInt(100), 80, rand.nextInt(100));
                    thePlayer.server.getPlayerList().transferPlayerToDimension(thePlayer, id, new TeleporterDarkRealm(thePlayer.server.getWorld(id)));
                }
            } else {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                livingBase.timeUntilPortal = livingBase.getPortalCooldown();
                if (ForgeHooks.onTravelToDimension(livingBase, id)) {
                    int dim = livingBase.dimension;
                    livingBase.dimension = ACLib.omothol_id;
                    world.removeEntityDangerously(livingBase);
                    livingBase.isDead = false;
                    livingBase.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 80, 255));
                    livingBase.setPosition(rand.nextInt(100), 80, rand.nextInt(100));
                    server.getPlayerList().transferEntityToWorld(livingBase, dim, server.getWorld(dim), server.getWorld(id), new TeleporterDarkRealm(server.getWorld(id)));
                }
            }
        } else {
            livingBase.timeUntilPortal = livingBase.getPortalCooldown();
        }

        if (ACConfig.hardcoreMode) {
            if (livingBase instanceof EntityPlayer) {
                // A18：morph 释放的黑洞对玩家走可玩性伤害档（≤15），不做 20+50 秒杀
                // 与 setDead 清场；NPC 保留原生 hardcore 行为
                livingBase.attackEntityFrom(DamageSource.CRAMMING.setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(), 15.0F);
            } else {
                livingBase.setHealth(livingBase.getHealth() - 20.0F);
                livingBase.attackEntityFrom(DamageSource.CRAMMING.setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(), 50.0F);
                if (!livingBase.isEntityAlive()) {
                    if (livingBase instanceof EntityLiving) {
                        ((EntityLiving) livingBase).spawnExplosionParticle();
                    }
                    livingBase.setDead();
                }
            }
        }
    }

    public boolean isExempt(Entity entity, EntityLivingBase owner) {
        if (entity == null) return true;
        if (entity == owner || (owner != null && BossNativeHelper.isOwnedBy(entity, owner))) return true;

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player == owner || (owner != null && player.isOnSameTeam(owner))) return true;

            IMorphing morphing = Morphing.get(player);
            if (morphing != null && morphing.isMorphed()) {
                AbstractMorph morph = morphing.getCurrentMorph();
                if (morph != null && morph.name != null) {
                    String n = morph.name.toLowerCase();
                    if (n.contains("jzahar") || n.contains("sacthoth") || n.contains("omothol")
                            || n.contains("shoggoth") || n.contains("chagaroth") || n.contains("dragonboss")) {
                        return true;
                    }
                }
            }
        }

        if (entity instanceof IOmotholEntity || entity.getClass().getName().contains("Shoggoth")) {
            return true;
        }

        return false;
    }

    public EntityLivingBase getOwner() {
        if (ownerUUID != null) {
            return world.getPlayerEntityByUUID(ownerUUID);
        }
        return BossNativeHelper.getOwner(this);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (ownerUUID != null) {
            compound.setString("OwnerUUID", ownerUUID.toString());
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("OwnerUUID")) {
            try {
                ownerUUID = UUID.fromString(compound.getString("OwnerUUID"));
            } catch (Exception ignored) {}
        }
    }
}
