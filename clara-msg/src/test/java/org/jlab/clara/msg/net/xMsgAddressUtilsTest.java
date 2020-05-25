/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class xMsgAddressUtilsTest {

    @Test
    public void checkValidIPs() throws Exception {
        String[] ips = new String[] {
            "1.1.1.1",
            "255.255.255.255",
            "192.168.1.1",
            "10.10.1.1",
            "132.254.111.10",
            "26.10.2.10",
            "127.0.0.1",
        };

        for (String ip : ips) {
            assertTrue(xMsgAddressUtils.isIP(ip), "isIP: " + ip);
        }
    }

    @Test
    public void checkInvalidIPs() throws Exception {
        String[] ips = new String[] {
            "10.10.10",
            "10.10",
            "10",
            "a.a.a.a",
            "10.10.10.a",
            "10.10.10.256",
            "222.222.2.999",
            "999.10.10.20",
            "2222.22.22.22",
            "22.2222.22.2",
        };

        for (String ip : ips) {
            assertFalse(xMsgAddressUtils.isIP(ip), "is IP: " + ip);
        }
    }

    @Test
    public void checkOnlyIPv4() throws Exception {
        String[] ips = new String[] {
            "2001:cdba:0000:0000:0000:0000:3257:9652",
            "2001:cdba:0:0:0:0:3257:9652",
            "2001:cdba::3257:9652",
        };

        for (String ip : ips) {
            assertFalse(xMsgAddressUtils.isIP(ip), "is IP: " + ip);
        }
    }
}
