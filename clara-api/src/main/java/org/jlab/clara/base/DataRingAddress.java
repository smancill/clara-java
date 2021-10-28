/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.base.core.ClaraConstants;

/**
 * The address of a Clara data-ring.
 */
public class DataRingAddress extends ClaraAddress {

    /**
     * Identify a Clara data-ring.
     *
     * @param host the host address of the data ring.
     */
    public DataRingAddress(String host) {
        super(host, ClaraConstants.MONITOR_PORT);
    }

    /**
     * Identify a Clara data-ring.
     *
     * @param host the host address of the data ring.
     * @param port the port used by the data ring.
     */
    public DataRingAddress(String host, int port) {
        super(host, port);
    }

    /**
     * Identify a Clara data-ring.
     *
     * @param dpe the DPE acting as a data ring.
     */
    public DataRingAddress(DpeName dpe) {
        super(dpe.address().host(), dpe.address().port());
    }
}
