/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.DpeName;

/**
 * Stores properties of a DPE.
 * <p>
 * Currently, these properties are:
 * <ul>
 * <li>name (IP address)
 * <li>number of cores
 * <li>value of {@code $CLARA_HOME}
 * </ul>
 */
record DpeInfo(DpeName name, int cores, String claraHome) {

    DpeInfo {
        if (name == null) {
            throw new IllegalArgumentException("Null DPE name");
        }
        if (cores < 0) {
            throw new IllegalArgumentException("Invalid number of cores");
        }
    }

    DpeInfo(String name, int cores, String claraHome) {
        this(new DpeName(name), cores, claraHome);
    }
}
