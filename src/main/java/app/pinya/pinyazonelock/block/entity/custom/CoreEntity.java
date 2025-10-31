package app.pinya.pinyazonelock.block.entity.custom;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.pinya.pinyazonelock.block.custom.Core;
import app.pinya.pinyazonelock.block.entity.ModBlocksEntities;
import app.pinya.pinyazonelock.networking.ModMessages;
import app.pinya.pinyazonelock.networking.UpdateZoneDimensionsC2SPacket;
import app.pinya.pinyazonelock.screen.custom.CoreMenu;
import app.pinya.pinyazonelock.sound.ModSound;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

public class CoreEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);

    private Rotation rotation = Rotation.NONE;

    private int northBlocks = 8;
    private int southBlocks = 8;
    private int eastBlocks = 8;
    private int westBlocks = 8;
    private int upBlocks = 8;
    private int downBlocks = 8;

    public static final int MAX_SIDE_IRON = 8;
    public static final int MAX_SIDE_GOLD = 16;
    public static final int MAX_SIDE_EMERALD = 24;
    public static final int MAX_SIDE_LAPIS = 32;
    public static final int MAX_SIDE_DIAMOND = 40;

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.IRON_BLOCK) ||
                    stack.is(Items.GOLD_BLOCK) ||
                    stack.is(Items.EMERALD_BLOCK) ||
                    stack.is(Items.LAPIS_BLOCK) ||
                    stack.is(Items.DIAMOND_BLOCK);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            if (!level.isClientSide()) {
                boolean hasItem = !getStackInSlot(0).isEmpty();
                BlockPos zonePos = getBlockPos();

                BlockState newState = getBlockState().setValue(Core.ACTIVE, hasItem);
                level.playSound(null, zonePos, hasItem ? ModSound.ACTIVATE_ZONE.get() : ModSound.DEACTIVATE_ZONE.get(),
                        SoundSource.BLOCKS, 0.6f, 1f);
                level.setBlockAndUpdate(zonePos, newState);

                if (level instanceof ServerLevel serverLevel)
                    LockedZones.get(serverLevel).setActive(zonePos, hasItem);

                resetBlockValues();
                level.sendBlockUpdated(zonePos, getBlockState(), newState, 3);
            }
        }
    };

    public CoreEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlocksEntities.ZONE_LOCK_CORE_ENTITY.get(), pPos, pBlockState);
        this.rotation = pBlockState.getValue(Core.ROTATION);
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
        if (!level.isClientSide && level instanceof ServerLevel sLevel) {
            BlockPos zonePos = getBlockPos();
            BlockState state = getBlockState();
            LockedZones lockedZones = LockedZones.get(sLevel);

            if (!lockedZones.hasZoneAt(zonePos))
                lockedZones.addZone(zonePos, upBlocks, downBlocks, northBlocks, southBlocks, eastBlocks, westBlocks);

            boolean hasItem = !inventory.getStackInSlot(0).isEmpty();
            lockedZones.setActive(zonePos, hasItem);
            level.setBlockAndUpdate(zonePos, state.setValue(Core.ACTIVE, hasItem));
            setChanged();

            if (rotation != Rotation.NONE)
                handleRotation(rotation, zonePos);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("inventory", inventory.serializeNBT(pRegistries));

        pTag.putInt("upBlocks", upBlocks);
        pTag.putInt("downBlocks", downBlocks);
        pTag.putInt("northBlocks", northBlocks);
        pTag.putInt("southBlocks", southBlocks);
        pTag.putInt("eastBlocks", eastBlocks);
        pTag.putInt("westBlocks", westBlocks);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        inventory.deserializeNBT(pRegistries, pTag.getCompound("inventory"));

        upBlocks = pTag.getInt("upBlocks");
        downBlocks = pTag.getInt("downBlocks");
        northBlocks = pTag.getInt("northBlocks");
        southBlocks = pTag.getInt("southBlocks");
        eastBlocks = pTag.getInt("eastBlocks");
        westBlocks = pTag.getInt("westBlocks");
    }

    public void setZoneDimensions(int up, int down, int north, int south, int east, int west) {
        upBlocks = up;
        downBlocks = down;
        northBlocks = north;
        southBlocks = south;
        eastBlocks = east;
        westBlocks = west;

        if (level.isClientSide)
            ModMessages.CHANNEL.send(
                    new UpdateZoneDimensionsC2SPacket(worldPosition, up, down, north, south, east, west),
                    PacketDistributor.SERVER.noArg());
    }

    public void setZoneDimensionsServer(int up, int down, int north, int south, int east, int west) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            upBlocks = up;
            downBlocks = down;
            northBlocks = north;
            southBlocks = south;
            eastBlocks = east;
            westBlocks = west;

            LockedZones.get(serverLevel).updateZone(worldPosition, up, down, north, south, east, west);

            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getMaxSideBlocks() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty())
            return 0;

        Item item = stack.getItem();

        if (item == Items.IRON_BLOCK)
            return MAX_SIDE_IRON;
        if (item == Items.GOLD_BLOCK)
            return MAX_SIDE_GOLD;
        if (item == Items.EMERALD_BLOCK)
            return MAX_SIDE_EMERALD;
        if (item == Items.LAPIS_BLOCK)
            return MAX_SIDE_LAPIS;
        if (item == Items.DIAMOND_BLOCK)
            return MAX_SIDE_DIAMOND;

        return 0;
    }

    public int getUpBlocks() {
        return upBlocks;
    }

    public void setUpBlocks(int value) {
        setZoneDimensions(value, downBlocks, northBlocks, southBlocks, eastBlocks, westBlocks);
    }

    public int getDownBlocks() {
        return downBlocks;
    }

    public void setDownBlocks(int value) {
        setZoneDimensions(upBlocks, value, northBlocks, southBlocks, eastBlocks, westBlocks);
    }

    public int getNorthBlocks() {
        return northBlocks;
    }

    public void setNorthBlocks(int value) {
        setZoneDimensions(upBlocks, downBlocks, value, southBlocks, eastBlocks, westBlocks);
    }

    public int getSouthBlocks() {
        return southBlocks;
    }

    public void setSouthBlocks(int value) {
        setZoneDimensions(upBlocks, downBlocks, northBlocks, value, eastBlocks, westBlocks);
    }

    public int getEastBlocks() {
        return eastBlocks;
    }

    public void setEastBlocks(int value) {
        setZoneDimensions(upBlocks, downBlocks, northBlocks, southBlocks, value, westBlocks);
    }

    public int getWestBlocks() {
        return westBlocks;
    }

    public void setWestBlocks(int value) {
        setZoneDimensions(upBlocks, downBlocks, northBlocks, southBlocks, eastBlocks, value);
    }

    private void resetBlockValues() {
        int maxSide = getMaxSideBlocks();
        if (maxSide == 0)
            return;

        boolean needsUpdate = false;

        if (upBlocks > maxSide) {
            upBlocks = maxSide;
            needsUpdate = true;
        }
        if (downBlocks > maxSide) {
            downBlocks = maxSide;
            needsUpdate = true;
        }
        if (northBlocks > maxSide) {
            northBlocks = maxSide;
            needsUpdate = true;
        }
        if (southBlocks > maxSide) {
            southBlocks = maxSide;
            needsUpdate = true;
        }
        if (eastBlocks > maxSide) {
            eastBlocks = maxSide;
            needsUpdate = true;
        }
        if (westBlocks > maxSide) {
            westBlocks = maxSide;
            needsUpdate = true;
        }

        if (needsUpdate && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            LockedZones.get(serverLevel).updateZone(worldPosition, upBlocks, downBlocks, northBlocks, southBlocks,
                    eastBlocks, westBlocks);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        CompoundTag tag = super.getUpdateTag(pRegistries);
        saveAdditional(tag, pRegistries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, Provider pRegistries) {
        super.handleUpdateTag(tag, pRegistries);
        loadAdditional(tag, pRegistries);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void handleRotation(Rotation rotation, BlockPos zonePos) {
        if (level != null && level instanceof ServerLevel serverLevel) {
            LOGGER.info("CoreEntity handling rotation: " + rotation);

            int newNorth, newSouth, newEast, newWest;

            switch (rotation) {
                case CLOCKWISE_90 -> {
                    newNorth = westBlocks;
                    newSouth = eastBlocks;
                    newEast = northBlocks;
                    newWest = southBlocks;
                }
                case CLOCKWISE_180 -> {
                    newNorth = southBlocks;
                    newSouth = northBlocks;
                    newEast = westBlocks;
                    newWest = eastBlocks;
                }
                case COUNTERCLOCKWISE_90 -> {
                    newNorth = eastBlocks;
                    newSouth = westBlocks;
                    newEast = southBlocks;
                    newWest = northBlocks;
                }
                default -> {
                    newNorth = northBlocks;
                    newSouth = southBlocks;
                    newEast = eastBlocks;
                    newWest = westBlocks;
                }
            }

            serverLevel.setBlockAndUpdate(zonePos, getBlockState().setValue(Core.ROTATION, Rotation.NONE));
            rotation = Rotation.NONE;
            setZoneDimensionsServer(upBlocks, downBlocks, newNorth, newSouth, newEast, newWest);
            setChanged();
        }
    }

}