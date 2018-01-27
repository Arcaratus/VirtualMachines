package com.arcaratus.virtualmachines.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.thermalexpansion.gui.element.ElementSlotOverlay;
import com.arcaratus.virtualmachines.init.VMConstants;

public class ElementSlotOverlay3x3 extends ElementSlotOverlay
{
    public ElementSlotOverlay3x3(GuiContainerCore gui, int posX, int posY)
    {
        super(gui, posX, posY);
        texture = VMConstants.PATH_SLOTS_9;
    }

    public ElementSlotOverlay3x3 setSlotInfo(SlotColor color, SlotRender render)
    {
        slotColor = color;
        slotRender = render;
        return this;
    }

    protected void drawSlotNoBorder(int x, int y)
    {
        sizeX = 56;
        sizeY = 56;
        int offsetX = 6;
        int offsetY = 6 + slotColor.ordinal() * 60;

        switch (slotRender)
        {
            case TOP:
                offsetX += 60;
                break;
            case BOTTOM:
                offsetX += 120;
                break;
            default:
                break;
        }

        gui.drawSizedTexturedModalRect(x, y, offsetX, offsetY, sizeX, sizeY, 384, 384);
    }

    protected void drawSlotWithBorder(int x, int y)
    {
        int sizeX = 60;
        int sizeY = 60;
        int offsetX = 4;
        int offsetY = 4 + slotColor.ordinal() * 60;

        x -= 2;
        y -= 2;

        switch (slotRender)
        {
            case TOP:
                offsetX += 60;
                break;
            case BOTTOM:
                offsetX += 120;
                break;
            default:
                break;
        }

        gui.drawSizedTexturedModalRect(x, y, offsetX, offsetY, sizeX, sizeY, 384, 384);
    }
}
