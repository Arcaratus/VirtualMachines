package com.arcaratus.virtualmachines.plugins;

import cofh.core.util.PluginCore;
import cofh.thermalexpansion.util.managers.device.TapperManager;
import com.arcaratus.virtualmachines.VirtualMachines;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.PotionTypes;
import net.minecraft.potion.PotionType;
import net.minecraftforge.fml.common.Loader;

public class PluginVMBase extends PluginCore
{
    public PluginVMBase(String modId, String modName)
    {
        super(modId, modName);
    }

    /* IInitializer */
    @Override
    public boolean preInit()
    {
        String category = "Plugins";
        String comment = "If TRUE, support for " + modName + " is enabled.";
        enable = VirtualMachines.CONFIG.getConfiguration().getBoolean(modName, category, true, comment) && Loader.isModLoaded(modId);

        if (!enable)
            return false;

        preInitDelegate();
        return !error;
    }

    public boolean initialize()
    {
        if (!enable)
        {
            return false;
        }
        try
        {
            initializeDelegate();
        }
        catch (Throwable t)
        {
            VirtualMachines.LOGGER.error("Virtual Machines: " + modName + " Plugin encountered an error:", t);
            error = true;
        }
        if (!error)
        {
            VirtualMachines.LOGGER.info("Virtual Machines: " + modName + " Plugin Enabled.");
        }

        return !error;
    }

    public void preInitDelegate()
    {

    }

    public void initializeDelegate()
    {

    }

    /* HELPERS */
    public void addLeafMapping(Block logBlock, int logMetadata, Block leafBlock, int leafMetadata)
    {
        IBlockState logState = logBlock.getStateFromMeta(logMetadata);

        for (Boolean check_decay : BlockLeaves.CHECK_DECAY.getAllowedValues())
        {
            IBlockState leafState = leafBlock.getStateFromMeta(leafMetadata).withProperty(BlockLeaves.DECAYABLE, Boolean.TRUE).withProperty(BlockLeaves.CHECK_DECAY, check_decay);
            TapperManager.addLeafMapping(logState, leafState);
        }
    }

    public PotionType getPotionType(String baseName, String qualifier)
    {
        if (qualifier.isEmpty())
            return PotionType.getPotionTypeForName(modId + ":" + baseName);

        PotionType ret = PotionType.getPotionTypeForName(modId + ":" + baseName + "_" + qualifier);

        if (ret == PotionTypes.EMPTY)
            ret = PotionType.getPotionTypeForName(modId + ":" + qualifier + "_" + baseName);

        return ret;
    }
}