/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;

import java.util.Set;

/**
 * Handles the reports published by Clara services.
 */
public interface EngineReportHandler extends AutoCloseable {

    /**
     * Processes the reported output event.
     *
     * @param event the reported output
     */
    void handleEvent(EngineData event);

    /**
     * Gets the set of output data types reported by the service.
     *
     * @return the expected output data types, it cannot be null nor empty
     */
    Set<EngineDataType> dataTypes();

    @Override
    default void close() throws Exception { }
}
