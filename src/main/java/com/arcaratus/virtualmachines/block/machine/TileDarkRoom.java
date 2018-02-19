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
import com.arcaratus.virtualmachines.gui.client.machine.GuiDarkRoom;
import com.arcaratus.virtualmachines.gui.container.machine.ContainerDarkRoom;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.Utils;
import com.arcaratus.virtualmachines.virtual.IVirtualMachine;
import com.arcaratus.virtualmachines.virtual.VirtualDarkRoom;
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

public class TileDarkRoom extends TileVirtualMachine
{
    private static final int TYPE = Type.DARK_ROOM.getMetadata();
    public static int basePower = 80;

    public static int SLOT_SWORD = 0;
    public static int SLOT_OUTPUT_START = 1;

    public static final int EXPERIENCE_MOD = 80;
    public static final int NETHER_MOD = 40;

    public static final int EXPERIENCE = 50;

    public static void init()
    {
        SIDE_CONFIGS[TYPE] = new SideConfig();
        SIDE_CONFIGS[TYPE].numConfig = 5;
        SIDE_CONFIGS[TYPE].slotGroups = new int[][] { {}, { SLOT_SWORD }, IntStream.range(SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9).toArray(), {}, IntStream.range(0, 11).toArray() };
        SIDE_CONFIGS[TYPE].sideTypes = new int[] { NONE, INPUT_ALL, OUTPUT_ALL, OPEN, OMNI };
        SIDE_CONFIGS[TYPE].defaultSides = new byte[] { 3, 1, 2, 2, 2, 2 };

        SLOT_CONFIGS[TYPE] = new SlotConfig();
        SLOT_CONFIGS[TYPE].allowInsertionSlot = new boolean[] { true, false, false, false, false, false, false, false, false, false };
        SLOT_CONFIGS[TYPE].allowExtractionSlot = new boolean[] { false, true, true, true, true, true, true, true, true, true };

        VALID_AUGMENTS[TYPE] = new HashSet<>();
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_EXPERIENCE);
        VALID_AUGMENTS[TYPE].add(VMConstants.MACHINE_NETHER);

        LIGHT_VALUES[TYPE] = 14;

        GameRegistry.registerTileEntity(TileDarkRoom.class, "virtualmachines:virtual_dark_room");

        config();
    }

    public static void config()
    {
        String category = "VirtualMachine.DarkRoom";
        BlockVirtualMachine.enable[TYPE] = VirtualMachines.CONFIG.get(category, "Enable", true);

        String comment = "Adjust this value to change the Energy consumption (in RF/t) for a Virtual Dark Room. This base value will scale with block level and Augments.";
        basePower = VirtualMachines.CONFIG.getConfiguration().getInt("BasePower", category, basePower, MIN_BASE_POWER, MAX_BASE_POWER, comment);

        ENERGY_CONFIGS[TYPE] = new EnergyConfig();
        ENERGY_CONFIGS[TYPE].setDefaultParams(basePower, smallStorage);
    }

    private int inputTracker;
    private int outputTracker;
    private int outputTrackerFluid;

    private FluidTankCore tank = new FluidTankCore(TEProps.MAX_FLUID_LARGE);

    private boolean updateOutputs = true;
    public VirtualDarkRoom virtualDarkRoom = new VirtualDarkRoom();

    protected boolean augmentExperience;
    protected boolean flagExperience;
    protected boolean augmentNether;

    public TileDarkRoom()
    {
        super();

        inventory = new ItemStack[11]; // 9 + 1 + 1 = 11
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

        List<ItemStack> outputs = virtualDarkRoom.getOutputs();
        if (updateOutputs)
        {
            outputs.clear();
            ItemStack morbStack = ItemMorb.morbStandard.copy();

            if (augmentNether || world.provider.isNether())
            {
                int randomInt = world.rand.nextInt(10);
                if (randomInt < 3)       // [0, 2]
                    ;
                else if (randomInt < 7)  // [3, 6]
                    ItemMorb.setTag(morbStack, "minecraft:zombie_pigman", true);
                else if (randomInt < 8)  // [7]
                    ItemMorb.setTag(morbStack, "minecraft:blaze", true);
                else if (randomInt < 9)  // [8]
                    ItemMorb.setTag(morbStack, "minecraft:magma_cube", true);
                else if (randomInt < 10 && world.rand.nextFloat() < 0.2)
                    ItemMorb.setTag(morbStack, "minecraft:wither_skeleton", true);
            }
            else
            {
                int randomInt = world.rand.nextInt(12);
                if (randomInt < 2)       // [0, 1]
                    ;
                else if (randomInt < 5)  // [2, 4]
                    ItemMorb.setTag(morbStack, "minecraft:zombie", true);
                else if (randomInt < 7)  // [5, 6]
                    ItemMorb.setTag(morbStack, "minecraft:skeleton", true);
                else if (randomInt < 9)  // [7, 8]
                    ItemMorb.setTag(morbStack, "minecraft:spider", true);
                else if (randomInt < 11) // [9, 10]
                    ItemMorb.setTag(morbStack, "minecraft:creeper", true);
                else if (randomInt < 12) // [11]
                    ItemMorb.setTag(morbStack, "minecraft:enderman", true);
            }

            CentrifugeRecipe recipe = CentrifugeManager.getRecipeMob(morbStack);

            if (recipe != null)
            {
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
                outputs.forEach(stack ->
                {
                    float f = (float) EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, getStackInSlot(SLOT_SWORD)) * world.rand.nextFloat();
                    stack.grow(Math.round(f));
                });
                virtualDarkRoom.setOutputs(outputs);
                updateOutputs = false;
            }
        }

        return Utils.canFitOutputs(this, outputs, SLOT_OUTPUT_START, SLOT_OUTPUT_START + 9);
    }

    @Override
    protected boolean hasValidInput()
    {
        return !getStackInSlot(SLOT_SWORD).isEmpty();
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
        List<ItemStack> outputs = virtualDarkRoom.getOutputs();

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
        return new GuiDarkRoom(inventory, this);
    }

    @Override
    public Object getGuiServer(InventoryPlayer inventory)
    {
        return new ContainerDarkRoom(inventory, this);
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
        virtualDarkRoom.readFromNBT(nbt);
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
        virtualDarkRoom.writeToNBT(nbt);
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
        payload.addBool(augmentNether);
        payload.addFluidStack(tank.getFluid());

        return payload;
    }

    @Override
    protected void handleGuiPacket(PacketBase payload)
    {
        super.handleGuiPacket(payload);

        augmentExperience = payload.getBool();
        flagExperience = augmentExperience;
        augmentNether = payload.getBool();
        tank.setFluid(payload.getFluidStack());
    }

    @Override
    protected void preAugmentInstall()
    {
        super.preAugmentInstall();

        augmentExperience = false;
        augmentNether = false;
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
        if (augmentNether && VMConstants.MACHINE_NETHER.equals(id))
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

        if (!augmentNether && VMConstants.MACHINE_NETHER.equals(id))
        {
            augmentNether = true;
            energyMod += NETHER_MOD;
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
        return virtualDarkRoom;
    }
}
