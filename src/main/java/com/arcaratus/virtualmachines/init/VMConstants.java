package com.arcaratus.virtualmachines.init;

import codechicken.lib.block.property.unlisted.UnlistedGenericTile;
import com.arcaratus.virtualmachines.block.machine.TileVirtualMachine;
import net.minecraft.util.ResourceLocation;

public class VMConstants
{
    public static final String PATH_GFX = "virtualmachines:textures/";
    public static final String PATH_GUI = PATH_GFX + "gui/";
    public static final String PATH_ELEMENTS = PATH_GUI + "elements/";

    public static final ResourceLocation PATH_SLOTS_9 = new ResourceLocation(PATH_ELEMENTS + "slots_9.png");

    public static final String PATH_MACHINE_GUI = PATH_GUI + "machine/";

    public static final String MACHINE_GUI_INFO = "tab.virtualmachines.machine.";

    public static final UnlistedGenericTile<TileVirtualMachine> TILE_VIRTUAL_MACHINE = new UnlistedGenericTile<>("tile_virtual_machine", TileVirtualMachine.class);

    public static final String NBT_PLANT_HANDLERS = "plantHandlers";
    public static final String NBT_OUTPUTS = "outputs";
    public static final String NBT_CURRENT_SEEDS = "currentSeeds";
    public static final String NBT_CURRENT_TOOLS = "currentTools";
    public static final String NBT_TOOLS = "tools";

    public static final String MACHINE_FARM_SOIL = "machineFarmSoil";
    public static final String MACHINE_EXPERIENCE = "machineExperience";
    public static final String MACHINE_NETHER = "machineNether";
    public static final String MACHINE_RANCHER = "machineRancher";
    public static final String MACHINE_PERMAMORB = "machinePermamorb";
}
