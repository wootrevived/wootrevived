package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import wootrevived.woot.Woot;
import wootrevived.woot.blocks.heart.HeartBlockEntity;

public record WootOpenUpgradeMenu(BlockPos blockPos, int slot) implements CustomPacketPayload {
    public static final Identifier ID = Woot.identifier("woot_open_upgrade_menu");
    public static final Type<WootOpenUpgradeMenu> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, WootOpenUpgradeMenu> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WootOpenUpgradeMenu::blockPos,
            ByteBufCodecs.INT, WootOpenUpgradeMenu::slot,
            WootOpenUpgradeMenu::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(final WootOpenUpgradeMenu pkt, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player sender = ctx.player();
            if (!(sender instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(pkt.blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(pkt.blockPos);
            if (blockEntity instanceof HeartBlockEntity heartBlockEntity && heartBlockEntity.canPlayerAccess(player)) {
                heartBlockEntity.handleUpgradeMenu(player, pkt);
            }
        });
    }
}
