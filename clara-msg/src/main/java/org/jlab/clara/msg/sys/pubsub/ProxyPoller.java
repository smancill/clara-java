/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.pubsub;

import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import java.io.Closeable;

public class ProxyPoller implements Closeable {

    final Socket subSocket;
    final Poller poller;

    public ProxyPoller(ProxyDriver connection) {
        this.subSocket = connection.getSocket();
        this.poller = connection.getContext().poller(1);
        this.poller.register(subSocket, Poller.POLLIN);
    }

    public boolean poll(long timeout) {
        var rc = poller.poll(timeout);
        if (rc < 0) {
            throw new ZMQException("error polling subscription", subSocket.base().errno());
        }
        return poller.pollin(0);
    }

    @Override
    public void close() {
        poller.close();
    }
}
