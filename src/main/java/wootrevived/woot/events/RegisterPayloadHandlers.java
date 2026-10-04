package wootrevived.woot.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import wootrevived.api.menus.WootUpgradeItemMenu;
import wootrevived.woot.Woot;
import wootrevived.woot.network.WootFakeSpawnerUpdate;
import wootrevived.woot.network.WootMachineUpdate;
import wootrevived.woot.network.WootOpenUpgradeMenu;
import wootrevived.woot.network.WootUpgradeItemUpdate;
import wootrevived.woot.registries.BlocksRegistry;

@Mod.EventBusSubscriber(modid = Woot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterPayloadHandlers {
    @SubscribeEvent
    public static void registerPayloadHandler(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(Woot.MOD_ID).versioned("1").optional();
        WootUpgradeItemMenu.SYNC = WootUpgradeItemUpdate::sync;
        WootUpgradeItemMenu.GET_FACTORY_BLOCK = BlocksRegistry.FACTORY_UPGRADE_BLOCK;

        registrar.play(WootMachineUpdate.ID, WootMachineUpdate::read, handler -> handler.server(WootMachineUpdate::handle));
        registrar.play(WootFakeSpawnerUpdate.ID, WootFakeSpawnerUpdate::read, handler -> handler.server(WootFakeSpawnerUpdate::handle));
        registrar.play(WootOpenUpgradeMenu.ID, WootOpenUpgradeMenu::read, handler -> handler.server(WootOpenUpgradeMenu::handle));
        registrar.play(WootUpgradeItemUpdate.ID, WootUpgradeItemUpdate::read, handler -> handler.server(WootUpgradeItemUpdate::handle));
    }
}
