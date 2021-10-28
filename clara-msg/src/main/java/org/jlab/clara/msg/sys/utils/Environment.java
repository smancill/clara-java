/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.utils;

public final class Environment {

    private Environment() { }


    public static long getLong(String var, long def) {
        String value = System.getenv(var);
        if (value == null) {
            value = String.valueOf(def);
        }
        try {
            long timeout = Long.parseLong(value);
            if (timeout < 0) {
                throw new IllegalArgumentException("invalid value " + var + "=" + value);
            }
            return timeout;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid value " + var + "=" + value);
        }
    }


    public static boolean isDefined(String var) {
        return System.getenv(var) != null;
    }
}
