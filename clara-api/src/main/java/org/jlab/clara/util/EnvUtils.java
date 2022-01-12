/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class EnvUtils {

    private static final String ID_GROUP = "([A-Za-z_][A-Za-z0-9_]*)";
    private static final String ENV_VAR_PATTERN = "((?:[\\\\$])\\$)"
            + "|\\$(?:" + ID_GROUP + "|\\{" + ID_GROUP + "(?::-([^\\}]*))?\\})"
            + "|(\\$)";
    private static final Pattern ENV_VAR_EXPR = Pattern.compile(ENV_VAR_PATTERN);

    private EnvUtils() { }

    /**
     * Gets the value of the given environment variable, if exists.
     *
     * @param variable the name of the environment variable
     * @return an Optional that contains the value of the variable if exists
     */
    public static Optional<String> get(String variable) {
        return Optional.ofNullable(System.getenv(variable));
    }

    /**
     * Gets the value of the CLARA_HOME environment variable.
     *
     * @return the Clara home directory
     */
    public static String claraHome() {
        var claraHome = System.getenv("CLARA_HOME");
        if (claraHome == null) {
            throw new RuntimeException("Missing CLARA_HOME environment variable");
        }
        return claraHome;
    }

    /**
     * Gets the user account name.
     *
     * @return the account name of the user running Clara.
     */
    public static String userName() {
        var userName = System.getProperty("user.name");
        if (userName == null || userName.equals("?")) {
            if (inDockerContainer()) {
                return "docker";
            }
            throw new RuntimeException("Missing 'user.name' system property");
        }
        return userName;
    }

    /**
     * Gets the user home directory.
     *
     * @return the home directory of the user running Clara.
     */
    public static String userHome() {
        var userHome = System.getProperty("user.home");
        if (userHome == null || userHome.equals("?")) {
            if (inDockerContainer()) {
                return "/";
            }
            throw new RuntimeException("Missing 'user.home' system property");
        }
        return userHome;
    }

    /**
     * Expands any environment variable present in the input string.
     *
     * @param input the string to be expanded
     * @param environment the map containing the environment variables
     *
     * @return the input string with all environment variables replaced by their values
     */
    public static String expandEnvironment(String input, Map<String, String> environment) {
        var sb = new StringBuilder();
        var matcher = ENV_VAR_EXPR.matcher(input);
        while (matcher.find()) {
            var variable = matcher.group(2);
            if (variable == null) {
                variable = matcher.group(3);
            }
            if (variable != null) {
                var value = environment.get(variable);
                if (value == null) {
                    var defaultValue = matcher.group(4);
                    value = Objects.requireNonNullElse(defaultValue, "");
                }
                matcher.appendReplacement(sb, value);
            } else if (matcher.group(1) != null) {
                matcher.appendReplacement(sb, "\\$");
            } else {
                throw new IllegalArgumentException("Invalid environment variable format");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static boolean inDockerContainer() {
        return Files.exists(Paths.get("/.dockerenv"));
    }
}
