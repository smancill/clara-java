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
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.zeromq.SocketType;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

public abstract class ProxyDriver {

    protected final ProxyAddress address;
    protected final Socket socket;

    private final SocketFactory factory;


    public static ProxyDriver publisher(ProxyAddress address, SocketFactory factory)
            throws ClaraMsgException {
        return new Pub(address, factory);
    }

    public static ProxyDriver subscriber(ProxyAddress address, SocketFactory factory)
            throws ClaraMsgException {
        return new Sub(address, factory);
    }


    private ProxyDriver(SocketType type, ProxyAddress address, SocketFactory factory)
            throws ClaraMsgException {
        this.address = address;
        this.socket = factory.createSocket(type);
        this.factory = factory;
    }

    public void connect() throws ClaraMsgException {
        factory.connectSocket(socket, address.host(), getPort());
    }

    abstract int getPort();

    public boolean checkConnection(long timeout) throws ClaraMsgException {
        String identity = IdentityGenerator.getCtrlId();
        Socket ctrlSocket = createControlSocket(identity);
        try (Poller poller = factory.context().poller(1)) {
            poller.register(ctrlSocket, Poller.POLLIN);

            long pollTimeout = timeout < 100 ? timeout : 100;
            long totalTime = 0;
            while (totalTime < timeout) {
                try {
                    ZMsg ctrlMsg = new ZMsg();
                    ctrlMsg.add(CtrlConstants.CTRL_TOPIC + ":con");
                    ctrlMsg.add(CtrlConstants.CTRL_CONNECT);
                    ctrlMsg.add(identity);
                    ctrlMsg.send(getSocket());

                    poller.poll(pollTimeout);
                    if (poller.pollin(0)) {
                        ZMsg replyMsg = ZMsg.recvMsg(ctrlSocket);
                        if (replyMsg.size() == 1) {
                            ZFrame typeFrame = replyMsg.pop();
                            String type = new String(typeFrame.getData());
                            if (type.equals(CtrlConstants.CTRL_CONNECT)) {
                                return true;
                            }
                        }
                    }
                    totalTime += pollTimeout;
                } catch (ZMQException e) {
                    e.printStackTrace();
                }
            }
            return false;
        } finally {
            factory.closeQuietly(ctrlSocket);
        }
    }

    public void subscribe(String topic) {
        socket.subscribe(topic.getBytes());
    }

    public boolean checkSubscription(String topic, long timeout) throws ClaraMsgException {
        Socket pubSocket = createPubSocket();
        try (Poller poller = factory.context().poller(1)) {
            poller.register(getSocket(), Poller.POLLIN);

            long pollTimeout = timeout < 100 ? timeout : 100;
            long totalTime = 0;
            while (totalTime < timeout) {
                try {
                    ZMsg ctrlMsg = new ZMsg();
                    ctrlMsg.add(CtrlConstants.CTRL_TOPIC + ":sub");
                    ctrlMsg.add(CtrlConstants.CTRL_SUBSCRIBE);
                    ctrlMsg.add(topic);
                    ctrlMsg.send(pubSocket);

                    poller.poll(pollTimeout);
                    if (poller.pollin(0)) {
                        ZMsg replyMsg = ZMsg.recvMsg(getSocket());
                        if (replyMsg.size() == 2) {
                            ZFrame idFrame = replyMsg.pop();
                            ZFrame typeFrame = replyMsg.pop();

                            String id = new String(idFrame.getData());
                            String type = new String(typeFrame.getData());
                            if (id.equals(topic) && type.equals(CtrlConstants.CTRL_SUBSCRIBE)) {
                                return true;
                            }
                        }
                    }
                    totalTime += pollTimeout;
                } catch (ZMQException e) {
                    e.printStackTrace();
                }
            }
            return false;
        } finally {
            factory.closeQuietly(pubSocket);
        }
    }

    public void unsubscribe(String topic) {
        socket.unsubscribe(topic.getBytes());
    }

    public void send(ZMsg msg) {
        msg.send(socket);
    }

    public ZMsg recv() {
        return ZMsg.recvMsg(socket);
    }

    public void close() {
        factory.closeQuietly(socket);
    }

    public void close(int linger) {
        factory.setLinger(socket, linger);
        factory.closeQuietly(socket);
    }

    public ProxyAddress getAddress() {
        return address;
    }

    public Socket getSocket() {
        return socket;
    }

    public Context getContext() {
        return factory.context();
    }


    static class Pub extends ProxyDriver {

        Pub(ProxyAddress address, SocketFactory factory) throws ClaraMsgException {
            super(SocketType.PUB, address, factory);
        }

        @Override
        int getPort() {
            return address.pubPort();
        }

        @Override
        public boolean checkSubscription(String topic, long timeout) {
            throw new UnsupportedOperationException("PUB socket cannot subscribe");
        }
    }


    static class Sub extends ProxyDriver {

        Sub(ProxyAddress address, SocketFactory factory) throws ClaraMsgException {
            super(SocketType.SUB, address, factory);
        }

        @Override
        int getPort() {
            return address.subPort();
        }

        @Override
        public boolean checkConnection(long timeout) {
            return true;
        }
    }


    private Socket createControlSocket(String identity) throws ClaraMsgException {
        Socket socket = factory.createSocket(SocketType.DEALER);
        try {
            socket.setIdentity(identity.getBytes());
            factory.connectSocket(socket, address.host(), address.pubPort() + 2);
            return socket;
        } catch (Exception e) {
            factory.closeQuietly(socket);
            throw e;
        }
    }

    private Socket createPubSocket() throws ClaraMsgException {
        Socket socket = factory.createSocket(SocketType.PUB);
        try {
            factory.connectSocket(socket, address.host(), address.pubPort());
            return socket;
        } catch (Exception e) {
            factory.closeQuietly(socket);
            throw e;
        }
    }
}
