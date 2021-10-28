/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.engine.EngineData;

/**
 * An interface to handle the reports published by CLARA services.
 */
public interface EngineCallback {

    /**
     * Receives and process the data that a service has published.
     * The data can contain the actual result of the service and/or status
     * information about the execution that generated the data.
     *
     * @param data the published data
     */
    void callback(EngineData data);
}
