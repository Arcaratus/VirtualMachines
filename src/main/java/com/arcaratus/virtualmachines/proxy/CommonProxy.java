package com.arcaratus.virtualmachines.proxy;

import cofh.core.render.IModelRegister;
import com.arcaratus.virtualmachines.init.*;
import net.minecraftforge.fml.common.event.*;

public class CommonProxy
{
    public void preInit(FMLPreInitializationEvent e)
    {
        VMBlocks.preInit();
        VMItems.preInit();

        VMPlugins.preInit();
    }

    public void init(FMLInitializationEvent e)
    {
    }

    public void postInit(FMLPostInitializationEvent e)
    {
        VMPlugins.postInit();
    }

    public boolean addIModelRegister(IModelRegister modelRegister)
    {
        return false;
    }
}
