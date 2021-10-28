/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

/**
 * An interface to handle the JSON reports published by CLARA.
 */
public interface DpeReportCallback {

    /**
     * Processes the parsed JSON report published by a CLARA DPE.
     *
     * @param registrationData the DPE registration data
     * @param runtimeData the DPE runtime data
     */
    void callback(DpeRegistrationData registrationData, DpeRuntimeData runtimeData);
}
