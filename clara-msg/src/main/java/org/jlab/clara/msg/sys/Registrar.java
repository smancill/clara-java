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

package org.jlab.clara.msg.sys;

import static java.util.Arrays.asList;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.regdis.RegService;
import org.jlab.clara.msg.sys.utils.Environment;
import org.jlab.clara.msg.sys.utils.LogUtils;
import org.jlab.clara.msg.sys.utils.ThreadUtils;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Main registration server.
 * It contains an in-memory database of registered actors.
 * <p>
 * Long-running actors subscribed to a topic of interest, or periodically
 * publishing messages, can register with the registrar service so others
 * actors can discover and communicate with them by using the same registered
 * topic.
 */
public class Registrar {

    private final RegAddress addr;
    private final Thread registrar;

    private static final Logger LOGGER = LogUtils.getConsoleLogger("Registrar");

    public static void main(String[] args) {
        try {
            OptionParser parser = new OptionParser();
            OptionSpec<Integer> portSpec = parser.accepts("port")
                    .withRequiredArg()
                    .ofType(Integer.class)
                    .defaultsTo(RegAddress.DEFAULT_PORT);
            parser.accepts("verbose");
            parser.acceptsAll(asList("h", "help")).forHelp();
            OptionSet options = parser.parse(args);

            if (options.has("help")) {
                usage(System.out);
                System.exit(0);
            }

            LOGGER.setLevel(Level.INFO);

            int port = options.valueOf(portSpec);
            RegAddress address = new RegAddress("localhost", port);

            Registrar registrar = new Registrar(Context.getInstance(), address);
            if (options.has("verbose")) {
                registrar.verbose();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Context.getInstance().destroy();
                registrar.shutdown();
            }));

            registrar.start();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("exiting...");
            System.exit(1);
        }
    }

    private static void usage(PrintStream out) {
        out.printf("usage: j_registrar [options]%n%n  Options:%n");
        out.printf("  %-22s  %s%n", "-port <port>", "use the given port");
        out.printf("  %-22s  %s%n", "-verbose", "print debug information");
    }

    /**
     * Constructs a registrar that uses the localhost and
     * {@link RegAddress#DEFAULT_PORT default port}.
     *
     * @param context the context to handle the registrar sockets
     * @throws ClaraMsgException if the address is already in use
     */
    public Registrar(Context context) throws ClaraMsgException {
        this(context, new RegAddress());
    }

    /**
     * Constructs a registrar that uses the specified address.
     *
     * @param context the context to handle the registrar sockets
     * @param address the address of the registrar service
     * @throws ClaraMsgException if the address is already in use
     */
    public Registrar(Context context, RegAddress address) throws ClaraMsgException {
        addr = address;
        registrar = ThreadUtils.newThread("registration-service",
                                          new RegService(context, address));

        if (Environment.isDefined("CLARA_REGISTRAR_DEBUG")) {
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
     * Starts the registration and discovery servicing thread.
     */
    public void start() {
        registrar.start();
    }

    /**
     * Stops the registration and discovery service.
     * The context must be destroyed first.
     */
    public void shutdown() {
        try {
            registrar.interrupt();
            registrar.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests if the registrar is running.
     *
     * @return true if the registrar is running, false otherwise
     */
    public boolean isAlive() {
        return registrar.isAlive();
    }

    /**
     * Returns the address of the registrar.
     *
     * @return the address used by the registrar
     */
    public RegAddress address() {
        return addr;
    }
}
