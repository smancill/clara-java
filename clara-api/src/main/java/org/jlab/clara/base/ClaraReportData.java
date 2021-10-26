/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

interface ClaraReportData<T extends ClaraName> {

    /**
     * Gets the CLARA canonical name.
     *
     * @return the canonical name
     */
    T name();
}
