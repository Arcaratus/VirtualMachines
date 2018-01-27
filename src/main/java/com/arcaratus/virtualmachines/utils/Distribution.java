package com.arcaratus.virtualmachines.utils;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Distribution
{
    private static Random random = new Random();

    private double[] probabilities;
    private int[] outputs;

    public Distribution(double[] probabilities)
    {
        this(probabilities, IntStream.range(0, probabilities.length).toArray());
    }

    public Distribution(double[] probabilities, int[] outputs)
    {
        if (probabilities.length <= 0 || outputs.length <= 0 || probabilities.length != outputs.length)
            throw new RuntimeException("u dimwit messed up with the probabilities or outputs");

//        if (Arrays.stream(probabilities).sum() != 1D)
//            throw new RuntimeException("u dimwit didnt make sure the sum of probabilities = 1");

        this.probabilities = probabilities;
        this.outputs = outputs;
    }

    public int getOutput()
    {
        double prob = random.nextDouble();
        int probInterval = getProbabilityInterval(prob);

        if (probInterval >= 0)
            return outputs[probInterval];

        return -1;
    }

    public int getOutput(Function<Integer, Integer> modifier)
    {
        return modifier.apply(getOutput());
    }

    private int getProbabilityInterval(double prob)
    {
        double currentSum = 0;
        for (int i = 0; i < probabilities.length; i++)
        {
            currentSum += probabilities[i];
            if (prob <= currentSum || i == probabilities.length - 1)
                return i;
        }

        return -1;
    }
}
