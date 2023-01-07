/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.AddressUtils;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.jlab.clara.msg.sys.pubsub.CtrlConstants;
import org.jlab.clara.msg.sys.utils.Environment;
import org.jlab.clara.msg.sys.utils.LogUtils;
import org.jlab.clara.msg.sys.utils.ThreadUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;
import org.zeromq.ZThread.IAttachedRunnable;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main pub/sub proxy server.
 * This is a simple stateless message switch, i.e. a device that forwards
 * messages without inspecting them.
 * <p>
 * The proxy simplifies the dynamic connection problems. All actors
 * (publishers and subscribers) connect to the proxy, instead of to each other.
 * It becomes trivial to add more subscribers or publishers.
 */
public class Proxy {

    private final ProxyAddress addr;
    private final Context ctx;

    private final Thread proxy;
    private final Thread controller;

    private static final Logger LOGGER = LogUtils.getConsoleLogger("Proxy");

    public static void main(String[] args) {
        try {
            var parser = new OptionParser();
            OptionSpec<String> hostSpec = parser.accepts("host")
                    .withRequiredArg()
                    .defaultsTo(AddressUtils.localhost());
            OptionSpec<Integer> portSpec = parser.accepts("port")
                    .withRequiredArg()
                    .ofType(Integer.class)
                    .defaultsTo(ProxyAddress.DEFAULT_PORT);
            parser.accepts("verbose");
            parser.acceptsAll(List.of("h", "help")).forHelp();
            var options = parser.parse(args);

            if (options.has("help")) {
                usage(System.out);
                System.exit(0);
            }

            LOGGER.setLevel(Level.INFO);

            var host = options.valueOf(hostSpec);
            var port = options.valueOf(portSpec);
            var address = new ProxyAddress(host, port);

            var proxy = new Proxy(Context.getInstance(), address);
            if (options.has("verbose")) {
                proxy.verbose();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Context.getInstance().destroy();
                proxy.shutdown();
            }));

            proxy.start();

        } catch (OptionException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void usage(PrintStream out) {
        out.printf("usage: j_proxy [options]%n%n  Options:%n");
        out.printf("  %-22s  %s%n", "-host <hostname>", "use the given hostname");
        out.printf("  %-22s  %s%n", "-port <port>", "use the given port");
        out.printf("  %-22s  %s%n", "-verbose", "print debug information");
    }

    /**
     * Construct a proxy that uses the localhost and
     * {@link ProxyAddress#DEFAULT_PORT default port}.
     *
     * @param context the context to handle the proxy sockets
     * @throws ClaraMsgException if the address is already in use
     */
    public Proxy(Context context) throws ClaraMsgException {
        this(context, new ProxyAddress());
    }

    /**
     * Construct the proxy with the given local address.
     *
     * @param context the context to handle the proxy sockets
     * @param address the local address
     * @throws ClaraMsgException if the address is already in use
     */
    public Proxy(Context context, ProxyAddress address) throws ClaraMsgException {
        ctx = context;
        addr = address;

        ProxyImpl proxyTask = null;
        Controller controllerTask = null;
        try {
            proxyTask = new ProxyImpl();
            controllerTask = new Controller();
            proxy = ThreadUtils.newThread("proxy", proxyTask);
            controller = ThreadUtils.newThread("control", controllerTask);
        } catch (Exception e) {
            if (proxyTask != null) {
                proxyTask.close();
            }
            if (controllerTask != null) {
                controllerTask.close();
            }
            throw e;
        }

        if (Environment.isDefined("CLARA_PROXY_DEBUG")) {
            verbose();
        }
    }

    /**
     * Prints every received message.
     */
    public void verbose() {
        LOGGER.setLevel(Level.FINE);
    }

    /**
     * Starts the proxy.
     */
    public void start() {
        proxy.start();
        controller.start();
    }

    /**
     * Stops the proxy. The context must be destroyed first.
     */
    public void shutdown() {
        try {
            proxy.interrupt();
            controller.interrupt();
            proxy.join();
            controller.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tests if the proxy is running.
     *
     * @return true if the proxy is running, false otherwise
     */
    public boolean isAlive() {
        return proxy.isAlive() && controller.isAlive();
    }

    /**
     * Returns the address of the proxy.
     *
     * @return the address used by the proxy
     */
    public ProxyAddress address() {
        return addr;
    }


    /**
     * The proxy forwarding pub/sub communications.
     */
    private class ProxyImpl implements Runnable {

        final Socket in;
        final Socket out;

        final SocketFactory factory = new SocketFactory(ctx.getContext());

        ProxyImpl() throws ClaraMsgException {
            Socket in = null;
            Socket out = null;
            try {
                in = factory.createSocket(SocketType.XSUB);
                out = factory.createSocket(SocketType.XPUB);
                factory.bindSocket(in, addr.pubPort());
                factory.bindSocket(out, addr.subPort());
            } catch (Exception e) {
                factory.closeQuietly(in);
                factory.closeQuietly(out);
                throw e;
            }
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try (ZContext wrapper = wrapContext()) {
                LOGGER.info("running on host = " + addr.host() + "  port = " + addr.pubPort());
                if (LOGGER.isLoggable(Level.FINE)) {
                    Socket listener = ZThread.fork(wrapper, new Listener());
                    ZMQ.proxy(in, out, listener);
                } else {
                    ZMQ.proxy(in, out, null);
                }
            } catch (Exception e) {
                LOGGER.severe(LogUtils.exceptionReporter(e));
            } finally {
                close();
            }
        }

        private ZContext wrapContext() {
            try {
                // I don't want to expose the ZContext private field in Context
                // since it is never actually used except here.
                Field zctx = ctx.getClass().getDeclaredField("ctx");
                zctx.setAccessible(true);
                return ((ZContext) zctx.get(ctx)).shadow();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            factory.closeQuietly(in);
            factory.closeQuietly(out);
        }
    }


    /**
     * The controller receives and replies synchronization control messages from
     * connections.
     */
    private class Controller implements Runnable {

        final Socket control;
        final Socket publisher;
        final Socket router;

        final SocketFactory factory = new SocketFactory(ctx.getContext());

        Controller() throws ClaraMsgException {
            Socket control = null;
            Socket publisher = null;
            Socket router = null;
            try {
                control = factory.createSocket(SocketType.SUB);
                publisher = factory.createSocket(SocketType.PUB);
                router = factory.createSocket(SocketType.ROUTER);

                factory.connectSocket(control, addr.host(), addr.subPort());
                factory.connectSocket(publisher, addr.host(), addr.pubPort());

                router.setRouterHandover(true);
                factory.bindSocket(router, addr.pubPort() + 2);

                control.subscribe(CtrlConstants.CTRL_TOPIC.getBytes());
            } catch (Exception e) {
                factory.closeQuietly(control);
                factory.closeQuietly(publisher);
                factory.closeQuietly(router);
                throw e;
            }
            this.control = control;
            this.publisher = publisher;
            this.router = router;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        var msg = ZMsg.recvMsg(control);
                        if (msg == null) {
                            break;
                        }
                        processRequest(msg);
                    } catch (ZMQException e) {
                        if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                            break;
                        }
                        LOGGER.warning(LogUtils.exceptionReporter(e));
                    }
                }
            } catch (Exception e) {
                LOGGER.severe(LogUtils.exceptionReporter(e));
            } finally {
                close();
            }
        }

        private void processRequest(ZMsg msg) {
            /* ZFrame topicFrame = */ msg.pop();
            var typeFrame = msg.pop();
            var idFrame = msg.pop();

            var type = new String(typeFrame.getData());
            var id = new String(idFrame.getData());

            switch (type) {
                case CtrlConstants.CTRL_CONNECT: {
                    var ack = new ZMsg();
                    ack.add(id);
                    ack.add(type);
                    ack.send(router);
                    break;
                }
                case CtrlConstants.CTRL_SUBSCRIBE: {
                    var ack = new ZMsg();
                    ack.add(id);
                    ack.add(type);
                    ack.send(publisher);
                    break;
                }
                case CtrlConstants.CTRL_REPLY: {
                    var ack = new ZMsg();
                    ack.add(id);
                    ack.add(type);
                    ack.send(router);
                    break;
                }
                default:
                    LOGGER.warning("unexpected request: " + type);
            }
        }

        public void close() {
            factory.closeQuietly(control);
            factory.closeQuietly(publisher);
            factory.closeQuietly(router);
        }
    }


    /**
     * The listener receives all messages flowing through the proxy,
     * on its pipe.
     */
    private static class Listener implements IAttachedRunnable {
        @Override
        public void run(Object[] args, ZContext ctx, Socket pipe) {
            //  Print everything that arrives at the pipe
            while (true) {
                try {
                    var msg = ZMsg.recvMsg(pipe);
                    if (msg == null) {
                        break;
                    }
                    var frame = msg.pop();
                    var data = frame.getData();
                    if (data[0] == 1) {
                        var topic = new String(data, 1, data.length - 1);
                        LOGGER.fine("subscribed topic = " + topic);
                    } else if (data[0] == 0) {
                        var topic = new String(data, 1, data.length - 1);
                        LOGGER.fine("unsubscribed topic = " + topic);
                    } else {
                        LOGGER.fine("received topic = " + frame);
                    }
                } catch (ZMQException e) {
                    if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                        break;
                    }
                    LOGGER.warning(LogUtils.exceptionReporter(e));
                } catch (Exception e) {
                    LOGGER.severe(LogUtils.exceptionReporter(e));
                    break;
                }
            }
        }
    }
}
