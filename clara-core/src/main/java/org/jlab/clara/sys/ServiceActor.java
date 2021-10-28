/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Connection;
import org.jlab.clara.msg.core.ConnectionPool;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;

class ServiceActor {

    private final ClaraBase base;
    private final ConnectionPools connectionPools;

    ServiceActor(ClaraComponent me, ClaraComponent frontEnd, ConnectionPools connectionPools) {
        this.base = new ClaraBase(me, frontEnd);
        this.connectionPools = connectionPools;
    }

    public void close() {
        base.close();
    }

    public void start() throws ClaraException {
        base.cacheLocalConnection();
    }

    public void send(Message msg) throws ClaraException {
        sendMsg(connectionPools.mainPool, getLocal(), msg);
    }

    public void send(ProxyAddress address, Message msg) throws ClaraException {
        sendMsg(connectionPools.mainPool, address, msg);
    }

    public void sendUncheck(Message msg) throws ClaraException {
        sendMsg(connectionPools.uncheckedPool, getLocal(), msg);
    }

    public void sendUncheck(ProxyAddress address, Message msg) throws ClaraException {
        sendMsg(connectionPools.uncheckedPool, address, msg);
    }

    private void sendMsg(ConnectionPool pool, ProxyAddress address, Message msg)
            throws ClaraException {
        try (Connection con = pool.getConnection(address)) {
            base.send(con, msg);
        } catch (ClaraMsgException e) {
            throw new ClaraException("Could not send message", e);
        }
    }

    public String getName() {
        return base.getName();
    }

    public String getEngine() {
        return base.getMe().getEngineName();
    }

    public ProxyAddress getLocal() {
        return base.getDefaultProxyAddress();
    }

    public ProxyAddress getFrontEnd() {
        return base.getFrontEnd().getProxyAddress();
    }
}
