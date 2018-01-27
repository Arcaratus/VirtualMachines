package com.arcaratus.virtualmachines.init;

import cofh.core.util.core.IInitializer;
import com.arcaratus.virtualmachines.item.ItemAugment;
import com.arcaratus.virtualmachines.item.ItemMaterial;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class VMItems
{
    public static final VMItems INSTANCE = new VMItems();

    private static ArrayList<IInitializer> initList = new ArrayList<>();

    public static ItemMaterial item_material;
    public static ItemAugment item_augment;

    private VMItems() {}

    public static void preInit()
    {
        item_material = new ItemMaterial();
        item_augment = new ItemAugment();

        initList.add(item_material);
        initList.add(item_augment);

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
