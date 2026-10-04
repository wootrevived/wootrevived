package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import wootrevived.woot.blocks.heart.HeartBlockEntity;

import java.util.function.Supplier;

public record WootOpenUpgradeMenu(BlockPos blockPos, int slot) {
    public static WootOpenUpgradeMenu decode(FriendlyByteBuf buf) {
        return new WootOpenUpgradeMenu(buf.readBlockPos(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.level() == null) return;
            if (!sender.level().isLoaded(blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(blockPos);
            if (blockEntity instanceof HeartBlockEntity heartBlockEntity && heartBlockEntity.canPlayerAccess(sender)) {
                heartBlockEntity.handleUpgradeMenu(sender, this);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
