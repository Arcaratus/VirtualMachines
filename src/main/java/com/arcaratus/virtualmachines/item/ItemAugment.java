package com.arcaratus.virtualmachines.item;

import cofh.api.core.IAugmentable;
import cofh.api.core.ISecurable;
import cofh.api.item.IAugmentItem;
import cofh.core.item.ItemMulti;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.item.ItemMorb;
import cofh.thermalfoundation.init.TFFluids;
import cofh.thermalfoundation.item.ItemMaterial;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.init.VMConstants;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;

public class ItemAugment extends ItemMulti implements IInitializer, IAugmentItem
{
    private TIntObjectHashMap<AugmentEntry> augmentMap = new TIntObjectHashMap<>();

    public static ItemStack machine_farm_soil;
    public static ItemStack machine_experience;
    public static ItemStack machine_nether;
    public static ItemStack machine_rancher;
    public static ItemStack machine_permamorb;

    public ItemAugment()
    {
        super(VirtualMachines.MOD_ID);

        setUnlocalizedName("augment");
        setCreativeTab(VirtualMachines.TAB_VIRTUAL_MACHINES);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return StringHelper.localize("info.thermalexpansion.augment.0") + ": " + super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown())
            tooltip.add(StringHelper.shiftForDetails());

        if (!StringHelper.isShiftKeyDown())
            return;

        AugmentType type = getAugmentType(stack);
        String id = getAugmentIdentifier(stack);

        if (id.isEmpty())
            return;

        int i = 0;
        String line = "info.virtualmachines.augment." + id + "." + i;
        while (StringHelper.canLocalize(line))
        {
            tooltip.add(StringHelper.localize(line));
            i++;
            line = "info.virtualmachines.augment." + id + "." + i;
        }

        i = 0;
        line = "info.virtualmachines.augment." + id + ".a." + i;
        while (StringHelper.canLocalize(line))
        {
            tooltip.add(StringHelper.BRIGHT_GREEN + StringHelper.localize(line));
            i++;
            line = "info.virtualmachines.augment." + id + ".a." + i;
        }

        i = 0;
        line = "info.virtualmachines.augment." + id + ".b." + i;
        while (StringHelper.canLocalize(line))
        {
            tooltip.add(StringHelper.RED + StringHelper.localize(line));
            i++;
            line = "info.virtualmachines.augment." + id + ".b." + i;
        }

        i = 0;
        line = "info.virtualmachines.augment." + id + ".c." + i;
        while (StringHelper.canLocalize(line))
        {
            tooltip.add(StringHelper.YELLOW + StringHelper.localize(line));
            i++;
            line = "info.virtualmachines.augment." + id + ".c." + i;
        }

        switch (type)
        {
            case ADVANCED:
                // tooltip.add(StringHelper.getNoticeText("info.virtualmachines.augment.noticeAdvanced"));
                break;
            case MODE:
                tooltip.add(StringHelper.getNoticeText("info.thermalexpansion.augment.noticeMode"));
                break;
            case ENDER:
                tooltip.add(StringHelper.getNoticeText("info.thermalexpansion.augment.noticeEnder"));
                break;
            case CREATIVE:
                tooltip.add(StringHelper.getNoticeText("info.thermalexpansion.augment.noticeCreative"));
                break;
            default:
        }
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player)
    {
        return true;
    }

    @Override
    public boolean isFull3D()
    {
        return true;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!block.hasTileEntity(state))
            return EnumActionResult.PASS;

        TileEntity tile = world.getTileEntity(pos);

        if (tile instanceof ISecurable && !((ISecurable) tile).canPlayerAccess(player))
            return EnumActionResult.PASS;

        if (tile instanceof IAugmentable)
        {
            if (((IAugmentable) tile).getAugmentSlots().length <= 0)
                return EnumActionResult.PASS;

            if (ServerHelper.isServerWorld(world))
            { // Server
                if (((IAugmentable) tile).installAugment(stack))
                {
                    if (!player.capabilities.isCreativeMode)
                        stack.shrink(1);

                    player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.4F, 0.8F);
                    ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("chat.thermalfoundation.augment.install.success"));
                }
                else
                {
                    ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("chat.thermalfoundation.augment.install.failure"));
                }

                return EnumActionResult.SUCCESS;
            }
        }

        return EnumActionResult.PASS;
    }

    /* IAugmentItem */
    @Override
    public AugmentType getAugmentType(ItemStack stack)
    {
        if (!augmentMap.containsKey(ItemHelper.getItemDamage(stack)))
            return AugmentType.CREATIVE;

        return augmentMap.get(ItemHelper.getItemDamage(stack)).type;
    }

    @Override
    public String getAugmentIdentifier(ItemStack stack)
    {
        if (!augmentMap.containsKey(ItemHelper.getItemDamage(stack)))
            return "";

        return augmentMap.get(ItemHelper.getItemDamage(stack)).identifier;
    }

    @Override
    public boolean initialize()
    {
        int metadata = 1024;
        machine_farm_soil = addAugmentItem(metadata++, VMConstants.MACHINE_FARM_SOIL, AugmentType.ADVANCED);
        machine_experience = addAugmentItem(metadata++, VMConstants.MACHINE_EXPERIENCE, AugmentType.MODE);
        machine_nether = addAugmentItem(metadata++, VMConstants.MACHINE_NETHER, AugmentType.ADVANCED);
        machine_rancher = addAugmentItem(metadata++, VMConstants.MACHINE_RANCHER, AugmentType.MODE);
        machine_permamorb = addAugmentItem(metadata++, VMConstants.MACHINE_PERMAMORB, AugmentType.ADVANCED);

        VirtualMachines.proxy.addIModelRegister(this);

        return true;
    }

    @Override
    public boolean register()
    {
        addShapedRecipe(machine_farm_soil,
                " G ",
                "PCP",
                "DSE",
                'G', "gearConstantan",
                'P', "plateInvar",
                'C', ItemMaterial.powerCoilElectrum,
                'D', "dirt",
                'S', "sand",
                'E', Blocks.END_STONE
        );

        addShapedRecipe(machine_experience,
                " G ",
                "PCP",
                "SSS",
                'G', "gearSignalum",
                'P', "plateLumium",
                'C', ItemMaterial.powerCoilElectrum,
                'S', FluidUtil.getFilledBucket(new FluidStack(TFFluids.fluidExperience, Fluid.BUCKET_VOLUME))
        );

        addShapedRecipe(machine_nether,
                " G ",
                "PCP",
                "NBN",
                'G', "gearBronze",
                'P', "plateSilver",
                'C', ItemMaterial.powerCoilElectrum,
                'N', "netherrack",
                'B', Items.BLAZE_ROD
        );

        addShapedRecipe(machine_rancher,
                " G ",
                "PCP",
                "BSB",
                'G', "gearGold",
                'P', "plateAluminum",
                'C', ItemMaterial.powerCoilElectrum,
                'B', Items.BUCKET,
                'S', Items.SHEARS
        );

        addShapedRecipe(machine_permamorb,
                " G ",
                "PCP",
                "SMS",
                'G', "gearEnderium",
                'P', "plateSteel",
                'C', ItemMaterial.powerCoilElectrum,
                'S', Items.NETHER_STAR,
                'M', ItemMorb.morbReusable
        );

        return true;
    }

    public class AugmentEntry
    {
        public final AugmentType type;
        public final String identifier;

        AugmentEntry(AugmentType type, String identifier)
        {
            this.type = type;
            this.identifier = identifier;
        }
    }

    private void addAugmentEntry(int metadata, AugmentType type, String identifier)
    {
        augmentMap.put(metadata, new AugmentEntry(type, identifier));
    }

    private ItemStack addAugmentItem(int metadata, String name)
    {
        addAugmentEntry(metadata, AugmentType.BASIC, name);
        return addItem(metadata, name);
    }

    private ItemStack addAugmentItem(int metadata, String name, EnumRarity rarity)
    {
        addAugmentEntry(metadata, AugmentType.BASIC, name);
        return addItem(metadata, name, rarity);
    }

    private ItemStack addAugmentItem(int metadata, String name, AugmentType type)
    {
        EnumRarity rarity;

        switch (type)
        {
            case ADVANCED:
            case MODE:
                rarity = EnumRarity.UNCOMMON;
                break;
            case ENDER:
                rarity = EnumRarity.RARE;
                break;
            case CREATIVE:
                rarity = EnumRarity.EPIC;
                break;
            default:
                rarity = EnumRarity.COMMON;
        }

        return addAugmentItem(metadata, name, type, rarity);
    }

    private ItemStack addAugmentItem(int metadata, String name, AugmentType type, EnumRarity rarity)
    {
        addAugmentEntry(metadata, type, name);
        return addItem(metadata, name, rarity);
    }
}