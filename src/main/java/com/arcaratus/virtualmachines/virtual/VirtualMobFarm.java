package com.arcaratus.virtualmachines.virtual;

import com.arcaratus.virtualmachines.init.VMConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

public class VirtualMobFarm implements IVirtualMachine
{
    private List<ItemStack> outputs = new ArrayList<>();

    public VirtualMobFarm() {}

    public List<ItemStack> getOutputs()
    {
        return outputs;
    }

    public void setOutputs(List<ItemStack> outputs)
    {
        this.outputs = outputs;
    }

    @Override
    public IVirtualMachine readFromNBT(NBTTagCompound tag)
    {
        if (tag.hasKey(VMConstants.NBT_OUTPUTS))
        {
            NBTTagList list = tag.getTagList(VMConstants.NBT_OUTPUTS, 10);
            for (int i = 0; i < list.tagCount(); i++)
                outputs.add(i, new ItemStack(list.getCompoundTagAt(i)));
        }

        return this;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        if (!outputs.isEmpty())
        {
            NBTTagList list = new NBTTagList();
            for (ItemStack output : outputs)
            {
                NBTTagCompound nbt = new NBTTagCompound();
                output.writeToNBT(nbt);
                list.appendTag(nbt);
            }

            tag.setTag(VMConstants.NBT_OUTPUTS, list);
        }

        return tag;
    }
}

