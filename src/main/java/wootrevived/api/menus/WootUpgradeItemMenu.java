package wootrevived.api.menus;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
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
 *     <li>the persistent {@link CompoundTag} stored on the installed upgrade
 *     item stack.</li>
 * </ul>
 * <p>
 * Subclasses are expected to expose their own typed state by reading from
 * {@link #itemTag}. When the menu changes that state, it should write the
 * updated data back into {@link #itemTag} and call {@link #syncItemTag()}.
 * Woot then sends the updated tag to the server-side factory upgrade block.
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
     * Persistent NBT data stored on the installed upgrade item stack.
     */
    protected CompoundTag itemTag;

    /**
     * Internal synchronization hook installed by Woot during network
     * initialization.
     * <p>
     * Addons should call {@link #syncItemTag()} instead of using this field
     * directly.
     */
    @ApiStatus.Internal
    public static BiConsumer<BlockPos, CompoundTag> SYNC;

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
     * {@link WootUpgradeItem#createMenu(int, BlockPos, CompoundTag, Inventory, Player)}.
     *
     * @param menuType    the registered menu type for the upgrade menu
     * @param containerId the vanilla container id
     * @param inv         the opening player's inventory
     * @param blockPos    the factory upgrade block position
     * @param itemTag     persistent data for the installed upgrade item
     */
    protected WootUpgradeItemMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, BlockPos blockPos, CompoundTag itemTag) {
        super(menuType, containerId);
        this.blockPos = blockPos;
        this.itemTag = itemTag;
    }

    /**
     * Creates a menu by reading upgrade context from the network buffer.
     * <p>
     * This constructor is typically used on the logical client by the menu type
     * factory registered for the upgrade.
     *
     * @param menuType    the registered menu type for the upgrade menu
     * @param containerId the vanilla container id
     * @param inv         the opening player's inventory
     * @param data        network data written when the menu was opened
     */
    protected WootUpgradeItemMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, FriendlyByteBuf data) {
        this(menuType, containerId, inv, data.readBlockPos(), data.readNbt());
    }

    /**
     * Synchronizes the current {@link #itemTag} back to the server-side factory
     * upgrade block.
     * <p>
     * Call this after the menu has written its validated state into
     * {@link #itemTag}. This method sends the complete upgrade item tag used by
     * the current menu architecture.
     */
    public final void syncItemTag() {
        SYNC.accept(blockPos, itemTag);
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
