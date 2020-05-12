package com.torutk.spectrum.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Find file utility class.
 */
public class FileFinder {
    /**
     * Find file by name from specified path within specified max depth.
     *
     * @param from search from this directory
     * @param name of file to be found
     * @param maxDepth search till this depth
     * @return found path
     * @throws IOException file not found, or cannot access file system
     */
    public static Path find(Path from, String name, int maxDepth) throws IOException {
        try (Stream<Path> stream = Files.find(from, maxDepth, (path, attr) ->
                path.getFileName().toString().equalsIgnoreCase(name))) {
            return stream.findFirst().orElseThrow(() -> new FileNotFoundException(name));
        }
    }
}
