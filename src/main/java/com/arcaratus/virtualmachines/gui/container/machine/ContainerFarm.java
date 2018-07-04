package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.container.ContainerTileAugmentable;
import cofh.core.gui.slot.SlotEnergy;
import cofh.core.gui.slot.SlotValidated;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager;
import com.arcaratus.virtualmachines.block.machine.TileFarm;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ContainerFarm extends ContainerTileAugmentable
{
    TileFarm myTile;

    public ContainerFarm(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileFarm) tile;

        /* Farm inventory */
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 9; j++)
                addSlotToContainer(new Slot(myTile, j + i * 9, 8 + j * 18, 77 + i * 18));

        /* Tools */
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                addSlotToContainer(new SlotValidated(stack -> Utils.checkTool(stack, "axe", "shears", "hoe", "sickle"), myTile, TileFarm.SLOT_TOOLS_START + j + i * 2, 34 + j * 18, 37 + i * 18));

        addSlotToContainer(new SlotValidated(VirtualFarm::isFertilizer, myTile, TileFarm.SLOT_FERTILIZER, 34, 11));

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                ItemStack lockStack = myTile.getItemLocks().get(j + i * 3);
                addSlotToContainer(new SlotValidated(stack -> InsolatorManager.isItemValid(stack) && (myTile.lockPrimary ? (lockStack.isItemEqual(stack) || lockStack.isEmpty()) : lockStack.isEmpty()), myTile, TileFarm.SLOT_FARM_START + j + i * 3, 95 + j * 18, 19 + i * 18));
            }
        }

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }

    @Override
    protected int getPlayerInventoryVerticalOffset()
    {
        return 126;
    }
}
