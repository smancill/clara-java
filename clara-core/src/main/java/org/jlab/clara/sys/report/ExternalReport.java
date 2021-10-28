/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

/**
 * @author gurjyan
 * @version 4.x
 */
public interface ExternalReport {
    String generateReport(DpeReport dpeData);
}
