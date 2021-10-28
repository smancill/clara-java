/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util.logging;

public class LoggerFactory {

    public LoggerFactory() {
        SimpleLogger.lazyInit();
    }

    public Logger getLogger(String name) {
        return new SimpleLogger(name);
    }
}
