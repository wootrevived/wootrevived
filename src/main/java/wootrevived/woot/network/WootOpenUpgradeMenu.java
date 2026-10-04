package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import wootrevived.woot.Woot;
import wootrevived.woot.blocks.heart.HeartBlockEntity;

public record WootOpenUpgradeMenu(BlockPos blockPos, int slot) implements CustomPacketPayload {
    public static final ResourceLocation ID = Woot.location("woot_open_upgrade_menu");

    public static WootOpenUpgradeMenu read(FriendlyByteBuf buf) {
        return new WootOpenUpgradeMenu(buf.readBlockPos(), buf.readInt());
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> {
            Player sender = ctx.player().orElse(null);
            if (!(sender instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(blockPos);
            if (blockEntity instanceof HeartBlockEntity heartBlockEntity && heartBlockEntity.canPlayerAccess(player)) {
                heartBlockEntity.handleUpgradeMenu(player, this);
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeInt(slot);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
