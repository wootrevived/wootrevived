package wootrevived.woot.upgrades.filter;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import wootrevived.woot.events.client.GlobalClientTicker;
import wootrevived.woot.util.render.WootContainerScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilterList extends AbstractWidget {
    protected OnSelect onSelect;
    protected FilterLogic logic;

    public FilterList(int width) {
        super(0, 0, width, HEIGHT, Component.empty());
    }

    public static int HEIGHT = 62;
    public static int TILE_HEIGHT = 20;
    public static int TAG_DISPLAY_TICKS = 20;

    private int scrollIndex = 0;
    private double scrollHeight = 0;

    private int selected = -1;

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if(active){
            gui.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF8B8B8B);
            gui.fill(getX(), getY(), getX() + getWidth() - 1, getY() + getHeight() - 1, 0xFF363637);
            gui.fill(getX() + 1, getY() + getHeight() - 1,  getX() + getWidth(), getY() + getHeight(), 0xFFFFFFFF);
            gui.fill(getX() + getWidth() - 1, getY() + 1,  getX() + getWidth(), getY() + getHeight(), 0xFFFFFFFF);
            gui.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xFFAEAEAE);
            gui.fill(getX() + getWidth() - 12, getY(), getX() + getWidth() - 11, getY() + getHeight() - 1, 0xFF363637);

            gui.blit(RenderPipelines.GUI_TEXTURED, WootContainerScreen.GUI, getX() + getWidth() - 11, getY() + 1 + (int)Math.round(43 * scrollHeight), 245, 132, 10, 17, WootContainerScreen.ATLAS_WIDTH, WootContainerScreen.ATLAS_HEIGHT);

            int len = logic.size();
            for(int i = 0; i < 3; i++){
                if(scrollIndex + i > len - 1) continue;

                gui.fill(getX() + 1, getY() + TILE_HEIGHT * i + 1, getX() + getWidth() - 12, getY() + TILE_HEIGHT * (i + 1) + 1, selected == scrollIndex + i ? 0xFF000000 : 0xFF555555);
                gui.fill(getX() + 2, getY() + TILE_HEIGHT * i + 2, getX() + getWidth() - 13, getY() + TILE_HEIGHT * (i + 1), selected == scrollIndex + i ? 0xFF8E8E8E : 0xFFC6C6C6);

                FilterLogic.FilterResource resource = logic.get(scrollIndex + i);
                renderResource(gui, resource, getX() + 3, getY() + TILE_HEIGHT * i + 3);
                renderResourceLabel(gui, resource, getX() + 22, getY() + TILE_HEIGHT * i + 4);
            }

            if(mouseX > getX() + 1 &&
                    mouseX < getX() + getWidth() - 12 &&
                    mouseY > getY() + 1 &&
                    mouseY < getY() + getHeight() - 1) {
                int hovered = scrollIndex + (mouseY - getY() - 1) / TILE_HEIGHT;
                renderResourceTooltip(gui, mouseX, mouseY, hovered);
            }
        }
    }

    protected void renderResource(@NotNull GuiGraphics gui, FilterLogic.FilterResource resource, int x, int y){
        if(resource.getTag()){
            if(resource.getType() == FilterLogic.FilterType.ITEM){
                Item item = getItemFromTag(resource.getLocation());
                if(item != null){
                    gui.renderItem(applyComponents(new ItemStack(item), resource.getNBT()), x, y);
                    return;
                }
            } else {
                Fluid fluid = getFluidFromTag(resource.getLocation());
                if(fluid != null){
                    renderFluid(gui, x, y, applyComponents(new FluidStack(fluid, 1000), resource.getNBT()));
                    return;
                }
            }

            gui.drawString(WootContainerScreen.getMCFont(), "#", x + 5, y + 4, 0xFF363637, false);
            return;
        }

        if(resource.getType() == FilterLogic.FilterType.ITEM){
            Item item = getItem(resource.getLocation());
            if(item != null)
                gui.renderItem(applyComponents(new ItemStack(item), resource.getNBT()), x, y);
        } else {
            Fluid fluid = getFluid(resource.getLocation());
            if(fluid != null)
                renderFluid(gui, x, y, applyComponents(new FluidStack(fluid, 1000), resource.getNBT()));
        }
    }

    public static ItemStack applyComponents(ItemStack stack, @Nullable CompoundTag tag){
        if(tag != null)
            stack.applyComponents(getPatch(tag));

        return stack;
    }

    public static FluidStack applyComponents(FluidStack stack, @Nullable CompoundTag tag){
        if(tag != null)
            stack.applyComponents(getPatch(tag));

        return stack;
    }

    public static DataComponentPatch getPatch(CompoundTag tag){
        Minecraft minecraft = Minecraft.getInstance();
        return DataComponentPatch.CODEC.decode(minecraft.level == null ? NbtOps.INSTANCE : minecraft.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag)
                .result()
                .map(Pair::getFirst)
                .orElse(DataComponentPatch.EMPTY);
    }

    protected void renderResourceLabel(@NotNull GuiGraphics gui, FilterLogic.FilterResource resource, int x, int y){
        Component name = getResourceName(resource);

        Matrix3x2fStack pose = gui.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(0.5F, 0.5F);
        gui.drawString(WootContainerScreen.getMCFont(), name, 0, 0, 0xFF363637, false);

        Component sub = Component.translatable(resource.getType() == FilterLogic.FilterType.ITEM ? "gui.woot_revived.filter.item" : "gui.woot_revived.filter.fluid");
        if(resource.getTag())
            sub = sub.copy().append(Component.literal(" ")).append(Component.translatable("gui.woot_revived.filter.tag"));

        gui.drawString(WootContainerScreen.getMCFont(), sub, 0, 10, 0xFF555555, false);

        Component state = Component.empty();
        if(resource.getNBT() != null && resource.getNBT().size() > 0){
            state = Component.translatable("gui.woot_revived.filter.nbt");
            if(resource.getExactNBT())
                state = state.copy().append(Component.literal(" ")).append(Component.translatable("gui.woot_revived.filter.exact"));
        }

        if(resource.getInverted()){
            if(!state.getString().isEmpty())
                state = state.copy().append(Component.literal(" "));
            state = state.copy().append(Component.translatable("gui.woot_revived.filter.inverted"));
        }

        if(!state.getString().isEmpty())
            gui.drawString(WootContainerScreen.getMCFont(), state, 0, 20, resource.getInverted() ? 0xFFAA0000 : 0xFF555555, false);
        pose.popMatrix();
    }

    protected Component getResourceName(FilterLogic.FilterResource resource){
        if(resource.getTag())
            return Component.literal(resource.getLocation() == null ? "#" : "#" + resource.getLocation());

        if(resource.getType() == FilterLogic.FilterType.ITEM){
            Item item = getItem(resource.getLocation());
            if(item != null)
                return Component.translatable(item.getDescriptionId());
        } else {
            Fluid fluid = getFluid(resource.getLocation());
            if(fluid != null)
                return new FluidStack(fluid, 1000).getHoverName();
        }

        return Component.literal(resource.getLocation() == null ? "" : resource.getLocation().toString());
    }

    protected void renderFluid(@NotNull GuiGraphics gui, int x, int y, FluidStack fluid){
        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluid.getFluid().getFluidType());
        TextureAtlasSprite texture = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(fluidTypeExtensions.getStillTexture());
        WootContainerScreen.renderTiledFluidTextureAtlas(gui, texture, x, y, 16, 16, fluidTypeExtensions.getTintColor(), false);
    }

    protected void renderResourceTooltip(@NotNull GuiGraphics gui, int mouseX, int mouseY, int index){
        if(index < 0 || index >= logic.size())
            return;

        FilterLogic.FilterResource resource = logic.get(index);
        List<Component> tooltip = new ArrayList<>();

        if(resource.getInverted())
            tooltip.add(Component.translatable("gui.woot_revived.filter.not", getResourceName(resource)));
        else
            tooltip.add(getResourceName(resource));

        if(resource.getLocation() != null)
            tooltip.add(Component.literal((resource.getTag() ? "#" : "") + resource.getLocation()));

        if(resource.getNBT() != null && resource.getNBT().size() > 0){
            tooltip.add(Component.translatable("gui.woot_revived.filter.nbt_tags", resource.getNBT().size()));
            if(resource.getExactNBT())
                tooltip.add(Component.translatable("gui.woot_revived.filter.exact_nbt"));
        }

        gui.setTooltipForNextFrame(WootContainerScreen.getMCFont(), tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

    public void addLogic(FilterLogic logic){
        this.logic = logic;
    }

    public void setOnSelect(OnSelect onSelect){
        this.onSelect = onSelect;
    }

    public void reset(){
        this.selected = -1;
        this.scrollIndex = 0;
        this.scrollHeight = 0;
    }

    public int getSelected(){
        if(selected >= logic.size())
            selected = -1;

        return selected;
    }

    public void setSelected(int selected){
        this.selected = selected;
    }

    private void scrollFilter(double height){
        double len = logic.size() - 3;
        if(len > 0){
            scrollIndex = (int)Math.round(height * len);
            scrollHeight = (double)scrollIndex / len;
        } else {
            scrollIndex = 0;
            scrollHeight = 0;
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
        if(event.x() > getX() + getWidth() - 11 &&
                event.x() < getX() + getWidth() - 1 &&
                event.y() > getY() + 1 &&
                event.y() < getY() + getHeight() - 1) {
            double height = (event.y() - getY() - 1 - 8.5) / ((double)getHeight() - 2.0 - 8.5 * 2);
            height = Math.min(Math.max(height, 0), 1);
            scrollFilter(height);
        }
        super.onDrag(event, mouseX, mouseY);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if(event.x() > getX() + getWidth() - 11 &&
           event.x() < getX() + getWidth() - 1 &&
           event.y() > getY() + 1 &&
           event.y() < getY() + getHeight() - 1) {
            double height = (event.y() - getY() - 1 - 8.5) / ((double)getHeight() - 2.0 - 8.5 * 2);
            height = Math.min(Math.max(height, 0), 1);
            scrollFilter(height);
        } else if(event.x() > getX() + 1 &&
                  event.x() < getX() + getWidth() - 12 &&
                  event.y() > getY() + 1 &&
                  event.y() < getY() + getHeight() - 1) {
            if(event.y() < getY() + 21){
                if(logic.size() > 0) {
                    selected = scrollIndex;
                    this.onSelect.onSelect(this, selected);
                }
            } else if(event.y() < getY() + 41){
                if(scrollIndex + 1 < logic.size()) {
                    selected = scrollIndex + 1;
                    this.onSelect.onSelect(this, selected);
                }
            } else {
                if(scrollIndex + 2 < logic.size()){
                    selected = scrollIndex + 2;
                    this.onSelect.onSelect(this, selected);
                }
            }
        }
        super.onClick(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double len = logic.size() - 3;
        if(len > 0){
            if(scrollY > 0 && scrollIndex > 0){ // scroll up
                scrollFilter((scrollIndex - 1) / len);
                return true;
            } else if(scrollY < 0 && scrollIndex < len){ // scroll down
                scrollFilter((scrollIndex + 1) / len);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected static @Nullable Item getItem(@Nullable ResourceLocation location){
        if(location == null)
            return null;

        return BuiltInRegistries.ITEM.get(location).map(holder -> holder.value()).orElse(null);
    }

    protected static @Nullable Fluid getFluid(@Nullable ResourceLocation location){
        if(location == null)
            return null;

        return BuiltInRegistries.FLUID.get(location).map(holder -> holder.value()).orElse(null);
    }

    protected static @Nullable Item getItemFromTag(@Nullable ResourceLocation location){
        if(location == null)
            return null;

        List<Item> items = new ArrayList<>();
        BuiltInRegistries.ITEM.get(FilterLogic.createItemTag(location)).ifPresent(tag -> {
            for(var holder : tag)
                items.add(holder.value());
        });

        if(items.isEmpty())
            return null;

        return items.get(getTagDisplayIndex(items.size()));
    }

    protected static @Nullable Fluid getFluidFromTag(@Nullable ResourceLocation location){
        if(location == null)
            return null;

        List<Fluid> fluids = new ArrayList<>();
        BuiltInRegistries.FLUID.get(FilterLogic.createFluidTag(location)).ifPresent(tag -> {
            for(var holder : tag)
                fluids.add(holder.value());
        });

        if(fluids.isEmpty())
            return null;

        return fluids.get(getTagDisplayIndex(fluids.size()));
    }

    protected static int getTagDisplayIndex(int size){
        return (GlobalClientTicker.tickCounter / TAG_DISPLAY_TICKS) % size;
    }

    public interface OnSelect {
        void onSelect(FilterList list, int selected);
    }
}
