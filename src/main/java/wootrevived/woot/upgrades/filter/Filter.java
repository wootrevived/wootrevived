package wootrevived.woot.upgrades.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
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

public class Filter extends WootUpgradeItem<UpgradeNoVariant> {
    public Filter() {
        super(new Properties(), UpgradeNoVariant.NONE);
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
    public @Nullable AbstractContainerMenu createMenu(int containerId, BlockPos blockPos, @NotNull CompoundTag itemTag, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new FilterMenu(containerId, playerInventory, blockPos, itemTag);
    }

    @Override
    public void modifyDrops(@NotNull WootDropsProperties properties, @NotNull CompoundTag itemTag) {
        FilterLogic logic = FilterLogic.fromTag(itemTag);

        logic.filterItems(properties.getItemDrops());
        logic.filterFluids(properties.getFluidDrops());
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
    public static final DeferredHolder<Item, Filter> FILTER_ITEM = ITEMS.register(FILTER_TAG, Filter::new);
    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER_ITEM_MENU = MENU_TYPES.register(FILTER_TAG, () -> IMenuTypeExtension.create((FilterMenu::new)));

}
