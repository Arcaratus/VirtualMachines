package com.arcaratus.virtualmachines.plugins;

import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.arcaratus.virtualmachines.virtual.VirtualFarm.ezPairs;

public class PluginActuallyAdditions extends PluginVMBase
{
    public static final String MOD_ID = "actuallyadditions";
    public static final String MOD_NAME = "Actually Additions";

    public PluginActuallyAdditions()
    {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public void initializeDelegate()
    {
        final double[] standardCropDrops = new double[] { 0.079, 0.315, 0.42, 0.186 };
        // 17 = rice metadata
        // 14 = canola metadata
        VirtualFarm.defaultHandler.register(getItemStack("item_rice_seed"), ezPairs(new double[][] { new double[] { 0, 0.5, 0.5 }, standardCropDrops }, getItemStack("item_food", 1, 16), getItemStack("item_rice_seed")), 300, 2400);
        VirtualFarm.defaultHandler.register(getItemStack("item_canola_seed"), ezPairs(new double[][] { new double[] { 0, 0, 0.3333, 0.3334, 0.3333 }, standardCropDrops }, getItemStack("item_misc", 1, 13), getItemStack("item_canola_seed")), 200, 2400);
        VirtualFarm.defaultHandler.register(getItemStack("item_coffee_seed"), ezPairs(new double[][] { new double[] { 0, 0, 0.5, 0.5 }, standardCropDrops }, getItemStack("item_coffee_beans"), getItemStack("item_coffee_seed")), 200, 2400);
        VirtualFarm.defaultHandler.register(getItemStack("item_flax_seed"), ezPairs(new double[][] { new double[] { 0, 0, 0.25, 0.25, 0.25, 0.25 }, standardCropDrops }, new ItemStack(Items.STRING), getItemStack("item_flax_seed")), 200, 2400);
    }
}