/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;

public final class SystemStats {

    private SystemStats() { }

    public static double getCpuUsage() {
        try {
            var mbs = ManagementFactory.getPlatformMBeanServer();
            var name = ObjectName.getInstance("java.lang:type=OperatingSystem");
            var list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

            if (list.isEmpty()) {
                return Double.NaN;
            }

            var att = (Attribute) list.get(0);
            var value = (Double) att.getValue();

            if (value == -1.0) {
                return Double.NaN;
            }

            // returns a percentage value with 1 decimal point precision
            return (int) (value * 1000) / 10.0;
        } catch (MalformedObjectNameException | NullPointerException
                | InstanceNotFoundException | ReflectionException e) {
            System.err.println("Could not obtain CPU usage: " + e.getMessage());
            return Double.NaN;
        }
    }

    public static long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public static double getSystemLoad() {
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }
}
