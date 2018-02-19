package com.arcaratus.virtualmachines.gui.client.machine;

import cofh.core.gui.element.*;
import cofh.thermalexpansion.gui.client.GuiPoweredBase;
import cofh.thermalexpansion.gui.element.ElementSlotOverlay;
import cofh.thermalexpansion.gui.element.ElementSlotOverlay.*;
import com.arcaratus.virtualmachines.block.machine.TileFishery;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerFishery;
import com.arcaratus.virtualmachines.gui.element.ElementSlotOverlay3x3;
import com.arcaratus.virtualmachines.init.VMConstants;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class GuiFishery extends GuiPoweredBase
{
    public static final String TEX_PATH = VMConstants.PATH_MACHINE_GUI + "fishery.png";
    public static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

    private ElementSlotOverlay[] slotInputPrimary = new ElementSlotOverlay[2];
    private ElementSlotOverlay[] slotInputSecondary = new ElementSlotOverlay[2];
    private ElementSlotOverlay3x3 slotOutput;

    private ElementDualScaled progressOverlay;
    private ElementFluid progressFluid;

    private TileFishery myTile;

    public GuiFishery(InventoryPlayer inventory, TileEntity tile)
    {
        super(new ContainerFishery(inventory, tile), tile, inventory.player, TEXTURE);

        generateInfo(VMConstants.MACHINE_GUI_INFO + "fishery");

        myTile = (TileFishery) tile;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        slotInputPrimary[0] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 38, 46).setSlotInfo(SlotColor.BLUE, SlotType.STANDARD, SlotRender.FULL));
        slotInputPrimary[1] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 38, 46).setSlotInfo(SlotColor.GREEN, SlotType.STANDARD, SlotRender.BOTTOM));

        slotInputSecondary[0] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 38, 23).setSlotInfo(SlotColor.BLUE, SlotType.STANDARD, SlotRender.FULL));
        slotInputSecondary[1] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 38, 23).setSlotInfo(SlotColor.PURPLE, SlotType.STANDARD, SlotRender.BOTTOM));

        slotOutput = (ElementSlotOverlay3x3) addElement(new ElementSlotOverlay3x3(this, 92, 17).setSlotInfo(SlotColor.ORANGE, SlotRender.FULL));

        if (!myTile.smallStorage())
            addElement(new ElementEnergyStored(this, 8, 8, myTile.getEnergyStorage()));

        addElement(new ElementFluidTank(this, 152, 9, myTile.getTank()).setGauge(1).setAlwaysShow(true));
        progressFluid = (ElementFluid) addElement(new ElementFluid(this, 62, 34).setFluid(myTile.getTankFluid()).setSize(24, 16));
        progressOverlay = (ElementDualScaled) addElement(new ElementDualScaled(this, 62, 34).setMode(1).setBackground(false).setSize(24, 16).setTexture(TEX_ARROW_FLUID_RIGHT, 64, 16));
    }

    @Override
    protected void updateElementInformation()
    {
        super.updateElementInformation();

        slotInputPrimary[0].setEnabled(myTile.hasSideType(INPUT_ALL) || myTile.hasSideType(OMNI));
        slotInputPrimary[1].setEnabled(myTile.hasSideType(INPUT_PRIMARY));
        slotInputSecondary[0].setEnabled(myTile.hasSideType(INPUT_ALL) || myTile.hasSideType(OMNI));
        slotInputSecondary[1].setEnabled(myTile.hasSideType(INPUT_SECONDARY));
        slotOutput.setEnabled(myTile.hasSideType(OUTPUT_ALL) || myTile.hasSideType(OMNI));

        progressFluid.setFluid(baseTile.getTankFluid());
        progressFluid.setSize(baseTile.getScaledProgress(PROGRESS), 16);
        progressOverlay.setQuantity(baseTile.getScaledProgress(PROGRESS));

        if (!myTile.hasSideType(INPUT_ALL))
        {
            slotInputPrimary[1].setSlotRender(SlotRender.FULL);
            slotInputSecondary[1].setSlotRender(SlotRender.FULL);
        }
        else
        {
            slotInputPrimary[1].setSlotRender(SlotRender.BOTTOM);
            slotInputSecondary[1].setSlotRender(SlotRender.BOTTOM);
        }
    }
}
