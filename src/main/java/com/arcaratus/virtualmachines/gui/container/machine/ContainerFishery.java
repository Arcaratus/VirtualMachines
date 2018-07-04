package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.container.ContainerTileAugmentable;
import cofh.core.gui.slot.*;
import cofh.thermalexpansion.util.managers.device.FisherManager;
import com.arcaratus.virtualmachines.block.machine.TileFishery;
import com.arcaratus.virtualmachines.utils.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerFishery extends ContainerTileAugmentable
{
    TileFishery myTile;

    public ContainerFishery(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileFishery) tile;

        addSlotToContainer(new SlotValidated(stack -> Utils.checkTool(stack, "fishing_rod"), myTile, TileFishery.SLOT_FISHING_ROD, 38, 46));
        addSlotToContainer(new SlotValidated(FisherManager::isValidBait, myTile, TileFishery.SLOT_BAIT, 38, 23));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                addSlotToContainer(new SlotRemoveOnly(myTile, TileFishery.SLOT_OUTPUT_START + j + i * 3, 92 + j * 18, 17 + i * 18));

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }
}
