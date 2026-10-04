package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import wootrevived.woot.blocks.factory_upgrade.FactoryUpgradeBlockEntity;

import java.util.function.Supplier;

public record WootUpgradeItemUpdate(BlockPos blockPos, CompoundTag itemTag) {
    public static WootUpgradeItemUpdate decode(FriendlyByteBuf buf) {
        return new WootUpgradeItemUpdate(buf.readBlockPos(), buf.readNbt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeNbt(itemTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.level() == null) return;
            if (!sender.level().isLoaded(blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(blockPos);
            if (blockEntity instanceof FactoryUpgradeBlockEntity factoryUpgradeBlockEntity && factoryUpgradeBlockEntity.canPlayerAccess(sender)) {
                factoryUpgradeBlockEntity.handleNewState(this);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sync(BlockPos blockPos, CompoundTag itemTag) {
        NetworkChannel.channel.sendToServer(new WootUpgradeItemUpdate(blockPos, itemTag));
    }
}
