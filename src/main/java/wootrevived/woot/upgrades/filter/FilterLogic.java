package wootrevived.woot.upgrades.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class FilterLogic {
    public static final String ID = "filter_logic_data";

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

    public static FilterLogic empty(){
        return new FilterLogic(new ArrayList<>(), new ArrayList<>(), true);
    }

    public static FilterLogic fromComponent(Component component){
        List<FilterItem> items = new ArrayList<>();
        for(FilterItem item : component.itemList())
            items.add((FilterItem) item.copy());

        List<FilterFluid> fluids = new ArrayList<>();
        for(FilterFluid fluid : component.fluidList())
            fluids.add((FilterFluid) fluid.copy());

        return new FilterLogic(items, fluids, new ArrayList<>(component.resourceOrder()), component.whitelistMode());
    }

    public Component toComponent(){
        List<FilterItem> items = new ArrayList<>();
        for(FilterItem item : itemList)
            items.add((FilterItem) item.copy());

        List<FilterFluid> fluids = new ArrayList<>();
        for(FilterFluid fluid : fluidList)
            fluids.add((FilterFluid) fluid.copy());

        return new Component(items, fluids, new ArrayList<>(resourceOrder), whitelistMode);
    }

    public FilterLogic copy(){
        List<FilterItem> items = new ArrayList<>();
        for(FilterItem item : itemList)
            items.add((FilterItem) item.copy());

        List<FilterFluid> fluids = new ArrayList<>();
        for(FilterFluid fluid : fluidList)
            fluids.add((FilterFluid) fluid.copy());

        return new FilterLogic(items, fluids, new ArrayList<>(resourceOrder), whitelistMode);
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

    public record Component(
            List<FilterItem> itemList,
            List<FilterFluid> fluidList,
            List<FilterType> resourceOrder,
            boolean whitelistMode
    ) {
        public static final Codec<Component> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        FilterItem.CODEC.listOf().fieldOf("items").forGetter(Component::itemList),
                        FilterFluid.CODEC.listOf().fieldOf("fluids").forGetter(Component::fluidList),
                        FilterType.CODEC.listOf().fieldOf("order").forGetter(Component::resourceOrder),
                        Codec.BOOL.fieldOf("whitelist").forGetter(Component::whitelistMode)
                ).apply(inst, Component::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Component decode(RegistryFriendlyByteBuf buf) {
                List<FilterItem> itemList = FilterItem.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).decode(buf);
                List<FilterFluid> fluidList = FilterFluid.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).decode(buf);
                List<FilterType> resourceOrder = FilterType.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).decode(buf);
                boolean whitelistMode = ByteBufCodecs.BOOL.decode(buf);
                return new Component(itemList, fluidList, resourceOrder, whitelistMode);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, Component component) {
                FilterItem.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).encode(buf, new ArrayList<>(component.itemList));
                FilterFluid.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).encode(buf, new ArrayList<>(component.fluidList));
                FilterType.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)).encode(buf, new ArrayList<>(component.resourceOrder));
                ByteBufCodecs.BOOL.encode(buf, component.whitelistMode);
            }
        };

        public Component {
            itemList = List.copyOf(itemList);
            fluidList = List.copyOf(fluidList);
            resourceOrder = List.copyOf(resourceOrder);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FilterLogic logic))
            return false;

        return whitelistMode == logic.whitelistMode &&
                Objects.equals(itemList, logic.itemList) &&
                Objects.equals(fluidList, logic.fluidList) &&
                Objects.equals(resourceOrder, logic.resourceOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whitelistMode, itemList, fluidList, resourceOrder);
    }

    public void filterItems(List<ItemStack> items){
        filterItems(items, null);
    }

    public void filterItems(List<ItemStack> items, @Nullable HolderLookup.Provider lookupProvider){
        if(size() == 0)
            return;

        items.removeIf(stack -> !shouldKeepItem(stack, lookupProvider));
    }

    public void filterFluids(List<FluidStack> fluids){
        filterFluids(fluids, null);
    }

    public void filterFluids(List<FluidStack> fluids, @Nullable HolderLookup.Provider lookupProvider){
        if(size() == 0)
            return;

        fluids.removeIf(stack -> !shouldKeepFluid(stack, lookupProvider));
    }

    protected boolean shouldKeepItem(ItemStack stack, @Nullable HolderLookup.Provider lookupProvider){
        boolean matches = false;

        for(FilterItem item : itemList){
            boolean entryMatches = matchesItem(stack, item, lookupProvider);

            if(item.getInverted())
                entryMatches = !entryMatches;

            if(entryMatches){
                matches = true;
                break;
            }
        }

        return whitelistMode == matches;
    }

    protected boolean shouldKeepFluid(FluidStack stack, @Nullable HolderLookup.Provider lookupProvider){
        boolean matches = false;

        for(FilterFluid fluid : fluidList){
            boolean entryMatches = matchesFluid(stack, fluid, lookupProvider);

            if(fluid.getInverted())
                entryMatches = !entryMatches;

            if(entryMatches){
                matches = true;
                break;
            }
        }

        return whitelistMode == matches;
    }

    protected boolean matchesItem(ItemStack stack, FilterItem item, @Nullable HolderLookup.Provider lookupProvider){
        ResourceLocation location = item.getLocation();

        if(location == null)
            return false;

        if(item.getTag()){
            if(!stack.is(createItemTag(location)))
                return false;
        } else if(!Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), location)) {
            return false;
        }

        return matchesNBT(getComponentTag(stack.getComponentsPatch(), lookupProvider), item);
    }

    protected boolean matchesFluid(FluidStack stack, FilterFluid fluid, @Nullable HolderLookup.Provider lookupProvider){
        ResourceLocation location = fluid.getLocation();

        if(location == null)
            return false;

        if(fluid.getTag()){
            if(!stack.getFluid().defaultFluidState().is(createFluidTag(location)))
                return false;
        } else {
            Fluid filterFluid = BuiltInRegistries.FLUID.get(location);
            if(filterFluid == Fluids.EMPTY || !FluidStack.isSameFluidSameComponents(stack, new FluidStack(filterFluid, stack.getAmount())))
                return false;
        }

        return matchesNBT(getComponentTag(stack.getComponentsPatch(), lookupProvider), fluid);
    }

    protected CompoundTag getComponentTag(DataComponentPatch patch, @Nullable HolderLookup.Provider lookupProvider){
        if(patch.isEmpty())
            return new CompoundTag();

        return DataComponentPatch.CODEC.encodeStart(lookupProvider == null ? NbtOps.INSTANCE : lookupProvider.createSerializationContext(NbtOps.INSTANCE), patch)
                .result()
                .filter(tag -> tag instanceof CompoundTag)
                .map(CompoundTag.class::cast)
                .orElseGet(CompoundTag::new);
    }

    protected boolean matchesNBT(CompoundTag stackTag, FilterResource resource){
        CompoundTag filterTag = resource.getNBT();

        if(filterTag == null)
            return true;

        if(resource.getExactNBT())
            return deepExactCompare(filterTag, stackTag);

        if(stackTag.isEmpty())
            return filterTag.size() <= 0;

        return deepFuzzyCompare(filterTag, stackTag);
    }

    protected boolean deepExactCompare(Tag filterTag, Tag stackTag){
        if(filterTag instanceof CompoundTag filterCompound){
            if(!(stackTag instanceof CompoundTag stackCompound))
                return false;

            Set<String> keys = new HashSet<>();
            keys.addAll(filterCompound.getAllKeys());
            keys.addAll(stackCompound.getAllKeys());

            for(String key : keys){
                if(!filterCompound.contains(key) || !stackCompound.contains(key))
                    return false;

                if(!deepExactCompare(filterCompound.get(key), stackCompound.get(key)))
                    return false;
            }

            return true;
        } else if(filterTag instanceof ListTag filterList){
            if(!(stackTag instanceof ListTag stackList))
                return false;

            if(!filterList.stream().allMatch(filterEntry -> stackList.stream().anyMatch(stackEntry -> deepExactCompare(filterEntry, stackEntry))))
                return false;

            return stackList.stream().allMatch(stackEntry -> filterList.stream().anyMatch(filterEntry -> deepExactCompare(stackEntry, filterEntry)));
        }

        return filterTag != null && filterTag.equals(stackTag);
    }

    protected boolean deepFuzzyCompare(Tag filterTag, Tag stackTag){
        if(filterTag instanceof CompoundTag filterCompound){
            if(!(stackTag instanceof CompoundTag stackCompound))
                return false;

            for(String key : filterCompound.getAllKeys()){
                Tag child = filterCompound.get(key);
                if(!stackCompound.contains(key, child.getId()))
                    return false;

                if(!deepFuzzyCompare(child, stackCompound.get(key)))
                    return false;
            }

            return true;
        } else if(filterTag instanceof ListTag filterList){
            if(!(stackTag instanceof ListTag stackList))
                return false;

            return filterList.stream().allMatch(filterEntry -> stackList.stream().anyMatch(stackEntry -> deepFuzzyCompare(filterEntry, stackEntry)));
        }

        return filterTag != null && filterTag.equals(stackTag);
    }

    public static TagKey<Item> createItemTag(ResourceLocation location){
        return TagKey.create(Registries.ITEM, location);
    }

    public static TagKey<Fluid> createFluidTag(ResourceLocation location){
        return TagKey.create(Registries.FLUID, location);
    }

    public enum FilterType {
        ITEM,
        FLUID;

        public static final Codec<FilterType> CODEC = Codec.INT.xmap(
                ordinal -> ordinal == FLUID.ordinal() ? FLUID : ITEM,
                FilterType::ordinal
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FilterType> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FilterType decode(RegistryFriendlyByteBuf buf) {
                return ByteBufCodecs.INT.decode(buf) == FLUID.ordinal() ? FLUID : ITEM;
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FilterType type) {
                ByteBufCodecs.INT.encode(buf, type.ordinal());
            }
        };
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
        public static final Codec<FilterItem> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        ResourceLocation.CODEC.optionalFieldOf("location").forGetter(FilterItem::locationOptional),
                        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(FilterItem::nbtOptional),
                        Codec.BOOL.fieldOf("inverted").forGetter(FilterItem::getInverted),
                        Codec.BOOL.fieldOf("exact_nbt").forGetter(FilterItem::getExactNBT),
                        Codec.BOOL.fieldOf("tag").forGetter(FilterItem::getTag)
                ).apply(inst, FilterItem::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FilterItem> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FilterItem decode(RegistryFriendlyByteBuf buf) {
                Optional<ResourceLocation> location = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                Optional<CompoundTag> nbtTag = ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).decode(buf);
                boolean isInverted = ByteBufCodecs.BOOL.decode(buf);
                boolean isExactNBT = ByteBufCodecs.BOOL.decode(buf);
                boolean isTag = ByteBufCodecs.BOOL.decode(buf);
                return new FilterItem(location, nbtTag, isInverted, isExactNBT, isTag);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FilterItem item) {
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, item.locationOptional());
                ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).encode(buf, item.nbtOptional());
                ByteBufCodecs.BOOL.encode(buf, item.isInverted);
                ByteBufCodecs.BOOL.encode(buf, item.isExactNBT);
                ByteBufCodecs.BOOL.encode(buf, item.isTag);
            }
        };

        public @Nullable ResourceLocation location;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterItem(Optional<ResourceLocation> location, Optional<CompoundTag> nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this(location.orElse(null), nbtTag.map(CompoundTag::copy).orElse(null), isInverted, isExactNBT, isTag);
        }

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

        protected Optional<ResourceLocation> locationOptional(){
            return Optional.ofNullable(location);
        }

        protected Optional<CompoundTag> nbtOptional(){
            return Optional.ofNullable(nbtTag).map(CompoundTag::copy);
        }

        @Override
        public void setLocation(@Nullable ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void setNBT(@Nullable CompoundTag nbtTag) {
            this.nbtTag = nbtTag == null ? null : nbtTag.copy();
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
            return new FilterItem(location, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterFluid(location, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof FilterItem item))
                return false;

            return isInverted == item.isInverted &&
                    isExactNBT == item.isExactNBT &&
                    isTag == item.isTag &&
                    Objects.equals(location, item.location) &&
                    Objects.equals(nbtTag, item.nbtTag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, nbtTag, isInverted, isExactNBT, isTag);
        }
    }

    public static class FilterFluid implements FilterResource {
        public static final Codec<FilterFluid> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        ResourceLocation.CODEC.optionalFieldOf("location").forGetter(FilterFluid::locationOptional),
                        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(FilterFluid::nbtOptional),
                        Codec.BOOL.fieldOf("inverted").forGetter(FilterFluid::getInverted),
                        Codec.BOOL.fieldOf("exact_nbt").forGetter(FilterFluid::getExactNBT),
                        Codec.BOOL.fieldOf("tag").forGetter(FilterFluid::getTag)
                ).apply(inst, FilterFluid::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FilterFluid> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FilterFluid decode(RegistryFriendlyByteBuf buf) {
                Optional<ResourceLocation> location = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                Optional<CompoundTag> nbtTag = ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).decode(buf);
                boolean isInverted = ByteBufCodecs.BOOL.decode(buf);
                boolean isExactNBT = ByteBufCodecs.BOOL.decode(buf);
                boolean isTag = ByteBufCodecs.BOOL.decode(buf);
                return new FilterFluid(location, nbtTag, isInverted, isExactNBT, isTag);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FilterFluid fluid) {
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, fluid.locationOptional());
                ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).encode(buf, fluid.nbtOptional());
                ByteBufCodecs.BOOL.encode(buf, fluid.isInverted);
                ByteBufCodecs.BOOL.encode(buf, fluid.isExactNBT);
                ByteBufCodecs.BOOL.encode(buf, fluid.isTag);
            }
        };

        public @Nullable ResourceLocation location;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterFluid(Optional<ResourceLocation> location, Optional<CompoundTag> nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this(location.orElse(null), nbtTag.map(CompoundTag::copy).orElse(null), isInverted, isExactNBT, isTag);
        }

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

        protected Optional<ResourceLocation> locationOptional(){
            return Optional.ofNullable(location);
        }

        protected Optional<CompoundTag> nbtOptional(){
            return Optional.ofNullable(nbtTag).map(CompoundTag::copy);
        }

        @Override
        public void setLocation(@Nullable ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void setNBT(@Nullable CompoundTag nbtTag) {
            this.nbtTag = nbtTag == null ? null : nbtTag.copy();
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
            return new FilterFluid(location, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterItem(location, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof FilterFluid fluid))
                return false;

            return isInverted == fluid.isInverted &&
                    isExactNBT == fluid.isExactNBT &&
                    isTag == fluid.isTag &&
                    Objects.equals(location, fluid.location) &&
                    Objects.equals(nbtTag, fluid.nbtTag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, nbtTag, isInverted, isExactNBT, isTag);
        }
    }
}
