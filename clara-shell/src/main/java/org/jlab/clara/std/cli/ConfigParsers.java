/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.util.ArgUtils;
import org.jlab.clara.util.FileUtils;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parsers to set the value of configuration variables.
 */
public final class ConfigParsers {

    private ConfigParsers() { }

    /**
     * Parses the set command arguments into a string.
     *
     * @param args the command arguments
     * @return the first argument
     */
    public static String toString(String... args) {
        return requireArg(args);
    }

    /**
     * Parses the set command arguments into a string.
     *
     * @param args the command arguments
     * @return the first argument
     */
    public static String toStringOrEmpty(String... args) {
        if (args.length > 0) {
            return args[0];
        }
        return "";
    }

    /**
     * Parses the set command arguments into a single alphanumeric word.
     *
     * @param args the command arguments
     * @return the first argument
     */
    public static String toAlphaNum(String... args) {
        var word = requireArg(args);
        if (!word.matches("[0-9A-Za-z]+")) {
            throw new IllegalArgumentException("argument is not an alphanumeric word");
        }
        return word;
    }

    /**
     * Parses the set command arguments into a single alphanumeric word.
     *
     * @param args the command arguments
     * @return the first argument
     */
    public static String toAlphaNumOrEmpty(String... args) {
        if (args.length == 0) {
            return "";
        }
        var word = args[0];
        if (word.isEmpty()) {
            return "";
        }
        if (!word.matches("[0-9A-Za-z]+")) {
            throw new IllegalArgumentException("argument is not an alphanumeric word");
        }
        return word;
    }

    /**
     * Parses the set command arguments into a single string without whitespace.
     *
     * @param args the command arguments
     * @return the first argument
     */
    public static String toNonWhitespace(String... args) {
        var word = requireArg(args);
        if (!word.matches("\\S+")) {
            throw new IllegalArgumentException("argument contains whitespace characters");
        }
        return word;
    }

    /**
     * Parses the set command arguments into an integer.
     *
     * @param args the command arguments
     * @return the integer value represented by the first argument
     */
    public static Integer toInteger(String... args) {
        return Integer.parseInt(requireArg(args));
    }

    /**
     * Parses the set command arguments into a long.
     *
     * @param args the command arguments
     * @return the {@code long} value represented by the first argument
     */
    public static Long toLong(String... args) {
        return Long.parseLong(requireArg(args));
    }

    /**
     * Parses the set command arguments into a float.
     *
     * @param args the shell arguments
     * @return the {@code float} value represented by the first argument
     */
    public static Float toFloat(String... args) {
        return Float.parseFloat(requireArg(args));
    }

    /**
     * Parses the set command arguments into a double.
     *
     * @param args the command arguments
     * @return the {@code double} value represented by the first argument
     */
    public static Double toDouble(String... args) {
        return Double.parseDouble(requireArg(args));
    }

    /**
     * Parses the set command arguments into a boolean.
     *
     * @param args the command arguments
     * @return the boolean represented by the first argument
     */
    public static Boolean toBoolean(String... args) {
        return Boolean.parseBoolean(requireArg(args));
    }

    /**
     * Parses the set command arguments into a positive integer.
     *
     * @param args the command arguments
     * @return the integer value represented by the first argument
     */
    public static Integer toPositiveInteger(String... args) {
        return requirePositive(Integer.parseInt(requireArg(args)));
    }

    /**
     * Parses the set command arguments into a non-negative integer.
     *
     * @param args the command arguments
     * @return the integer value represented by the first argument
     */
    public static Integer toNonNegativeInteger(String... args) {
        return requireNonNegative(Integer.parseInt(requireArg(args)));
    }

    /**
     * Parses the set command arguments into a positive long.
     *
     * @param args the command arguments
     * @return the {@code long} value represented by the first argument
     */
    public static Long toPositiveLong(String... args) {
        return requirePositive(Long.parseLong(requireArg(args)));
    }

    /**
     * Parses the set command arguments into a file path string.
     * The file may not exist.
     *
     * @param args the command arguments
     * @return the path string represented by the first argument
     */
    public static Path toFile(String... args) {
        var path = FileUtils.expandHome(requireArg(args));
        if (Files.exists(path)) {
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("argument is not a regular file");
            }
        }
        return path;
    }

    /**
     * Parses the set command arguments into a directory path string.
     * The directory may not exist.
     *
     * @param args the command arguments
     * @return the path string represented by the first argument
     */
    public static Path toDirectory(String... args) {
        var path = FileUtils.expandHome(requireArg(args));
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException("argument is not a directory");
            }
        }
        return path;
    }

    /**
     * Parses the set command arguments into an existing file path string.
     * The file must exist.
     *
     * @param args the command arguments
     * @return the path string represented by the first argument
     */
    public static Path toExistingFile(String... args) {
        var path = FileUtils.expandHome(requireArg(args));
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("file does not exist");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("argument is not a regular file");
        }
        return path;
    }

    /**
     * Parses the set command arguments into an existing directory path string.
     * The directory must exist.
     *
     * @param args the command arguments
     * @return the path string represented by the first argument
     */
    public static Path toExistingDirectory(String... args) {
        var path = FileUtils.expandHome(requireArg(args));
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("directory does not exist");
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("argument is not a directory");
        }
        return path;
    }

    /**
     * Parses the set command arguments into an IPv4 address.
     * The directory must exist.
     *
     * @param args the command arguments
     * @return the IP address represented by the first argument
     */
    public static String toHostAddress(String... args) {
        var arg = requireArg(args);
        try {
            return ClaraUtil.toHostAddress(arg);
        } catch (UncheckedIOException e) {
            throw new IllegalArgumentException("argument could not be resolved to a host address");
        }
    }

    private static String requireArg(String... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("missing argument");
        }
        return ArgUtils.requireNonEmpty(args[0], "argument");
    }

    private static <T extends Number> T requirePositive(T n) {
        if (n.longValue() <= 0) {
            throw new IllegalArgumentException("the argument must be positive");
        }
        return n;
    }

    private static <T extends Number> T requireNonNegative(T n) {
        if (n.longValue() < 0) {
            throw new IllegalArgumentException("the argument must be positive");
        }
        return n;
    }
}
