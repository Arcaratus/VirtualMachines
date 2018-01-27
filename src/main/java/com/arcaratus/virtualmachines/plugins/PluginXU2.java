package com.arcaratus.virtualmachines.plugins;

import cofh.core.util.ModPlugin;
import cofh.thermalexpansion.ThermalExpansion;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import static com.arcaratus.virtualmachines.virtual.VirtualFarm.ezPairs;

public class PluginXU2 extends ModPlugin
{
    public static final String MOD_ID = "extrautils2";
    public static final String MOD_NAME = "Extra Utilities 2";

    public PluginXU2()
    {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public boolean initialize()
    {
        String category = "Plugins";
        String comment = "If TRUE, support for " + MOD_NAME + " is enabled.";
        enable = ThermalExpansion.CONFIG.getConfiguration().getBoolean(MOD_NAME, category, true, comment) && Loader.isModLoaded(MOD_ID);

        if (!enable)
            return false;
        return !error;
    }

    @Override
    public boolean register()
    {
        if (!enable)
            return false;

        try
        {
            VirtualFarm.defaultHandler.register(getItemStack("enderlilly"), ezPairs(new double[][] { new double[] { 0, 1 }, new double[] { 0, 0.98, 0.02 } }, new ItemStack(Items.ENDER_PEARL), getItemStack("enderlilly")), 800, 24000, false);
            VirtualFarm.defaultHandler.register(getItemStack("redorchid"), ezPairs(new double[][] { new double[] { 0, 1 }, new double[] { 0, 0.9, 0.0666, 0.0334 } }, new ItemStack(Items.REDSTONE), getItemStack("redorchid")), 1000, 9600, false);
        }
        catch (Throwable t)
        {
            VirtualMachines.LOGGER.error("Virtual Machines: " + MOD_NAME + " Plugin encountered an error:", t);
            error = true;
        }

        if (!error)
            VirtualMachines.LOGGER.info("Virtual Machines: " + MOD_NAME + " Plugin Enabled.");

        return !error;
    }
}
