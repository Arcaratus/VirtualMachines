package com.arcaratus.virtualmachines.plugins;

import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.arcaratus.virtualmachines.virtual.VirtualFarm.ezPairs;

public class PluginXU2 extends PluginVMBase
{
    public static final String MOD_ID = "extrautils2";
    public static final String MOD_NAME = "Extra Utilities 2";

    public PluginXU2()
    {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public void initializeDelegate()
    {
        VirtualFarm.defaultHandler.register(getItemStack("enderlilly"), ezPairs(new double[][] { new double[] { 0, 1 }, new double[] { 0, 0.98, 0.02 } }, new ItemStack(Items.ENDER_PEARL), getItemStack("enderlilly")), 800, 24000, false);
        VirtualFarm.defaultHandler.register(getItemStack("redorchid"), ezPairs(new double[][] { new double[] { 0, 1 }, new double[] { 0, 0.9, 0.0666, 0.0334 } }, new ItemStack(Items.REDSTONE), getItemStack("redorchid")), 1000, 9600, false);
    }
}
