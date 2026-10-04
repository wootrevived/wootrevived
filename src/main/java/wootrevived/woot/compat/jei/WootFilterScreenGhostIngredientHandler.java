package wootrevived.woot.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import wootrevived.woot.upgrades.filter.FilterScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WootFilterScreenGhostIngredientHandler implements IGhostIngredientHandler<FilterScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(FilterScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> list = new ArrayList<>();

        ingredient.getIngredient(VanillaTypes.ITEM_STACK)
                .ifPresent(stack -> list.add(this.getItemHoverAreaTarget(screen, i -> screen.onInsertJEIStack(stack))));
        ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)
                .ifPresent(stack -> list.add(this.getItemHoverAreaTarget(screen, i -> screen.onInsertJEIStack(stack))));

        return list;
    }

    private <I> Target<I> getItemHoverAreaTarget(FilterScreen screen, Consumer<I> consumer) {
        return new Target<I>() {
            @Override
            public Rect2i getArea() {
                if(screen.isInFilterListView())
                    return new Rect2i(screen.getGuiLeft() + FilterScreen.CHOSE_ADD_BUTTON_X + 1, screen.getGuiTop() + FilterScreen.CHOSE_ADD_BUTTON_Y + 1, 12, 12);

                return new Rect2i(screen.getGuiLeft() + FilterScreen.EDIT_SLOT_X + 1, screen.getGuiTop() + FilterScreen.EDIT_SLOT_Y + 1, 16, 16);
            }

            @Override
            public void accept(I ingredient) {
                consumer.accept(ingredient);
            }
        };
    }

    @Override
    public void onComplete() {

    }
}
