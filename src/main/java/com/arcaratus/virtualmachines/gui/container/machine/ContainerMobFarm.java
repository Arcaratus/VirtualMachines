package com.arcaratus.virtualmachines.gui.container.machine;

import cofh.core.gui.slot.*;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.init.TEItems;
import com.arcaratus.virtualmachines.block.machine.TileMobFarm;
import com.arcaratus.virtualmachines.utils.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerMobFarm extends ContainerTEBase
{
    TileMobFarm myTile;

    public ContainerMobFarm(InventoryPlayer player, TileEntity tile)
    {
        super(player, tile);

        myTile = (TileMobFarm) tile;

        addSlotToContainer(new SlotValidated(stack -> Utils.checkTool(stack, "sword"), myTile, TileMobFarm.SLOT_SWORD, 39, 35));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                addSlotToContainer(new SlotValidated(stack -> stack.getItem() == TEItems.itemMorb, myTile, TileMobFarm.SLOT_MORB_START + j + i * 3, 95 + j * 18, 17 + i * 18));

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 9; j++)
                addSlotToContainer(new SlotRemoveOnly(myTile, TileMobFarm.SLOT_OUTPUT_START + j + i * 9, 8 + j * 18, 77 + i * 18));

        addSlotToContainer(new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53));
    }

    @Override
    protected int getPlayerInventoryVerticalOffset()
    {
        return 126;
    }
}
