package com.arcaratus.virtualmachines.utils;

import cofh.core.util.helpers.ItemHelper;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils
{
    public static double[] emptyDoubleArray(int size)
    {
        double[] array = new double[size];
        Arrays.fill(array, 0);
        return array;
    }

    // Too many streams
    public static double[] joinArrays(double[]... arrays)
    {
        int size = Arrays.stream(arrays).mapToInt(d -> d.length).sum();
        List<Double> list = new ArrayList<>(size);
        Arrays.stream(arrays).forEach(d -> Arrays.stream(d).forEach(list::add));
        return list.stream().mapToDouble(d -> d).toArray();
    }

    public static double[] applyToArray(double[] array, DoubleUnaryOperator operator)
    {
        return Arrays.stream(array).map(operator).toArray();
    }

    public static <T> List<T> arrayToListWithRange(T[] array, int startInclusive, int endExclusive)
    {
        return Arrays.stream(Arrays.copyOfRange(array, startInclusive, endExclusive)).collect(Collectors.toList());
    }

    public static boolean checkItemStackRange(ItemStack[] inventory, int startInclusive, int endExclusive, Predicate<ItemStack> checkeroo)
    {
        return Arrays.stream(Arrays.copyOfRange(inventory, startInclusive, endExclusive)).allMatch(checkeroo);
    }

    public static boolean compareToOreName(ItemStack stack, String oreName)
    {
        if (!isExistingOreName(oreName))
            return false;
//        ItemStack comp = copyStackWithAmount(stack, 1);
        List<ItemStack> s = OreDictionary.getOres(oreName);
        for (ItemStack st : s)
            if (OreDictionary.itemMatches(st, stack, false))
                return true;

        return false;
    }

    public static boolean isExistingOreName(String name)
    {
        if (!OreDictionary.doesOreNameExist(name))
            return false;
        else
            return !OreDictionary.getOres(name).isEmpty();
    }

    public static ItemStack getPreferredOreStack(String oreName)
    {
        return isExistingOreName(oreName) ? getPreferredStackByMod(OreDictionary.getOres(oreName)) : ItemStack.EMPTY;
    }

    public static ItemStack getPreferredStackByMod(List<ItemStack> list)
    {
        ItemStack preferredStack = ItemStack.EMPTY;
        for (ItemStack stack : list)
            if (!stack.isEmpty())
                return stack.copy();

        return preferredStack;
    }

    public static boolean checkTool(ItemStack itemStack, String... names)
    {
        return Arrays.stream(names).anyMatch(s -> checkTool(itemStack, s));
    }

    public static boolean checkTool(ItemStack itemStack, String name)
    {
        switch (name)
        {
            case "axe":
                return itemStack.getItem() instanceof ItemAxe || itemStack.getItem().getToolClasses(itemStack).contains("axe");

            case "pickaxe":
                return itemStack.getItem() instanceof ItemPickaxe || itemStack.getItem().getToolClasses(itemStack).contains("pickaxe");

            case "fishing_rod":
                return itemStack.getItem() instanceof ItemFishingRod;

            case "hoe":
                return itemStack.getItem() instanceof ItemHoe;

            case "shears":
                return itemStack.getItem() instanceof ItemShears;

            case "shovel":
                return itemStack.getItem() instanceof ItemSpade || itemStack.getItem().getToolClasses(itemStack).contains("shovel");

            case "sickle":
                return itemStack.getItem().getToolClasses(itemStack).contains("sickle");

            case "sword":
                return itemStack.getItem() instanceof ItemSword;

            default:
                return false;
        }
    }

    public static boolean isSlotInRange(int slot, int startInclusive, int endExclusive)
    {
        return slot >= startInclusive && slot < endExclusive;
    }

    /**
     * Returns true if can fit the outputs into the inventory
     */
    public static boolean canFitOutputs(IInventory inventory, List<ItemStack> outputs, int start, int end)
    {
        List<Integer> skipSlots = new ArrayList<>();
        first:
        for (ItemStack stack : outputs)
        {
            for (int i = start; i < end; i++)
            {
                if (skipSlots.contains(i))
                {
                    continue;
                }

                if (inventory.getStackInSlot(i).isEmpty())
                {
                    skipSlots.add(i);
                    continue first;
                }
                else if (inventory.getStackInSlot(i).isItemEqual(stack))
                {
                    if (inventory.getStackInSlot(i).getCount() + stack.getCount() <= 64)
                        continue first;
                    else if (i == end - 1)
                        return false;
                }
                else if (i == end - 1)
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static void distributeOutput(IInventory inventory, ItemStack output, int start, int end)
    {
        if (!output.isEmpty() && output.getCount() > 0)
        {
            for (int slot = start; slot < end; slot++)
            {
                ItemStack s = inventory.getStackInSlot(slot);
                if (s.isEmpty())
                {
                    inventory.setInventorySlotContents(slot, ItemHelper.cloneStack(output));
                    break;
                }
                else if (s.isItemEqual(output) && s.getCount() < 64)
                {
                    int count = output.getCount();
                    int grow = Math.min(count, 64 - s.getCount());
                    s.grow(grow);
                    inventory.setInventorySlotContents(slot, s);
                    output.setCount(count - grow);
                    if (output.getCount() <= 0)
                        break;
                }
            }
        }
    }

    public static ItemStack damageItem(ItemStack itemStack, int amount, Random random)
    {
        ItemStack result;

        if (itemStack.attemptDamageItem(amount, random, null))
            result = ItemStack.EMPTY;
        else
            result = itemStack.copy();

        return result;
    }

    public static boolean[] buildFilterArray(int size, int... ranges)
    {
        if (ranges.length % 2 == 1)
            throw new IllegalArgumentException("Unclosed range!");

        boolean[] filterArray = new boolean[size];

        for (int i = 0; i < ranges.length; i += 2)
            Arrays.fill(filterArray, ranges[i], ranges[i + 1], true);

        return filterArray;
    }
}
