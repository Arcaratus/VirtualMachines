package com.arcaratus.virtualmachines.virtual;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/* I dunno what to do with this tbh */

/* Either make a new dimension and virtually check that, but seems like overkill
 * Store the data somewhere, RNG and shit
 * Shud probably make soil a requirement...
 * Oh wait, i could look into the garden cloche @immersiveengineering
 * would prob make a lot more sense
*/
public class VirtualMachine
{
    protected int ticksPerCycle;

    protected List<ItemStack> inputs = new ArrayList<>();
    protected List<ItemStack> generatedItems = new ArrayList<>();

    public VirtualMachine()
    {
    }

    public VirtualMachine readFromNBT(NBTTagCompound tag)
    {
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        return tag;
    }
}
