package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.container.ContainerTileAugmentable;
import cofh.core.gui.slot.*;
import cofh.thermalexpansion.init.TEItems;
import com.arcaratus.virtualmachines.block.machine.TileAnimalFarm;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerAnimalFarm extends ContainerTileAugmentable
{
    TileAnimalFarm myTile;

    public ContainerAnimalFarm(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileAnimalFarm) tile;

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                addSlotToContainer(new SlotValidated(stack -> stack.getItem() != TEItems.itemMorb, myTile, j + i * 2, 30 + j * 18, 35 + i * 18));

        addSlotToContainer(new SlotValidated(stack -> stack.getItem() == TEItems.itemMorb, myTile, TileAnimalFarm.SLOT_ANIMAL_MORB, 70, 24));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                addSlotToContainer(new SlotRemoveOnly(myTile, TileAnimalFarm.SLOT_OUTPUT_START + j + i * 3, 92 + j * 18, 17 + i * 18));

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }
}
