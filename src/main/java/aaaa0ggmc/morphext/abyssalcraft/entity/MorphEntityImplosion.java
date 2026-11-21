package aaaa0ggmc.morphext.abyssalcraft.entity;

import aaaa0ggmc.morphext.abyssalcraft.action.BossNativeHelper;
import com.shinoow.abyssalcraft.common.entity.EntityBlackHole;
import com.shinoow.abyssalcraft.common.entity.EntityImplosion;
import com.shinoow.abyssalcraft.lib.ACConfig;
import com.shinoow.abyssalcraft.lib.ACSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.List;
import java.util.UUID;

/**
 * 变身扎哈尔安全奇点内爆 (Morph Safe EntityImplosion):
 * 1. 还原原生 360 tick 内爆音画、末影龙破空特效与范围雷击；
 * 2. 宿主玩家、队友与古神变身 100% 免疫引力拉扯与 100 点致死雷击自伤。
 */
public class MorphEntityImplosion extends EntityImplosion implements IEntityAdditionalSpawnData {

    private UUID ownerUUID;
    private double pullSpeed = 0.0D;
    private double size = 32.0D;

    public MorphEntityImplosion(World worldIn) {
        super(worldIn);
    }

    public MorphEntityImplosion(World worldIn, EntityLivingBase caster) {
        super(worldIn);
        if (caster != null) {
            this.ownerUUID = caster.getUniqueID();
            BossNativeHelper.tagOwner(this, caster);
        }
        // M5：寿命 360t=18s 保持现值，改走 TimedSummons 同口径持久时效
        //（ulSummonUntil 世界总 tick，随存档持久，重载后寿命不重计）
        BossNativeHelper.markSummonExpiry(this, 360);
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        EntityLivingBase owner = getOwner();
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().grow(size, size, size));

        int multiplier = ACConfig.hardcoreMode ? 2 : 1;

        // M5：引爆时机优先按持久时效到期触发（服务端权威，重登/重载不延后引爆）；
        // ticksExisted > 360 保留为无害兜底（不持久，仅连续加载时生效）
        if (BossNativeHelper.isSummonExpired(this) || ticksExisted > 360) {
            world.playEvent(3000, getPosition(), 0);
            playSound(ACSounds.jzahar_blast, 5.0F, 1.0F);

            for (Entity entity : list) {
                if (isExempt(entity, owner)) continue;

                double scale = (size - entity.getDistance(posX, posY, posZ)) / size;
                Vec3d dir = new Vec3d(entity.posX - posX, entity.posY - posY, entity.posZ - posZ).normalize();

                if (entity.isEntityAlive()) {
                    if (ACConfig.hardcoreMode && entity.getDistanceSq(this) <= 25.0D) {
                        if (entity instanceof EntityPlayer) {
                            // A18：morph 释放的内爆对玩家走可玩性伤害档（≤15），
                            // 不重置无敌帧、不做 100 秒杀；NPC 保留原生 hardcore 行为
                            entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 15.0F);
                        } else {
                            entity.hurtResistantTime = 0;
                            entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 100.0F);
                        }
                    }
                    entity.addVelocity(dir.x * 2.5D * scale, 1.5D + rand.nextDouble(), dir.z * 2.5D * scale);
                }
            }

            setDead();
            return;
        }

        if (ticksExisted == 1) {
            playSound(ACSounds.jzahar_charge, 5.0F, 1.0F);
        }

        for (Entity entity : list) {
            if (isExempt(entity, owner)) continue;

            double scale = (size - entity.getDistance(posX, posY, posZ)) / size;
            Vec3d dir = new Vec3d(entity.posX - posX, entity.posY - posY, entity.posZ - posZ).normalize();

            if (entity.isEntityAlive()) {
                entity.addVelocity(dir.x * -ticksExisted * 0.0005D * scale, dir.y * -ticksExisted * 0.0005D * scale, dir.z * -ticksExisted * 0.0005D * scale);
                if (entity.getDistanceSq(this) <= 4.0D) {
                    entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 4.0F * multiplier);
                }
            }
        }

        pullSpeed += 0.0001D;
    }

    /**
     * FVP-28：豁免判定改问阵营图（见 {@link BossNativeHelper#isFactionExempt}）。
     *
     * <p>改前：与 {@link MorphEntityBlackHole#isExempt} 完全重复的
     * morph.name 六子串 + {@code IOmotholEntity} + {@code contains("Shoggoth")} 硬判。
     * 现已合并为 BossNativeHelper 的单一真值，两份复制删除。</p>
     */
    public boolean isExempt(Entity entity, EntityLivingBase owner) {
        return BossNativeHelper.isFactionExempt(entity, owner);
    }

    public EntityLivingBase getOwner() {
        if (ownerUUID != null) {
            // M5：跨维解析——world.getPlayerEntityByUUID 只查本维度玩家列表，
            // 主人换维度后解析为 null、豁免失效（根因修复）；见 BossNativeHelper.findPlayerAcrossDims
            return BossNativeHelper.findPlayerAcrossDims(world, ownerUUID);
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

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        BossNativeHelper.writeUuid(buffer, ownerUUID);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.ownerUUID = BossNativeHelper.readUuid(additionalData);
    }
}
