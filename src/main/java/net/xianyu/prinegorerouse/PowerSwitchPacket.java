package net.xianyu.prinegorerouse;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkEvent;
import net.xianyu.prinegorerouse.aquatime.AquaTimeData;
import net.xianyu.prinegorerouse.energy.FEBladeStorage;

import java.util.function.Supplier;

public class PowerSwitchPacket {
    private final String message;

    public PowerSwitchPacket(FriendlyByteBuf buffer) {
        message = buffer.readUtf(Short.MAX_VALUE);
    }

    public PowerSwitchPacket(String message) {
        this.message = message;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.message);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player.isSpectator())
                return;

            ItemStack mainHandItem = player.getMainHandItem();
            if (!mainHandItem.getCapability(ItemSlashBlade.BLADESTATE).isPresent())
                return;

            mainHandItem.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                if (energy instanceof FEBladeStorage bladeFE) {
                    if (!bladeFE.isPowered()) {
                        // 开启电刀模式的启动耗电：优先扣 FE；电量不够时用时间素无缝衔接（不退同款兑换规则）
                        boolean paid = false;
                        if (bladeFE.extractEnergy(bladeFE.getPowerupExtract(), true) == bladeFE.getPowerupExtract()) {
                            bladeFE.extractEnergy(bladeFE.getPowerupExtract(), false);
                            paid = true;
                        } else if (AquaTimeData.get(player) >= bladeFE.getPowerupExtract()) {
                            AquaTimeData.add(player, -bladeFE.getPowerupExtract());
                            paid = true;
                        }
                        if (paid) {
                            bladeFE.setPowered(true);
                            ServerLevel serverLevel = player.serverLevel();
                            var random = serverLevel.random;
                            for (int i = 0; i < 32; ++i) {
                                double xDist = (random.nextFloat() * 2.0F - 1.0F);
                                double yDist = (random.nextFloat() * 2.0F - 1.0F);
                                double zDist = (random.nextFloat() * 2.0F - 1.0F);
                                if (!(xDist * xDist + yDist * yDist + zDist * zDist > 1.0D)) {
                                    double x = player.getX(xDist / 4.0D);
                                    double y = player.getY(0.5D + yDist / 4.0D);
                                    double z = player.getZ(zDist / 4.0D);
                                    serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 0, xDist, yDist + 0.2D, zDist,
                                            1);
                                }
                            }
                            player.playNotifySound(SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.5F, 1F);
                        } else {
                            bladeFE.setPowered(false);
                            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1F, 1F);
                        }
                    } else {
                        bladeFE.setPowered(false);
                        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1F, 1F);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

}
