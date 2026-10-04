package wootrevived.woot.upgrades.filter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import wootrevived.woot.Woot;
import wootrevived.woot.upgrades.filter.buttons.WootFilterGPButton;
import wootrevived.woot.upgrades.filter.buttons.WootNBTButton;
import wootrevived.woot.upgrades.filter.buttons.WootWBListButton;
import wootrevived.woot.util.render.WootContainerScreen;
import wootrevived.woot.util.render.WootSlot;

import java.util.Optional;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    public static final ResourceLocation GUI = Woot.location("textures/gui/atlas.png");

    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 256;

    public static final int GUI_XSIZE = 176;
    public static final int GUI_YSIZE = 184;

    public static final int CHOSE_LIST_X = 7;
    public static final int CHOSE_LIST_Y = 18;
    public static final int CHOSE_LIST_W = 162;

    public static final int CHOSE_ADD_BUTTON_X = 8;
    public static final int CHOSE_ADD_BUTTON_Y = 84;

    public static final int CHOSE_EDIT_BUTTON_X = 26;
    public static final int CHOSE_EDIT_BUTTON_Y = 84;

    public static final int CHOSE_DELETE_BUTTON_X = 44;
    public static final int CHOSE_DELETE_BUTTON_Y = 84;

    public static final int CHOSE_WBLIST_BUTTON_X = 154;
    public static final int CHOSE_WBLIST_BUTTON_Y = 84;

    public static final int EDIT_SLOT_TEXT_X = 8;
    public static final int EDIT_SLOT_TEXT_Y = 19;

    public static final int EDIT_SLOT_X = 7;
    public static final int EDIT_SLOT_Y = 29;

    public static final int EDIT_SLOT_TEXTBOX_X = 30;
    public static final int EDIT_SLOT_TEXTBOX_Y = 30;
    public static final int EDIT_SLOT_TEXTBOX_W = 138;
    public static final int EDIT_SLOT_TEXTBOX_H = 16;

    public static final int EDIT_NBT_TEXT_X = 8;
    public static final int EDIT_NBT_TEXT_Y = 51;

    public static final int EDIT_NBT_TEXTBOX_X = 8;
    public static final int EDIT_NBT_TEXTBOX_Y = 62;
    public static final int EDIT_NBT_TEXTBOX_W = 160;
    public static final int EDIT_NBT_TEXTBOX_H = 16;

    public static final int EDIT_CONFIRM_BUTTON_X = 8;
    public static final int EDIT_CONFIRM_BUTTON_Y = 84;

    public static final int EDIT_CANCEL_BUTTON_X = 26;
    public static final int EDIT_CANCEL_BUTTON_Y = 84;

    public static final int EDIT_NBT_BUTTON_X = 136;
    public static final int EDIT_NBT_BUTTON_Y = 84;

    public static final int EDIT_WBLIST_BUTTON_X = 154;
    public static final int EDIT_WBLIST_BUTTON_Y = 84;

    public static final int TEXTBOX_VALID_COLOR = 0xFFFFFFFF;
    public static final int TEXTBOX_INVALID_COLOR = 0xFFAA0000;

    public boolean isInChoseWindow = true;
    public final FilterLogic filterLogic;

    public FilterScreen(FilterMenu container, Inventory inventory, Component title) {
        super(container, inventory, title);
        imageWidth = GUI_XSIZE;
        imageHeight = GUI_YSIZE;

        filterLogic = container.getLogic();
    }

    protected void renderMenuBackground(GuiGraphics gui) {
        if(!isInChoseWindow){
            gui.drawString(font, Component.translatable("gui.woot_revived.filter.resource"), EDIT_SLOT_TEXT_X, EDIT_SLOT_TEXT_Y, 0xFF404040, false);
            gui.blit(RenderPipelines.GUI_TEXTURED, GUI, EDIT_SLOT_X, EDIT_SLOT_Y, 7, 101, 18, 18, ATLAS_WIDTH, ATLAS_HEIGHT);
            gui.drawString(font, Component.translatable("gui.woot_revived.filter.nbt"), EDIT_NBT_TEXT_X, EDIT_NBT_TEXT_Y, 0xFF404040, false);
        }
    }

    protected void renderState(GuiGraphics gui) {
        if(!isInChoseWindow)
            renderResource(gui, resourceNewer, EDIT_SLOT_X + 1, EDIT_SLOT_Y + 1);
    }

    /* Widgets */

    private final FilterList filterList = new FilterList(CHOSE_LIST_W);

    private EditBox slot;
    private EditBox nbt;

    private WootFilterGPButton addButton;
    private WootFilterGPButton editButton;
    private WootFilterGPButton deleteButton;
    private WootFilterGPButton confirmButton;
    private WootFilterGPButton cancelButton;
    private WootWBListButton wblistButton;
    private WootWBListButton wblistAltButton;
    private WootNBTButton nbtButton;

    private @Nullable FilterLogic.FilterResource resourceInitial = null;
    private FilterLogic.FilterResource resourceNewer = new FilterLogic.FilterItem();

    @Override
    protected void init(){
        super.init();

        slot = new EditBox(font, leftPos + EDIT_SLOT_TEXTBOX_X, topPos + EDIT_SLOT_TEXTBOX_Y, EDIT_SLOT_TEXTBOX_W, EDIT_SLOT_TEXTBOX_H, Component.empty());
        slot.setTextColor(TEXTBOX_VALID_COLOR);
        slot.setBordered(true);
        slot.setMaxLength(Integer.MAX_VALUE);
        slot.setValue("");
        slot.setFilter(s -> {
            if(s.equals("#"))
                return true;

            if(s.startsWith("#"))
                s = s.substring(1);
            return ResourceLocation.tryParse(s) != null;
        });
        slot.setResponder(this::onItemTextChanged);
        addRenderableWidget(slot);

        nbt = new EditBox(font, leftPos + EDIT_NBT_TEXTBOX_X, topPos + EDIT_NBT_TEXTBOX_Y, EDIT_NBT_TEXTBOX_W, EDIT_NBT_TEXTBOX_H, Component.empty());
        nbt.setTextColor(TEXTBOX_VALID_COLOR);
        nbt.setBordered(true);
        nbt.setMaxLength(Integer.MAX_VALUE);
        nbt.setValue("");
        nbt.setResponder(this::onNbtTextChanged);
        addRenderableWidget(nbt);

        addRenderableWidget(addButton = new WootFilterGPButton(leftPos + CHOSE_ADD_BUTTON_X, topPos + CHOSE_ADD_BUTTON_Y, false, WootFilterGPButton.Type.NEW, button -> {
            startNewResource();
        }));

        addRenderableWidget(editButton = new WootFilterGPButton(leftPos + CHOSE_EDIT_BUTTON_X, topPos + CHOSE_EDIT_BUTTON_Y, true, WootFilterGPButton.Type.EDIT, button -> {
            if(resourceInitial == null)
                return;

            updateConfirmButton();
            switchView();
        }));

        addRenderableWidget(deleteButton = new WootFilterGPButton(leftPos + CHOSE_DELETE_BUTTON_X, topPos + CHOSE_DELETE_BUTTON_Y, true, WootFilterGPButton.Type.DELETE, button -> {
            if(resourceInitial == null)
                return;

            filterLogic.remove(resourceInitial);
            menu.saveLogic();
            filterList.setSelected(-1);
            resourceInitial = null;
            resourceNewer = new FilterLogic.FilterItem();
            editButton.disabled = true;
            deleteButton.disabled = true;
        }));

        addRenderableWidget(wblistButton = new WootWBListButton(leftPos + CHOSE_WBLIST_BUTTON_X, topPos + CHOSE_WBLIST_BUTTON_Y, false, WootWBListButton.State.WHITELIST, button -> {
            WootWBListButton.State state = button.nextState();
            filterLogic.whitelistMode = state == WootWBListButton.State.WHITELIST;
            menu.saveLogic();
        }));
        wblistButton.setState(filterLogic.whitelistMode ? WootWBListButton.State.WHITELIST : WootWBListButton.State.BLACKLIST);

        addRenderableWidget(confirmButton = new WootFilterGPButton(leftPos + EDIT_CONFIRM_BUTTON_X, topPos + EDIT_CONFIRM_BUTTON_Y, true, WootFilterGPButton.Type.CONFIRM, button -> {
            if(!isResourceValid(resourceNewer))
                return;

            if(resourceInitial == null)
                filterLogic.add(resourceNewer.copy());
            else
                filterLogic.replace(resourceInitial, resourceNewer.copy());

            menu.saveLogic();
            switchView();
        }));

        addRenderableWidget(cancelButton = new WootFilterGPButton(leftPos + EDIT_CANCEL_BUTTON_X, topPos + EDIT_CANCEL_BUTTON_Y, false, WootFilterGPButton.Type.CANCEL, button -> {
            switchView();
        }));

        addRenderableWidget(nbtButton = new WootNBTButton(leftPos + EDIT_NBT_BUTTON_X, topPos + EDIT_NBT_BUTTON_Y, false, WootNBTButton.State.ONLY, button -> {
            resourceNewer.setExactNBT(button.nextState() == WootNBTButton.State.EXACT);
            updateConfirmButton();
        }));

        addRenderableWidget(wblistAltButton = new WootWBListButton(leftPos + EDIT_WBLIST_BUTTON_X, topPos + EDIT_WBLIST_BUTTON_Y, true, WootWBListButton.State.WHITELIST, button -> {
            resourceNewer.setInverted(button.nextState() == WootWBListButton.State.BLACKLIST);
            updateConfirmButton();
        }));

        filterList.setPosition(leftPos + CHOSE_LIST_X, topPos + CHOSE_LIST_Y);
        filterList.addLogic(filterLogic);
        filterList.setOnSelect((list, selected) -> {
            resourceInitial = filterLogic.get(selected);
            resourceNewer = filterLogic.get(selected).copy();

            slot.setValue((resourceInitial.getTag() ? "#" : "") + resourceInitial.getLocation().toString());
            if(resourceInitial.getNBT() != null){
                nbt.setValue(resourceInitial.getNBT().toString());
            } else {
                nbt.setValue("");
            }

            wblistAltButton.setState(resourceInitial.getInverted() ? WootWBListButton.State.BLACKLIST : WootWBListButton.State.WHITELIST);
            nbtButton.setState(resourceInitial.getExactNBT() ? WootNBTButton.State.EXACT : WootNBTButton.State.ONLY);
            nbtButton.disabled = resourceInitial.getNBT() == null;
            editButton.disabled = false;
            deleteButton.disabled = false;
            updateConfirmButton();
        });
        addRenderableWidget(filterList);

        filterList.active = isInChoseWindow;
        addButton.active = isInChoseWindow;
        editButton.active = isInChoseWindow;
        deleteButton.active = isInChoseWindow;
        wblistButton.active = isInChoseWindow;
        slot.active = !isInChoseWindow;
        nbt.active = !isInChoseWindow;
        slot.visible = !isInChoseWindow;
        nbt.visible = !isInChoseWindow;
        confirmButton.active = !isInChoseWindow;
        cancelButton.active = !isInChoseWindow;
        nbtButton.active = !isInChoseWindow;
        wblistAltButton.active = !isInChoseWindow;
    }

    protected void switchView(){
        isInChoseWindow = !isInChoseWindow;
        slot.active = !slot.active;
        nbt.active = !nbt.active;
        slot.visible = !slot.visible;
        nbt.visible = !nbt.visible;
        filterList.active = !filterList.active;
        addButton.active = !addButton.active;
        editButton.active = !editButton.active;
        deleteButton.active = !deleteButton.active;
        wblistButton.active = !wblistButton.active;
        confirmButton.active = !confirmButton.active;
        cancelButton.active = !cancelButton.active;
        nbtButton.active = !nbtButton.active;
        wblistAltButton.active = !wblistAltButton.active;

        if(isInChoseWindow){
            slot.setValue("");
            nbt.setValue("");
            wblistAltButton.setState(WootWBListButton.State.WHITELIST);
            nbtButton.setState(WootNBTButton.State.ONLY);
            nbtButton.disabled = true;
            resourceInitial = null;
            resourceNewer = new FilterLogic.FilterItem();
            editButton.disabled = true;
            deleteButton.disabled = true;
            filterList.reset();
        }
    }

    /* EditBox */

    public void onItemTextChanged(String text) {
        if(text.trim().isEmpty()){
            slot.setTextColor(TEXTBOX_VALID_COLOR);
            resourceNewer.setLocation(null);
            resourceNewer.setTag(false);
            confirmButton.disabled = true;
            return;
        }

        boolean isTag = text.startsWith("#");
        if(text.startsWith("#"))
            text = text.substring(1);

        resourceNewer.setLocation(ResourceLocation.tryParse(text));
        resourceNewer.setTag(isTag);
        switchResourceTypeIfNeeded();

        if(isResourceValid(resourceNewer))
            slot.setTextColor(TEXTBOX_VALID_COLOR);
        else
            slot.setTextColor(TEXTBOX_INVALID_COLOR);

        updateConfirmButton();
    }

    public void onNbtTextChanged(String text) {
        if(text.trim().isEmpty()){
            nbt.setTextColor(TEXTBOX_VALID_COLOR);
            nbtButton.disabled = true;
            nbtButton.setState(WootNBTButton.State.ONLY);
            resourceNewer.setNBT(null);
            resourceNewer.setExactNBT(false);
            updateConfirmButton();
            return;
        }

        nbtButton.disabled = false;

        try {
            CompoundTag tag = TagParser.parseCompoundFully(text);
            resourceNewer.setNBT(tag);
            nbt.setTextColor(TEXTBOX_VALID_COLOR);
        } catch (Exception e) {
            resourceNewer.setNBT(null);
            nbt.setTextColor(TEXTBOX_INVALID_COLOR);
        }

        updateConfirmButton();
    }

    /* JEI */

    public void startNewResource(){
        slot.setValue("");
        nbt.setValue("");
        wblistAltButton.setState(WootWBListButton.State.WHITELIST);
        nbtButton.setState(WootNBTButton.State.ONLY);
        nbtButton.disabled = true;
        resourceInitial = null;
        resourceNewer = new FilterLogic.FilterItem();
        confirmButton.disabled = true;

        if(isInChoseWindow)
            switchView();
    }

    public boolean isInFilterListView(){
        return isInChoseWindow;
    }

    public void onInsertJEIStack(ItemStack stack) {
        if(isInChoseWindow)
            startNewResource();

        onInsertStack(stack);
    }

    public void onInsertJEIStack(FluidStack stack) {
        if(isInChoseWindow)
            startNewResource();

        onInsertStack(stack);
    }

    public void onInsertStack(ItemStack stack) {
        if(stack == null || stack.isEmpty())
            return;

        Optional<FluidStack> containedFluid = getFluidContained(stack);

        if(resourceNewer.getType() == FilterLogic.FilterType.FLUID && containedFluid.isPresent()){
            containedFluid.ifPresent(this::onInsertStack);
            return;
        }

        if(stack.getItem() != Items.AIR){
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if(resourceNewer.getType() != FilterLogic.FilterType.ITEM)
                resourceNewer = resourceNewer.switchType();

            resourceNewer.setTag(false);
            slot.setValue(location.toString());
            nbt.setValue(getComponentPatchText(stack.getComponentsPatch()));
            return;
        }

        containedFluid.ifPresent(this::onInsertStack);
    }

    protected Optional<FluidStack> getFluidContained(ItemStack stack){
        ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);

        if(handler == null)
            return Optional.empty();

        for(int i = 0; i < handler.size(); i++){
            FluidResource resource = handler.getResource(i);
            int amount = handler.getAmountAsInt(i);

            if(!resource.isEmpty() && amount > 0)
                return Optional.of(resource.toStack(amount));
        }

        return Optional.empty();
    }

    public void onInsertStack(FluidStack stack) {
        if(stack == null || stack.isEmpty())
            return;

        if(stack.getFluid() == Fluids.EMPTY)
            return;

        ResourceLocation location = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if(resourceNewer.getType() != FilterLogic.FilterType.FLUID)
            resourceNewer = resourceNewer.switchType();

        resourceNewer.setTag(false);
        slot.setValue(location.toString());
        nbt.setValue(getComponentPatchText(stack.getComponentsPatch()));
    }

    /* UTILS */

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks){
        for(Slot slot : menu.slots){
            if(slot instanceof WootSlot wootSlot)
                wootSlot.setActive(isInChoseWindow);
        }

        super.render(gui, mouseX, mouseY, partialTicks);
        super.renderTooltip(gui, mouseX, mouseY);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        Matrix3x2fStack pose = gui.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        gui.blit(RenderPipelines.GUI_TEXTURED, GUI, 0, 0, 0, 0, imageWidth, imageHeight, ATLAS_WIDTH, ATLAS_HEIGHT);

        renderMenuBackground(gui);
        renderState(gui);

        pose.popMatrix();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY){
        int titleX = 1 + (imageWidth - font.width(title)) / 2;
        gui.drawString(font, title, titleX, 6, 0xFF404040, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.player.closeContainer();
            return true;
        }

        return slot.keyPressed(event) ||
                slot.canConsumeInput() ||
                nbt.keyPressed(event) ||
                nbt.canConsumeInput() || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if(!isInChoseWindow &&
                event.x() >= leftPos + EDIT_SLOT_X + 1 &&
                event.x() < leftPos + EDIT_SLOT_X + 17 &&
                event.y() >= topPos + EDIT_SLOT_Y + 1 &&
                event.y() < topPos + EDIT_SLOT_Y + 17) {
            if(Minecraft.getInstance().hasShiftDown()){
                slot.setValue("");
                nbt.setValue("");
                resourceNewer.setLocation(null);
                resourceNewer.setNBT(null);
                resourceNewer.setTag(false);
                updateConfirmButton();
            } else {
                onInsertStack(menu.getCarried());
            }
            return true;
        }

        if(!isInChoseWindow && Minecraft.getInstance().hasShiftDown()){
            Slot slot = getSlotUnderMouse();
            if(slot != null){
                onInsertStack(slot.getItem());
                return true;
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY){
        if(isInChoseWindow && filterList.mouseDragged(event, mouseX, mouseY))
            return true;

        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(isInChoseWindow && filterList.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
            return true;

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        String slotValue = slot == null ? "" : slot.getValue();
        String nbtValue = nbt == null ? "" : nbt.getValue();

        init(minecraft, width, height);

        slot.setValue(slotValue);
        nbt.setValue(nbtValue);
    }

    protected boolean isResourceValid(FilterLogic.FilterResource resource){
        ResourceLocation location = resource.getLocation();
        if(location == null)
            return false;

        return isResourceValid(resource.getType(), resource.getTag(), location);
    }

    protected boolean isResourceValid(FilterLogic.FilterType type, boolean isTag, ResourceLocation location){
        if(type == FilterLogic.FilterType.ITEM){
            if(isTag)
                return BuiltInRegistries.ITEM.get(FilterLogic.createItemTag(location)).isPresent();

            return BuiltInRegistries.ITEM.get(location).isPresent();
        }

        if(isTag)
            return BuiltInRegistries.FLUID.get(FilterLogic.createFluidTag(location)).isPresent();

        return BuiltInRegistries.FLUID.get(location).isPresent();
    }

    protected void switchResourceTypeIfNeeded(){
        ResourceLocation location = resourceNewer.getLocation();
        if(location == null || isResourceValid(resourceNewer))
            return;

        FilterLogic.FilterType other = resourceNewer.getType() == FilterLogic.FilterType.ITEM ? FilterLogic.FilterType.FLUID : FilterLogic.FilterType.ITEM;
        if(isResourceValid(other, resourceNewer.getTag(), location))
            resourceNewer = resourceNewer.switchType();
    }

    protected void updateConfirmButton(){
        confirmButton.disabled = !isResourceValid(resourceNewer) || (!nbt.getValue().trim().isEmpty() && resourceNewer.getNBT() == null);
    }

    protected String getComponentPatchText(DataComponentPatch patch){
        if(patch.isEmpty())
            return "";

        return DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, patch)
                .result()
                .filter(tag -> tag instanceof CompoundTag)
                .map(CompoundTag.class::cast)
                .map(CompoundTag::toString)
                .orElse("");
    }

    protected void renderResource(@NotNull GuiGraphics gui, FilterLogic.FilterResource resource, int x, int y){
        if(resource.getLocation() == null)
            return;

        if(resource.getTag()){
            if(resource.getType() == FilterLogic.FilterType.ITEM){
                Item item = FilterList.getItemFromTag(resource.getLocation());
                if(item != null){
                    gui.renderItem(FilterList.applyComponents(new ItemStack(item), resource.getNBT()), x, y);
                    return;
                }
            } else {
                Fluid fluid = FilterList.getFluidFromTag(resource.getLocation());
                if(fluid != null){
                    renderFluid(gui, x, y, FilterList.applyComponents(new FluidStack(fluid, 1000), resource.getNBT()));
                    return;
                }
            }

            gui.drawString(font, "#", x + 5, y + 4, 0xFF404040, false);
            return;
        }

        if(resource.getType() == FilterLogic.FilterType.ITEM){
            Item item = BuiltInRegistries.ITEM.get(resource.getLocation()).map(holder -> holder.value()).orElse(Items.AIR);
            if(item != Items.AIR)
                gui.renderItem(FilterList.applyComponents(new ItemStack(item), resource.getNBT()), x, y);
        } else {
            Fluid fluid = BuiltInRegistries.FLUID.get(resource.getLocation()).map(holder -> holder.value()).orElse(Fluids.EMPTY);
            if(fluid != Fluids.EMPTY)
                renderFluid(gui, x, y, FilterList.applyComponents(new FluidStack(fluid, 1000), resource.getNBT()));
        }
    }

    protected void renderFluid(@NotNull GuiGraphics gui, int x, int y, FluidStack fluid){
        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluid.getFluid().getFluidType());
        TextureAtlasSprite texture = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(fluidTypeExtensions.getStillTexture());
        WootContainerScreen.renderTiledFluidTextureAtlas(gui, texture, x, y, 16, 16, fluidTypeExtensions.getTintColor(), false);
    }
}
