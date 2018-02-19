package com.arcaratus.virtualmachines.virtual;

import cofh.thermalfoundation.item.ItemFertilizer;
import com.arcaratus.virtualmachines.init.VMConstants;
import com.arcaratus.virtualmachines.utils.*;
import com.google.common.collect.*;
import gnu.trove.map.hash.THashMap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;

public class VirtualFarm implements IVirtualMachine
{
    private static HashSet<IPlantHandler> plantHandlers = new HashSet<>();
    private static HashSet<IItemFertilizerHandler> itemFertilizers = new HashSet<>();
    private static HashSet<Predicate<ItemStack>> fertilizers = new HashSet<>(); // For efficiency purposes

    public static float solidFertilizerModifier = 1;

    public static void registerHandler(IPlantHandler handler)
    {
        plantHandlers.add(handler);
    }

    public static IPlantHandler getHandler(ItemStack seed)
    {
        if (seed.isEmpty())
            return null;

        for (IPlantHandler handler : plantHandlers)
            if (handler.isValid(seed))
                return handler;

        return null;
    }

    public static void registerItemFertilizer(IItemFertilizerHandler handler)
    {
        itemFertilizers.add(handler);
        fertilizers.add(handler::isValid); // Sketchy but mb works
    }

    public static IItemFertilizerHandler getItemFertilizerHandler(ItemStack itemStack)
    {
        if (itemStack.isEmpty())
            return null;

        for (IItemFertilizerHandler handler : itemFertilizers)
            if (handler.isValid(itemStack))
                return handler;

        return null;
    }

    public static boolean isFertilizer(ItemStack itemStack)
    {
        return fertilizers.stream().anyMatch(p -> p.test(itemStack));
    }

    public static void registerBasicItemFertilizer(final ItemStack stack, final float growthMultiplier)
    {
        registerItemFertilizer(new IItemFertilizerHandler()
        {
            @Override
            public boolean isValid(ItemStack fertilizer)
            {
                return OreDictionary.itemMatches(stack, fertilizer, false);
            }

            @Override
            public float getGrowthMultiplier(ItemStack fertilizer)
            {
                return solidFertilizerModifier * growthMultiplier;
            }
        });
    }

    public static void init()
    {
        registerHandler(defaultHandler);
        registerHandler(saplingHandler);

        // Mostly verified by trials
        final double[] standardCropDrops = new double[] { 0.079, 0.315, 0.42, 0.186 };
        final double[] simpleDrop = new double[] { 0, 1 };
        final double[] doubleDrop = new double[] { 0, 0, 1 };
        int defaultWater, defaultEnergy;

        // Crops
        {
            defaultWater = 200;
            defaultEnergy = 2400;

            defaultHandler.register(new ItemStack(Items.WHEAT_SEEDS), ezPairs(new double[][] { standardCropDrops, simpleDrop }, new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.WHEAT)), defaultWater, defaultEnergy);
            defaultHandler.register(new ItemStack(Items.POTATO), ezPairs(new double[][] { standardCropDrops, { 0.98, 0.02 } }, new ItemStack(Items.POTATO), new ItemStack(Items.POISONOUS_POTATO)), defaultWater, defaultEnergy);
            defaultHandler.register(new ItemStack(Items.CARROT), ezPairs(new double[][] { standardCropDrops }, new ItemStack(Items.CARROT)), defaultWater, defaultEnergy);
            defaultHandler.register(new ItemStack(Items.BEETROOT_SEEDS), ezPairs(new double[][] { standardCropDrops, simpleDrop }, new ItemStack(Items.BEETROOT_SEEDS), new ItemStack(Items.BEETROOT)), defaultWater, defaultEnergy);

            defaultHandler.register(new ItemStack(Items.NETHER_WART), ezPairs(new double[][] { { 0, 0, 0.3333, 0.3334, 0.3333 } }, new ItemStack(Items.NETHER_WART)), 0, 3000, false);

            defaultHandler.register(new ItemStack(Blocks.RED_MUSHROOM), ezPairs(new double[][] { ezDArray(3, 0.001, 0.0026, 0.0055, 0.0105, 0.0177, 0.0273, 0.0389, 0.052, 0.0640, 0.0749, 0.083, 0.087, 0.087, 0.083, 0.0756, 0.0666, 0.056, 0.0455, 0.0356, 0.027, 0.02, 0.015, 0.0096, 0.0064, 0.0041, 0.0026, 0.0016) }, new ItemStack(Blocks.RED_MUSHROOM)), 500, 6000, false);
            defaultHandler.register(new ItemStack(Blocks.BROWN_MUSHROOM), ezPairs(new double[][] { ezDArray(3, 0.001, 0.0026, 0.0055, 0.0105, 0.0177, 0.0273, 0.0389, 0.052, 0.0640, 0.0749, 0.083, 0.087, 0.087, 0.083, 0.0756, 0.0666, 0.056, 0.0455, 0.0356, 0.027, 0.02, 0.015, 0.0096, 0.0064, 0.0041, 0.0026, 0.0016) }, new ItemStack(Blocks.BROWN_MUSHROOM)), 500, 6000, false);
        }

        // Stems
        {
            defaultWater = 400;
            defaultEnergy = 3600;

            defaultHandler.register(new ItemStack(Items.PUMPKIN_SEEDS), ezPairs(new double[][] { simpleDrop }, new ItemStack(Blocks.PUMPKIN)), defaultWater, defaultEnergy);
            defaultHandler.register(new ItemStack(Items.MELON_SEEDS), ezPairs(new double[][] { simpleDrop }, new ItemStack(Blocks.MELON_BLOCK)), defaultWater, defaultEnergy);
        }

        // Stacking
        {
            defaultWater = 400;
            defaultEnergy = 3000;

            defaultHandler.register(new ItemStack(Items.REEDS), ezPairs(new double[][] { doubleDrop }, new ItemStack(Items.REEDS)), defaultWater, defaultEnergy, false);
            defaultHandler.register(new ItemStack(Blocks.CACTUS), ezPairs(new double[][] { doubleDrop }, new ItemStack(Blocks.CACTUS)), defaultWater, defaultEnergy, false);
            defaultHandler.register(new ItemStack(Blocks.CHORUS_FLOWER), ezPairs(new double[][] { simpleDrop }, new ItemStack(Items.CHORUS_FRUIT)), 0, defaultEnergy, false);
        }

        // Saplings
        {
            defaultWater = 800;
            defaultEnergy = 6000;

            final double[] saplingDrops = new double[] { 0.0629, 0.1798, 0.2483, 0.2265, 0.1517, 0.08, 0.0345, 0.0127 };
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(4, 0.3333, 0.3333, 0.3334), saplingDrops }, new ItemStack(Blocks.LOG), new ItemStack(Blocks.SAPLING)), ezPairs(new double[][] { ezDArray(47, 0.0002, 0.003, 0.0162, 0.0539, 0.1208, 0.1936, 0.2254, 0.1933, 0.1207, 0.0537, 0.0161, 0.0029, 0.0002) }, new ItemStack(Blocks.LEAVES))), defaultWater, defaultEnergy);
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING, 1, 1), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(4, 0.0833, 0.167, 0.25, 0.25, 0.1665, 0.0832), new double[] { 0, 0.25, 0.25, 0.125, 0.125, 0.2189, 0.0311 } }, new ItemStack(Blocks.LOG, 1, 1), new ItemStack(Blocks.SAPLING, 1, 1)), ezPairs(new double[][] { Utils.joinArrays(ezDArray(33, 0.09375), ezDArray(3, 0.09375, 0.20833), ezDArray(19, 0.20834, 0.20833), ezDArray(3, 0.09375, 0.09375)) }, new ItemStack(Blocks.LEAVES, 1, 1))), defaultWater, defaultEnergy);
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING, 1, 2), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(5, 0.3334, 0.3333, 0.3333), saplingDrops }, new ItemStack(Blocks.LOG, 1, 2), new ItemStack(Blocks.SAPLING, 1, 2)), ezPairs(new double[][] { ezDArray(52, 0.0002, 0.003, 0.0162, 0.0537, 0.121, 0.1933, 0.2254, 0.1933, 0.1207, 0.0538, 0.0162, 0.003, 0.0002) }, new ItemStack(Blocks.LEAVES, 1, 2))), defaultWater, defaultEnergy);
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING, 1, 3), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(4, 0.0475, 0.095, 0.143, 0.143, 0.143, 0.143, 0.143, 0.095, 0.0475), new double[] { 0.051, 0.157, 0.2335, 0.2293, 0.166, 0.0945, 0.044, 0.0173, 0.0058, 0.0016 } }, new ItemStack(Blocks.LOG, 1, 3), new ItemStack(Blocks.SAPLING, 1, 3)), ezPairs(new double[][] { ezDArray(52, 0.0002, 0.003, 0.0162, 0.0537, 0.121, 0.1933, 0.2254, 0.1933, 0.1207, 0.0538, 0.0162, 0.003, 0.0002) }, new ItemStack(Blocks.LEAVES, 1, 3))), defaultWater, defaultEnergy);
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING, 1, 4), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(5, 0.0416, 0.0902, 0.1667, 0.2152, 0.219, 0.163, 0.08, 0.0243), new double[] { 0.0213, 0.0791, 0.148, 0.19, 0.19, 0.1518, 0.1042, 0.0612, 0.0319, 0.0145, 0.006, 0.002 } }, new ItemStack(Blocks.LOG2), new ItemStack(Blocks.SAPLING, 1, 4)), ezPairs(new double[][] { Utils.joinArrays(ezDArray(58, 0.2706), ezDArray(29, 0.7294)) }, new ItemStack(Blocks.LEAVES2))), defaultWater, defaultEnergy);
            saplingHandler.registerSapling(new ItemStack(Blocks.SAPLING, 1, 5), ezPredicates(s -> Utils.checkTool(s, "axe"), s -> Utils.checkTool(s, "shears")), ezLists(ezPairs(new double[][] { ezDArray(6, 0.0064, 0.0395, 0.1132, 0.2, 0.24, 0.204, 0.1234, 0.0536, 0.0164, 0.0035), new double[] { 0.0592, 0.161, 0.2255, 0.2155, 0.1582, 0.0958, 0.0493, 0.0222, 0.0089, 0.0033, 0.0011 } }, new ItemStack(Blocks.LOG2, 1, 1), new ItemStack(Blocks.SAPLING, 1, 5)), ezPairs(new double[][] { Utils.joinArrays(ezDArray(28, 0.003, 0.003), ezDArray(6, 0.0232, 0.0232), ezDArray(5, 0.064, 0.064), ezDArray(6, 0.106, 0.106), ezDArray(5, 0.12, 0.12), ezDArray(6, 0.0954, 0.0954), ezDArray(5, 0.0556), ezDArray(6, 0.0237, 0.0237), ezDArray(5, 0.0075, 0.0075), ezDArray(6, 0.0016, 0.0016)) }, new ItemStack(Blocks.LEAVES2, 1, 1))), defaultWater, defaultEnergy);
        }

        registerItemFertilizer(new IItemFertilizerHandler()
        {
            final ItemStack bonemeal = new ItemStack(Items.DYE, 1, 15);

            @Override
            public boolean isValid(ItemStack fertilizer)
            {
                return !fertilizer.isEmpty() && OreDictionary.itemMatches(bonemeal, fertilizer, true);
            }

            @Override
            public float getGrowthMultiplier(ItemStack fertilizer)
            {
                return solidFertilizerModifier * 1.25F;
            }
        });

        registerBasicItemFertilizer(ItemFertilizer.fertilizerBasic, 1.25F);
        registerBasicItemFertilizer(ItemFertilizer.fertilizerRich, 1.5F);
        registerBasicItemFertilizer(ItemFertilizer.fertilizerFlux, 2F);
    }

    public static List<Pair<Distribution, ItemStack>> ezPairs(double[][] probabilities, ItemStack... outputs)
    {
        List<Pair<Distribution, ItemStack>> ezList = new ArrayList<>();

        for (int i = 0; i < probabilities.length; i++)
            ezList.add(Pair.of(new Distribution(probabilities[i]), outputs[i]));

        return ezList;
    }

    @SafeVarargs
    public static List<List<Pair<Distribution, ItemStack>>> ezLists(List<Pair<Distribution, ItemStack>>... pairs)
    {
        return Arrays.asList(pairs);
    }

    @SafeVarargs
    public static List<Predicate<ItemStack>> ezPredicates(Predicate<ItemStack>... predicates)
    {
        return Arrays.asList(predicates);
    }

    public static double[] ezDArray(int emptyArraySize, double... probValues)
    {
        return ArrayUtils.addAll(Utils.emptyDoubleArray(emptyArraySize), probValues);
    }

    private static HashMap<ComparableItemStack, Boolean> seedSoilMap = new HashMap<>();
    private static HashMap<ComparableItemStack, List<Pair<Distribution, ItemStack>>> seedOutputMap = new HashMap<>();
    private static Table<ComparableItemStack, Predicate<ItemStack>, List<Pair<Distribution, ItemStack>>> saplingOutputTable = HashBasedTable.create();

    public interface IPlantHandler
    {
        boolean isValid(ItemStack seed);

        boolean requiresDirt(ItemStack seed);

        List<Pair<Distribution, ItemStack>> getOutput(ItemStack seed, List<ItemStack> tools);

        String getName();

        int waterRequired(ItemStack seed);

        int energyRequired(ItemStack seed);

        default List<ItemStack> getTools()
        {
            return Lists.newArrayList();
        }

        default void clearTools() {}
    }

    public abstract static class DefaultPlantHandler implements IPlantHandler
    {
        protected abstract Map<ComparableItemStack, Pair<Integer, Integer>> getSeedMap();

        public List<ItemStack> usableTools = new ArrayList<>();

        @Override
        public boolean isValid(ItemStack seed)
        {
            return !seed.isEmpty() && (getSeedMap().keySet().contains(new ComparableItemStack(seed, false)) || getSeedMap().keySet().stream().anyMatch(s -> s.equals(new ComparableItemStack(seed))));
        }

        @Override
        public boolean requiresDirt(ItemStack seed)
        {
            return seedSoilMap.get(new ComparableItemStack(seed, false));
        }

        @Override
        public List<Pair<Distribution, ItemStack>> getOutput(ItemStack seed, List<ItemStack> tools)
        {
            for (ItemStack t : tools)
                if (Utils.checkTool(t, "hoe", "sickle"))
                    usableTools.add(t);

            if (seedOutputMap.get(new ComparableItemStack(seed, false)) == null)
                return seedOutputMap.values().stream().filter(l -> l.stream().anyMatch(p -> p.getRight().isItemEqual(seed))).findFirst().get();

            return seedOutputMap.getOrDefault(new ComparableItemStack(seed, false), Lists.newArrayList());
        }

        @Override
        public int waterRequired(ItemStack seed)
        {
            return getSeedMap().getOrDefault(new ComparableItemStack(seed, false), getSeedMap().entrySet().stream().filter(e -> e.getKey().equals(new ComparableItemStack(seed))).findFirst().get().getValue()).getLeft();
        }

        @Override
        public int energyRequired(ItemStack seed)
        {
            return getSeedMap().getOrDefault(new ComparableItemStack(seed, false), getSeedMap().entrySet().stream().filter(e -> e.getKey().equals(new ComparableItemStack(seed))).findFirst().get().getValue()).getRight();
        }

        @Override
        public List<ItemStack> getTools()
        {
            return usableTools;
        }

        @Override
        public void clearTools()
        {
            usableTools.clear();
        }

        public void register(ItemStack seed, List<Pair<Distribution, ItemStack>> output, int waterRequired, int energyRequired)
        {
            register(seed, output, waterRequired, energyRequired, true);
        }

        public void register(ItemStack seed, List<Pair<Distribution, ItemStack>> output, int waterRequired, int energyRequired, boolean requiresDirt)
        {
            ComparableItemStack comp = new ComparableItemStack(seed, false);
            getSeedMap().put(comp, Pair.of(waterRequired, energyRequired));
            seedSoilMap.put(comp, requiresDirt);
            seedOutputMap.put(comp, output);
        }

        public void registerSapling(ItemStack sapling, List<Predicate<ItemStack>> tools, List<List<Pair<Distribution, ItemStack>>> outputs, int waterRequired, int energyRequired)
        {
            ComparableItemStack comp = new ComparableItemStack(sapling, false);
            getSeedMap().put(comp, Pair.of(waterRequired, energyRequired));
            seedSoilMap.put(comp, true);
            for (int i = 0; i < tools.size(); i++)
                saplingOutputTable.put(comp, tools.get(i), outputs.get(i));
        }
    }

    public static DefaultPlantHandler defaultHandler = new DefaultPlantHandler()
    {
        private Map<ComparableItemStack, Pair<Integer, Integer>> validSeeds = new THashMap<>();

        @Override
        protected Map<ComparableItemStack, Pair<Integer, Integer>> getSeedMap()
        {
            return validSeeds;
        }

        @Override
        public String getName()
        {
            return "defaultHandler";
        }
    };

    public static DefaultPlantHandler saplingHandler = new DefaultPlantHandler()
    {
        private Map<ComparableItemStack, Pair<Integer, Integer>> validSaplings = new THashMap<>();

        @Override
        protected Map<ComparableItemStack, Pair<Integer, Integer>> getSeedMap()
        {
            return validSaplings;
        }

        @Override
        public List<Pair<Distribution, ItemStack>> getOutput(ItemStack seed, List<ItemStack> tools)
        {
            if (!isValid(seed))
                return super.getOutput(seed, tools);

            List<Predicate<ItemStack>> p = new ArrayList<>();
            ComparableItemStack comp = new ComparableItemStack(seed, false);

            first:
            for (Predicate<ItemStack> pp : saplingOutputTable.row(comp).keySet())
            {
                for (ItemStack t : tools)
                {
                    if (pp.test(t))
                    {
                        if (saplingOutputTable.contains(comp, pp))
                        {
                            p.add(pp);
                            if (!usableTools.contains(t))
                                usableTools.add(t);
                            continue first;
                        }
                    }
                }
            }

            if (p.isEmpty())
                return Lists.newArrayList();

            List<Pair<Distribution, ItemStack>> outputs = new ArrayList<>();

            for (Predicate<ItemStack> pp : p)
                outputs.addAll(saplingOutputTable.get(comp, pp));

            return outputs;
        }

        @Override
        public String getName()
        {
            return "saplingHandler";
        }
    };

    public interface IItemFertilizerHandler
    {
        boolean isValid(ItemStack fertilizer);

        float getGrowthMultiplier(ItemStack fertilizer);
    }

    public static IPlantHandler getHandlerFromName(String name)
    {
        for (IPlantHandler handler : plantHandlers)
            if (handler.getName().equals(name))
                return handler;

        return null;
    }

    private List<IPlantHandler> currentPlantHandlers;
    private List<ItemStack> currentSeeds = new ArrayList<>();
    private List<List<ItemStack>> currentTools = new ArrayList<>();

    public VirtualFarm()
    {
        List<ItemStack> emptyList = new ArrayList<>();
        for (int j = 0; j < 4; j++)
            emptyList.add(ItemStack.EMPTY);

        for (int i = 0; i < 9; i++)
        {
            currentSeeds.add(i, ItemStack.EMPTY);
            currentTools.add(i, emptyList);
        }
    }

    public List<ItemStack> getCurrentSeeds()
    {
        return currentSeeds;
    }

    public void setCurrentSeeds(List<ItemStack> currentSeeds)
    {
        this.currentSeeds = currentSeeds;
    }

    public List<List<ItemStack>> getCurrentTools()
    {
        return currentTools;
    }

    public void setCurrentTools(List<List<ItemStack>> currentTools)
    {
        this.currentTools = currentTools;
    }

    public List<IPlantHandler> getCurrentPlantHandlers()
    {
        return currentPlantHandlers;
    }

    public List<IPlantHandler> getPlantHandlers(List<ItemStack> inventory)
    {
        if (currentPlantHandlers == null || inventory.size() <= 0)
        {
            currentPlantHandlers = new ArrayList<>(9);
            for (int i = 0; i < 9; i++)
                currentPlantHandlers.add(i, getHandler(inventory.get(i)));
        }
        else
        {
            for (int i = 0; i < 9; i++)
                if (currentPlantHandlers.get(i) == null || currentPlantHandlers.get(i).isValid(inventory.get(i)))
                    currentPlantHandlers.set(i, getHandler(inventory.get(i)));
        }

        return currentPlantHandlers;
    }

    public void clear()
    {
        currentTools.clear();
        List<ItemStack> emptyList = new ArrayList<>();
        for (int j = 0; j < 4; j++)
            emptyList.add(ItemStack.EMPTY);

        for (int i = 0; i < 9; i++)
        {
            currentSeeds.set(i, ItemStack.EMPTY);
            currentPlantHandlers.set(i, null);

            currentTools.add(i, emptyList);
        }
    }

    @Override
    public IVirtualMachine readFromNBT(NBTTagCompound tag)
    {
        NBTTagList seedList = tag.getTagList(VMConstants.NBT_CURRENT_SEEDS, 10);
        NBTTagList omegaLUL = tag.getTagList(VMConstants.NBT_CURRENT_TOOLS, 9);
        for (int i = 0; i < 9; i++)
        {
            if (currentPlantHandlers == null)
            {
                currentPlantHandlers = new ArrayList<>(9);
                for (int j = 0; j < 9; j++)
                    currentPlantHandlers.add(j, getHandlerFromName(tag.getTagList(VMConstants.NBT_PLANT_HANDLERS, 8).getStringTagAt(j)));
            }
            else
            {
                for (int j = 0; j < 9; j++)
                    if (currentPlantHandlers.get(i) == null)
                        currentPlantHandlers.set(i, getHandlerFromName(tag.getTagList(VMConstants.NBT_PLANT_HANDLERS, 8).getStringTagAt(j)));
            }

            currentSeeds.set(i, new ItemStack(seedList.getCompoundTagAt(i)));

            NBTTagList toolsList = (NBTTagList) omegaLUL.get(i);
            for (int j = 0; j < 4; j++)
                currentTools.get(i).set(j, new ItemStack(toolsList.getCompoundTagAt(j)));
        }

        return this;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        NBTTagList list = new NBTTagList();
        NBTTagList seedList = new NBTTagList();
        NBTTagList omegaLul = new NBTTagList();

        for (int i = 0; i < 9; i++)
        {
            if (currentPlantHandlers == null || currentPlantHandlers.get(i) == null)
                list.appendTag(new NBTTagString());
            else
                list.appendTag(new NBTTagString(currentPlantHandlers.get(i).getName()));

            NBTTagCompound nbt = new NBTTagCompound();
            currentSeeds.get(i).writeToNBT(nbt);
            seedList.appendTag(nbt);

            NBTTagList toolsList = new NBTTagList();
            for (int j = 0; j < 4; j++)
            {
                nbt = new NBTTagCompound();
                currentTools.get(i).get(j).writeToNBT(nbt);
                toolsList.appendTag(nbt);
            }

            omegaLul.appendTag(toolsList);
        }

        tag.setTag(VMConstants.NBT_PLANT_HANDLERS, list);
        tag.setTag(VMConstants.NBT_CURRENT_SEEDS, seedList);
        tag.setTag(VMConstants.NBT_CURRENT_TOOLS, omegaLul);

        return tag;
    }
}
