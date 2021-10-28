/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineStatus;

/**
 * A collection of utilities to write services.
 */
public final class ServiceUtils {

    private ServiceUtils() { }

    /**
     * Sets the given engine data with an error status.
     *
     * @param output the engine data that will be returned by the service
     * @param msg a description for the error
     */
    public static void setError(EngineData output, String msg) {
        output.setDescription(msg);
        output.setStatus(EngineStatus.ERROR, 1);
    }

    /**
     * Sets the given engine data with an error status.
     *
     * @param output the engine data that will be returned by the service
     * @param msg a description for the error
     * @param severity the severity of the error, as a positive integer
     */
    public static void setError(EngineData output, String msg, int severity) {
        output.setDescription(msg);
        output.setStatus(EngineStatus.ERROR, severity);
    }

    /**
     * Sets the given engine data with an error status.
     *
     * @param output the engine data that will be returned by the service
     * @param format a format string with a description for the error
     * @param args arguments referenced by the format specifiers in the format string
     */
    public static void setError(EngineData output, String format, Object... args) {
        ServiceUtils.setError(output, String.format(format, args));
    }
}
