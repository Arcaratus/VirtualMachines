package com.arcaratus.virtualmachines.block.machine;

import cofh.api.tileentity.IRedstoneControl;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.block.ItemBlockTEBase;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.util.helpers.ReconfigurableHelper;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine.Type;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockVirtualMachine extends ItemBlockTEBase
{
    public ItemBlockVirtualMachine(Block block)
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
    public String getUnlocalizedName(ItemStack stack)
    {
        return "tile.virtualmachines." + Type.byMetadata(ItemHelper.getItemDamage(stack)).getName() + ".name";
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

        String name = BlockVirtualMachine.Type.byMetadata(ItemHelper.getItemDamage(stack)).getName();
        tooltip.add(StringHelper.getInfoText("info.virtualmachines." + name));

        if (getLevel(stack) >= TEProps.levelRedstoneControl)
            RedstoneControlHelper.addRSControlInformation(stack, tooltip);
    }
}
