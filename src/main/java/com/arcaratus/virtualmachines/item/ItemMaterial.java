package com.arcaratus.virtualmachines.item;

import cofh.core.item.ItemMulti;
import cofh.core.util.core.IInitializer;
import cofh.thermalexpansion.util.managers.machine.ChargerManager;
import cofh.thermalexpansion.util.managers.machine.TransposerManager;
import cofh.thermalfoundation.init.TFFluids;
import com.arcaratus.virtualmachines.VirtualMachines;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;

public class ItemMaterial extends ItemMulti implements IInitializer
{
    public static ItemStack virtual_machine_core_empty;
    public static ItemStack virtual_machine_core_filled;
    public static ItemStack virtual_machine_core_flux;

    public ItemMaterial()
    {
        super(VirtualMachines.MOD_ID);

        setUnlocalizedName("material");
        setCreativeTab(VirtualMachines.TAB_VIRTUAL_MACHINES);
    }

    @Override
    public boolean initialize()
    {
        int number = 2560;
        virtual_machine_core_empty = addItem(number++, "virtual_machine_core_empty");
        virtual_machine_core_filled = addItem(number++, "virtual_machine_core_filled", EnumRarity.UNCOMMON);
        virtual_machine_core_flux = addItem(number++, "virtual_machine_core_flux", EnumRarity.RARE);

        VirtualMachines.proxy.addIModelRegister(this);

        return true;
    }

    @Override
    public boolean register()
    {
        addShapedRecipe(virtual_machine_core_empty, "ESE", "SIS", "ESE", 'E', "plateElectrum", 'S', "plateSignalum", 'I', "gearInvar");

        TransposerManager.addFillRecipe(16000, virtual_machine_core_empty, virtual_machine_core_filled, new FluidStack(TFFluids.fluidEnder, 8000), false);

        ChargerManager.addRecipe(160000, virtual_machine_core_filled, virtual_machine_core_flux);

        return true;
    }
}
