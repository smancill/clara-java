/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

/**
 * Creates and connects new 0MQ sockets.
 */
public class SocketFactory {

    private final ZMQ.Context ctx;

    /**
     * Creates a new socket factory.
     *
     * @param ctx the ZMQ context to be used by sockets
     */
    public SocketFactory(ZMQ.Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates a new 0MQ socket.
     *
     * @param type the type of the socket
     * @return a new socket of the given type
     * @throws ClaraMsgException if the context cannot create more sockets
     */
    public ZMQ.Socket createSocket(SocketType type) throws ClaraMsgException {
        try {
            ZMQ.Socket socket = ctx.socket(type);
            socket.setRcvHWM(0);
            socket.setSndHWM(0);
            return socket;
        } catch (IllegalStateException e) {
            throw new ClaraMsgException("reached maximum number of sockets: " + ctx.getMaxSockets());
        }
    }

    /**
     * Binds the given socket to the given port.
     *
     * @param socket the socket to bind
     * @param port the listening port
     * @throws ClaraMsgException if the port is in use
     */
    public void bindSocket(ZMQ.Socket socket, int port) throws ClaraMsgException {
        try {
            socket.bind("tcp://*:" + port);
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.EADDRINUSE.getCode()) {
                throw new ClaraMsgException("could not bind to port " + port);
            }
            throw e;
        }
    }

    /**
     * Connects the given socket to the given port.
     *
     * @param socket the socket to be connected
     * @param host the address of the host
     * @param port the connection port
     * @throws ClaraMsgException if no 0MQ I/O threads are available for the connection
     */
    public void connectSocket(ZMQ.Socket socket, String host, int port) throws ClaraMsgException {
        try {
            socket.connect("tcp://" + host + ":" + port);
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.EMTHREAD.getCode()) {
                throw new ClaraMsgException("no I/O thread available", e);
            }
        }
    }

    /**
     * Sets the linger period for the given socket.
     *
     * @param socket the socket to be configured
     * @param linger the linger period, in milliseconds
     */
    public void setLinger(ZMQ.Socket socket, int linger) {
        try {
            socket.setLinger(linger);
        } catch (ZError.CtxTerminatedException e) {
            // ignore
        }
    }

    /**
     * Closes the given socket.
     *
     * @param socket the socket to be closed, it can be null
     */
    public void closeQuietly(ZMQ.Socket socket) {
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Returns the ZMQ context used by this factory.
     *
     * @return the context
     */
    public ZMQ.Context context() {
        return ctx;
    }
}
