package com.arcaratus.virtualmachines.block;

import codechicken.lib.model.ModelRegistryHelper;
import codechicken.lib.model.bakery.*;
import codechicken.lib.model.bakery.generation.IBakery;
import codechicken.lib.texture.IWorldBlockTextureProvider;
import codechicken.lib.texture.TextureUtils;
import cofh.core.init.CoreProps;
import cofh.core.render.IModelRegister;
import cofh.core.util.helpers.*;
import cofh.thermalexpansion.block.BlockTEBase;
import cofh.thermalexpansion.block.device.BlockDevice;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.init.TETextures;
import cofh.thermalexpansion.item.ItemAugment;
import cofh.thermalfoundation.item.*;
import com.arcaratus.virtualmachines.VirtualMachines;
import com.arcaratus.virtualmachines.block.machine.*;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.init.VMTextures;
import com.arcaratus.virtualmachines.render.BakeryVirtualMachine;
import com.arcaratus.virtualmachines.virtual.VirtualFarm;
import com.arcaratus.virtualmachines.virtual.VirtualFishery;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static cofh.core.util.helpers.RecipeHelper.*;

public class BlockVirtualMachine extends BlockTEBase implements IModelRegister, IBakeryProvider, IWorldBlockTextureProvider
{
    public static final PropertyEnum<Type> VARIANT = PropertyEnum.create("type", Type.class);
    public static boolean[] enable = new boolean[Type.values().length];

    public static ItemStack virtual_farm;
    public static ItemStack virtual_fishery;
    public static ItemStack virtual_dark_room;
    public static ItemStack virtual_animal_farm;
    public static ItemStack virtual_mob_spawner;
    public static ItemStack virtual_mob_farm;

    public static ItemBlockVirtualMachine itemBlock;

    public BlockVirtualMachine()
    {
        super(Material.IRON);
        modName = VirtualMachines.MOD_ID;
        setUnlocalizedName("virtual_machine");
        setCreativeTab(VirtualMachines.TAB_VIRTUAL_MACHINES);

        setHardness(15.0F);
        setResistance(25.0F);
        setDefaultState(getBlockState().getBaseState().withProperty(VARIANT, Type.FARM));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        BlockStateContainer.Builder builder = new BlockStateContainer.Builder(this);
        // Listed
        builder.add(VARIANT);
        // UnListed
        builder.add(ModelErrorStateProperty.ERROR_STATE);
        builder.add(VMConstants.TILE_VIRTUAL_MACHINE);
        builder.add(TEProps.BAKERY_WORLD);

        return builder.build();
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        for (int i = 0; i < Type.values().length; i++)
        {
            if (enable[i])
            {
                if (TEProps.creativeTabShowAllBlockLevels)
                {
                    for (int j = 0; j <= CoreProps.LEVEL_MAX; j++)
                    {
                        items.add(itemBlock.setDefaultTag(new ItemStack(this, 1, i), j));
                    }
                }
                else
                {
                    items.add(itemBlock.setDefaultTag(new ItemStack(this, 1, i), TEProps.creativeTabLevel));
                }

                if (TEProps.creativeTabShowCreative)
                {
                    items.add(itemBlock.setCreativeTag(new ItemStack(this, 1, i)));
                }
            }
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        return "tile.virtualmachines." + Type.values()[ItemHelper.getItemDamage(stack)].getName() + ".name";
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return getDefaultState().withProperty(VARIANT, Type.values()[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        int meta = state.getBlock().getMetaFromState(state);
        if (meta >= Type.values().length)
            return null;

        switch (Type.values()[meta])
        {
            case FARM:
                return new TileFarm();
            case FISHERY:
                return new TileFishery();
            case DARK_ROOM:
                return new TileDarkRoom();
            case ANIMAL_FARM:
                return new TileAnimalFarm();
            case MOB_SPAWNER:
                return new TileMobSpawner();
            case MOB_FARM:
                return new TileMobFarm();
            default:
                return null;
        }
    }

    /* BLOCK METHODS */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack)
    {
        if (stack.getTagCompound() != null)
        {
            TileVirtualMachine tile = (TileVirtualMachine) world.getTileEntity(pos);

            tile.setLevel(stack.getTagCompound().getByte("Level"));
            tile.readAugmentsFromNBT(stack.getTagCompound());
            tile.updateAugmentStatus();
            tile.setEnergyStored(stack.getTagCompound().getInteger("Energy"));

            int facing = BlockHelper.determineXZPlaceFacing(living);
            int storedFacing = ReconfigurableHelper.getFacing(stack);
            byte[] sideCache = ReconfigurableHelper.getSideCache(stack, tile.getDefaultSides());

            tile.sideCache[0] = sideCache[0];
            tile.sideCache[1] = sideCache[1];
            tile.sideCache[facing] = 0;
            tile.sideCache[BlockHelper.getLeftSide(facing)] = sideCache[BlockHelper.getLeftSide(storedFacing)];
            tile.sideCache[BlockHelper.getRightSide(facing)] = sideCache[BlockHelper.getRightSide(storedFacing)];
            tile.sideCache[BlockHelper.getOppositeSide(facing)] = sideCache[BlockHelper.getOppositeSide(storedFacing)];
        }

        super.onBlockPlacedBy(world, pos, state, living, stack);
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return true;
    }

    @Override
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        return true;
    }

    @Override
    public boolean onBlockActivatedDelegate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        TileVirtualMachine tile = (TileVirtualMachine) world.getTileEntity(pos);

        if (tile == null || !tile.canPlayerAccess(player))
            return false;

        if (tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null))
        {
            ItemStack heldItem = player.getHeldItem(hand);
            IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);

            if (FluidHelper.isFluidHandler(heldItem))
            {
                FluidHelper.drainItemToHandler(heldItem, handler, player, hand);
                return true;
            }
        }

        return false;
    }

    /* RENDERING METHODS */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
    {
        return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return ModelBakery.handleExtendedState((IExtendedBlockState) super.getExtendedState(state, world, pos), world, pos);
    }

    /* IBakeryProvider */
    @Override
    public IBakery getBakery()
    {
        return BakeryVirtualMachine.INSTANCE;
    }

    /* IWorldBlockTextureProvider */
    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(EnumFacing side, ItemStack stack)
    {
        if (side == EnumFacing.DOWN)
            return TETextures.MACHINE_BOTTOM;
        else if (side == EnumFacing.UP)
            return TETextures.MACHINE_TOP;

        return side != EnumFacing.NORTH ? TETextures.MACHINE_SIDE : VMTextures.MACHINE_FACE[stack.getMetadata() % Type.values().length];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(EnumFacing side, IBlockState state, BlockRenderLayer layer, IBlockAccess world, BlockPos pos)
    {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileVirtualMachine)
        {
            TileVirtualMachine tile = (TileVirtualMachine) tileEntity;
            TextureAtlasSprite texture = tile.getTexture(side.ordinal(), layer == BlockRenderLayer.SOLID ? 0 : 1);

            for (int i = 0; i < TETextures.MACHINE_ACTIVE.length; i++)
                if (texture == TETextures.MACHINE_ACTIVE[i])
                    return VMTextures.MACHINE_ACTIVE[i];

            for (int i = 0; i < TETextures.MACHINE_FACE.length; i++)
                if (texture == TETextures.MACHINE_FACE[i])
                    return VMTextures.MACHINE_FACE[i];

            return texture;
        }

        return TextureUtils.getMissingSprite();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels()
    {
        StateMap.Builder stateMap = new StateMap.Builder();
        stateMap.ignore(VARIANT);
        ModelLoader.setCustomStateMapper(this, stateMap.build());

        ModelResourceLocation location = new ModelResourceLocation(getRegistryName(), "normal");
        for (Type type : Type.values())
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), type.getMetadata(), location);

        ModelRegistryHelper.register(location, new CCBakeryModel());

        ModelBakery.registerBlockKeyGenerator(this, state -> {
            TileVirtualMachine tile = state.getValue(VMConstants.TILE_VIRTUAL_MACHINE);
            StringBuilder builder = new StringBuilder(state.getBlock().getRegistryName() + "|" + state.getBlock().getMetaFromState(state));
            builder.append(",creative=").append(tile.isCreative);
            builder.append(",level=").append(tile.getLevel());
            builder.append(",facing=").append(tile.getFacing());
            builder.append(",active=").append(tile.isActive);
            builder.append(",side_config={");

            for (int i : tile.sideCache)
                builder.append(",").append(i);

            builder.append("}");

            if (tile.hasFluidUnderlay() && tile.isActive)
            {
                FluidStack stack = tile.getRenderFluid();
                builder.append(",fluid=").append(stack != null ? FluidHelper.getFluidHash(stack) : tile.getTexture(tile.getFacing(), 0).getIconName());
            }

            return builder.toString();
        });

        ModelBakery.registerItemKeyGenerator(itemBlock, stack -> ModelBakery.defaultItemKeyGenerator.generateKey(stack) + ",creative=" + itemBlock.isCreative(stack) + ",level=" + itemBlock.getLevel(stack));
    }

    @Override
    public boolean preInit()
    {
        setRegistryName("virtual_machine");
        ForgeRegistries.BLOCKS.register(this);

        itemBlock = new ItemBlockVirtualMachine(this);
        itemBlock.setRegistryName(getRegistryName());
        ForgeRegistries.ITEMS.register(itemBlock);

        TileVirtualMachine.config();

        TileFarm.init();
        VirtualFarm.init();
        TileFishery.init();
        VirtualFishery.init();
        TileDarkRoom.init();
        TileAnimalFarm.init();
        TileMobSpawner.init();
        TileMobFarm.init();

        VirtualMachines.proxy.addIModelRegister(this);

        return true;
    }

    @Override
    public boolean initialize()
    {
        virtual_farm = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.FARM.getMetadata()));
        virtual_fishery = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.FISHERY.getMetadata()));
        virtual_dark_room = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.DARK_ROOM.getMetadata()));
        virtual_animal_farm = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.ANIMAL_FARM.getMetadata()));
        virtual_mob_spawner = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.MOB_SPAWNER.getMetadata()));
        virtual_mob_farm = itemBlock.setDefaultTag(new ItemStack(this, 1, Type.MOB_FARM.getMetadata()));

        addRecipes();
        addUpgradeRecipes();
        addClassicRecipes();

        return true;
    }

    private void addRecipes()
    {
        if (enable[Type.FARM.getMetadata()])
        {
            addShapedRecipe(virtual_farm,
                    "PAP",
                    "FIF",
                    "EVE",
                    'P', "plateLumium",
                    'A', ItemAugment.machineInsolatorTree,
                    'F', ItemFertilizer.fertilizerFlux,
                    'I', BlockMachine.machineInsolator,
                    'E', "gearEnderium",
                    'V', com.arcaratus.virtualmachines.item.ItemMaterial.virtual_machine_core_flux
            );
        }

        if (enable[Type.FISHERY.getMetadata()])
        {
            addShapedRecipe(virtual_fishery,
                    "PAP",
                    "FIF",
                    "EVE",
                    'P', "plateInvar",
                    'A', "blockGlassHardened",
                    'F', ItemBait.baitFlux,
                    'I', BlockDevice.deviceFisher,
                    'E', "gearSignalum",
                    'V', com.arcaratus.virtualmachines.item.ItemMaterial.virtual_machine_core_flux
            );
        }

        if (enable[Type.DARK_ROOM.getMetadata()])
        {
            addShapedRecipe(virtual_dark_room,
                    "PAP",
                    "FIF",
                    "EVE",
                    'P', "plateConstantan",
                    'A', ItemAugment.machineCentrifugeMobs,
                    'F', "blockGlassBlack",
                    'I', BlockMachine.machinePulverizer,
                    'E', "gearInvar",
                    'V', com.arcaratus.virtualmachines.item.ItemMaterial.virtual_machine_core_flux
            );
        }

        if (enable[Type.ANIMAL_FARM.getMetadata()])
        {
            addShapedRecipe(virtual_animal_farm,
                    "PAP",
                    "FIF",
                    "EVE",
                    'P', "plateSilver",
                    'A', ItemAugment.machineCentrifugeMobs,
                    'F', Blocks.HAY_BLOCK,
                    'I', BlockMachine.machinePulverizer,
                    'E', "gearTin",
                    'V', com.arcaratus.virtualmachines.item.ItemMaterial.virtual_machine_core_flux
            );
        }

        if (enable[Type.MOB_SPAWNER.getMetadata()])
        {
            addShapedRecipe(virtual_mob_spawner,
                    "PAP",
                    "FIF",
                    "EVE",
                    'P', "plateElectrum",
                    'A', BlockVirtualMachine.virtual_dark_room,
                    'F', Blocks.BEACON,
                    'I', BlockVirtualMachine.virtual_animal_farm,
                    'E', "gearLumium",
                    'V', com.arcaratus.virtualmachines.item.ItemMaterial.virtual_machine_core_flux
            );
        }

        if (enable[Type.MOB_FARM.getMetadata()])
        {
            addShapedRecipe(virtual_mob_farm,
                    "SBS",
                    "SFS",
                    "STS",
                    'S', virtual_mob_spawner,
                    'B', Blocks.BEACON,
                    'F', Items.DRAGON_BREATH,
                    'T', Items.TOTEM_OF_UNDYING
            );
        }
    }

    private void addUpgradeRecipes()
    {
        if (!BlockMachine.enableUpgradeKitCrafting)
            return;

        for (int i = 0; i < Type.values().length; i++)
        {
            if (enable[i])
            {
                ItemStack[] block = new ItemStack[5];

                for (int j = 0; j < 5; j++)
                    block[j] = itemBlock.setDefaultTag(new ItemStack(this, 1, i), j);

                for (int j = 0; j < 4; j++)
                    addShapelessUpgradeKitRecipe(block[j + 1], block[j], ItemUpgrade.upgradeIncremental[j]);

                for (int j = 1; j < 4; j++)
                    for (int k = 0; k <= j; k++)
                        addShapelessUpgradeKitRecipe(block[j + 1], block[k], ItemUpgrade.upgradeFull[j]);
            }
        }
    }

    private void addClassicRecipes()
    {
        if (!BlockMachine.enableClassicRecipes)
            return;

        for (int i = 0; i < Type.values().length; i++)
        {
            if (enable[i])
            {
                ItemStack[] machine = new ItemStack[5];

                for (int j = 0; j < 5; j++)
                    machine[j] = itemBlock.setDefaultTag(new ItemStack(this, 1, i), j);

                addShapedUpgradeRecipe(machine[1],
                        " I ",
                        "ICI",
                        " I ",
                        'C', machine[0],
                        'I', "ingotInvar"
                );
                addShapedUpgradeRecipe(machine[2], "YIY",
                        "ICI",
                        "YIY",
                        'C', machine[1],
                        'I', "ingotElectrum",
                        'Y', "blockGlassHardened"
                );
                addShapedUpgradeRecipe(machine[3], " I ",
                        "ICI",
                        " I ",
                        'C', machine[2],
                        'I', "ingotSignalum"
                );
                addShapedUpgradeRecipe(machine[4], " I ",
                        "ICI",
                        " I ",
                        'C', machine[3],
                        'I', "ingotEnderium"
                );
            }
        }
    }

    public enum Type implements IStringSerializable
    {
        FARM(0, "farm"),
        FISHERY(1, "fishery"),
        DARK_ROOM(2, "dark_room"),
        ANIMAL_FARM(3, "animal_farm"),
        MOB_SPAWNER(4, "mob_spawner"),
        MOB_FARM(5, "mob_farm"),
        ;

        private final int metadata;
        private final String name;

        Type(int metadata, String name)
        {
            this.metadata = metadata;
            this.name = name;
        }

        public int getMetadata()
        {
            return this.metadata;
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }
}
