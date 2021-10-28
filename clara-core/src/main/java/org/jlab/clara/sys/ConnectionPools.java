/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
