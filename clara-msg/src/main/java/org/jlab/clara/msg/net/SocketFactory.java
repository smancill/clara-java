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
            int maxSockets = Context.getInstance().getMaxSockets();
            throw new ClaraMsgException("reached maximum number of sockets: " + maxSockets);
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
