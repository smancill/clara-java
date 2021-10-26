/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.pubsub;

import org.jlab.clara.msg.net.AddressUtils;

import java.util.Random;

final class IdentityGenerator {

    private IdentityGenerator() { }

    private static final Random randomGenerator = new Random(); // nocheck: ConstantName
    private static final long CTRL_ID_PREFIX = getCtrlIdPrefix();

    private static long getCtrlIdPrefix() {
        final int javaId = 1;
        final int ipHash = AddressUtils.localhost().hashCode() & Integer.MAX_VALUE;
        return javaId * 100000000 + (ipHash % 1000) * 100000;
    }

    static String getCtrlId() {
        return Long.toString(CTRL_ID_PREFIX + randomGenerator.nextInt(100000));
    }
}
