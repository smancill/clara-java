/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
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
