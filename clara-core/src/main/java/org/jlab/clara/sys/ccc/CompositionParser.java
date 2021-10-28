/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.ccc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

final class CompositionParser {

    private CompositionParser() { }

    public static String removeFirst(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(1);
    }

    public static String removeFirst(String input, String firstCharacter) {
        input = input.startsWith(firstCharacter) ? input.substring(1) : input;
        return input;
    }

    public static String removeLast(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, s.length() - 1);
    }

    public static String getFirstService(String composition) {
        StringTokenizer st = new StringTokenizer(composition, ";");
        String a = st.nextToken();

        if (a.contains(",")) {
            StringTokenizer stk = new StringTokenizer(a, ",");
            return stk.nextToken();
        } else {
            return a;
        }
    }

    public static String getJSetElementAt(List<String> set, int index) {
        int ind = -1;
        for (String s : set) {
            ind++;
            if (index == ind) {
                return s;
            }
        }
        throw new NoSuchElementException();
    }
}
