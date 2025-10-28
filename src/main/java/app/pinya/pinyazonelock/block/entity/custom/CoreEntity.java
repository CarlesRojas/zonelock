package app.pinya.pinyazonelock.block.entity.custom;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.block.custom.Core;
import app.pinya.pinyazonelock.block.entity.ModBlocksEntities;
import app.pinya.pinyazonelock.screen.custom.CoreMenu;
import app.pinya.pinyazonelock.world.LockedZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class CoreEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);
    private boolean initialized = false;

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            if (!level.isClientSide()) {
                boolean hasItem = !getStackInSlot(0).isEmpty();
                BlockPos zonePos = getBlockPos();

                BlockState newState = getBlockState().setValue(Core.ACTIVE, hasItem);
                level.setBlockAndUpdate(zonePos, newState);

                if (level instanceof ServerLevel serverLevel)
                    LockedZones.get(serverLevel).setActive(zonePos, hasItem);

                level.sendBlockUpdated(zonePos, getBlockState(), newState, 3);
            }
        }
    };

    public CoreEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlocksEntities.ZONE_LOCK_CORE_ENTITY.get(), pPos, pBlockState);
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public void dropContents() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    // MenuProvider

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new CoreMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pinyazonelock.core");
    }

    // BlockEntity

    @Override
    public void onLoad() {
        if (!level.isClientSide)
            initialize();

    }

    public void initialize() {
        if (initialized)
            return;

        initialized = true;
        setChanged();

        LOGGER.info("Core block created at {}", getBlockPos());

        if (!level.isClientSide && level instanceof ServerLevel sLevel) {
            BlockPos zonePos = getBlockPos();
            BlockState state = getBlockState();

            LockedZones lockedZones = LockedZones.get(sLevel);
            lockedZones.addZone(zonePos, 8, 8, 8, 8, 8, 8);

            boolean hasItem = level.getBlockEntity(zonePos) instanceof CoreEntity coreEntity
                    && !coreEntity.inventory.getStackInSlot(0).isEmpty();

            lockedZones.setActive(zonePos, hasItem);
            level.setBlockAndUpdate(zonePos, state.setValue(Core.ACTIVE, hasItem));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", inventory.serializeNBT(pRegistries));
        pTag.putBoolean("initialized", initialized);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        inventory.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        initialized = pTag.getBoolean("initialized");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}