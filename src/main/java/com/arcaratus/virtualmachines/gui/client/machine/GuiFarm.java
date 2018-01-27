package com.arcaratus.virtualmachines.gui.client.machine;

import cofh.core.gui.element.*;
import cofh.thermalexpansion.gui.client.GuiPoweredBase;
import cofh.thermalexpansion.gui.element.ElementSlotOverlay;
import cofh.thermalexpansion.gui.element.ElementSlotOverlay.*;
import cofh.thermalexpansion.gui.element.ElementSlotOverlayQuad;
import com.arcaratus.virtualmachines.block.machine.TileFarm;
import com.arcaratus.virtualmachines.gui.client.GhostItemRenderer;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerFarm;
import com.arcaratus.virtualmachines.gui.element.ElementSlotOverlay2x9;
import com.arcaratus.virtualmachines.gui.element.ElementSlotOverlay3x3;
import com.arcaratus.virtualmachines.init.VMConstants;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class GuiFarm extends GuiPoweredBase
{
    public static final String TEX_PATH = VMConstants.PATH_MACHINE_GUI + "farm.png";
    public static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

    private ElementSlotOverlay3x3[] slotInputPrimary = new ElementSlotOverlay3x3[2];
    private ElementSlotOverlayQuad[] slotInputSecondary = new ElementSlotOverlayQuad[2];
    private ElementSlotOverlay[] slotInputOtherSecondary = new ElementSlotOverlay[2];
    private ElementSlotOverlay2x9 slotOutput;

    private ElementFluid progressFluid;
    private ElementDualScaled progressOverlay;

    private ElementButton mode;
    private ElementSimple[] modeOverlay = new ElementSimple[9];

    private TileFarm myTile;

    public GuiFarm(InventoryPlayer inventory, TileEntity tile)
    {
        super(new ContainerFarm(inventory, tile), tile, inventory.player, TEXTURE);

        generateInfo(VMConstants.MACHINE_GUI_INFO + "farm");

        myTile = (TileFarm) tile;

        ySize = 208;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        slotInputPrimary[0] = (ElementSlotOverlay3x3) addElement(new ElementSlotOverlay3x3(this, 95, 19).setSlotInfo(SlotColor.BLUE, SlotRender.FULL));
        slotInputPrimary[1] = (ElementSlotOverlay3x3) addElement(new ElementSlotOverlay3x3(this, 95, 19).setSlotInfo(SlotColor.GREEN, SlotRender.BOTTOM));

        slotInputSecondary[0] = (ElementSlotOverlayQuad) addElement(new ElementSlotOverlayQuad(this, 34, 37).setSlotInfo(SlotColor.BLUE, SlotRender.FULL));
        slotInputSecondary[1] = (ElementSlotOverlayQuad) addElement(new ElementSlotOverlayQuad(this, 34, 37).setSlotInfo(SlotColor.PURPLE, SlotRender.BOTTOM));

        slotInputOtherSecondary[0] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 34, 11).setSlotInfo(SlotColor.BLUE, SlotType.STANDARD, SlotRender.FULL));
        slotInputOtherSecondary[1] = (ElementSlotOverlay) addElement(new ElementSlotOverlay(this, 34, 11).setSlotInfo(SlotColor.PURPLE, SlotType.STANDARD, SlotRender.BOTTOM));

        slotOutput = (ElementSlotOverlay2x9) addElement(new ElementSlotOverlay2x9(this, 8, 77));

        if (!myTile.smallStorage())
            addElement(new ElementEnergyStored(this, 8, 8, myTile.getEnergyStorage()));

        addElement(new ElementFluidTank(this, 152, 9, myTile.getTank()).setGauge(1).setAlwaysShow(true));
        progressFluid = (ElementFluid) addElement(new ElementFluid(this, 62, 16).setFluid(myTile.getTankFluid()).setSize(24, 16));
        progressOverlay = (ElementDualScaled) addElement(new ElementDualScaled(this, 62, 16).setBackground(false).setMode(1).setSize(24, 16).setTexture(TEX_ARROW_FLUID_RIGHT, 64, 16));

        mode = (ElementButton) addElement(new ElementButton(this, 73, 56, "Mode", 176, 0, 176, 16, 176, 32, 16, 16, TEX_PATH));
        for (int i = 0; i < 9; i++)
            modeOverlay[i] = (ElementSimple) addElement(new ElementSimple(this, 95 + ((i % 3) * 18), 19 + ((i / 3) * 18)).setTextureOffsets(176, 48).setSize(16, 16).setTexture(TEX_PATH, 256, 256));
    }

    @Override
    protected void updateElementInformation()
    {
        super.updateElementInformation();

        slotInputPrimary[0].setVisible(myTile.hasSideType(INPUT_ALL) || baseTile.hasSideType(OMNI));
        slotInputPrimary[1].setVisible(myTile.hasSideType(INPUT_PRIMARY));
        slotInputSecondary[0].setVisible(myTile.hasSideType(INPUT_ALL) || baseTile.hasSideType(OMNI));
        slotInputSecondary[1].setVisible(myTile.hasSideType(INPUT_SECONDARY));
        slotInputOtherSecondary[0].setVisible(myTile.hasSideType(INPUT_ALL) || baseTile.hasSideType(OMNI));
        slotInputOtherSecondary[1].setVisible(myTile.hasSideType(INPUT_SECONDARY));

        slotOutput.setVisible(myTile.hasSideType(OUTPUT_ALL));

        if (!baseTile.hasSideType(INPUT_ALL))
        {
            slotInputPrimary[1].setSlotRender(SlotRender.FULL);
            slotInputSecondary[1].setSlotRender(SlotRender.FULL);
            slotInputOtherSecondary[1].setSlotRender(SlotRender.FULL);
        }
        else
        {
            slotInputPrimary[1].setSlotRender(SlotRender.BOTTOM);
            slotInputSecondary[1].setSlotRender(SlotRender.BOTTOM);
            slotInputOtherSecondary[1].setSlotRender(SlotRender.BOTTOM);
        }

        progressFluid.setFluid(baseTile.getTankFluid());
        progressFluid.setSize(baseTile.getScaledProgress(PROGRESS), 16);
        progressOverlay.setQuantity(baseTile.getScaledProgress(PROGRESS));
        progressFluid.setVisible(true);
        progressOverlay.setVisible(true);

        if (myTile.lockPrimary)
        {
            mode.setToolTip("gui.virtualmachines.farm.modeLocked");
            mode.setSheetX(176);
            mode.setHoverX(176);
            for (int i = 0; i < 9; i++)
                modeOverlay[i].setVisible(true);
        }
        else
        {
            mode.setToolTip("gui.virtualmachines.farm.modeUnlocked");
            mode.setSheetX(192);
            mode.setHoverX(192);
            for (int i = 0; i < 9; i++)
                modeOverlay[i].setVisible(false);
        }
    }

    @Override
    public void handleElementButtonClick(String buttonName, int mouseButton)
    {
        if (buttonName.equalsIgnoreCase("Mode"))
        {
            if (myTile.lockPrimary)
                playClickSound(0.6F);
            else
                playClickSound(0.8F);

            myTile.setMode(!myTile.lockPrimary);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y)
    {
        super.drawGuiContainerForegroundLayer(x, y);

        List<ItemStack> itemLocks = myTile.getItemLocks();
        if (!itemLocks.isEmpty())
        {
            for (int i = 0; i < itemLocks.size() /* SHOULD BE EQUAL TO 9 */; i++)
            {
                ItemStack stack = itemLocks.get(i);
                if (!stack.isEmpty())
                {
                    RenderHelper.enableGUIStandardItemLighting();
                    GhostItemRenderer.renderItemInGui(itemRender, stack, 95 + 18 * (i % 3), 19 + 18 * (i / 3), 0.42F);
                    RenderHelper.disableStandardItemLighting();
                }
            }
        }
    }
}
