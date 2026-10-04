package wootrevived.woot.upgrades.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wootrevived.api.menus.WootUpgradeItemMenu;
import wootrevived.woot.registries.ComponentsRegistry;
import wootrevived.woot.util.render.WootSlot;

public class FilterMenu extends WootUpgradeItemMenu {
    protected FilterLogic filterLogic;

    public FilterMenu(int containerId, Inventory inv, BlockPos blockPos, ItemStack itemStack) {
        super(Filter.FILTER_ITEM_MENU.get(), containerId, inv, blockPos, itemStack);
        createPlayerInventory(inv);

        filterLogic = getFilterLogic();
    }

    public FilterMenu(int containerId, Inventory inv, FriendlyByteBuf data) {
        super(Filter.FILTER_ITEM_MENU.get(), containerId, inv, data, Filter.FILTER_ITEM.get().getDefaultInstance());
        createPlayerInventory(inv);

        filterLogic = getFilterLogic();
    }

    private void createPlayerInventory(Inventory playerInventory) {
        for(int k = 0; k < 9; k++){
            this.addSlot(new WootSlot(playerInventory, k, 8 + k * 18, 160));
        }
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 9; j++){
                this.addSlot(new WootSlot(playerInventory, j + i * 9 + 9, 8 + j * 18, 102 + i * 18));
            }
        }
    }

    public FilterLogic getLogic(){
        return filterLogic;
    }

    public void saveLogic(){
        itemStack.set(ComponentsRegistry.FILTER_LOGIC_DATA.get(), filterLogic.toComponent());
        syncItemStackComponents();
    }

    protected FilterLogic getFilterLogic() {
        FilterLogic.Component component = itemStack.get(ComponentsRegistry.FILTER_LOGIC_DATA.get());

        if(component == null)
            return FilterLogic.empty();

        return FilterLogic.fromComponent(component);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }
}
