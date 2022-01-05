/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

record DpeConfig(int maxCores, int poolSize, long reportPeriod) {

    static int calculatePoolSize(int cores) {
        int halfCores = cores / 2;
        if (halfCores <= 2) {
            return 2;
        }
        int poolSize = (halfCores % 2 == 0) ? halfCores : halfCores + 1;
        return Math.min(poolSize, 16);
    }
}
