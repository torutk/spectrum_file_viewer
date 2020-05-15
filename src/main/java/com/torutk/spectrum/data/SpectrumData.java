package com.torutk.spectrum.data;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * One spectrum data.
 *
 * <pre>
 *     Reference Level -->  +------------------------------+    <-- power = 0
 *                          |                              |
 *                    ^ --  +------------------------------+
 *              scale  |    |                              |
 *                    V --  +------------------------------+
 *                          |                              |
 *                          :                              :
 *                          +------------------------------+    <-- power = 255
 *                          ^                              ^
 *                    Start Frequency                   Stop Frequency
 * </pre>
 */
public class SpectrumData {
    private static final Logger logger = Logger.getLogger(SpectrumData.class.getName());
    private static final float RC_WEIGHT = 0.65f;

    private int id;
    private final String name;
    private final double stopFrequency;
    private final double startFrequency;
    private final float referenceLevel;
    private final float scale;
    private final byte[] powers;

    private float averagePower = Float.NaN; // lazy

    /**
     * Unit conversion from dBm to mW.
     *
     * @param dbm to be converted [dBm]
     * @return converted value [mW]
     */
    public static double toMilliwatt(double dbm) {
        return Math.pow(10, dbm / 10);
    }

    /**
     * Unit conversion from mW to dBm.
     *
     * @param milliwatt to be converted [mW]
     * @return converted value [dBm]
     */
    public static double toDbm(double milliwatt) {
        return 10 * Math.log10(milliwatt);
    }

    /**
     * Constructor with full parameters.
     *
     * @param name of this spectrum
     * @param startFrequency of this spectrum [MHz]
     * @param stopFrequency of this spectrum [MHz]
     * @param referenceLevel of this spectrum [dBm]
     * @param scale of this spectrum [dBm/DIV]
     * @param powers power with range 0-255, 0 is the highest power in scales, 255 is the lowers power in scales.
     */
    public SpectrumData(
            String name, double startFrequency, double stopFrequency, float referenceLevel, float scale, byte[] powers
    ) {
        this(name.hashCode(), name, startFrequency, stopFrequency, referenceLevel, scale, powers);
    }

    public SpectrumData(
            int id, String name, double startFrequency, double stopFrequency, float referenceLevel, float scale,
            byte[] powers
    ) {
        this.id = id;
        this.name = name;
        this.startFrequency = startFrequency;
        this.stopFrequency = stopFrequency;
        this.referenceLevel = referenceLevel;
        this.scale = scale;
        this.powers = powers;
    }

    /**
     * Apply RC filter (Low-pass filter) to previous value and current value with specified weight.
     *
     * <pre>
     *     OUTn = weight * OUTn-1 +  (1 - weight) * CURRENT
     * </pre>
     *
     * @param previous previous output
     * @param current input
     * @return RC filtered output
     */
    public static double applyRcFilter(double previous, double current) {
        double previousMilliwatt = toMilliwatt(previous);
        double currentMilliwatt = toMilliwatt(current);
        double filteredMilliwatt = RC_WEIGHT * previousMilliwatt + (1 - RC_WEIGHT) * currentMilliwatt;
        return toDbm(filteredMilliwatt);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public double getStartFrequency() {
        return startFrequency;
    }

    public double getStopFrequency() {
        return stopFrequency;
    }

    public float getReferenceLevel() {
        return referenceLevel;
    }

    public float getScale() {
        return scale;
    }

    public int size() {
        return powers.length;
    }

    /**
     * Gets all powers from start frequency to stop frequency with interval of sampling.
     * @return powers array, the unit of each element is [dBm]
     */
    public float[] getPowers() {
        float[] decodedPowers = new float[powers.length];
        for (int i = 0; i < powers.length; i++) {
            decodedPowers[i] = decode(powers[i]);
        }
        return decodedPowers;
    }

    /**
     * Gets all frequencies of all sampling point.
     * @return frequencies array, the unit of each element is [MHz]
     */
    public float[] getFrequencies() {
        float[] frequencies = new float[powers.length];
        for (int i = 0; i < powers.length; i++) {
            frequencies[i] = getFrequencyAt(i);
        }
        return frequencies;
    }

    /**
     * Get powers from start frequency to stop frequency with detrend specified spectrum.
     *
     * detrend calculation is on dBm scale, not mW.
     *
     * @param detrend detrend spectrum
     * @return detrended powers [dBm]
     */
    public float[] getPowersDetrend(SpectrumData detrend) {
        if (detrend == null) {
            return getPowers();
        }
        float[] decodedPowers = new float[powers.length];
        for (int i = 0; i < powers.length; i++) {
            float frequency = getFrequencyAt(i);
            if (detrend.containsFrequency(frequency)) {
                double bias = detrend.getPowerAt(frequency) - detrend.getAveragePower();
                double detrended = decode(powers[i]) - bias;
                decodedPowers[i] = (float) detrended;
            } else {
                decodedPowers[i] = decode(powers[i]);
            }
        }
        return decodedPowers;
    }

    /**
     * decode power to dBm.
     *
     * @param encodedPower encoded power value
     * @return decoded power [dBm]
     */
    public float decode(byte encodedPower) {
        return referenceLevel - scale * 10 * Byte.toUnsignedInt(encodedPower) / 255;
    }

    /**
     * get the frequency at the specified index.
     * @param index of frequency [MHz]
     * @return frequency of the specified index
     */
    public float getFrequencyAt(int index) {
        float start = (float) startFrequency;
        float stop = (float) stopFrequency;
        return start + (stop - start) * index / powers.length;
    }

    /**
     * Returns true if the specified frequency is between start frequency and stop frequency.
     * @param frequency the frequency to search for
     * @return true if the specified frequency is in, false otherwise.
     */
    public boolean containsFrequency(float frequency) {
        return (startFrequency <= frequency) && (frequency <= stopFrequency);
    }

    /**
     * Returns a power at the specified frequency.
     *
     * @param frequency to specified [MHz]
     * @return power at the frequency [dBm]
     */
    public float getPowerAt(float frequency) {
        assert containsFrequency(frequency);
        int index = (int) Math.floor((frequency - startFrequency) / getSamplingRate());
        return decode(powers[index]);
    }

    /**
     * Calculate sampling rate.
     * @return sampling rate [MHz]
     */
    public double getSamplingRate() {
        return (stopFrequency - startFrequency) / powers.length;
    }

    public float getAveragePower() {
        if (Float.isNaN(averagePower)) {
            double averagePowerMilliwatt = (float) IntStream.range(0, powers.length)
                    .mapToDouble(i -> (double) decode(powers[i]))
                    .map(SpectrumData::toMilliwatt)
                    .average().orElseGet(() -> powers[0]);
            averagePower = (float) toDbm(averagePowerMilliwatt);
        }
        return averagePower;
    }

    /**
     *
     * @return byte array expression of powers
     */
    public byte[] getPowersAsBytes() {
        return powers;
    }

    @Override
    public String toString() {
        return "SpectrumData{" +
                "name='" + name + '\'' +
                ", startFrequency=" + startFrequency +
                ", stopFrequency=" + stopFrequency +
                ", referenceLevel=" + referenceLevel +
                ", scale=" + scale +
                ", powers.length=" + powers.length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpectrumData that = (SpectrumData) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
