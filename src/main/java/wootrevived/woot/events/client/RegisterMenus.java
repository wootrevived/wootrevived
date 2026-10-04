package wootrevived.woot.events.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import wootrevived.woot.Woot;
import wootrevived.woot.client.render.dye_liquifier.DyeLiquifierContainerScreen;
import wootrevived.woot.client.render.enchanted_liquifier.EnchantedLiquifierContainerScreen;
import wootrevived.woot.client.render.fluid_infuser.FluidInfuserContainerScreen;
import wootrevived.woot.client.render.heart.HeartContainerScreen;
import wootrevived.woot.client.render.item_infuser.ItemInfuserContainerScreen;
import wootrevived.woot.registries.BlocksRegistry;
import wootrevived.woot.upgrades.filter.Filter;
import wootrevived.woot.upgrades.filter.FilterScreen;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Woot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = { Dist.CLIENT })
public class RegisterMenus {
    @SubscribeEvent
    public static void registerMenus(RegisterMenuScreensEvent event) {
        event.register(BlocksRegistry.ITEM_INFUSER_BLOCK_MENU.get(), ItemInfuserContainerScreen::new);
        event.register(BlocksRegistry.DYE_LIQUIFIER_BLOCK_MENU.get(), DyeLiquifierContainerScreen::new);
        event.register(BlocksRegistry.ENCHANTED_LIQUIFIER_BLOCK_MENU.get(), EnchantedLiquifierContainerScreen::new);
        event.register(BlocksRegistry.FLUID_INFUSER_BLOCK_MENU.get(), FluidInfuserContainerScreen::new);
        event.register(BlocksRegistry.HEART_BLOCK_MENU.get(), HeartContainerScreen::new);

        MenuScreens.register(Filter.FILTER_ITEM_MENU.get(), FilterScreen::new);
    }
}
