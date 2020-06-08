package com.arcaratus.virtualmachines.block.machine;

import cofh.api.item.IAugmentItem.AugmentType;
import cofh.core.fluid.FluidTankCore;
import cofh.core.network.PacketBase;
import cofh.core.util.core.*;
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
import com.arcaratus.virtualmachines.gui.client.machine.GuiMobSpawner;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerMobSpawner;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.IVirtualMachine;
import com.arcaratus.virtualmachines.virtual.VirtualMobSpawner;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
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

import static cofh.core.util.core.SideConfig.*;

public class TileMobSpawner extends TileVirtualMachine
{
    private static final int TYPE = Type.MOB_SPAWNER.getMetadata();
    private static final int SLOT_COUNT = 12; // 9 + 1 + 1 + 1 = 12
    public static int basePower = 80;

    public static int SLOT_SWORD = 0;
    public static int SLOT_MORB = 1;
    public static int SLOT_OUTPUT_START = 2;

    public static final int EXPERIENCE_MOD = 80;
    public static final int PERMAMORB_MOD = 220;
    public static final int MORB_CAPTURE_MOD = 60;

    public static final int EXPERIENCE = 50;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 5;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, { SLOT_SWORD, SLOT_MORB }, IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9).toArray(), { SLOT_SWORD }, { SLOT_MORB }, {}, IntStream.range(SLOT_SWORD, SLOT_OUTPUT_START + 9).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, INPUT_PRIMARY, INPUT_SECONDARY, OPEN, OMNI };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 3, 1, 2, 2, 2, 2 };

        ALT_SIDE_CONFIGS[TYPE] = new SideConfig();
        ALT_SIDE_CONFIGS[TYPE].numConfig = 2;
        ALT_SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, { SLOT_SWORD, SLOT_MORB }, IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9).toArray(), { SLOT_SWORD }, { SLOT_MORB }, {}, IntStream.range(SLOT_SWORD, SLOT_OUTPUT_START + 9).toArray() };
        ALT_SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, OPEN };
        ALT_SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 1, 1, 1, 1, 1, 1 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = Utils.buildFilterArray(SLOT_COUNT, 0, 2); // sword, morb
        SLOT_CONFIGS[TYPE].allowExtractionSlot = Utils.buildFilterArray(SLOT_COUNT, 1, 11); // morb, outputs

        VALID_AUGMENTS[TYPE] = new HashSet<>();
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_EXPERIENCE);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_PERMAMORB);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_MORB_CAPTURE);

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileMobSpawner.class, "virtualmachines:virtual_mob_spawner");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.MobSpawner";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Mob Spawner. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    private int inputTracker;
    private int outputTracker;
    private int outputTrackerFluid;

    private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);

    private boolean updateOutputs = true;
    public VirtualMobSpawner virtualMobSpawner = new VirtualMobSpawner();

    protected boolean augmentExperience;
    protected boolean flagExperience;
    protected boolean augmentPermamorb;
    protected boolean augmentMorbCapture;

    public TileMobSpawner()
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
        if (getStackInSlot(SLOT_SWORD).isEmpty() || getStackInSlot(SLOT_MORB).isEmpty())
            return false;

        List<ItemStack> outputs = virtualMobSpawner.getOutputs();
        if (updateOutputs)
        {
            outputs.clear();
            ItemStack morbStack = getStackInSlot(SLOT_MORB);

            CentrifugeRecipe recipe = CentrifugeManager.getRecipeMob(morbStack);

            if (recipe != null)
            {
                if (augmentMorbCapture)
                {
                    if (getStackInSlot(SLOT_SWORD).getItem() != TEItems.itemMorb)
                        return false;

                    ItemStack morbOutput = ItemStack.areItemsEqual(getStackInSlot(SLOT_SWORD), ItemMorb.morbStandard) ? ItemMorb.morbStandard.copy() : ItemMorb.morbReusable.copy();
                    NBTTagCompound morbNBT = morbStack.getTagCompound();
                    if (morbNBT != null && ItemMorb.validMobs.contains(morbNBT.getString("id")))
                        ItemMorb.setTag(morbOutput, morbNBT.getString("id"), false);

                    outputs.add(morbOutput);

                    getStackInSlot(SLOT_SWORD).shrink(1);

                    virtualMobSpawner.setOutputs(outputs);
                    updateOutputs = false;

                    return Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
                }

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

                if (!augmentPermamorb)
                {
                    ItemStack morbOutput = ItemStack.areItemsEqual(getStackInSlot(SLOT_MORB), ItemMorb.morbStandard) ? ItemMorb.morbStandard.copy() : ItemMorb.morbReusable.copy();
                    setInventorySlotContents(SLOT_MORB, morbOutput);
                }

                outputs.removeIf(s -> s.getItem() == TEItems.itemMorb);
                outputs.forEach(stack ->
                {
                    float f = (float) EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, getStackInSlot(SLOT_SWORD)) * world.rand.nextFloat();
                    stack.grow(Math.round(f));
                });
                virtualMobSpawner.setOutputs(outputs);
                updateOutputs = false;
            }
            else
            {
                return false;
            }
        }

        return Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
    }

    @Override
    protected boolean hasValidInput()
    {
        ItemStack swordStack = getStackInSlot(SLOT_SWORD);
        return augmentMorbCapture ? swordStack.isEmpty() || swordStack.getItem() == TEItems.itemMorb : Utils.checkTool(swordStack, "sword");
    }

    @Override
    protected void processStart()
    {
        double maxProcess = 32000;

        processMax = (int) maxProcess;
        processRem = processMax;
    }

    @Override
    protected void processFinish()
    {
        List<ItemStack> outputs = virtualMobSpawner.getOutputs();

        int damage = 0;
        for (ItemStack itemStack : outputs)
        {
            Utils.distributeOutput(this, itemStack, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
            damage++;
        }

        if (!augmentMorbCapture)
        {
            ItemStack damagedSwordStack = getStackInSlot(SLOT_SWORD);
            damagedSwordStack = Utils.damageItem(damagedSwordStack, damage, world.rand);

            setInventorySlotContents(SLOT_SWORD, damagedSwordStack);
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
        return new GuiMobSpawner(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerMobSpawner(inventory, this);
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

    public boolean augmentMorbCapture()
    {
        return augmentMorbCapture;
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
        virtualMobSpawner.readFromNBT(nbt);
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
        virtualMobSpawner.writeToNBT(nbt);
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
        payload.addBool(augmentMorbCapture);

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
        augmentMorbCapture = payload.getBool();
    }

    @Override
    protected void preAugmentInstall()
    {
        super.preAugmentInstall();

        augmentExperience = false;
        augmentPermamorb = false;
        augmentMorbCapture = false;
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
        if (augmentMorbCapture && VMConstants.MACHINE_MORB_CAPTURE.equals(id))
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

        if (!augmentMorbCapture && VMConstants.MACHINE_MORB_CAPTURE.equals(id))
        {
            augmentMorbCapture = true;
            energyMod += MORB_CAPTURE_MOD;
            return true;
        }

        return super.installAugmentToSlot(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        if (slot == SLOT_SWORD)
        {
            return augmentMorbCapture ? stack.getItem() == TEItems.itemMorb : Utils.checkTool(stack, "sword");
        }
        if (slot == SLOT_MORB)
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
        return virtualMobSpawner;
    }
}
