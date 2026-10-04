package wootrevived.woot.upgrades.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.*;

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
        if(resource.getIdentifier() != null){
            if(resource.getType() == FilterType.ITEM)
                itemList.add((FilterItem) resource);
            else
                fluidList.add((FilterFluid) resource);

            resourceOrder.add(resource.getType());
        }
    }

    protected void add(int index, FilterResource resource){
        if(resource.getIdentifier() != null){
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

        if(index == -1 || newer.getIdentifier() == null)
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

    public void filterItems(List<ItemStack> items, HolderLookup.@Nullable Provider lookupProvider){
        if(size() == 0)
            return;

        items.removeIf(stack -> !shouldKeepItem(stack, lookupProvider));
    }

    public void filterFluids(List<FluidStack> fluids){
        filterFluids(fluids, null);
    }

    public void filterFluids(List<FluidStack> fluids, HolderLookup.@Nullable Provider lookupProvider){
        if(size() == 0)
            return;

        fluids.removeIf(stack -> !shouldKeepFluid(stack, lookupProvider));
    }

    protected boolean shouldKeepItem(ItemStack stack, HolderLookup.@Nullable Provider lookupProvider){
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

    protected boolean shouldKeepFluid(FluidStack stack, HolderLookup.@Nullable Provider lookupProvider){
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

    protected boolean matchesItem(ItemStack stack, FilterItem item, HolderLookup.@Nullable Provider lookupProvider){
        Identifier identifier = item.getIdentifier();

        if(identifier == null)
            return false;

        if(item.getTag()){
            if(!stack.is(createItemTag(identifier)))
                return false;
        } else if(!Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), identifier)) {
            return false;
        }

        return matchesNBT(getComponentTag(stack.getComponentsPatch(), lookupProvider), item);
    }

    protected boolean matchesFluid(FluidStack stack, FilterFluid fluid, HolderLookup.@Nullable Provider lookupProvider){
        Identifier identifier = fluid.getIdentifier();

        if(identifier == null)
            return false;

        if(fluid.getTag()){
            if(!stack.getFluid().defaultFluidState().is(createFluidTag(identifier)))
                return false;
        } else {
            Fluid filterFluid = BuiltInRegistries.FLUID.get(identifier).map(holder -> holder.value()).orElse(Fluids.EMPTY);
            if(filterFluid == Fluids.EMPTY || !FluidStack.isSameFluidSameComponents(stack, new FluidStack(filterFluid, stack.getAmount())))
                return false;
        }

        return matchesNBT(getComponentTag(stack.getComponentsPatch(), lookupProvider), fluid);
    }

    protected CompoundTag getComponentTag(DataComponentPatch patch, HolderLookup.@Nullable Provider lookupProvider){
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
            keys.addAll(filterCompound.keySet());
            keys.addAll(stackCompound.keySet());

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

            for(String key : filterCompound.keySet()){
                Tag child = filterCompound.get(key);
                if(!stackCompound.contains(key) || stackCompound.get(key).getId() != child.getId())
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

    public static TagKey<Item> createItemTag(Identifier identifier){
        return TagKey.create(Registries.ITEM, identifier);
    }

    public static TagKey<Fluid> createFluidTag(Identifier identifier){
        return TagKey.create(Registries.FLUID, identifier);
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
        void setIdentifier(@Nullable Identifier identifier);
        void setNBT(@Nullable CompoundTag nbtTag);
        void setInverted(boolean isInverted);
        void setExactNBT(boolean isExactNBT);
        void setTag(boolean isTag);

        @Nullable Identifier getIdentifier();
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
                        Identifier.CODEC.optionalFieldOf("location").forGetter(FilterItem::identifierOptional),
                        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(FilterItem::nbtOptional),
                        Codec.BOOL.fieldOf("inverted").forGetter(FilterItem::getInverted),
                        Codec.BOOL.fieldOf("exact_nbt").forGetter(FilterItem::getExactNBT),
                        Codec.BOOL.fieldOf("tag").forGetter(FilterItem::getTag)
                ).apply(inst, FilterItem::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FilterItem> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FilterItem decode(RegistryFriendlyByteBuf buf) {
                Optional<Identifier> identifier = ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf);
                Optional<CompoundTag> nbtTag = ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).decode(buf);
                boolean isInverted = ByteBufCodecs.BOOL.decode(buf);
                boolean isExactNBT = ByteBufCodecs.BOOL.decode(buf);
                boolean isTag = ByteBufCodecs.BOOL.decode(buf);
                return new FilterItem(identifier, nbtTag, isInverted, isExactNBT, isTag);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FilterItem item) {
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, item.identifierOptional());
                ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).encode(buf, item.nbtOptional());
                ByteBufCodecs.BOOL.encode(buf, item.isInverted);
                ByteBufCodecs.BOOL.encode(buf, item.isExactNBT);
                ByteBufCodecs.BOOL.encode(buf, item.isTag);
            }
        };

        public @Nullable Identifier identifier;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterItem(Optional<Identifier> identifier, Optional<CompoundTag> nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this(identifier.orElse(null), nbtTag.map(CompoundTag::copy).orElse(null), isInverted, isExactNBT, isTag);
        }

        protected FilterItem(@Nullable Identifier identifier, @Nullable CompoundTag nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this.identifier = identifier;
            this.nbtTag = nbtTag;
            this.isInverted = isInverted;
            this.isExactNBT = isExactNBT;
            this.isTag = isTag;
        }

        public FilterItem(){
            this.identifier = null;
            this.nbtTag = null;
            this.isInverted = false;
            this.isExactNBT = false;
            this.isTag = false;
        }

        protected Optional<Identifier> identifierOptional(){
            return Optional.ofNullable(identifier);
        }

        protected Optional<CompoundTag> nbtOptional(){
            return Optional.ofNullable(nbtTag).map(CompoundTag::copy);
        }

        @Override
        public void setIdentifier(@Nullable Identifier identifier) {
            this.identifier = identifier;
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
        public @Nullable Identifier getIdentifier() {
            return identifier;
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
            return new FilterItem(identifier, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterFluid(identifier, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof FilterItem item))
                return false;

            return isInverted == item.isInverted &&
                    isExactNBT == item.isExactNBT &&
                    isTag == item.isTag &&
                    Objects.equals(identifier, item.identifier) &&
                    Objects.equals(nbtTag, item.nbtTag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, nbtTag, isInverted, isExactNBT, isTag);
        }
    }

    public static class FilterFluid implements FilterResource {
        public static final Codec<FilterFluid> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        Identifier.CODEC.optionalFieldOf("location").forGetter(FilterFluid::identifierOptional),
                        CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(FilterFluid::nbtOptional),
                        Codec.BOOL.fieldOf("inverted").forGetter(FilterFluid::getInverted),
                        Codec.BOOL.fieldOf("exact_nbt").forGetter(FilterFluid::getExactNBT),
                        Codec.BOOL.fieldOf("tag").forGetter(FilterFluid::getTag)
                ).apply(inst, FilterFluid::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FilterFluid> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FilterFluid decode(RegistryFriendlyByteBuf buf) {
                Optional<Identifier> identifier = ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf);
                Optional<CompoundTag> nbtTag = ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).decode(buf);
                boolean isInverted = ByteBufCodecs.BOOL.decode(buf);
                boolean isExactNBT = ByteBufCodecs.BOOL.decode(buf);
                boolean isTag = ByteBufCodecs.BOOL.decode(buf);
                return new FilterFluid(identifier, nbtTag, isInverted, isExactNBT, isTag);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FilterFluid fluid) {
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, fluid.identifierOptional());
                ByteBufCodecs.optional(ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)).encode(buf, fluid.nbtOptional());
                ByteBufCodecs.BOOL.encode(buf, fluid.isInverted);
                ByteBufCodecs.BOOL.encode(buf, fluid.isExactNBT);
                ByteBufCodecs.BOOL.encode(buf, fluid.isTag);
            }
        };

        public @Nullable Identifier identifier;
        public @Nullable CompoundTag nbtTag;
        public boolean isInverted;
        public boolean isExactNBT;
        public boolean isTag;

        protected FilterFluid(Optional<Identifier> identifier, Optional<CompoundTag> nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this(identifier.orElse(null), nbtTag.map(CompoundTag::copy).orElse(null), isInverted, isExactNBT, isTag);
        }

        protected FilterFluid(@Nullable Identifier identifier, @Nullable CompoundTag nbtTag, boolean isInverted, boolean isExactNBT, boolean isTag){
            this.identifier = identifier;
            this.nbtTag = nbtTag;
            this.isInverted = isInverted;
            this.isExactNBT = isExactNBT;
            this.isTag = isTag;
        }

        public FilterFluid(){
            this.identifier = null;
            this.nbtTag = null;
            this.isInverted = false;
            this.isExactNBT = false;
            this.isTag = false;
        }

        protected Optional<Identifier> identifierOptional(){
            return Optional.ofNullable(identifier);
        }

        protected Optional<CompoundTag> nbtOptional(){
            return Optional.ofNullable(nbtTag).map(CompoundTag::copy);
        }

        @Override
        public void setIdentifier(@Nullable Identifier identifier) {
            this.identifier = identifier;
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
        public @Nullable Identifier getIdentifier() {
            return identifier;
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
            return new FilterFluid(identifier, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public FilterResource switchType() {
            return new FilterItem(identifier, nbtTag == null ? null : nbtTag.copy(), isInverted, isExactNBT, isTag);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof FilterFluid fluid))
                return false;

            return isInverted == fluid.isInverted &&
                    isExactNBT == fluid.isExactNBT &&
                    isTag == fluid.isTag &&
                    Objects.equals(identifier, fluid.identifier) &&
                    Objects.equals(nbtTag, fluid.nbtTag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, nbtTag, isInverted, isExactNBT, isTag);
        }
    }
}
