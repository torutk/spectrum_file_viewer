package com.torutk.spectrum.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parse data file saved from Glowlink Model 1030/8000.
 *
 * Data File Format:
 * <pre>
 *     |Number of samples | Start Frequency |
 *     | Stop Frequency   | Ref.Lev. |Scale |
 *     | data#1 | data#2  | data#3 | data#4 |
 *     | data#5 | data#6  | data#7 | data#8 |
 *     |   :    |    :    |    :   |    :   |
 * </pre>
 * <ul>
 *     <li>Number of samples : 64bit integer</li>
 *     <li>Start Frequency : 64bit double</li>
 *     <li>Stop Frequency : 64bit double</li>
 *     <li>Reference Level : 32bit float</li>
 *     <li>Scale : 32bit float</li>
 *     <li>data#x : 32bit integer</li>
 * </ul>
 */
public class SpectrumDataParser {

    public static SpectrumData parse(Path path) throws IOException {
        var buffer = ByteBuffer.wrap(Files.readAllBytes(path));
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        var numData = (int) buffer.getLong();
        var startFrequency = buffer.getDouble();
        var stopFrequency = buffer.getDouble();
        var referenceLevel = buffer.getFloat();
        var scale = buffer.getFloat();
        var powers = new byte[numData];
        for (int i = 0; i < numData; i++) {
            powers[i] = (byte) buffer.getInt();
        }
        var name = getBaseName(path.getFileName().toString());
        return new SpectrumData(name, startFrequency, stopFrequency, referenceLevel, scale, powers);
    }

    /**
     * Returns the name omitted the extension.
     * @param name full name included extension
     * @return the name omitted the extension
     */
    private static String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        if (index <= 0) {
            return name;
        } else {
            return name.substring(0, index);
        }
    }
}
