package com.arcaratus.virtualmachines.block.machine;

import cofh.api.item.IAugmentItem.AugmentType;
import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketBase;
import cofh.core.util.core.*;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.init.TEItems;
import cofh.thermalexpansion.item.ItemMorb;
import cofh.thermalexpansion.util.managers.machine.CentrifugeManager;
import cofh.thermalexpansion.util.managers.machine.CentrifugeManager.CentrifugeRecipe;
import cofh.thermalfoundation.init.TFFluids;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine;
import com.arcaratus.virtualmachines.block.BlockVirtualMachine.Type;
import com.arcaratus.virtualmachines.gui.client.machine.GuiMobFarm;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerMobFarm;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.IVirtualMachine;
import com.arcaratus.virtualmachines.virtual.VirtualMobFarm;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;

import static cofh.core.util.core.SideConfig.*;

public class TileMobFarm extends TileVirtualMachine
{
    private static final int TYPE = Type.MOB_FARM.getMetadata();
    private static final int SLOT_COUNT = 29; // 1 + 9 + 18 + 1 = 29
    public static int basePower = 200;

    public static int SLOT_SWORD = 0;
    public static int SLOT_MORB_START = 1;
    public static int SLOT_OUTPUT_START = 10;

    public static final int EXPERIENCE_MOD = 80;
    public static final int PERMAMORB_MOD = 220;

    public static final int EXPERIENCE = 50;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 5;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, IntStream.range(SLOT_SWORD, SLOT_MORB_START + 9).toArray(), IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 18).toArray(), { SLOT_SWORD }, IntStream.range(SLOT_MORB_START, SLOT_MORB_START + 9).toArray(), {}, IntStream.range(SLOT_SWORD, SLOT_OUTPUT_START + 18).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, INPUT_PRIMARY, INPUT_SECONDARY, OPEN, OMNI };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 3, 1, 2, 2, 2, 2 };

        ALT_SIDE_CONFIGS[TYPE] = new SideConfig();
        ALT_SIDE_CONFIGS[TYPE].numConfig = 2;
        ALT_SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, IntStream.range(SLOT_SWORD, SLOT_MORB_START + 9).toArray(), IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 18).toArray(), { SLOT_SWORD }, IntStream.range(SLOT_MORB_START, SLOT_MORB_START + 9).toArray(), {}, IntStream.range(SLOT_SWORD, SLOT_OUTPUT_START + 18).toArray() };
        ALT_SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, OPEN };
        ALT_SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 1, 1, 1, 1, 1, 1 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = Utils.buildFilterArray(SLOT_COUNT, 0, 10); // sword, morbs
        SLOT_CONFIGS[TYPE].allowExtractionSlot = Utils.buildFilterArray(SLOT_COUNT, 1, 28); // morbs, outputs

        VALID_AUGMENTS[TYPE] = new HashSet<>();
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_EXPERIENCE);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_PERMAMORB);

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileMobFarm.class, "virtualmachines:virtual_mob_farm");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.MobFarm";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Mob Farm. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    private int inputTracker;
    private int outputTracker;
    private int outputTrackerFluid;

    private FluidTankCore tank = new FluidTankCore(Fluid.BUCKET_VOLUME * 50);

    private boolean updateOutputs = true;
    public VirtualMobFarm virtualMobFarm = new VirtualMobFarm();

    protected boolean augmentExperience;
    protected boolean flagExperience;
    protected boolean augmentPermamorb;

    public TileMobFarm()
    {
        super();

        inventory = new ItemStack[SLOT_COUNT];
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
        if (getStackInSlot(SLOT_SWORD).isEmpty())
            return false;

        if (Utils.checkItemStackRange(inventory, SLOT_MORB_START, SLOT_MORB_START + 9, ItemStack::isEmpty))
            return false;

        List<ItemStack> outputs = virtualMobFarm.getOutputs();
        if (updateOutputs)
        {
            outputs.clear();

            for (int slot = SLOT_MORB_START; slot < SLOT_MORB_START + 9; slot++)
            {
                ItemStack morbStack = getStackInSlot(slot);

                CentrifugeRecipe recipe = CentrifugeManager.getRecipeMob(morbStack);

                if (recipe == null)
                {
                    if (slot == SLOT_MORB_START + 9)
                    {
                        return false;
                    }
                    else continue;
                }

                List<ItemStack> recipeOutputs = recipe.getOutput();
                List<Integer> chances = recipe.getChance();
                List<ItemStack> tempOutputs = new ArrayList<>();
                for (int i = 0; i < recipeOutputs.size(); i++)
                {
                    for (int j = 0; j < recipeOutputs.get(i).getCount(); j++)
                    {
                        if (world.rand.nextInt(secondaryChance + j * 10) < chances.get(i))
                        {
                            int itx = i;
                            if (tempOutputs.stream().findFirst().filter(s -> s.getItem() == recipeOutputs.get(itx).getItem()).isPresent())
                                tempOutputs.stream().findFirst().filter(s -> ItemStack.areItemsEqual(s, recipeOutputs.get(itx))).ifPresent(s -> s.grow(1));
                            else
                                tempOutputs.add(ItemHelper.cloneStack(recipeOutputs.get(i), 1));
                        }
                    }
                }

                if (!augmentPermamorb)
                {
                    ItemStack morbOutput = ItemStack.areItemsEqual(getStackInSlot(slot), ItemMorb.morbStandard) ? ItemMorb.morbStandard.copy() : ItemMorb.morbReusable.copy();
                    setInventorySlotContents(slot, morbOutput);
                }

                tempOutputs.removeIf(s -> s.getItem() == TEItems.itemMorb);
                tempOutputs.forEach(stack -> {
                    float f = (float) EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, getStackInSlot(SLOT_SWORD)) * world.rand.nextFloat();
                    stack.grow(Math.round(f));
                });

                outputs.addAll(tempOutputs);
            }

            virtualMobFarm.setOutputs(outputs);
            updateOutputs = false;
        }

        return !outputs.isEmpty() && Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
    }

    @Override
    protected boolean hasValidInput()
    {
        return Utils.checkTool(getStackInSlot(SLOT_SWORD), "sword");
    }

    @Override
    protected void processStart()
    {
        double maxProcess = 32000;

        for (int i = SLOT_MORB_START; i < SLOT_MORB_START + 9; i++)
        {
            ItemStack morbStack = getStackInSlot(i);
            if (!morbStack.isEmpty() && CentrifugeManager.getRecipeMob(morbStack) != null)
            {
                maxProcess += 4000;
            }
        }

        processMax = (int) maxProcess;
        processRem = processMax;
    }

    @Override
    protected void processFinish()
    {
        List<ItemStack> outputs = virtualMobFarm.getOutputs();

        int damage = 0;
        for (ItemStack itemStack : outputs)
        {
            Utils.distributeOutput(this, itemStack, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
            damage++;
        }

        ItemStack damagedSwordStack = getStackInSlot(SLOT_SWORD);
        damagedSwordStack = Utils.damageItem(damagedSwordStack, damage, world.rand);

        setInventorySlotContents(SLOT_SWORD, damagedSwordStack);

        if (augmentExperience && damage > 0)
            tank.fill(new FluidStack(TFFluids.fluidExperience, (int) (Math.round(world.rand.nextGaussian() * 25F + EXPERIENCE) * (double) outputs.size() / 3D)), true);

        updateOutputs = true;
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
            if (extractItem(SLOT_SWORD, ITEM_TRANSFER[level], EnumFacing.VALUES[side]))
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

        if (augmentExperience)
            transferOutputFluid();

        int side;

        if (Utils.checkItemStackRange(inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 18, ItemStack::isEmpty))
            return;

        for (int i = outputTracker + 1; i <= outputTracker + 6; i++)
        {
            side = i % 6;
            for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_START + 18; slot++)
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
        return new GuiMobFarm(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerMobFarm(inventory, this);
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

        inputTracker = nbt.getInteger("TrackIn");
        outputTracker = nbt.getInteger("TrackOut");
        outputTrackerFluid = nbt.getInteger("TrackOutFluid");
        updateOutputs = nbt.getBoolean("UpdateOutputs");
        tank.readFromNBT(nbt);
        virtualMobFarm.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setInteger("TrackIn", inputTracker);
        nbt.setInteger("TrackOut", outputTracker);
        nbt.setInteger("TrackOutFluid", outputTrackerFluid);
        nbt.setBoolean("UpdateOutputs", updateOutputs);
        tank.writeToNBT(nbt);
        virtualMobFarm.writeToNBT(nbt);
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
        payload.addFluidStack(tank.getFluid());
        payload.addBool(augmentPermamorb);

        return payload;
    }

    @Override
    protected void handleGuiPacket(PacketBase payload)
    {
        super.handleGuiPacket(payload);

        augmentExperience = payload.getBool();
        flagExperience = augmentExperience;
        tank.setFluid(payload.getFluidStack());
        augmentPermamorb = payload.getBool();
    }

    @Override
    protected void preAugmentInstall()
    {
        super.preAugmentInstall();

        augmentExperience = false;
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
        if (slot == SLOT_SWORD)
        {
            return Utils.checkTool(stack, "sword");
        }
        if (Utils.isSlotInRange(slot, SLOT_MORB_START, SLOT_MORB_START + 9))
        {
            return stack.getItem() == TEItems.itemMorb;
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
        return virtualMobFarm;
    }
}
