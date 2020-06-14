package com.torutk.spectrum.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

/**
 * Generating a test spectrum data file by markov model.
 *
 * recommend parameters
 * <ul>
 *     <li>probability: between 0.65</li>
 *     <li>upper shift: 2</li>
 *     <li>lower shift: 4</li>
 * </ul>
 *
 */
public class RandomGenerator {
    private static double startFrequency = 10750d;
    private static double stopFrequency = 11750d;
    private static float referenceLevel = -30f;
    private static float scale = 5f;
    private static long numSamples = 10_000;

    private static Path outPath;
    private static double markovProbability = 0.5;
    private static int markovUpperShift = 2;
    private static int markovLowerShift = 3;

    private static Random random = new Random();

    /**
     * Entry method.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if ("-f".equals(args[i])) {
                outPath = Paths.get(args[++i]);
            } else if ("-p".equals(args[i])) {
                markovProbability = Double.parseDouble(args[++i]);
            } else if ("-u".equals(args[i])) {
                markovUpperShift = Integer.parseInt(args[++i]);
            } else if ("-l".equals(args[i])) {
                markovLowerShift = Integer.parseInt(args[++i]);
            } else {
                printUsageAndExit();
            }
        }
        if (outPath == null) {
            System.err.println("file path must be specified.");
            printUsageAndExit();
        }

        System.out.println("Random Spectrum Generator");
        System.out.printf("outfile=%s probability=%f upper shift=%d lower shift=%d%n",
                outPath, markovProbability, markovUpperShift, markovLowerShift
        );

        try (SeekableByteChannel channel = Files.newByteChannel(
                outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        ) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) (numSamples * 4 + 32));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(numSamples);
            buffer.putDouble(startFrequency);
            buffer.putDouble(stopFrequency);
            buffer.putFloat(referenceLevel);
            buffer.putFloat(scale);
            int previous = 250;
            for (int i = 0; i < numSamples; i++) {
                int value = markov(markovProbability, previous, markovUpperShift, markovLowerShift);
                previous = value;
                buffer.putInt(value);
            }
            buffer.flip();
            channel.write(buffer);
        }
    }

    /**
     * Generate the power value with specified probability, current power, shift values toward increasing and decreasing.
     *
     * @param p the probalility toward increasing power(0,1)
     * @param previous current power[0,255]
     * @param upperShift shift power toward increasing[1,16]
     * @param lowerShift shift power toward decreasing[1,16]
     * @return next power[0,255]
     */
    static int markov(double p, int previous, int upperShift, int lowerShift) {
        if (random.nextDouble() < p) {
            return Math.max(0, previous - upperShift);
        } else {
            return Math.min(255, previous + lowerShift);
        }
    }

    static void printUsageAndExit() {
        System.out.println("Command line usage: -f <file> -p <probability> -u <upper shift> -l <lower shift>");
        System.out.println("\tfile: spectrum data file to be generated");
        System.out.println("\tprobability: of power increased (0.0 to 1.0) by markov model");
        System.out.println("\tupper shift: amount of power increase in a step");
        System.out.println("\tlower shift: amount of power decrease in a step");
        System.exit(1);
    }
}
