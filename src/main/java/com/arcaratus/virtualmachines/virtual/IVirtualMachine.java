package com.arcaratus.virtualmachines.virtual;

import net.minecraft.nbt.NBTTagCompound;

/* I dunno what to do with this tbh */
public interface IVirtualMachine
{
    IVirtualMachine readFromNBT(NBTTagCompound tag);

    NBTTagCompound writeToNBT(NBTTagCompound tag);
}
