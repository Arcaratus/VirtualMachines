package com.arcaratus.virtualmachines.init;

import cofh.core.util.core.IInitializer;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class VMBlocks
{
    public static final VMBlocks INSTANCE = new VMBlocks();

    private VMBlocks() {}

    private static ArrayList<IInitializer> initList = new ArrayList<>();

    public static BlockVirtualMachine virtual_machine;

    public static void preInit()
    {
        virtual_machine = new BlockVirtualMachine();

        initList.add(virtual_machine);

        for (IInitializer init : initList)
            init.initialize();

        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event)
    {
        for (IInitializer init : initList)
            init.register();
    }
}
