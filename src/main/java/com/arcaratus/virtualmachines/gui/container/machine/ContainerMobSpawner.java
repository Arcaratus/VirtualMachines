package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.slot.*;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.init.TEItems;
import com.arcaratus.virtualmachines.block.machine.TileMobSpawner;
import com.arcaratus.virtualmachines.utils.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerMobSpawner extends ContainerTEBase
{
    TileMobSpawner myTile;

    public ContainerMobSpawner(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileMobSpawner) tile;

        addSlotToContainer(new SlotValidated(stack -> myTile.augmentMorbCapture() ? stack.getItem() == TEItems.itemMorb : Utils.checkTool(stack, "sword"), myTile, TileMobSpawner.SLOT_SWORD, 38, 35));

        addSlotToContainer(new SlotValidated(stack -> stack.getItem() == TEItems.itemMorb, myTile, TileMobSpawner.SLOT_MORB, 66, 25));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                addSlotToContainer(new SlotRemoveOnly(myTile, TileMobSpawner.SLOT_OUTPUT_START + j + i * 3, 92 + j * 18, 17 + i * 18));

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }
}
