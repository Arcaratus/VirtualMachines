package com.arcaratus.virtualmachines.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.thermalexpansion.gui.element.ElementSlotOverlayCrafter;
import com.arcaratus.virtualmachines.init.VMConstants;

public class ElementSlotOverlay2x9 extends ElementSlotOverlayCrafter
{
    public ElementSlotOverlay2x9(GuiContainerCore gui, int posX, int posY)
    {
        super(gui, posX, posY);
        texture = VMConstants.PATH_SLOTS_9;
    }

    @Override
    protected void drawSlotNoBorder(int x, int y)
    {
        sizeX = 160;
        sizeY = 34;
        int offsetX = 186;
        int offsetY = 186;

        gui.drawSizedTexturedModalRect(x, y, offsetX, offsetY, sizeX, sizeY, 384, 384);
    }

    @Override
    protected void drawSlotWithBorder(int x, int y)
    {
        int sizeX = 164;
        int sizeY = 38;
        int offsetX = 184;
        int offsetY = 184;

        x -= 2;
        y -= 2;

        gui.drawSizedTexturedModalRect(x, y, offsetX, offsetY, sizeX, sizeY, 384, 384);
    }
}
