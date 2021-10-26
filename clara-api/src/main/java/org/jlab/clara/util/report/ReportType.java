/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util.report;

import org.jlab.clara.base.core.ClaraConstants;

public enum ReportType {
    INFO(ClaraConstants.SERVICE_REPORT_INFO),
    DONE(ClaraConstants.SERVICE_REPORT_DONE),
    DATA(ClaraConstants.SERVICE_REPORT_DATA),
    RING(ClaraConstants.SERVICE_REPORT_RING);

    private final String stringValue;

    ReportType(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getValue() {
        return stringValue;
    }

}
