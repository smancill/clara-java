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

    protected final ConcurrentMap<ProxyAddress, ProxyDriver> items;

    private final Context context;

    private final Thread pollingThread;
    private volatile boolean isRunning = false;


    public ProxyListener(String name, Context context) {
        this.items = new ConcurrentHashMap<>();
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
        items.values().forEach(ProxyDriver::close);
    }

    protected abstract void handle(ZMsg msg) throws ClaraMsgException;

    @Override
    public void run() {
        try (Poller poller = context.getContext().poller(items.size())) {
            while (isRunning) {
                for (ProxyDriver connection : items.values()) {
                    poller.register(connection.getSocket(), Poller.POLLIN);
                }
                checkMessages(poller);
                for (ProxyDriver connection : items.values()) {
                    poller.unregister(connection.getSocket());
                }
            }
        }
    }

    private void checkMessages(Poller poller) {
        int rc = poller.poll(TIMEOUT);
        if (rc == 0) {
            return;
        }
        for (int i = 0; i < poller.getSize(); i++) {
            if (poller.pollin(i)) {
                ZMsg rawMsg = ZMsg.recvMsg(poller.getSocket(i));
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
