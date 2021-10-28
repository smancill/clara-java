/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util;

import java.util.Objects;

public final class ArgUtils {

    private ArgUtils() { }

    public static String requireNonEmpty(String arg, String desc) {
        if (arg == null) {
            throw new NullPointerException("null " + desc);
        }
        if (arg.isEmpty()) {
            throw new IllegalArgumentException("empty " + desc);
        }
        return arg;
    }

    public static <T> T requireNonNull(T obj, String desc) {
        return Objects.requireNonNull(obj, "null " + desc);
    }
}
