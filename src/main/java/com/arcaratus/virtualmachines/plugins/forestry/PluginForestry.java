package com.arcaratus.virtualmachines.plugins.forestry;

import cofh.core.util.ModPlugin;
import cofh.thermalexpansion.ThermalExpansion;
import com.arcaratus.virtualmachines.VirtualMachines;
import net.minecraftforge.fml.common.Loader;

public class PluginForestry extends ModPlugin
{
    public static final String MOD_ID = "forestry";
    public static final String MOD_NAME = "Forestry";

    public PluginForestry()
    {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public boolean initialize()
    {
        String category = "Plugins";
        String comment = "If TRUE, support for " + MOD_NAME + " is enabled.";
        enable = Loader.isModLoaded(MOD_ID) && ThermalExpansion.CONFIG.getConfiguration().getBoolean(MOD_NAME, category, true, comment);

        return enable && !error;
    }

    @Override
    public boolean register()
    {
        if (!enable)
            return false;

        try
        {

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
