package wootrevived.api.menus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import wootrevived.api.WootUpgradeItem;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Base menu implementation for configuration screens attached to installed
 * {@link WootUpgradeItem} instances.
 * <p>
 * A {@code WootUpgradeItemMenu} is opened from a factory upgrade block and
 * receives two pieces of upgrade context:
 * <ul>
 *     <li>the {@link BlockPos} of the factory upgrade block, used for validity
 *     checks and synchronization,</li>
 *     <li>a mutable {@link ItemStack} copy containing the installed upgrade
 *     item's current data components.</li>
 * </ul>
 * <p>
 * Subclasses are expected to expose their own typed state by reading from and
 * writing to {@link #itemStack}. When the menu changes that state, it should
 * call {@link #syncItemStackComponents()}. Woot then sends only the
 * {@link DataComponentPatch} produced by the mutable stack, allowing the server
 * to update the installed upgrade's components without trusting a client-sent
 * item id, stack size, or arbitrary item stack state.
 * <p>
 * This class intentionally does not define slots or screen layout. Concrete
 * upgrade menus remain responsible for adding player slots, custom slots, and
 * any menu-specific accessors required by their screen.
 */
public abstract class WootUpgradeItemMenu extends AbstractContainerMenu {
    /**
     * Position of the factory upgrade block containing the configured upgrade.
     */
    protected BlockPos blockPos;

    /**
     * Mutable copy of the installed upgrade item stack used by this menu.
     */
    protected ItemStack itemStack;

    /**
     * Registry access from the player level that opened this menu.
     * <p>
     * Upgrade menus can use this when encoding or decoding data components
     * that need a registry-aware {@link HolderLookup.Provider}.
     */
    protected RegistryAccess registryAccess;

    /**
     * Internal synchronization hook installed by Woot during network
     * initialization.
     * <p>
     * Addons should call {@link #syncItemStackComponents()} instead of using
     * this field directly.
     */
    @ApiStatus.Internal
    public static BiConsumer<BlockPos, DataComponentPatch> SYNC;

    /**
     * Internal supplier for the factory upgrade block used by
     * {@link #stillValid(Player)}.
     */
    @ApiStatus.Internal
    public static Supplier<Block> GET_FACTORY_BLOCK;

    private WootUpgradeItemMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    /**
     * Creates a menu with explicit upgrade context.
     * <p>
     * This constructor is typically used on the logical server from
     * {@link WootUpgradeItem#createMenu(int, BlockPos, ItemStack, Inventory, Player)}.
     *
     * @param menuType    the registered menu type for the upgrade menu
     * @param containerId the vanilla container id
     * @param inv         the opening player's inventory
     * @param blockPos    the factory upgrade block position
     * @param itemStack   mutable copy of the installed upgrade item stack
     */
    protected WootUpgradeItemMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, BlockPos blockPos, ItemStack itemStack) {
        super(menuType, containerId);
        this.blockPos = blockPos;
        this.itemStack = itemStack;
        this.registryAccess = inv.player.level().registryAccess();
    }

    /**
     * Creates a menu by reading upgrade context from the network buffer.
     * <p>
     * This constructor is typically used on the logical client by the menu type
     * factory registered for the upgrade. Only the component patch is read from
     * the buffer; the concrete menu supplies the local mutable stack instance
     * that receives those components.
     *
     * @param menuType    the registered menu type for the upgrade menu
     * @param containerId the vanilla container id
     * @param inv         the opening player's inventory
     * @param data        network data written when the menu was opened
     * @param itemStack   mutable local stack for this menu's upgrade item
     */
    protected WootUpgradeItemMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, FriendlyByteBuf data, ItemStack itemStack) {
        this(menuType, containerId, inv, data.readBlockPos(), itemStack);
        this.itemStack.applyComponents(DataComponentPatch.STREAM_CODEC.decode((RegistryFriendlyByteBuf) data));
    }

    /**
     * Synchronizes the current {@link #itemStack} components back to the
     * server-side factory upgrade block.
     * <p>
     * Call this after the menu has written its validated state into
     * {@link #itemStack}. This sends only the stack's component patch; the
     * server applies the patch to the already installed upgrade stack after its
     * own access and consistency checks.
     */
    public final void syncItemStackComponents() {
        SYNC.accept(blockPos, itemStack.getComponentsPatch());
    }

    /**
     * Checks whether the menu is still usable by the player.
     * <p>
     * The default implementation uses the stored factory upgrade block position
     * and the factory upgrade block supplied internally by Woot.
     *
     * @param player the player using the menu
     * @return {@code true} while the player can still access the upgrade block
     */
    @Override
    @ApiStatus.Internal
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, GET_FACTORY_BLOCK.get());
    }
}
