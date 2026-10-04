package wootrevived.woot.upgrades.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FilterLogic {
    public boolean whitelistMode;
    public List<FilterItem> itemList;
    public List<FilterFluid> fluidList;
    protected List<FilterType> resourceOrder;

    protected FilterLogic(List<FilterItem> itemList, List<FilterFluid> fluidList, List<FilterType> resourceOrder, boolean whitelistMode){
        this.itemList = itemList;
        this.fluidList = fluidList;
        this.resourceOrder = resourceOrder;
        this.whitelistMode = whitelistMode;
    }

    protected FilterLogic(List<FilterItem> itemList, List<FilterFluid> fluidList, boolean whitelistMode){
        this.itemList = itemList;
        this.fluidList = fluidList;
        this.resourceOrder = new ArrayList<>();
        this.whitelistMode = whitelistMode;

        for(int i = 0; i < itemList.size(); i++)
            resourceOrder.add(FilterType.ITEM);

        for(int i = 0; i < fluidList.size(); i++)
            resourceOrder.add(FilterType.FLUID);
    }

    public int size(){
        return resourceOrder.size();
    }

    public FilterResource get(int index){
        FilterType type = resourceOrder.get(index);
        int typeIndex = getTypeIndex(index);

        if(type == FilterType.ITEM)
            return itemList.get(typeIndex);
        else
            return fluidList.get(typeIndex);
    }

    protected int getTypeIndex(int index){
        FilterType type = resourceOrder.get(index);
        int typeIndex = 0;

        for(int i = 0; i < index; i++){
            if(resourceOrder.get(i) == type)
                typeIndex++;
        }

        return typeIndex;
    }

    protected int indexOf(FilterResource resource){
        for(int i = 0; i < size(); i++){
            if(get(i) == resource)
                return i;
        }

        return -1;
    }

    public void add(FilterResource resource){
        if(resource.getLocation() != null){
            if(resource.getType() == FilterType.ITEM)
                itemList.add((FilterItem) resource);
            else
                fluidList.add((FilterFluid) resource);

            resourceOrder.add(resource.getType());
        }
    }

    protected void add(int index, FilterResource resource){
        if(resource.getLocation() != null){
            int typeIndex = 0;

            for(int i = 0; i < index; i++){
                if(resourceOrder.get(i) == resource.getType())
                    typeIndex++;
            }

            if(resource.getType() == FilterType.ITEM)
                itemList.add(typeIndex, (FilterItem) resource);
            else
                fluidList.add(typeIndex, (FilterFluid) resource);

            resourceOrder.add(index, resource.getType());
        }
    }

    public void replace(FilterResource initial, FilterResource newer){
        int index = indexOf(initial);

        if(index == -1 || newer.getLocation() == null)
            return;

        int typeIndex = getTypeIndex(index);

        if(initial.getType() == newer.getType()){
            if(initial.getType() == FilterType.ITEM)
                itemList.set(typeIndex, (FilterItem) newer);
            else
                fluidList.set(typeIndex, (FilterFluid) newer);
        } else {
            remove(index);
            add(index, newer);
        }
    }

    public void remove(FilterResource resource){
        int index = indexOf(resource);

        if(index != -1)
            remove(index);
    }

    protected void remove(int index){
        FilterType type = resourceOrder.get(index);
        int typeIndex = getTypeIndex(index);

        if(type == FilterType.ITEM)
            itemList.remove(typeIndex);
        else
            fluidList.remove(typeIndex);

        resourceOrder.remove(index);
    }

    public static FilterLogic fromTag(CompoundTag tag){
        List<FilterItem> itemList = new ArrayList<>();
        List<FilterFluid> fluidList = new ArrayList<>();
        List<FilterType> resourceOrder = new ArrayList<>();
        boolean whitelistMode = true;

        if(tag.contains("FilterItems")){
            ListTag itemListTag = tag.getList("FilterItems", Tag.TAG_COMPOUND);
            for(int i = 0; i < itemListTag.size(); i++)
                itemList.add(FilterItem.fromTag(itemListTag.getCompound(i)));
        }

        if(tag.contains("FilterFluids")){
            ListTag fluidListTag = tag.getList("FilterFluids", Tag.TAG_COMPOUND);
            for(int i = 0; i < fluidListTag.size(); i++)
                fluidList.add(FilterFluid.fromTag(fluidListTag.getCompound(i)));
        }

        if(tag.contains("FilterOrder")){
            int[] order = tag.getIntArray("FilterOrder");

            int itemCount = 0;
            int fluidCount = 0;

            for(int type : order){
                if(type == FilterType.ITEM.ordinal() && itemCount < itemList.size()){
                    resourceOrder.add(FilterType.ITEM);
                    itemCount++;
                } else if(type == FilterType.FLUID.ordinal() && fluidCount < fluidList.size()){
                    resourceOrder.add(FilterType.FLUID);
                    fluidCount++;
                }
            }

            while(itemCount < itemList.size()){
                resourceOrder.add(FilterType.ITEM);
                itemCount++;
            }

            while(fluidCount < fluidList.size()){
                resourceOrder.add(FilterType.FLUID);
                fluidCount++;
            }
        } else {
            for(int i = 0; i < itemList.size(); i++)
                resourceOrder.add(FilterType.ITEM);

            for(int i = 0; i < fluidList.size(); i++)
                resourceOrder.add(FilterType.FLUID);
        }

        if(tag.contains("FilterWhitelist"))
            whitelistMode = tag.getBoolean("FilterWhitelist");

        return new FilterLogic(itemList, fluidList, resourceOrder, whitelistMode);
    }

    public void toTag(CompoundTag tag){
        ListTag itemListTag = new ListTag();
        for(FilterItem item : itemList){
            CompoundTag itemTag = new CompoundTag();
            item.toTag(itemTag);
            itemListTag.add(itemTag);
        }
        tag.put("FilterItems", itemListTag);

        ListTag fluidListTag = new ListTag();
        for(FilterFluid fluid : fluidList){
            CompoundTag fluidTag = new CompoundTag();
            fluid.toTag(fluidTag);
            fluidListTag.add(fluidTag);
        }
        tag.put("FilterFluids", fluidListTag);

        int[] order = new int[resourceOrder.size()];
        for(int i = 0; i < resourceOrder.size(); i++)
            order[i] = resourceOrder.get(i).ordinal();
        tag.putIntArray("FilterOrder", order);

        tag.putBoolean("FilterWhitelist", whitelistMode);
    }

    public void filterItems(List<ItemStack> items){
        if(size() == 0)
            return;

        items.removeIf(stack -> !shouldKeepItem(stack));
    }

    public void filterFluids(List<FluidStack> fluids){
        if(size() == 0)
            return;

        fluids.removeIf(stack -> !shouldKeepFluid(stack));
    }

    protected boolean shouldKeepItem(ItemStack stack){
        boolean matches = false;

        for(FilterItem item : itemList){
            boolean entryMatches = matchesItem(stack, item);

            if(item.getInverted())
                entryMatches = !entryMatches;

            if(entryMatches){
                matches = true;
                break;
            }
        }

        return whitelistMode == matches;
    }

    protected boolean shouldKeepFluid(FluidStack stack){
        boolean matches = false;

        for(FilterFluid fluid : fluidList){
            boolean entryMatches = matchesFluid(stack, fluid);

            if(fluid.getInverted())
                entryMatches = !entryMatches;

            if(entryMatches){
                matches = true;
                break;
            }
        }

        return whitelistMode == matches;
    }

    protected boolean matchesItem(ItemStack stack, FilterItem item){
        ResourceLocation location = item.getLocation();

        if(location == null)
            return false;

        if(item.getTag()){
            if(!stack.is(createItemTag(location)))
                return false;
        } else if(!Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), location)) {
            return false;
        }

        return matchesNBT(stack.getTag(), item);
    }

    protected boolean matchesFluid(FluidStack stack, FilterFluid fluid){
        ResourceLocation location = fluid.getLocation();

        if(location == null)
            return false;

        if(fluid.getTag()){
            if(!stack.getFluid().defaultFluidState().is(createFluidTag(location)))
                return false;
        } else {
            Fluid filterFluid = BuiltInRegistries.FLUID.get(location);
            if(filterFluid == Fluids.EMPTY || !stack.isFluidEqual(new FluidStack(filterFluid, 1)))
                return false;
        }

        return matchesNBT(stack.getTag(), fluid);
    }

    protected boolean matchesNBT(@Nullable CompoundTag stackTag, FilterResource resource){
        CompoundTag filterTag = resource.getNBT();

        if(filterTag == null)
            return true;

        if(resource.getExactNBT())
            return Objects.equals(stackTag, filterTag);

        return NbtUtils.compareNbt(filterTag, stackTag, true);
    }

    public static TagKey<Item> createItemTag(ResourceLocation location){
        return TagKey.create(Registries.ITEM, location);
    }

    public static TagKey<Fluid> createFluidTag(ResourceLocation location){
        return TagKey.create(Registries.FLUID, location);
    }

    public enum FilterType {
        ITEM,
        FLUID
    }

    public interface FilterResource {
        void setLocation(@Nullable ResourceLocation location);
        void setNBT(@Nullable CompoundTag nbtTag);
        void setInverted(boolean isInverted);
        void setExactNBT(boolean isExactNBT);
        void setTag(boolean isTag);

        @Nullable ResourceLocation getLocation();
        @Nullable CompoundTag getNBT();
        boolean getInverted();
        boolean getExactNBT();
        boolean getTag();

        FilterType getType();
        FilterResource copy();
        FilterResource switchType();
    }

    public static class FilterItem implements FilterResource {
        public @Nullable ResourceLocation location;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterItem(@Nullable ResourceLocation location, @Nullable CompoundTag nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this.location = location;
            this.nbtTag = nbtTag;
            this.isInverted = isInverted;
            this.isExactNBT = isExactNBT;
            this.isTag = isTag;
        }

        public FilterItem(){
            this.location = null;
            this.nbtTag = null;
            this.isInverted = false;
            this.isExactNBT = false;
            this.isTag = false;
        }

        public static FilterItem fromTag(CompoundTag tag){
            ResourceLocation location = null;
            CompoundTag nbtTag = null;
            boolean isInverted = false;
            boolean isExactNBT = false;
            boolean isTag = false;

            if(tag.contains("Location"))
                location = ResourceLocation.tryParse(tag.getString("Location"));

            if(tag.contains("NBT"))
                nbtTag = (CompoundTag) tag.get("NBT");

            if(tag.contains("Inverted"))
                isInverted = tag.getBoolean("Inverted");

            if(tag.contains("ExactNBT"))
                isExactNBT = tag.getBoolean("ExactNBT");

            if(tag.contains("Tag"))
                isTag = tag.getBoolean("Tag");

            return new FilterItem(location, nbtTag, isInverted, isExactNBT, isTag);
        }

        public void toTag(CompoundTag tag){
            if(location != null)
                tag.putString("Location", location.toString());

            if(nbtTag != null)
                tag.put("NBT", nbtTag);

            tag.putBoolean("Inverted", isInverted);
            tag.putBoolean("ExactNBT", isExactNBT);
            tag.putBoolean("Tag", isTag);
        }

        @Override
        public void setLocation(@Nullable ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void setNBT(@Nullable CompoundTag nbtTag) {
            this.nbtTag = nbtTag;
        }

        @Override
        public void setInverted(boolean isInverted) {
            this.isInverted = isInverted;
        }

        @Override
        public void setExactNBT(boolean isExactNBT) {
            this.isExactNBT = isExactNBT;
        }

        @Override
        public void setTag(boolean isTag) {
            this.isTag = isTag;
        }

        @Override
        public @Nullable ResourceLocation getLocation() {
            return location;
        }

        @Override
        public @Nullable CompoundTag getNBT() {
            return nbtTag;
        }

        @Override
        public boolean getInverted() {
            return isInverted;
        }

        @Override
        public boolean getExactNBT() {
            return isExactNBT;
        }

        @Override
        public boolean getTag() {
            return isTag;
        }

        @Override
        public FilterType getType() {
            return FilterType.ITEM;
        }

        @Override
        public FilterResource copy() {
            return new FilterItem(location, nbtTag, isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterFluid(location, nbtTag, isInverted, isExactNBT, isTag);
        }
    }

    public static class FilterFluid implements FilterResource {
        public @Nullable ResourceLocation location;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterFluid(@Nullable ResourceLocation location, @Nullable CompoundTag nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this.location = location;
            this.nbtTag = nbtTag;
            this.isInverted = isInverted;
            this.isExactNBT = isExactNBT;
            this.isTag = isTag;
        }

        public FilterFluid(){
            this.location = null;
            this.nbtTag = null;
            this.isInverted = false;
            this.isExactNBT = false;
            this.isTag = false;
        }

        public static FilterFluid fromTag(CompoundTag tag){
            ResourceLocation location;
            CompoundTag nbtTag = null;
            boolean isInverted = false;
            boolean isExactNBT = false;
            boolean isTag = false;

            if(tag.contains("Location"))
                location = ResourceLocation.tryParse(tag.getString("Location"));
            else
                location = ResourceLocation.tryParse("minecraft:empty");

            if(tag.contains("NBT"))
                nbtTag = (CompoundTag) tag.get("NBT");

            if(tag.contains("Inverted"))
                isInverted = tag.getBoolean("Inverted");

            if(tag.contains("ExactNBT"))
                isExactNBT = tag.getBoolean("ExactNBT");

            if(tag.contains("Tag"))
                isTag = tag.getBoolean("Tag");

            return new FilterFluid(location, nbtTag, isInverted, isExactNBT, isTag);
        }

        public void toTag(CompoundTag tag){
            if(location != null)
                tag.putString("Location", location.toString());

            if(nbtTag != null)
                tag.put("NBT", nbtTag);

            tag.putBoolean("Inverted", isInverted);
            tag.putBoolean("ExactNBT", isExactNBT);
            tag.putBoolean("Tag", isTag);
        }

        @Override
        public void setLocation(@Nullable ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void setNBT(@Nullable CompoundTag nbtTag) {
            this.nbtTag = nbtTag;
        }

        @Override
        public void setInverted(boolean isInverted) {
            this.isInverted = isInverted;
        }

        @Override
        public void setExactNBT(boolean isExactNBT) {
            this.isExactNBT = isExactNBT;
        }

        @Override
        public void setTag(boolean isTag) {
            this.isTag = isTag;
        }

        @Override
        public @Nullable ResourceLocation getLocation() {
            return location;
        }

        @Override
        public @Nullable CompoundTag getNBT() {
            return nbtTag;
        }

        @Override
        public boolean getInverted() {
            return isInverted;
        }

        @Override
        public boolean getExactNBT() {
            return isExactNBT;
        }

        @Override
        public boolean getTag() {
            return isTag;
        }

        @Override
        public FilterType getType() {
            return FilterType.FLUID;
        }

        @Override
        public FilterResource copy() {
            return new FilterFluid(location, nbtTag, isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterItem(location, nbtTag, isInverted, isExactNBT, isTag);
        }
    }
}
