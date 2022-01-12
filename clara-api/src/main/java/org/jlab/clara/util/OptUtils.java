/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpec;

public final class OptUtils {

    private OptUtils() { }

    public static <V> String optionHelp(String name, String... help) {
        var sb = new StringBuilder();
        for (int i = 0; i < help.length; i++) {
            sb.append(String.format("  %-25s  %s%n", i == 0 ? name : "", help[i]));
        }
        return sb.toString();
    }

    public static <V> String optionHelp(OptionSpec<V> spec, String arg, String... help) {
        var lhs = optionName(spec, arg);
        return optionHelp(lhs, help);
    }

    public static <V> String optionName(OptionSpec<V> spec, String arg) {
        var sb = new StringBuilder();
        var name = spec.options().get(0);
        sb.append("-");
        if (name.length() > 1) {
            sb.append("-");
        }
        sb.append(name);
        if (arg != null) {
            sb.append(" <").append(arg).append(">");
        }
        return sb.toString();
    }

    public static <V> String getDefault(OptionSpec<V> stageDir) {
        var spec = (ArgumentAcceptingOptionSpec<V>) stageDir;
        return "(default: " + spec.defaultValues().get(0) + ")";
    }
}
