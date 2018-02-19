package com.arcaratus.virtualmachines.block.machine;

import cofh.api.item.IAugmentItem.AugmentType;
import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.init.TEItems;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.item.ItemMorb;
import cofh.thermalexpansion.util.managers.machine.CentrifugeManager;
import cofh.thermalexpansion.util.managers.machine.CentrifugeManager.CentrifugeRecipe;
import cofh.thermalfoundation.init.TFFluids;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine.Type;
import com.arcaratus.virtualmachines.gui.client.machine.GuiAnimalFarm;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerAnimalFarm;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.IVirtualMachine;
import com.arcaratus.virtualmachines.virtual.VirtualAnimalFarm;
import com.google.common.collect.Lists;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class TileAnimalFarm extends TileVirtualMachine
{
    private static final int TYPE = Type.ANIMAL_FARM.getMetadata();
    public static int basePower = 80;

    public static int SLOT_TOOLS_START = 0;
    public static int SLOT_ANIMAL_MORB = 4;
    public static int SLOT_OUTPUT_START = 5;

    public static final int EXPERIENCE_MOD = 40;
    public static final int RANCHER_MOD = 40;
    public static final int PERMAMORB_MOD = 220;

    public static final int EXPERIENCE = 50;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 7;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, IntStream.range(SLOT_TOOLS_START, SLOT_ANIMAL_MORB + 1).toArray(), IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9).toArray(), { SLOT_ANIMAL_MORB }, IntStream.range(SLOT_TOOLS_START, SLOT_TOOLS_START + 4).toArray(), {}, IntStream.range(0, 15).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, INPUT_PRIMARY, INPUT_SECONDARY, OPEN, OMNI };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 5, 1, 2, 2, 2, 2 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = new boolean[] { true, true, true, true, true, false, false, false, false, false, false, false, false, false };
        SLOT_CONFIGS[TYPE].allowExtractionSlot = new boolean[] { false, false, false, false, false, true, true, true, true, true, true, true, true, true };

        VALID_AUGMENTS[TYPE] = new HashSet<>();
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_EXPERIENCE);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_RANCHER);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_PERMAMORB);

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileAnimalFarm.class, "virtualmachines:virtual_animal_farm");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.AnimalFarm";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Animal Farm. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    public static final List<String> ANIMAL_LIST = Lists.newArrayList("minecraft:chicken", "minecraft:pig", "minecraft:cow", "minecraft:sheep", "minecraft:rabbit", "minecraft:squid", "minecraft:mooshroom", "minecraft:horse", "minecraft:llama");
    public static final List<String> RANCHABLE_ANIMAL_LIST = Lists.newArrayList("minecraft:chicken", "minecraft:cow", "minecraft:sheep", "minecraft:mooshroom");

    private int inputTrackerPrimary;
    private int inputTrackerSecondary;
    private int outputTracker;
    private int outputTrackerFluid;

    private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);

    private boolean updateOutputs = true;
    public VirtualAnimalFarm virtualAnimalFarm = new VirtualAnimalFarm();

    protected boolean augmentExperience;
    protected boolean flagExperience;
    protected boolean augmentRancher;
    protected boolean augmentPermamorb;

    public TileAnimalFarm()
    {
        super();

        inventory = new ItemStack[15]; // 4 + 9 + 1 + 1
        Arrays.fill(inventory, ItemStack.EMPTY);
        createAllSlots(inventory.length);
        tank.setLock(TFFluids.fluidExperience);
    }

    @Override
    public int getType()
    {
        return TYPE;
    }

    @Override
    public void update()
    {
        super.update();
    }

    @Override
    protected int calcEnergy()
    {
        return super.calcEnergy();
    }

    @Override
    protected boolean canStart()
    {
        if (getStackInSlot(SLOT_ANIMAL_MORB).isEmpty() || getStackInSlot(SLOT_ANIMAL_MORB).getItem() != TEItems.itemMorb)
            return false;

        List<ItemStack> tools = Utils.arrayToListWithRange(inventory, SLOT_TOOLS_START, SLOT_TOOLS_START + 4);

        if (tools.stream().allMatch(ItemStack::isEmpty))
            return false;

        ItemStack morbStack = getStackInSlot(SLOT_ANIMAL_MORB).copy();
        List<ItemStack> outputs = virtualAnimalFarm.getOutputs();
        if (updateOutputs)
        {
            outputs.clear();

            if (!morbStack.hasTagCompound() || !ANIMAL_LIST.contains(morbStack.getTagCompound().getString("id")))
                return false;

            if (augmentRancher)
            {
                if (RANCHABLE_ANIMAL_LIST.contains(morbStack.getTagCompound().getString("id")))
                {
                    String id = morbStack.getTagCompound().getString("id");

                    if (id.equals("minecraft:chicken"))
                    {
                        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
                        {
                            ItemStack s = getStackInSlot(itx);
                            if (Utils.checkTool(s, "shears"))
                            {
                                int damage = world.rand.nextInt(3);
                                outputs.add(new ItemStack(Items.FEATHER, damage));
                                s = Utils.damageItem(s, damage, world.rand);

                                setInventorySlotContents(itx, s);
                            }
                        }
                    }

                    if (id.equals("minecraft:cow"))
                    {
                        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
                        {
                            ItemStack s = getStackInSlot(itx);
                            if (s.getItem() == Items.BUCKET)
                            {
                                s.shrink(1);
                                setInventorySlotContents(itx, s);
                                outputs.add(new ItemStack(Items.MILK_BUCKET));
                            }
                        }
                    }

                    if (id.equals("minecraft:sheep"))
                    {
                        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
                        {
                            ItemStack s = getStackInSlot(itx);
                            if (Utils.checkTool(s, "shears"))
                            {
                                int damage = world.rand.nextInt(2) + 1;
                                outputs.add(new ItemStack(Blocks.WOOL, damage));
                                s = Utils.damageItem(s, damage, world.rand);

                                setInventorySlotContents(itx, s);
                            }
                        }
                    }

                    if (id.equals("minecraft:mooshroom"))
                    {
                        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
                        {
                            ItemStack s = getStackInSlot(itx);
                            if (s.getItem() == Items.BOWL)
                            {
                                s.shrink(1);
                                setInventorySlotContents(itx, s);
                                outputs.add(new ItemStack(Items.MUSHROOM_STEW));
                            }
                        }
                    }
                }
            }
            else
            {
                if (!tools.stream().findAny().filter(s -> Utils.checkTool(s, "sword")).isPresent())
                    return false;

                CentrifugeRecipe recipe = CentrifugeManager.getRecipeMob(morbStack);

                List<ItemStack> recipeOutputs = recipe.getOutput();
                List<Integer> chances = recipe.getChance();
                for (int i = 0; i < recipeOutputs.size(); i++)
                {
                    for (int j = 0; j < recipeOutputs.get(i).getCount(); j++)
                    {
                        if (world.rand.nextInt(secondaryChance + j * 10) < chances.get(i))
                        {
                            int itx = i;
                            if (outputs.stream().findFirst().filter(s -> s.getItem() == recipeOutputs.get(itx).getItem()).isPresent())
                                outputs.stream().findFirst().filter(s -> ItemStack.areItemsEqual(s, recipeOutputs.get(itx))).ifPresent(s -> s.grow(1));
                            else
                                outputs.add(ItemHelper.cloneStack(recipeOutputs.get(i), 1));
                        }
                    }
                }

                outputs.removeIf(s -> s.getItem() == TEItems.itemMorb);
            }
        }

        if (!augmentPermamorb)
        {
            ItemStack morbOutput = ItemStack.areItemsEqual(getStackInSlot(SLOT_ANIMAL_MORB), ItemMorb.morbStandard) ? ItemMorb.morbStandard.copy() : ItemMorb.morbReusable.copy();
            outputs.add(morbOutput);
            getStackInSlot(SLOT_ANIMAL_MORB).shrink(1);
        }

        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
        {
            if (Utils.checkTool(getStackInSlot(itx), "sword"))
            {
                int it = itx;
                outputs.forEach(stack ->
                {
                    float f = (float) EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, getStackInSlot(it)) * world.rand.nextFloat();
                    stack.grow(Math.round(f));
                });
                break;
            }
        }

        virtualAnimalFarm.setOutputs(outputs);
        updateOutputs = false;

        return !outputs.isEmpty() && Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
    }

    @Override
    protected boolean hasValidInput()
    {
        if (augmentRancher)
            return true;

        List<ItemStack> tools = Utils.arrayToListWithRange(inventory, SLOT_TOOLS_START, SLOT_TOOLS_START + 4);
        return !tools.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    protected void processStart()
    {
        double maxProcess = 16000;

        processMax = (int) maxProcess;
        processRem = processMax;
    }

    @Override
    protected void processFinish()
    {
        List<ItemStack> outputs = virtualAnimalFarm.getOutputs();

        int damage = 0;
        for (ItemStack itemStack : outputs)
        {
            Utils.distributeOutput(this, itemStack, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
            damage++;
        }

        for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
        {
            ItemStack damagedSwordStack = getStackInSlot(itx);
            if (Utils.checkTool(damagedSwordStack, "sword"))
            {
                damagedSwordStack = Utils.damageItem(damagedSwordStack, damage, world.rand);
                setInventorySlotContents(itx, damagedSwordStack);
                break;
            }
        }

        if (augmentExperience && damage > 0)
            tank.fill(new FluidStack(TFFluids.fluidExperience, (int) Math.round(world.rand.nextGaussian() * 25F + EXPERIENCE)), true);

        updateOutputs = true;
    }

    @Override
    protected void transferInput()
    {
        if (!enableAutoInput)
            return;

        int side;
        for (int i = inputTrackerPrimary + 1; i <= inputTrackerPrimary + 6; i++)
        {
            side = i % 6;
            for (int slot = SLOT_TOOLS_START; slot < SLOT_TOOLS_START + 4; slot++)
            {
                if (extractItem(SLOT_TOOLS_START, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
                {
                    inputTrackerPrimary = side;
                    break;
                }
            }
        }

        for (int i = inputTrackerSecondary + 1; i <= inputTrackerSecondary + 6; i++)
        {
            side = i % 6;
            if (extractItem(SLOT_ANIMAL_MORB, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
            {
                inputTrackerSecondary = side;
                break;
            }
        }
    }

    @Override
    protected void transferOutput()
    {
        if (!enableAutoOutput)
            return;

        if (augmentExperience)
            transferOutputFluid();

        int side;

        if (Utils.checkItemStackRange(inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9, ItemStack::isEmpty))
            return;

        for (int i = outputTracker + 1; i <= outputTracker + 6; i++)
        {
            side = i % 6;
            for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_START + 9; slot++)
            {
                if (transferItem(slot, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
                {
                    outputTracker = side;
                    break;
                }
            }
        }
    }

    private void transferOutputFluid()
    {
        if (tank.getFluidAmount() <= 0)
            return;

        int side;
        FluidStack output = new FluidStack(tank.getFluid(), Math.min(tank.getFluidAmount(), FLUID_TRANSFER[level]));
        for (int i = outputTrackerFluid + 1; i <= outputTrackerFluid + 6; i++)
        {
            side = i % 6;
            if (isSecondaryOutput(sideConfig.sideTypes[sideCache[side]]))
            {
                int toDrain = FluidHelper.insertFluidIntoAdjacentFluidHandler(this, EnumFacing.VALUES[side], output, true);
                if (toDrain > 0)
                {
                    tank.drain(toDrain, true);
                    outputTrackerFluid = side;
                    break;
                }
            }
        }
    }

    @Override
    public Object getGuiClient(InventoryPlayer inventory)
    {
        return new GuiAnimalFarm(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerAnimalFarm(inventory, this);
    }

    @Override
    public FluidTankCore getTank()
    {
        return tank;
    }

    @Override
    public FluidStack getTankFluid()
    {
        return tank.getFluid();
    }

    public boolean augmentExperience()
    {
        return augmentExperience && flagExperience;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        inputTrackerPrimary = nbt.getInteger("TrackIn1");
        inputTrackerSecondary = nbt.getInteger("TrackIn2");
        outputTracker = nbt.getInteger("TrackOut");
        outputTrackerFluid = nbt.getInteger("TrackOutFluid");
        updateOutputs = nbt.getBoolean("UpdateOutputs");
        tank.readFromNBT(nbt);
        virtualAnimalFarm.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("TrackIn1", inputTrackerPrimary);
        nbt.setInteger("TrackIn2", inputTrackerSecondary);
        nbt.setInteger("TrackOut", outputTracker);
        nbt.setInteger("TrackOutFluid", outputTrackerFluid);
        nbt.setBoolean("UpdateOutputs", updateOutputs);
        tank.writeToNBT(nbt);
        virtualAnimalFarm.writeToNBT(nbt);
        return nbt;
    }

    @Override
    public PacketBase getModePacket()
    {
        return super.getModePacket();
    }

    @Override
    protected void handleModePacket(PacketBase payload)
    {
        super.handleModePacket(payload);
    }

    @Override
    public PacketBase getGuiPacket()
    {
        PacketBase payload = super.getGuiPacket();

        payload.addBool(augmentExperience);
        payload.addBool(augmentRancher);
        payload.addBool(augmentPermamorb);
        payload.addFluidStack(tank.getFluid());

        return payload;
    }

    @Override
    protected void handleGuiPacket(PacketBase payload)
    {
        super.handleGuiPacket(payload);

        augmentExperience = payload.getBool();
        augmentRancher = payload.getBool();
        augmentPermamorb = payload.getBool();
        flagExperience = augmentExperience;
        tank.setFluid(payload.getFluidStack());
    }

    @Override
    protected void preAugmentInstall()
    {
        super.preAugmentInstall();

        augmentExperience = false;
        augmentRancher = false;
        augmentPermamorb = false;
    }

    @Override
    protected void postAugmentInstall()
    {
        super.postAugmentInstall();

        if (!augmentExperience)
        {
            tank.clearLocked();
            tank.setFluid(null);
        }
    }

    @Override
    protected boolean isValidAugment(AugmentType type, String id)
    {
        if (augmentExperience && VMConstants.MACHINE_EXPERIENCE.equals(id))
            return false;
        if (augmentRancher && VMConstants.MACHINE_RANCHER.equals(id))
            return false;
        if (augmentPermamorb && VMConstants.MACHINE_PERMAMORB.equals(id))
            return false;

        return super.isValidAugment(type, id);
    }

    @Override
    protected boolean installAugmentToSlot(int slot)
    {
        String id = AugmentHelper.getAugmentIdentifier(augments[slot]);

        if (!augmentExperience && VMConstants.MACHINE_EXPERIENCE.equals(id))
        {
            augmentExperience = true;
            hasModeAugment = true;
            energyMod += EXPERIENCE_MOD;
            return true;
        }

        if (!augmentRancher && VMConstants.MACHINE_RANCHER.equals(id))
        {
            augmentRancher = true;
            hasModeAugment = true;
            energyMod += RANCHER_MOD;
            return true;
        }

        if (!augmentPermamorb && VMConstants.MACHINE_PERMAMORB.equals(id))
        {
            augmentPermamorb = true;
            energyMod += PERMAMORB_MOD;
            return true;
        }

        return super.installAugmentToSlot(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        if (slot == SLOT_ANIMAL_MORB)
        {
            return stack.getItem() == TEItems.itemMorb;
        }
        else if (Utils.isSlotInRange(slot, SLOT_TOOLS_START, SLOT_TOOLS_START + 4))
        {
            return Utils.checkTool(stack, "sword") || stack.getItem() instanceof ItemShears || stack.getItem() == Items.BOWL || stack.getItem() == Items.BUCKET;
        }

        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing from)
    {
        return super.hasCapability(capability, from) || augmentExperience && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, final EnumFacing from)
    {
        if (augmentExperience && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
        {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new IFluidHandler()
            {
                @Override
                public IFluidTankProperties[] getTankProperties()
                {
                    FluidTankInfo info = tank.getInfo();
                    return new IFluidTankProperties[] { new FluidTankProperties(info.fluid, info.capacity, true, false) };
                }

                @Override
                public int fill(FluidStack resource, boolean doFill)
                {
                    if (from != null && !allowInsertion(sideConfig.sideTypes[sideCache[from.ordinal()]]))
                        return 0;

                    return tank.fill(resource, doFill);
                }

                @Nullable
                @Override
                public FluidStack drain(FluidStack resource, boolean doDrain)
                {
                    if (isActive)
                        return null;

                    return tank.drain(resource, doDrain);
                }

                @Nullable
                @Override
                public FluidStack drain(int maxDrain, boolean doDrain)
                {
                    if (isActive)
                        return null;

                    return tank.drain(maxDrain, doDrain);
                }
            });
        }

        return super.getCapability(capability, from);
    }

    @Override
    public void onInventoryChanged(int slot, ItemStack stack)
    {
        updateOutputs = true;
    }

    @Override
    public IVirtualMachine getVirtualMachine()
    {
        return virtualAnimalFarm;
    }
}
