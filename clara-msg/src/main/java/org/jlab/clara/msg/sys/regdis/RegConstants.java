/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

public final class RegConstants {

    public static final int REGISTRATION_TIMEOUT = 3000;
    public static final int DISCOVERY_TIMEOUT = 3000;

    public static final String REGISTER = "register";
    public static final String REMOVE = "remove";
    public static final String REMOVE_ALL = "remove_all";

    public static final String FIND_MATCHING = "find_matching";
    public static final String FIND_EXACT = "find_exact";
    public static final String FIND_ALL = "find_all";

    public static final String FILTER = "filter";

    public static final String UNDEFINED = "undefined";
    public static final String SUCCESS = "success";

    private RegConstants() { }
}
