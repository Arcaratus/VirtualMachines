package com.arcaratus.virtualmachines;

import cofh.core.gui.CreativeTabCore;
import cofh.core.init.CoreProps;
import cofh.core.util.ConfigHandler;
import com.arcaratus.virtualmachines.item.ItemMaterial;
import com.arcaratus.virtualmachines.proxy.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = VirtualMachines.MOD_ID, version = VirtualMachines.VERSION, name = VirtualMachines.NAME, dependencies = "required-after:redstoneflux;after:cofhcore;after:thermalfoundation;after:thermalexpansion", guiFactory = "com.arcaratus.virtualmachines.gui.GuiConfigVMFactory")
public class VirtualMachines
{
    public static final String MOD_ID = "virtualmachines";
    public static final String VERSION = "@VERSION@";
    public static final String NAME = "Virtual Machines";

    @SidedProxy(serverSide = "com.arcaratus.virtualmachines.proxy.CommonProxy", clientSide = "com.arcaratus.virtualmachines.proxy.ClientProxy")
    public static CommonProxy proxy;

    public static final ConfigHandler CONFIG = new ConfigHandler(VERSION);
    public static final Logger LOGGER = LogManager.getLogger(VirtualMachines.NAME);

    public static CreativeTabs TAB_VIRTUAL_MACHINES = new CreativeTabCore(MOD_ID)
    {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getIconItemStack()
        {
            return ItemMaterial.virtual_machine_core_flux;
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        CONFIG.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/common.cfg"), true));
//        CONFIG_CLIENT.setConfiguration(new Configuration(new File(CoreProps.configDir, "/cofh/" + MOD_ID + "/client.cfg"), true));

        proxy.preInit(e);
    }

    @EventHandler
    public void init(FMLInitializationEvent e)
    {
        proxy.init(e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
        proxy.postInit(e);
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event)
    {
        CONFIG.cleanUp(false, true);

        LOGGER.info(NAME + ": Load Complete.");
    }
}
