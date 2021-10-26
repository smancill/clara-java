/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

/**
 * An interface to handle the JSON reports published by CLARA.
 */
public interface GenericCallback {

    /**
     * Receives and process the data that CLARA has published.
     * The data is in JSON format and contain registration and runtime
     * information related to the current status of the CLARA cloud.
     *
     * @param json the published data
     */
    void callback(String json);
}
