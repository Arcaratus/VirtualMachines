package com.arcaratus.virtualmachines.block.machine;

import cofh.api.item.IAugmentItem.AugmentType;
import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.AugmentHelper;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.util.managers.machine.InsolatorManager;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine.Type;
import com.arcaratus.virtualmachines.gui.client.machine.GuiFarm;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerFarm;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.Distribution;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import com.arcaratus.virtualmachines.virtual.VirtualFarm.IItemFertilizerHandler;
import com.arcaratus.virtualmachines.virtual.VirtualFarm.IPlantHandler;
import com.arcaratus.virtualmachines.virtual.VirtualMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TileFarm extends TileVirtualMachine
{
    private static final int TYPE = Type.FARM.getMetadata();
    public static int basePower = 40;

    public static final int SLOT_TOOLS_START = 18;
    public static final int SLOT_FARM_START = 23;
    public static final int SLOT_FERTILIZER = 22;

    public static final int SOIL_ENERGY_MOD = 10;
    public static final int MONOCULTURE_ENERGY_MOD = 40;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 6;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, IntStream.range(SLOT_TOOLS_START, 33).toArray(), IntStream.range(0, SLOT_TOOLS_START).toArray(), {}, IntStream.range(SLOT_FARM_START, 33).toArray(), IntStream.range(SLOT_TOOLS_START, SLOT_FARM_START).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, OPEN, INPUT_PRIMARY, INPUT_SECONDARY };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 3, 1, 2, 2, 2, 2 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = new boolean[] { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true };
        SLOT_CONFIGS[TYPE].allowExtractionSlot = new boolean[] { true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false };

        VALID_AUGMENTS[TYPE] = new HashSet<>();
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_FARM_SOIL);
        VALID_AUGMENTS[TYPE].add(TEProps.MACHINE_INSOLATOR_FERTILIZER);
        VALID_AUGMENTS[TYPE].add(TEProps.MACHINE_INSOLATOR_MONOCULTURE);

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileFarm.class, "virtualmachines:virtual_farm");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.Farm";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Farm. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    private int inputTrackerPrimary;
    private int inputTrackerSecondary;
    private int outputTracker;

    public boolean lockPrimary = false;
    private List<ItemStack> itemLocks = new ArrayList<>();

    private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);

    protected boolean augmentSoil;
    protected boolean augmentFertilizer;
    protected boolean augmentMonoculture;

    private VirtualFarm virtualFarm = new VirtualFarm();

    public TileFarm()
    {
        super();

        inventory = new ItemStack[33]; // 18 + 4 + 9 + 1 + 1
        Arrays.fill(inventory, ItemStack.EMPTY);
        createAllSlots(inventory.length);
        tank.setLock(FluidRegistry.WATER);

        for (int i = 0; i < 9; i++)
            itemLocks.add(ItemStack.EMPTY);
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
        if (!virtualFarm.getCurrentSeeds().stream().allMatch(ItemStack::isEmpty))
            return true;

        if (Utils.checkItemStackRange(inventory, SLOT_FARM_START, SLOT_FARM_START + 9, ItemStack::isEmpty))
            return false;

        List<ItemStack> farm = Utils.arrayToListWithRange(inventory, SLOT_FARM_START, SLOT_FARM_START + 9);
        List<IPlantHandler> currentPlantHandlers = virtualFarm.getPlantHandlers(farm);
        List<ItemStack> farmTools = Utils.arrayToListWithRange(inventory, SLOT_TOOLS_START, SLOT_TOOLS_START + 4);

        if (farmTools.stream().allMatch(ItemStack::isEmpty))
            return false;

        List<ItemStack> lol = new ArrayList<>();
        for (int i = 0; i < 9; i++)
        {
            lol.add(i, ItemStack.EMPTY);
            IPlantHandler plantHandler = currentPlantHandlers.get(i);
            ItemStack seed = farm.get(i);
            if (!seed.isEmpty() && plantHandler != null)
            {
                if (augmentSoil || plantHandler.requiresDirt(seed))
                {
                    if (tank.getFluidAmount() < plantHandler.waterRequired(seed) || energyStorage.getEnergyStored() < plantHandler.energyRequired(seed))
                        return false;

                    List<Pair<Distribution, ItemStack>> outputs = plantHandler.getOutput(seed, farmTools);

                    if (outputs.isEmpty() || !Utils.canFitOutputs(this, outputs.stream().map(Pair::getRight).collect(Collectors.toList()), 0, SLOT_TOOLS_START))
                        return false;

                    ItemStack seedCopy = seed.copy();
                    lol.set(i, seedCopy);

                    if (!augmentMonoculture)
                        seed.shrink(1);
                }
            }
        }

        virtualFarm.setCurrentSeeds(lol);
        return true;
    }

    @Override
    protected boolean hasValidInput()
    {
        List<ItemStack> farmTools = Utils.arrayToListWithRange(inventory, SLOT_TOOLS_START, SLOT_TOOLS_START + 4);
        return !farmTools.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    protected void processStart()
    {
        double maxProcess = 0;

        for (int i = 0; i < 9; i++)
        {
            IPlantHandler plantHandler = virtualFarm.getCurrentPlantHandlers().get(i);
            ItemStack seed = virtualFarm.getCurrentSeeds().get(i);
            if (!seed.isEmpty() && plantHandler != null)
            {
                maxProcess += plantHandler.energyRequired(seed);
                if (augmentMonoculture)
                    energyMod += MONOCULTURE_ENERGY_MOD;
            }
        }

        maxProcess *= energyMod;
        maxProcess /= ENERGY_BASE;

        ItemStack fertilizerStack = getStackInSlot(SLOT_FERTILIZER);
        if (!fertilizerStack.isEmpty())
        {
            IItemFertilizerHandler fertilizerHandler = VirtualFarm.getItemFertilizerHandler(fertilizerStack);
            if (fertilizerHandler != null)
            {
                maxProcess /= fertilizerHandler.getGrowthMultiplier(fertilizerStack);

                if (reuseChance > 0)
                    if (world.rand.nextInt(SECONDARY_BASE) >= reuseChance)
                        fertilizerStack.shrink(1);
                else
                    fertilizerStack.shrink(1);
            }
        }

        processMax = (int) maxProcess;
        processRem = processMax;
    }

    @Override
    protected void processFinish()
    {
        List<ItemStack> farm = virtualFarm.getCurrentSeeds();
        List<IPlantHandler> currentPlantHandlers = virtualFarm.getCurrentPlantHandlers();
        List<ItemStack> farmTools = Utils.arrayToListWithRange(inventory, SLOT_TOOLS_START, SLOT_TOOLS_START + 4);

        for (int i = 0; i < 9; i++)
        {
            IPlantHandler plantHandler = currentPlantHandlers.get(i);
            ItemStack seed = farm.get(i);
            if (!seed.isEmpty() && plantHandler != null)
            {
                List<Pair<Distribution, ItemStack>> outputs = plantHandler.getOutput(seed, farmTools);

                if (!outputs.isEmpty())
                {
                    for (Pair<Distribution, ItemStack> p : outputs)
                    {
                        ItemStack output = new ItemStack(p.getRight().getItem(), p.getLeft().getOutput(), p.getRight().getMetadata());

                        if (output.isItemEqual(seed) && augmentMonoculture)
                            output.shrink(2);

                        if (!output.isEmpty() && output.getCount() > 0)
                        {
                            List<ItemStack> tools = plantHandler.getTools(); // Size should never be past 4

                            for (int itx = SLOT_TOOLS_START; itx < SLOT_TOOLS_START + 4; itx++)
                            {
                                final int it = itx;
                                ItemStack tool = tools.stream().findFirst().filter(s -> s.isItemEqualIgnoreDurability(getStackInSlot(it))).orElse(ItemStack.EMPTY);
                                if (getStackInSlot(itx).isItemEqualIgnoreDurability(tool))
                                {
                                    ItemStack s = getStackInSlot(itx);
                                    s = Utils.damageItem(s, Utils.checkTool(s, "hoe", "sickle") ? 1 : output.getCount(), world.rand);

                                    setInventorySlotContents(itx, s);
                                }
                            }

                            Utils.distributeOutput(this, output, 0, SLOT_TOOLS_START);
                        }
                    }

                    tank.modifyFluidStored(-plantHandler.waterRequired(seed));
                }

                plantHandler.clearTools();
            }
        }

        virtualFarm.clear();
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
            for (int slot = SLOT_FARM_START; slot < SLOT_FARM_START + 9; slot++)
            {
                if (extractItem(slot, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
                {
                    inputTrackerPrimary = side;
                    break;
                }
            }
        }

        for (int i = inputTrackerSecondary + 1; i <= inputTrackerSecondary + 6; i++)
        {
            side = i % 6;
            for (int slot = SLOT_TOOLS_START; slot < SLOT_TOOLS_START + 5; slot++) // + 5 bcuz fertilizer
            {
                if (extractItem(slot, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
                {
                    inputTrackerSecondary = side;
                    break;
                }
            }
        }
    }

    @Override
    protected void transferOutput()
    {
        if (!enableAutoOutput)
            return;

        int side;

        if (Utils.checkItemStackRange(inventory, 0, SLOT_TOOLS_START, ItemStack::isEmpty))
            return;

        for (int i = outputTracker + 1; i <= outputTracker + 6; i++)
        {
            side = i % 6;
            for (int slot = 0; slot < SLOT_TOOLS_START; slot++)
            {
                if (transferItem(slot, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
                {
                    outputTracker = side;
                    break;
                }
            }
        }
    }

    @Override
    protected boolean readPortableTagInternal(EntityPlayer player, NBTTagCompound tag)
    {
        if (!super.readPortableTagInternal(player, tag))
            return false;

        lockPrimary = tag.getBoolean("SlotLock");
        NBTTagList list = tag.getTagList("ItemLocks", 10);
        for (int i = 0; i < 9; i++)
            itemLocks.set(i, new ItemStack(list.getCompoundTagAt(i)));

        return true;
    }

    @Override
    protected boolean writePortableTagInternal(EntityPlayer player, NBTTagCompound tag)
    {
        if (!super.writePortableTagInternal(player, tag))
            return false;

        tag.setBoolean("SlotLock", lockPrimary);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 9; i++)
        {
            NBTTagCompound t = new NBTTagCompound();
            itemLocks.get(i).writeToNBT(t);
            list.appendTag(t);
        }

        tag.setTag("ItemLocks", list);

        return true;
    }

    @Override
    public Object getGuiClient(InventoryPlayer inventory)
    {
        return new GuiFarm(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerFarm(inventory, this);
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

    public void setMode(boolean mode)
    {
        boolean lastMode = lockPrimary;
        lockPrimary = mode;

        if (mode)
            for (int i = SLOT_FARM_START; i < SLOT_FARM_START + 9; i++)
                itemLocks.set(i - SLOT_FARM_START, getStackInSlot(i));
        else
            itemLocks = itemLocks.stream().map(s -> s = ItemStack.EMPTY).collect(Collectors.toList());

        sendModePacket();
        lockPrimary = lastMode;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        inputTrackerPrimary = nbt.getInteger("TrackIn1");
        inputTrackerSecondary = nbt.getInteger("TrackIn2");
        outputTracker = nbt.getInteger("TrackOut1");
        lockPrimary = nbt.getBoolean("SlotLock");

        NBTTagList list = nbt.getTagList("ItemLocks", 10);
        for (int i = 0; i < 9; i++)
            itemLocks.set(i, new ItemStack(list.getCompoundTagAt(i)));

        tank.readFromNBT(nbt);
        virtualFarm.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("TrackIn1", inputTrackerPrimary);
        nbt.setInteger("TrackIn2", inputTrackerSecondary);
        nbt.setInteger("TrackOut1", outputTracker);
        nbt.setBoolean("SlotLock", lockPrimary);

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 9; i++)
        {
            NBTTagCompound tag = new NBTTagCompound();
            itemLocks.get(i).writeToNBT(tag);
            list.appendTag(tag);
        }

        nbt.setTag("ItemLocks", list);

        tank.writeToNBT(nbt);
        virtualFarm.writeToNBT(nbt);
        return nbt;
    }

    @Override
    public PacketBase getModePacket()
    {
        PacketBase payload = super.getModePacket();

        payload.addBool(lockPrimary);

        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 9; i++)
        {
            NBTTagCompound tag = new NBTTagCompound();
            itemLocks.get(i).writeToNBT(tag);
            list.appendTag(tag);
        }

        nbt.setTag("ItemLocks", list);

        try
        {
            payload.writeNBT(nbt);
        } catch (Exception e) {}

        return payload;
    }

    @Override
    protected void handleModePacket(PacketBase payload)
    {
        super.handleModePacket(payload);

        lockPrimary = payload.getBool();

        try
        {
            NBTTagCompound nbt = payload.readNBT();
            NBTTagList list = nbt.getTagList("ItemLocks", 10);
            for (int i = 0; i < 9; i++)
                itemLocks.set(i, new ItemStack(list.getCompoundTagAt(i)));
        } catch (Exception e) {}

        callNeighborTileChange();
    }

    @Override
    public PacketBase getGuiPacket()
    {
        PacketBase payload = super.getGuiPacket();

        payload.addBool(lockPrimary);
        payload.addFluidStack(tank.getFluid());

        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < 9; i++)
        {
            NBTTagCompound tag = new NBTTagCompound();
            itemLocks.get(i).writeToNBT(tag);
            list.appendTag(tag);
        }

        nbt.setTag("ItemLocks", list);

        try
        {
            payload.writeNBT(nbt);
        } catch (Exception e) {}

        return payload;
    }

    @Override
    protected void handleGuiPacket(PacketBase payload)
    {
        super.handleGuiPacket(payload);

        lockPrimary = payload.getBool();
        tank.setFluid(payload.getFluidStack());

        try
        {
            NBTTagCompound nbt = payload.readNBT();
            NBTTagList list = nbt.getTagList("ItemLocks", 10);
            for (int i = 0; i < 9; i++)
                itemLocks.set(i, new ItemStack(list.getCompoundTagAt(i)));
        } catch (Exception e) {}
    }

    @Override
    protected void preAugmentInstall()
    {
        super.preAugmentInstall();

        augmentSoil = false;
        augmentMonoculture = false;
    }

    @Override
    protected boolean isValidAugment(AugmentType type, String id)
    {
        if (augmentSoil && VMConstants.MACHINE_FARM_SOIL.equals(id))
            return false;
        if (augmentMonoculture && TEProps.MACHINE_INSOLATOR_MONOCULTURE.equals(id))
            return false;

        return super.isValidAugment(type, id);
    }

    @Override
    protected boolean installAugmentToSlot(int slot)
    {
        String id = AugmentHelper.getAugmentIdentifier(augments[slot]);

        if (TEProps.MACHINE_INSOLATOR_FERTILIZER.equals(id))
        {
            reuseChance += 20;
            energyMod += 10;
        }

        if (!augmentSoil && VMConstants.MACHINE_FARM_SOIL.equals(id))
        {
            augmentSoil = true;
            energyMod += SOIL_ENERGY_MOD;
            return true;
        }

        if (!augmentMonoculture && TEProps.MACHINE_INSOLATOR_MONOCULTURE.equals(id))
        {
            augmentMonoculture = true;
            hasModeAugment = true;
            reuseChance += 10;
            energyMod += 10;
            return true;
        }

        return super.installAugmentToSlot(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        if (Utils.isSlotInRange(slot, SLOT_TOOLS_START, SLOT_TOOLS_START + 4))
        {
            return Utils.checkTool(stack, "axe", "shears", "hoe", "sickle");
        }
        else if (slot == SLOT_FERTILIZER)
        {
            return VirtualFarm.isFertilizer(stack);
        }
        else if (Utils.isSlotInRange(slot, SLOT_FARM_START, SLOT_FARM_START + 9))
        {
            return InsolatorManager.isItemValid(stack) && (!lockPrimary || stack.isItemEqual(itemLocks.get(slot - SLOT_FARM_START)));
        }

        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing from)
    {
        return super.hasCapability(capability, from) || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, final EnumFacing from)
    {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
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
        if (lockPrimary)
        {
            if (Utils.isSlotInRange(slot, SLOT_FARM_START, SLOT_FARM_START + 9))
            {
                if (itemLocks.get(slot - SLOT_FARM_START).isEmpty())
                {
                    itemLocks.set(slot - SLOT_FARM_START, stack);
                    sendModePacket();
                }
            }
        }
    }

    @Override
    public VirtualMachine getVirtualMachine()
    {
        return virtualFarm;
    }

    public List<ItemStack> getItemLocks()
    {
        return itemLocks;
    }
}
