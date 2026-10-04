package wootrevived.woot.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import wootrevived.api.menus.WootUpgradeItemMenu;
import wootrevived.woot.Woot;
import wootrevived.woot.registries.BlocksRegistry;

import java.util.Objects;
import java.util.Optional;

public class NetworkChannel {

    private static final ResourceLocation resourceLocation = Woot.location("net");

    public static void init(){
        WootUpgradeItemMenu.SYNC = WootUpgradeItemUpdate::sync;
        WootUpgradeItemMenu.GET_FACTORY_BLOCK = BlocksRegistry.FACTORY_UPGRADE_BLOCK;
    }

    public static SimpleChannel channel;
    static {
        channel = NetworkRegistry.ChannelBuilder.named(resourceLocation)
                .clientAcceptedVersions(s -> Objects.equals(s, "1"))
                .serverAcceptedVersions(s -> Objects.equals(s, "1"))
                .networkProtocolVersion(() -> "1")
                .simpleChannel();

        channel.registerMessage(
                0,
                WootMachineUpdate.class,
                WootMachineUpdate::encode,
                WootMachineUpdate::decode,
                WootMachineUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        channel.registerMessage(
                1,
                WootFakeSpawnerUpdate.class,
                WootFakeSpawnerUpdate::encode,
                WootFakeSpawnerUpdate::decode,
                WootFakeSpawnerUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        channel.registerMessage(
                2,
                WootUpgradeItemUpdate.class,
                WootUpgradeItemUpdate::encode,
                WootUpgradeItemUpdate::decode,
                WootUpgradeItemUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        channel.registerMessage(
                3,
                WootOpenUpgradeMenu.class,
                WootOpenUpgradeMenu::encode,
                WootOpenUpgradeMenu::decode,
                WootOpenUpgradeMenu::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
