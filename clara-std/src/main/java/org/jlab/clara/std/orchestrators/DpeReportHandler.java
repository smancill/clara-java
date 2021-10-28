/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.DpeRegistrationData;
import org.jlab.clara.base.DpeRuntimeData;

/**
 * Handles the reports published by Clara DPEs.
 */
public interface DpeReportHandler extends AutoCloseable {

    /**
     * Processes the DPE report.
     *
     * @param dpeRegistration the DPE registration report
     * @param dpeRuntime the DPE runtime report
     */
    void handleReport(DpeRegistrationData dpeRegistration, DpeRuntimeData dpeRuntime);

    @Override
    default void close() throws Exception { }
}
