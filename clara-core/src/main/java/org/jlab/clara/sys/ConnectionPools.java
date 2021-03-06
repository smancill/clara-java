/*
 * Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.sys;

import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.ConnectionPool;
import org.jlab.clara.msg.net.ProxyAddress;

class ConnectionPools implements AutoCloseable {

    final ConnectionPool mainPool;
    final ConnectionPool uncheckedPool;

    ConnectionPools(ProxyAddress defaultProxy) {
        mainPool = ConnectionPool.newBuilder()
                .withProxy(defaultProxy)
                .withPreConnectionSetup(s -> {
                    s.setRcvHWM(0);
                    s.setSndHWM(0);
                })
                .withPostConnectionSetup(() -> ActorUtils.sleep(100))
                .build();

        uncheckedPool = ConnectionPool.newBuilder()
                .withProxy(defaultProxy)
                .withPreConnectionSetup(s -> {
                    s.setRcvHWM(0);
                    s.setSndHWM(0);
                })
                .withPostConnectionSetup(() -> ActorUtils.sleep(100))
                .checkConnection(false)
                .checkSubscription(false)
                .build();
    }

    @Override
    public void close() {
        mainPool.close();
        uncheckedPool.close();
    }
}
