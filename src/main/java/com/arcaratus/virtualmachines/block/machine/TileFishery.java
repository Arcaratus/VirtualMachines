package com.arcaratus.virtualmachines.block.machine;

import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketBase;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.util.managers.device.FisherManager;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine.Type;
import com.arcaratus.virtualmachines.gui.client.machine.GuiFishery;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerFishery;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.VirtualFishery;
import com.arcaratus.virtualmachines.virtual.IVirtualMachine;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

public class TileFishery extends TileVirtualMachine
{
    private static final int TYPE = Type.FISHERY.getMetadata();
    public static int basePower = 50;
    public static int baseFluid = 1000;

    public static final int SLOT_FISHING_ROD = 0;
    public static final int SLOT_BAIT = 1;
    public static final int SLOT_OUTPUT_START = 2;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 5;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, { SLOT_FISHING_ROD, SLOT_BAIT }, IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9).toArray(), { SLOT_FISHING_ROD }, { SLOT_BAIT }, {}, IntStream.range(0, 12).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, INPUT_PRIMARY, INPUT_SECONDARY, OPEN, OMNI };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 3, 1, 2, 2, 2, 2 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = new boolean[] { true, true, false, false, false, false, false, false, false, false, false };
        SLOT_CONFIGS[TYPE].allowExtractionSlot = new boolean[] { false, false, true, true, true, true, true, true, true, true, true };

        VALID_AUGMENTS[TYPE] = new HashSet<>();

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileFishery.class, "virtualmachines:virtual_fishery");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.Fishery";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Fishery. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    private int inputTracker;
    private int outputTracker;

    private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);

    private boolean updateOutputs = true;
    private VirtualFishery virtualFishery = new VirtualFishery();

    public TileFishery()
    {
        super();

        inventory = new ItemStack[12]; // 1 + 1 + 9 + 1
        Arrays.fill(inventory, ItemStack.EMPTY);
        createAllSlots(inventory.length);
        tank.setLock(FluidRegistry.WATER);
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
        if (getStackInSlot(SLOT_FISHING_ROD).isEmpty())
            return false;

        List<ItemStack> outputs = virtualFishery.getOutputs();
        if (updateOutputs)
        {
            outputs.clear();
            ItemStack rodStack = getStackInSlot(SLOT_FISHING_ROD);
            LootContext.Builder lootContext = new LootContext.Builder((WorldServer) world);
            lootContext.withLuck((float) EnchantmentHelper.getFishingLuckBonus(rodStack));
            outputs = world.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING).generateLootForPools(world.rand, lootContext.build());
            virtualFishery.setOutputs(outputs);
            updateOutputs = false;
        }

        return !outputs.isEmpty() && Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
    }

    @Override
    protected boolean hasValidInput()
    {
        return true;
    }

    @Override
    protected void processStart()
    {
        double maxProcess = 18000;

        ItemStack baitStack = getStackInSlot(SLOT_BAIT);
        if (!baitStack.isEmpty())
        {
            maxProcess /= (double) FisherManager.getBaitMultiplier(baitStack);
            baitStack.shrink(1);
        }

        processMax = (int) maxProcess;
        processRem = processMax;
    }

    @Override
    protected void processFinish()
    {
        ItemStack rodStack = getStackInSlot(SLOT_FISHING_ROD);
        List<ItemStack> outputs = virtualFishery.getOutputs();

        for (ItemStack itemStack : outputs)
        {
            ItemStack damagedRodStack = rodStack;
            damagedRodStack = Utils.damageItem(damagedRodStack, 1, world.rand);

            setInventorySlotContents(SLOT_FISHING_ROD, damagedRodStack);

            Utils.distributeOutput(this, itemStack, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
        }

        tank.modifyFluidStored(-baseFluid);
    }

    @Override
    protected void transferInput()
    {
        if (!enableAutoInput)
            return;

        int side;
        for (int i = inputTracker + 1; i <= inputTracker + 6; i++)
        {
            side = i % 6;
            if (extractItem(SLOT_FISHING_ROD, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
            {
                inputTracker = side;
                break;
            }

            if (extractItem(SLOT_BAIT, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
            {
                inputTracker = side;
                break;
            }
        }
    }

    @Override
    protected void transferOutput()
    {
        if (!enableAutoOutput)
            return;

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

    @Override
    public Object getGuiClient(InventoryPlayer inventory)
    {
        return new GuiFishery(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerFishery(inventory, this);
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

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        inputTracker = nbt.getInteger("TrackIn");
        outputTracker = nbt.getInteger("TrackOut");
        updateOutputs = nbt.getBoolean("UpdateOutputs");
        tank.readFromNBT(nbt);
        virtualFishery.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("TrackIn", inputTracker);
        nbt.setInteger("TrackOut", outputTracker);
        nbt.setBoolean("UpdateOutputs", updateOutputs);
        tank.writeToNBT(nbt);
        virtualFishery.writeToNBT(nbt);
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

        payload.addFluidStack(tank.getFluid());

        return payload;
    }

    @Override
    protected void handleGuiPacket(PacketBase payload)
    {
        super.handleGuiPacket(payload);

        tank.setFluid(payload.getFluidStack());
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return slot == SLOT_FISHING_ROD ? Utils.checkTool(stack, "fishing_rod") : (slot != SLOT_BAIT || FisherManager.isValidBait(stack));
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
        updateOutputs = true;
    }

    @Override
    public IVirtualMachine getVirtualMachine()
    {
        return virtualFishery;
    }
}
