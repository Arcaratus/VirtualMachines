package com.arcaratus.virtualmachines.proxy;

import cofh.core.render.IModelRegister;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;

import java.util.ArrayList;

public class ClientProxy extends CommonProxy
{
    private static ArrayList<IModelRegister> modelList = new ArrayList<>();

    @Override
    public void preInit(FMLPreInitializationEvent e)
    {
        super.preInit(e);

        MinecraftForge.EVENT_BUS.register(EventHandlerClient.INSTANCE);

        for (IModelRegister register : modelList)
            register.registerModels();
    }

    @Override
    public void init(FMLInitializationEvent e)
    {
        super.init(e);
    }

    @Override
    public void postInit(FMLPostInitializationEvent e)
    {
        super.postInit(e);
    }

    public boolean addIModelRegister(IModelRegister modelRegister)
    {
        return modelList.add(modelRegister);
    }
}
