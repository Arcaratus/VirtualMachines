package com.arcaratus.virtualmachines.block.machine;

import cofh.api.tileentity.IRedstoneControl;
import cofh.core.block.BlockCore;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.block.ItemBlockTEBase;
import cofh.thermalexpansion.init.TEProps;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockVirtualMachine extends ItemBlockTEBase
{
    public ItemBlockVirtualMachine(BlockCore block)
    {
        super(block);
    }

    @Override
    public ItemStack setDefaultTag(ItemStack stack, int level)
    {
        ReconfigurableHelper.setFacing(stack, 3);
        ReconfigurableHelper.setSideCache(stack, TileVirtualMachine.SIDE_CONFIGS[ItemHelper.getItemDamage(stack)].defaultSides);
        RedstoneControlHelper.setControl(stack, IRedstoneControl.ControlMode.DISABLED);
        EnergyHelper.setDefaultEnergyTag(stack, 0);
        stack.getTagCompound().setByte("Level", (byte) level);

        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        SecurityHelper.addOwnerInformation(stack, tooltip);

        if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown())
            tooltip.add(StringHelper.shiftForDetails());

        if (!StringHelper.isShiftKeyDown())
            return;

        SecurityHelper.addAccessInformation(stack, tooltip);

        String name = BlockVirtualMachine.Type.values()[ItemHelper.getItemDamage(stack)].getName();
        tooltip.add(StringHelper.getInfoText("info.virtualmachines." + name));

        if (getLevel(stack) >= TEProps.levelRedstoneControl)
            RedstoneControlHelper.addRSControlInformation(stack, tooltip);
    }
}
