package app.pinya.pinyazonelock.screen.custom;

import app.pinya.pinyazonelock.block.ModBlocks;
import app.pinya.pinyazonelock.block.entity.custom.CoreEntity;
import app.pinya.pinyazonelock.screen.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class CoreMenu extends AbstractContainerMenu {
    public final CoreEntity blockEntity;
    private final Level level;

    public static final int MIN_SIDE = 0;

    public CoreMenu(int contianerId, Inventory inv, FriendlyByteBuf extraData) {
        this(contianerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CoreMenu(int contianerId, Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.ZONE_LOCK_CORE_MENU.get(), contianerId);
        this.blockEntity = (CoreEntity) blockEntity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(this.blockEntity.inventory, 0, 130, 128));
    }

    public void incrementUpBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getUpBlocks() < maxSide) {
            int newValue = blockEntity.getUpBlocks() + 1;
            blockEntity.setUpBlocks(newValue);
        }
    }

    public void decrementUpBlocks() {
        if (blockEntity.getUpBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getUpBlocks() - 1;
            blockEntity.setUpBlocks(newValue);
        }
    }

    public void incrementDownBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getDownBlocks() < maxSide) {
            int newValue = blockEntity.getDownBlocks() + 1;
            blockEntity.setDownBlocks(newValue);
        }
    }

    public void decrementDownBlocks() {
        if (blockEntity.getDownBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getDownBlocks() - 1;
            blockEntity.setDownBlocks(newValue);
        }
    }

    public void incrementNorthBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getNorthBlocks() < maxSide) {
            int newValue = blockEntity.getNorthBlocks() + 1;
            blockEntity.setNorthBlocks(newValue);
        }
    }

    public void decrementNorthBlocks() {
        if (blockEntity.getNorthBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getNorthBlocks() - 1;
            blockEntity.setNorthBlocks(newValue);
        }
    }

    public void incrementSouthBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getSouthBlocks() < maxSide) {
            int newValue = blockEntity.getSouthBlocks() + 1;
            blockEntity.setSouthBlocks(newValue);
        }
    }

    public void decrementSouthBlocks() {
        if (blockEntity.getSouthBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getSouthBlocks() - 1;
            blockEntity.setSouthBlocks(newValue);
        }
    }

    public void incrementEastBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getEastBlocks() < maxSide) {
            int newValue = blockEntity.getEastBlocks() + 1;
            blockEntity.setEastBlocks(newValue);
        }
    }

    public void decrementEastBlocks() {
        if (blockEntity.getEastBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getEastBlocks() - 1;
            blockEntity.setEastBlocks(newValue);
        }
    }

    public void incrementWestBlocks() {
        int maxSide = blockEntity.getMaxSideBlocks();
        if (blockEntity.getWestBlocks() < maxSide) {
            int newValue = blockEntity.getWestBlocks() + 1;
            blockEntity.setWestBlocks(newValue);
        }
    }

    public void decrementWestBlocks() {
        if (blockEntity.getWestBlocks() > MIN_SIDE) {
            int newValue = blockEntity.getWestBlocks() - 1;
            blockEntity.setWestBlocks(newValue);
        }
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the
    // player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the
    // slotIndex, which means
    // 0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 -
    // 8)
    // 9 - 35 = player inventory slots (which map to the InventoryPlayer slot
    // numbers 9 - 35)
    // 36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 -
    // 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 1; // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem())
            return ItemStack.EMPTY; // EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY; // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                    false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), pPlayer,
                ModBlocks.ZONE_LOCK_CORE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 165 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 223));
        }
    }
}
