package wootrevived.woot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import wootrevived.woot.Woot;
import wootrevived.woot.blocks.factory_upgrade.FactoryUpgradeBlockEntity;

public record WootUpgradeItemUpdate(BlockPos blockPos, CompoundTag itemTag) implements CustomPacketPayload {
    public static final ResourceLocation ID = Woot.location("woot_upgrade_item_update");

    public static WootUpgradeItemUpdate read(FriendlyByteBuf buf) {
        return new WootUpgradeItemUpdate(buf.readBlockPos(), buf.readNbt());
    }

    public void handle(PlayPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> {
            Player sender = ctx.player().orElse(null);
            if (!(sender instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(blockPos)) return;

            BlockEntity blockEntity = sender.level().getBlockEntity(blockPos);
            if (blockEntity instanceof FactoryUpgradeBlockEntity factoryUpgradeBlockEntity && factoryUpgradeBlockEntity.canPlayerAccess(player)) {
                factoryUpgradeBlockEntity.handleNewState(this);
            }
        });
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeNbt(itemTag);
    }

    public static void sync(BlockPos blockPos, CompoundTag itemTag) {
        PacketDistributor.SERVER.noArg().send(new WootUpgradeItemUpdate(blockPos, itemTag));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
