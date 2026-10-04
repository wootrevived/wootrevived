package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import wootrevived.woot.Woot;
import wootrevived.woot.blocks.factory_upgrade.FactoryUpgradeBlockEntity;

public record WootUpgradeItemUpdate(BlockPos blockPos, DataComponentPatch componentsPatch) implements CustomPacketPayload {
    public static final ResourceLocation ID = Woot.location("woot_upgrade_item_update");
    public static final Type<WootUpgradeItemUpdate> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, WootUpgradeItemUpdate> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WootUpgradeItemUpdate::blockPos,
            DataComponentPatch.STREAM_CODEC, WootUpgradeItemUpdate::componentsPatch,
            WootUpgradeItemUpdate::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(final WootUpgradeItemUpdate pkt, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player sender = ctx.player();
            if (!(sender instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(pkt.blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(pkt.blockPos);
            if (blockEntity instanceof FactoryUpgradeBlockEntity factoryUpgradeBlockEntity && factoryUpgradeBlockEntity.canPlayerAccess(player)) {
                factoryUpgradeBlockEntity.handleNewState(pkt);
            }
        });
    }

    public static void sync(BlockPos blockPos, DataComponentPatch componentsPatch) {
        ClientPacketDistributor.sendToServer(new WootUpgradeItemUpdate(blockPos, componentsPatch));
    }
}
