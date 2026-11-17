package app.pinya.zonelock.menu;

import app.pinya.zonelock.block.CoreBlockEntity;
import app.pinya.zonelock.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The core's screen as a container, like a beacon's: one slot for the focus block (iron, gold, emerald, diamond,
 * netherite... it sets how far the zone may reach) and the player's inventory. The rest of the screen (name, rules,
 * size) is driven by {@link app.pinya.zonelock.network.CoreScreenPayload}. On the server the slot is the core's own
 * focus (see {@link FocusContainer}); on the client a plain container the server keeps in sync.
 */
public class CoreMenu extends AbstractContainerMenu {
    /** Size of the screen, and where its slots are, in screen pixels. */
    public static final int WIDTH = 248, HEIGHT = 214;
    public static final int FOCUS_X = 9, FOCUS_Y = 53;
    public static final int INVENTORY_X = 44, INVENTORY_Y = 132, HOTBAR_Y = 190;
    private static final int FOCUS_SLOT = 0, INVENTORY_START = 1, HOTBAR_START = 28, END = 37;

    private final BlockPos pos;
    private final Container focus;

    /** Client side. */
    public CoreMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, new SimpleContainer(1));
    }

    public CoreMenu(int id, Inventory inventory, BlockPos pos, Container focus) {
        super(ModMenus.CORE.get(), id);
        this.pos = pos;
        this.focus = focus;
        addSlot(new Slot(focus, 0, FOCUS_X, FOCUS_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CoreBlockEntity.isFocus(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
    }

    public BlockPos pos() {
        return pos;
    }

    /** Shift-click: a focus block goes into the empty focus slot, the focus back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == FOCUS_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, END, true))
                return ItemStack.EMPTY;
        } else if (CoreBlockEntity.isFocus(stack) && !slots.get(FOCUS_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, FOCUS_SLOT, FOCUS_SLOT + 1, false))
                return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, END, false))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return focus.stillValid(player);
    }
}
