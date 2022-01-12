/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.pubsub;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.sys.utils.ThreadUtils;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMsg;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ProxyListener implements Runnable {

    private static final long TIMEOUT = 100;

    protected final ConcurrentMap<ProxyAddress, ProxyDriver> connections;

    private final Context context;

    private final Thread pollingThread;
    private volatile boolean isRunning = false;


    public ProxyListener(String name, Context context) {
        this.connections = new ConcurrentHashMap<>();
        this.context = context;
        this.pollingThread = ThreadUtils.newThread(name, this);
    }

    public void start() {
        isRunning = true;
        pollingThread.start();
    }

    public void stop() {
        try {
            isRunning = false;
            pollingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connections.values().forEach(ProxyDriver::close);
    }

    protected abstract void handle(ZMsg msg) throws ClaraMsgException;

    @Override
    public void run() {
        try (var poller = context.getContext().poller(connections.size())) {
            while (isRunning) {
                for (var connection : connections.values()) {
                    poller.register(connection.getSocket(), Poller.POLLIN);
                }
                checkMessages(poller);
                for (var connection : connections.values()) {
                    poller.unregister(connection.getSocket());
                }
            }
        }
    }

    private void checkMessages(Poller poller) {
        var rc = poller.poll(TIMEOUT);
        if (rc == 0) {
            return;
        }
        for (int i = 0; i < poller.getSize(); i++) {
            if (poller.pollin(i)) {
                var rawMsg = ZMsg.recvMsg(poller.getSocket(i));
                if (rawMsg == null) {
                    isRunning = false; // interrupted
                    return;
                }
                try {
                    if (rawMsg.size() == 2) {
                        // ignore control message
                        // (which are composed of 2 frames)
                        continue;
                    }
                    handle(rawMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
