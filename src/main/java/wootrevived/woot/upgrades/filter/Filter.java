package wootrevived.woot.upgrades.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wootrevived.api.WootUpgradeItem;
import wootrevived.api.enums.UpgradeNoVariant;
import wootrevived.api.interfaces.WootDropsProperties;
import wootrevived.api.registrations.WootUpgradeItemRegistration;
import wootrevived.woot.Woot;
import wootrevived.woot.registries.ComponentsRegistry;

public class Filter extends WootUpgradeItem<UpgradeNoVariant> {
    public Filter(String tag) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, Woot.location(tag))), UpgradeNoVariant.NONE);
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public @NotNull Component getMenuDisplayName() {
        return Component.translatable("item.woot_revived.filter_upgrade");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull BlockPos blockPos, @NotNull ItemStack itemStack, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new FilterMenu(containerId, playerInventory, blockPos, itemStack);
    }

    @Override
    public void modifyDrops(@NotNull WootDropsProperties properties, @NotNull MutableDataComponentHolder dataComponentHolder) {
        FilterLogic.Component component = dataComponentHolder.get(ComponentsRegistry.FILTER_LOGIC_DATA.get());

        if(component == null)
            return;

        FilterLogic logic = FilterLogic.fromComponent(component);

        logic.filterItems(properties.getItemDrops(), properties.getRegistryAccess());
        logic.filterFluids(properties.getFluidDrops(), properties.getRegistryAccess());
    }

    /* Upgrade Item registration */

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Woot.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, Woot.MOD_ID);


    public static void register(WootUpgradeItemRegistration registration){
        ITEMS.register(registration.getWootEventBus());
        MENU_TYPES.register(registration.getWootEventBus());
        registration.register(FILTER_ITEM);
    }

    public static final String FILTER_TAG = "filter_upgrade";
    public static final DeferredHolder<Item, Filter> FILTER_ITEM = ITEMS.register(FILTER_TAG, () -> new Filter(FILTER_TAG));
    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER_ITEM_MENU = MENU_TYPES.register(FILTER_TAG, () -> IMenuTypeExtension.create((FilterMenu::new)));

}
