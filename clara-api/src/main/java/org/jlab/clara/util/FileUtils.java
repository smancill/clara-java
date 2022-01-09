/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class FileUtils {

    private FileUtils() { }

    public static Path expandHome(String path) {
        String home = EnvUtils.userHome();
        if (path.startsWith("~")) {
            path = path.replace("~", home);
        } else if (path.startsWith("$HOME")) {
            path = path.replace("$HOME", home);
        }
        return Paths.get(path);
    }

    public static Path claraPath(String... args) {
        return Paths.get(EnvUtils.claraHome(), args);
    }

    public static Path getFileName(Path path) {
        var fileName = path.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("empty path");
        }
        return fileName;
    }

    public static Path getParent(Path path) {
        var parent = path.getParent();
        if (parent == null) {
            return Paths.get(".");
        }
        return parent;
    }

    public static void createDirectories(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public static void deleteFileTree(Path dir) throws IOException {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException ex)
                            throws IOException {
                        if (ex == null) {
                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        } else if (ex instanceof NoSuchFileException) {
                            return FileVisitResult.CONTINUE;
                        }
                        throw ex;
                    }
            });
        } catch (NoSuchFileException e) {
            // ignore
        }
    }

    public static PrintWriter openOutputTextFile(Path path, boolean append) throws IOException {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(path.toFile(), append), StandardCharsets.UTF_8)));
    }
}
