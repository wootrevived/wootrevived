package wootrevived.woot.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import wootrevived.api.menus.WootUpgradeItemMenu;
import wootrevived.woot.Woot;
import wootrevived.woot.network.WootFakeSpawnerUpdate;
import wootrevived.woot.network.WootMachineUpdate;
import wootrevived.woot.network.WootOpenUpgradeMenu;
import wootrevived.woot.network.WootUpgradeItemUpdate;
import wootrevived.woot.registries.BlocksRegistry;

@EventBusSubscriber(modid = Woot.MOD_ID)
public class RegisterPayloadHandlers {
    @SubscribeEvent
    public static void registerPayloadHandler(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Woot.MOD_ID).versioned("1").optional();
        WootUpgradeItemMenu.SYNC = WootUpgradeItemUpdate::sync;
        WootUpgradeItemMenu.GET_FACTORY_BLOCK = BlocksRegistry.FACTORY_UPGRADE_BLOCK;

        registrar.playToServer(
                WootMachineUpdate.TYPE,
                WootMachineUpdate.STREAM_CODEC,
                WootMachineUpdate::handler
        );

        registrar.playToServer(
                WootFakeSpawnerUpdate.TYPE,
                WootFakeSpawnerUpdate.STREAM_CODEC,
                WootFakeSpawnerUpdate::handler
        );

        registrar.playToServer(
                WootOpenUpgradeMenu.TYPE,
                WootOpenUpgradeMenu.STREAM_CODEC,
                WootOpenUpgradeMenu::handler
        );

        registrar.playToServer(
                WootUpgradeItemUpdate.TYPE,
                WootUpgradeItemUpdate.STREAM_CODEC,
                WootUpgradeItemUpdate::handler
        );
    }
}
