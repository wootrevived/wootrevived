package wootrevived.woot.events.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
    public static void registerMenus(FMLClientSetupEvent event) {
        MenuScreens.register(BlocksRegistry.ITEM_INFUSER_BLOCK_MENU.get(), ItemInfuserContainerScreen::new);
        MenuScreens.register(BlocksRegistry.DYE_LIQUIFIER_BLOCK_MENU.get(), DyeLiquifierContainerScreen::new);
        MenuScreens.register(BlocksRegistry.ENCHANTED_LIQUIFIER_BLOCK_MENU.get(), EnchantedLiquifierContainerScreen::new);
        MenuScreens.register(BlocksRegistry.FLUID_INFUSER_BLOCK_MENU.get(), FluidInfuserContainerScreen::new);
        MenuScreens.register(BlocksRegistry.HEART_BLOCK_MENU.get(), HeartContainerScreen::new);

        MenuScreens.register(Filter.FILTER_ITEM_MENU.get(), FilterScreen::new);
    }
}
