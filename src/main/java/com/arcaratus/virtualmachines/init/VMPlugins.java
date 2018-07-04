package com.arcaratus.virtualmachines.init;

import cofh.core.util.core.IInitializer;
import com.arcaratus.virtualmachines.plugins.PluginActuallyAdditions;
import com.arcaratus.virtualmachines.plugins.PluginXU2;

import java.util.ArrayList;

public class VMPlugins
{
    private static ArrayList<IInitializer> initList = new ArrayList<>();

    public static PluginActuallyAdditions pluginActuallyAdditions;
    public static PluginXU2 pluginXU2;

    private VMPlugins() {}

    public static void preInit()
    {
        pluginActuallyAdditions = new PluginActuallyAdditions();
        pluginXU2 = new PluginXU2();

        initList.add(pluginActuallyAdditions);
        initList.add(pluginXU2);

        for (IInitializer init : initList)
            init.preInit();
    }

    public static void postInit()
    {
        for (IInitializer init : initList)
            init.initialize();
    }
}
