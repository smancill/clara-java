/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Topic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Extra helper methods for Clara orchestrator and services.
 *
 * @author gurjyan
 * @version 4.x
 */
@ParametersAreNonnullByDefault
public final class ClaraUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(ClaraConstants.DATE_FORMAT);

    /**
     * Regex to validate a full canonical name.
     * Groups are used to separate each component.
     * <p>
     * A canonical name should have any of the following structures:
     * <pre>
     * {@literal <host>%<port>_<language>}
     * {@literal <host>%<port>_<language>:<container>}
     * {@literal <host>%<port>_<language>:<container>:<engine>}
     *
     * With {@literal %<port>} part being optional.
     * </pre>
     */
    public static final Pattern CANONICAL_NAME_PATTERN =
            Pattern.compile("^" + ClaraComponent.CANONICAL_NAME_REGEX + "$");

    private static final int CONTAINER_GROUP = 2;
    private static final int SERVICE_GROUP = 3;


    private ClaraUtil() {
    }

    /**
     * Checks if the given name is a proper Clara canonical name.
     * <p>
     * A canonical name should have any of the following structures:
     * <pre>
     * {@literal <host>%<port>_<language>}
     * {@literal <host>%<port>_<language>:<container>}
     * {@literal <host>%<port>_<language>:<container>:<engine>}
     * </pre>
     *
     * @param name the name to be checked
     * @return true if the string is a Clara canonical name, false if not
     */
    public static boolean isCanonicalName(String name) {
        var matcher = CANONICAL_NAME_PATTERN.matcher(name);
        return matcher.matches();
    }

    /**
     * Checks if the given name is a proper DPE canonical name.
     * <p>
     * A DPE canonical name should have the following structure:
     * <pre>
     * {@literal <host>_<language>}
     * </pre>
     *
     * @param name the name to be checked
     * @return true if the string is a DPE canonical name, false if not
     */
    public static boolean isDpeName(String name) {
        var matcher = CANONICAL_NAME_PATTERN.matcher(name);
        return matcher.matches() && matcher.group(CONTAINER_GROUP) == null;
    }

    /**
     * Checks if the given name is a proper container canonical name.
     * <p>
     * A container canonical name should have the following structure:
     * <pre>
     * {@literal <host>_<language>:<container>}
     * </pre>
     *
     * @param name the name to be checked
     * @return true if the string is a container canonical name, false if not
     */
    public static boolean isContainerName(String name) {
        var matcher = CANONICAL_NAME_PATTERN.matcher(name);
        return matcher.matches()
                && matcher.group(CONTAINER_GROUP) != null
                && matcher.group(SERVICE_GROUP) == null;
    }

    /**
     * Checks if the given name is a proper service canonical name.
     * <p>
     * A service canonical name should have the following structure:
     * <pre>
     * {@literal <host>_<language>:<container>:<engine>}
     * </pre>
     *
     * @param name the name to be checked
     * @return true if the string is a service canonical name, false if not
     */
    public static boolean isServiceName(String name) {
        var matcher = CANONICAL_NAME_PATTERN.matcher(name);
        return matcher.matches() && matcher.group(SERVICE_GROUP) != null;
    }


    /**
     * Gets the DPE name from the given Clara canonical name.
     *
     * @param canonicalName a Clara canonical name
     * @return the DPE name
     */
    public static String getDpeName(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        var topic = Topic.wrap(canonicalName);
        return topic.domain();
    }

    /**
     * Gets the container name from the given Clara canonical name.
     * This returns just the container name part, without the DPE name
     * (i.e. not a container canonical name).
     *
     * @param canonicalName a Clara canonical name with a container part
     * @return the container name
     */
    public static String getContainerName(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        var topic = Topic.wrap(canonicalName);
        return topic.subject();
    }

    /**
     * Gets the container canonical name from the given Clara canonical name.
     * This returns the full canonical name, including the DPE name.
     *
     * @param canonicalName a Clara canonical name with a container part
     * @return the container canonical name
     */
    public static String getContainerCanonicalName(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        int firstSep = canonicalName.indexOf(Topic.SEPARATOR);
        if (firstSep < 0) {
            throw new IllegalArgumentException("Not a container name: " + canonicalName);
        }
        int secondSep = canonicalName.indexOf(Topic.SEPARATOR, firstSep + 1);
        if (secondSep < 0) {
            return canonicalName;
        }
        return canonicalName.substring(0, secondSep);
    }

    /**
     * Gets the service engine name from the given Clara canonical name.
     * This returns just the engine name part, without the container and DPE
     * names (i.e. not a service canonical name).
     *
     * @param canonicalName a service canonical name
     * @return the container name
     */
    public static String getEngineName(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        var topic = Topic.wrap(canonicalName);
        return topic.type();
    }

    /**
     * Gets the DPE host address from the given Clara canonical name.
     *
     * @param canonicalName a Clara canonical name
     * @return the DPE host address
     */
    public static String getDpeHost(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        int portSep = canonicalName.indexOf(ClaraConstants.PORT_SEP);
        if (portSep > 0) {
            return canonicalName.substring(0, portSep);
        } else {
            int langSep = canonicalName.indexOf(ClaraConstants.LANG_SEP);
            return canonicalName.substring(0, langSep);
        }
    }

    /**
     * Gets the DPE port from the given Clara canonical name.
     *
     * @param canonicalName a Clara canonical name
     * @return the DPE port or the default port if not set in the name
     */
    public static int getDpePort(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        int portSep = canonicalName.indexOf(ClaraConstants.PORT_SEP);
        int langSep = canonicalName.indexOf(ClaraConstants.LANG_SEP);
        if (portSep > 0) {
            var port = canonicalName.substring(portSep + 1, langSep);
            return Integer.parseInt(port);
        } else {
            return getPort(canonicalName, langSep + 1);
        }
    }

    /**
     * Gets the DPE language from the given Clara canonical name.
     *
     * @param canonicalName a Clara canonical name
     * @return the DPE language
     */
    public static String getDpeLang(String canonicalName) {
        if (!isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        var dpeName = getDpeName(canonicalName);
        return dpeName.substring(dpeName.indexOf(ClaraConstants.LANG_SEP) + 1);
    }

    /**
     * Gets the default DPE port for the given language.
     *
     * @param lang the given Clara language
     * @return the default port for a DPE of the language
     */
    public static int getDefaultPort(ClaraLang lang) {
        return getPort(lang.toString(), 0);
    }

    /**
     * Gets the default DPE port for the given language.
     *
     * @param lang a supported Clara language
     * @return the default port for a DPE of the given language
     */
    public static int getDefaultPort(String lang) {
        return getPort(lang, 0);
    }

    private static int getPort(String fullName, int index) {
        return switch (fullName.charAt(index)) {
            case 'j', 'J' -> ClaraConstants.JAVA_PORT;
            case 'c', 'C' -> ClaraConstants.CPP_PORT;
            case 'p', 'P' -> ClaraConstants.PYTHON_PORT;
            default -> throw new IllegalArgumentException("Invalid language:" + fullName);
        };
    }

    /**
     * Helps creating a set of engine data types.
     *
     * @param dataTypes all the data types
     * @return a set with the data types
     */
    public static Set<EngineDataType> buildDataTypes(EngineDataType... dataTypes) {
        var set = new HashSet<EngineDataType>();
        Collections.addAll(set, dataTypes);
        return set;
    }

    /**
     * Splits the input string into lines of the given maximum length.
     * Each line will be prefixed with the specified string.
     *
     * @param input the long string to split into lines
     * @param linePrefix the string to put before each line
     * @param maxLineLength the maximum line length, including the prefix
     * @return the text split into lines
     */
    public static String splitIntoLines(String input, String linePrefix, int maxLineLength) {
        var tokenizer = new StringTokenizer(input);
        var output = new StringBuilder();
        var lineLen = 0;
        while (tokenizer.hasMoreTokens()) {
            var word = tokenizer.nextToken();
            if (lineLen > 0) {
                lineLen++; // count the space before the word
            }
            if (lineLen + word.length() > maxLineLength) {
                output.append('\n');
                lineLen = 0;
            }
            if (lineLen == 0) {
                output.append(linePrefix);
            } else {
                output.append(' ');
            }
            output.append(word);
            lineLen += word.length();
        }
        return output.toString();
    }

    /**
     * Returns the stack trace of an exception as a string.
     * @param e an exception
     * @return a string with the stack trace of the exception
     */
    public static String reportException(Throwable e) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Returns the list of <code>Throwable</code> objects in the
     * exception chain.
     * <p>
     * A throwable without cause will return a list containing
     * one element: the input throwable.
     * A throwable with one cause will return a list containing
     * two elements: the input throwable and the cause throwable.
     * A <code>null</code> throwable will return an empty list.</p>
     *
     * @param throwable  the throwable to inspect, may be null
     * @return the list of throwables
     */
    public static List<Throwable> getThrowableList(Throwable throwable) {
        var throwables = new ArrayList<Throwable>();
        while (throwable != null && !throwables.contains(throwable)) {
            throwables.add(throwable);
            throwable = throwable.getCause();
        }
        return throwables;
    }

    /**
     * Obtains the root cause of the given the <code>Throwable</code>, if any.
     *
     * @param throwable the throwable to get the root cause for, may be null
     * @return the root cause of the <code>Throwable</code>,
     *         <code>null</code> if none found or null throwable input
     */
    public static Throwable getRootCause(Throwable throwable) {
        var throwables = getThrowableList(throwable);
        return throwables.size() < 2 ? null : throwables.get(throwables.size() - 1);
    }

    /**
     * Converts exception stack trace to a string.
     *
     * @param e exception
     * @return String of the stack trace
     */
    public static String stack2str(Exception e) {
        try {
            var sw = new StringWriter();
            var pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e2) {
            return "bad stack";
        }
    }


    /**
     * Checks to see if the service is locally deployed.
     *
     * @param serviceName service canonical name (dpe-ip:container:engine)
     * @return true/false
     */
    public static Boolean isRemoteService(String serviceName) {
        var topic = Topic.wrap(serviceName);
        for (var ip : ActorUtils.getLocalHostIps()) {
            if (ip.equals(topic.domain())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the IPv4 value for the localhost address.
     *
     * @return the localhost IP
     */
    public static String localhost() {
        return ActorUtils.localhost();
    }

    /**
     * Determines the IP address of the specified host.
     *
     * @param hostName The name of the host (accepts "localhost")
     * @return the host IP
     */
    public static String toHostAddress(String hostName) {
        return ActorUtils.toHostAddress(hostName);
    }

    /**
     * Gets the current time and returns string representation of it.
     * @return string representing the current time.
     */
    public static String getCurrentTime() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /**
     * Causes the currently executing thread to sleep for the given number of
     * milliseconds.
     * <p>
     * If any thread interrupts the current thread, this method will return
     * and the interrupt status will be set.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Causes the currently executing thread to sleep for the given duration of time.
     * <p>
     * If any thread interrupts the current thread, this method will return
     * and the interrupt status will be set.
     *
     * @param duration the length of time to sleep
     * @param unit the time unit for the duration of the sleep
     */
    public static void sleep(long duration, TimeUnit unit) {
        try {
            unit.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
