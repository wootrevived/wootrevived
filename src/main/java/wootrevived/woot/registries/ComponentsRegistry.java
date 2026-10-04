package wootrevived.woot.registries;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wootrevived.api.internal.WootUpgradeComponent;
import wootrevived.woot.Woot;
import wootrevived.woot.data.*;
import wootrevived.woot.upgrades.filter.FilterLogic;

public class ComponentsRegistry {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Woot.MOD_ID);

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    /* Woot Upgrade Component API */

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WootUpgradeComponent>> WOOT_UPGRADE_COMPONENT =
            COMPONENTS.register(WootUpgradeComponent.ID, WootUpgradeComponent::type);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterLogic.Component>> FILTER_LOGIC_DATA =
            COMPONENTS.registerComponentType(
                    FilterLogic.ID,
                    builder -> builder
                            .persistent(FilterLogic.Component.CODEC)
                            .networkSynchronized(FilterLogic.Component.STREAM_CODEC)
            );

    /* Block Entity Data */

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CellData.Component>> CELL_DATA =
            COMPONENTS.registerComponentType(
                    CellData.ID,
                    builder -> builder
                            .persistent(CellData.CODEC)
                            .networkSynchronized(CellData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CreativeTankData.Component>> CREATIVE_TANK_DATA =
            COMPONENTS.registerComponentType(
                    CreativeTankData.ID,
                    builder -> builder
                            .persistent(CreativeTankData.CODEC)
                            .networkSynchronized(CreativeTankData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FactoryUpgradeData.Component>> FACTORY_UPGRADE_DATA =
            COMPONENTS.registerComponentType(
                    FactoryUpgradeData.ID,
                    builder -> builder
                            .persistent(FactoryUpgradeData.CODEC)
                            .networkSynchronized(FactoryUpgradeData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FakeSpawnerData.Component>> FAKE_SPAWNER_DATA =
            COMPONENTS.registerComponentType(
                    FakeSpawnerData.ID,
                    builder -> builder
                            .persistent(FakeSpawnerData.CODEC)
                            .networkSynchronized(FakeSpawnerData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MultiBlockFactoryData.Component>> MULTI_BLOCK_FACTORY_DATA =
            COMPONENTS.registerComponentType(
                    MultiBlockFactoryData.ID,
                    builder -> builder
                            .persistent(MultiBlockFactoryData.CODEC)
                            .networkSynchronized(MultiBlockFactoryData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IngredientImportData.Component>> INGREDIENT_IMPORT_DATA =
            COMPONENTS.registerComponentType(
                    IngredientImportData.ID,
                    builder -> builder
                            .persistent(IngredientImportData.CODEC)
                            .networkSynchronized(IngredientImportData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LayoutData.Component>> LAYOUT_DATA =
            COMPONENTS.registerComponentType(
                    LayoutData.ID,
                    builder -> builder
                            .persistent(LayoutData.CODEC)
                            .networkSynchronized(LayoutData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FactoryBlockData.Component>> FACTORY_BLOCK_DATA =
            COMPONENTS.registerComponentType(
                    FactoryBlockData.ID,
                    builder -> builder
                            .persistent(FactoryBlockData.CODEC)
                            .networkSynchronized(FactoryBlockData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeLiquifierData.Component>> DYE_LIQUIFIER_DATA =
            COMPONENTS.registerComponentType(
                    DyeLiquifierData.ID,
                    builder -> builder
                            .persistent(DyeLiquifierData.CODEC)
                            .networkSynchronized(DyeLiquifierData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnchantedLiquifierData.Component>> ENCHANTED_LIQUIFIER_DATA =
            COMPONENTS.registerComponentType(
                    EnchantedLiquifierData.ID,
                    builder -> builder
                            .persistent(EnchantedLiquifierData.CODEC)
                            .networkSynchronized(EnchantedLiquifierData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemInfuserData.Component>> ITEM_INFUSER_DATA =
            COMPONENTS.registerComponentType(
                    ItemInfuserData.ID,
                    builder -> builder
                            .persistent(ItemInfuserData.CODEC)
                            .networkSynchronized(ItemInfuserData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidInfuserData.Component>> FLUID_INFUSER_DATA =
            COMPONENTS.registerComponentType(
                    FluidInfuserData.ID,
                    builder -> builder
                            .persistent(FluidInfuserData.CODEC)
                            .networkSynchronized(FluidInfuserData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MobShardData.Component>> MOB_SHARD_DATA =
            COMPONENTS.registerComponentType(
                    MobShardData.ID,
                    builder -> builder
                            .persistent(MobShardData.CODEC)
                            .networkSynchronized(MobShardData.STREAM_CODEC)
            );
}
