package com.arcaratus.virtualmachines.gui;

import cofh.thermalexpansion.ThermalExpansion;
import com.arcaratus.virtualmachines.VirtualMachines;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement.DummyCategoryElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class GuiConfigVM extends GuiConfig
{
    public GuiConfigVM(GuiScreen parentScreen)
    {
        super(parentScreen, getConfigElements(parentScreen), VirtualMachines.MOD_ID, false, true, VirtualMachines.NAME);
    }

    public static final String[] CATEGORIES_CLIENT = {};
    public static final String[] CATEGORIES_COMMON = {};

    private static List<IConfigElement> getConfigElements(GuiScreen parent)
    {
        List<IConfigElement> list = new ArrayList<>();

        list.add(new DummyCategoryElement("Client", "config.client", getClientConfigElements()));
        list.add(new DummyCategoryElement("Common", "config.common", getCommonConfigElements()));

        return list;
    }

    private static List<IConfigElement> getClientConfigElements()
    {
        List<IConfigElement> list = new ArrayList<>();

        for (String category : CATEGORIES_CLIENT)
            list.add(new ConfigElement(ThermalExpansion.CONFIG_CLIENT.getCategory(category)));

        return list;
    }

    private static List<IConfigElement> getCommonConfigElements()
    {
        List<IConfigElement> list = new ArrayList<>();

        for (String category : CATEGORIES_COMMON)
            list.add(new ConfigElement(ThermalExpansion.CONFIG.getCategory(category)));

        return list;
    }
}
