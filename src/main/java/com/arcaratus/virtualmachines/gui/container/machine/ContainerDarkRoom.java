package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.container.ContainerTileAugmentable;
import cofh.core.gui.slot.*;
import com.arcaratus.virtualmachines.block.machine.TileDarkRoom;
import com.arcaratus.virtualmachines.utils.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerDarkRoom extends ContainerTileAugmentable
{
    TileDarkRoom myTile;

    public ContainerDarkRoom(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileDarkRoom) tile;

        addSlotToContainer(new SlotValidated(stack -> Utils.checkTool(stack, "sword"), myTile, TileDarkRoom.SLOT_SWORD, 38, 35));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                addSlotToContainer(new SlotRemoveOnly(myTile, TileDarkRoom.SLOT_OUTPUT_START + j + i * 3, 92 + j * 18, 17 + i * 18));

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }
}
