package com.torutk.spectrum.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpectrumDataExporter {

    /**
     * Exports SpectrumData to CSV file.
     *
     * <pre>
     * "Frequency[MHz]", "Power[dBm]"
     * 3456.7, -89.1
     *   :       :
     * </pre>
     * @param toDirectory save file in this directory
     * @param data save this data as CSV
     * @throws IOException if the file failed to write
     */
    public static void exportAsCsv(Path toDirectory, SpectrumData data) throws IOException {
        Path csvPath = toDirectory.resolve(data.getName() + ".csv");
        float[] frequencies = data.getFrequencies();
        float[] powers = data.getPowers();
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, Charset.forName("Windows-31J"))) {
            writer.write("Frequency[MHz], Power[dBm]");
            writer.newLine();
            for (int i = 0; i < data.size(); i++) {
                writer.write(String.format("%f, %f", frequencies[i], powers[i]));
                writer.newLine();
            }
        }
    }
}
