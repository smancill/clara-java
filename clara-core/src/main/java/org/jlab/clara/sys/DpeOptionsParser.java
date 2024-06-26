/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.util.OptUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Parses the DPE settings from the command line.
 */
class DpeOptionsParser {

    private final OptionSpec<String> dpeHost;
    private final OptionSpec<Integer> dpePort;
    private final OptionSpec<String> feHost;
    private final OptionSpec<Integer> fePort;

    private final OptionSpec<String> session;

    private final OptionSpec<Integer> poolSize;
    private final OptionSpec<Integer> maxCores;
    private final OptionSpec<Long> reportPeriod;

    private final OptionSpec<Integer> maxSockets;
    private final OptionSpec<Integer> ioThreads;

    private final OptionSpec<String> description;

    private OptionParser parser;
    private OptionSet options;

    private boolean fe;
    private ProxyAddress localAddress;
    private ProxyAddress frontEndAddress;


    DpeOptionsParser() {
        parser = new OptionParser();

        parser.acceptsAll(List.of("fe", "frontend"));

        dpeHost = parser.acceptsAll(List.of("host")).withRequiredArg();
        dpePort = parser.acceptsAll(List.of("port"))
                        .withRequiredArg().ofType(Integer.class);

        feHost = parser.acceptsAll(List.of("fe-host")).withRequiredArg();
        fePort = parser.acceptsAll(List.of("fe-port"))
                       .withRequiredArg().ofType(Integer.class);

        session = parser.accepts("session").withRequiredArg();

        poolSize = parser.accepts("poolsize").withRequiredArg().ofType(Integer.class);
        maxCores = parser.accepts("max-cores").withRequiredArg().ofType(Integer.class);
        reportPeriod = parser.accepts("report").withRequiredArg().ofType(Long.class);

        maxSockets = parser.accepts("max-sockets").withRequiredArg().ofType(Integer.class);
        ioThreads = parser.accepts("io-threads").withRequiredArg().ofType(Integer.class);

        description = parser.accepts("description").withRequiredArg();

        parser.acceptsAll(List.of("version"));
        parser.acceptsAll(List.of("h", "help")).forHelp();
    }

    public void parse(String[] args) {
        try {
            options = parser.parse(args);

            // Act as front-end by default but if feHost or fePort are passed
            // act as a worker DPE with remote front-end
            fe = !options.has(feHost) && !options.has(fePort);

            // Get local DPE address
            var localHost = valueOf(dpeHost, Dpe.DEFAULT_PROXY_HOST);
            var localPort = valueOf(dpePort, Dpe.DEFAULT_PROXY_PORT);
            localAddress = new ProxyAddress(localHost, localPort);

            if (fe) {
                // Get local FE address (use same local DPE address)
                frontEndAddress = localAddress;
            } else {
                // Get remote FE address
                if (!options.has(feHost)) {
                    error("The remote front-end host is required");
                }
                var host = options.valueOf(feHost);
                var port = valueOf(fePort, Dpe.DEFAULT_PROXY_PORT);
                frontEndAddress = new ProxyAddress(host, port);
            }

        } catch (OptionException e) {
            throw new DpeOptionsException(e);
        }
    }

    private void error(String msg) {
        throw new DpeOptionsException(msg);
    }

    private <V> V valueOf(OptionSpec<V> spec, V defaultValue) {
        try {
            if (options.has(spec)) {
                return options.valueOf(spec);
            }
            return defaultValue;
        } catch (OptionException e) {
            throw new DpeOptionsException(e);
        }
    }

    public ProxyAddress localAddress() {
        return localAddress;
    }

    public ProxyAddress frontEnd() {
        return frontEndAddress;
    }

    public String session() {
        return valueOf(session, "");
    }

    public DpeConfig config() {
        int dpeMaxCores = valueOf(maxCores, Dpe.DEFAULT_MAX_CORES);
        int dpePoolSize = valueOf(poolSize, DpeConfig.calculatePoolSize(dpeMaxCores));

        long defaultPeriodSeconds = TimeUnit.MILLISECONDS.toSeconds(Dpe.DEFAULT_REPORT_PERIOD);
        long reportPeriodSeconds = valueOf(reportPeriod, defaultPeriodSeconds);
        long dpeReportPeriod = TimeUnit.SECONDS.toMillis(reportPeriodSeconds);

        return new DpeConfig(dpeMaxCores, dpePoolSize, dpeReportPeriod);
    }

    public int maxSockets() {
        return valueOf(maxSockets, Dpe.DEFAULT_MAX_SOCKETS);
    }

    public int ioThreads() {
        return valueOf(ioThreads, Dpe.DEFAULT_IO_THREADS);
    }

    public String description() {
        return valueOf(description, "");
    }

    public boolean isFrontEnd() {
        return fe;
    }

    public boolean hasVersion() {
        return options.has("version");
    }

    public boolean hasHelp() {
        return options.has("help");
    }

    public String usage() {
        return String.format("usage: j_dpe [options]%n%n  Options:%n")
             + OptUtils.optionHelp(dpeHost, "hostname", "use given host for this DPE")
             + OptUtils.optionHelp(dpePort, "port", "use given port for this DPE")
             + OptUtils.optionHelp(feHost, "hostname", "the host used by the remote front-end")
             + OptUtils.optionHelp(fePort, "port", "the port used by the remote front-end")
             + OptUtils.optionHelp(session, "id", "the session ID of this DPE")
             + OptUtils.optionHelp(description, "string", "a short description of this DPE")
             + String.format("%n  Config options:%n")
             + OptUtils.optionHelp(poolSize, "size", "size of thread pool to handle requests")
             + OptUtils.optionHelp(maxCores, "cores", "how many cores can be used by a service")
             + OptUtils.optionHelp(reportPeriod, "seconds", "the period to publish reports")
             + String.format("%n  Advanced options:%n")
             + OptUtils.optionHelp(maxSockets, "sockets", "maximum number of allowed ZMQ sockets")
             + OptUtils.optionHelp(ioThreads, "threads", "size of ZMQ thread pool to handle I/O");
    }

    static class DpeOptionsException extends RuntimeException {

        DpeOptionsException(String message) {
            super(message);
        }

        DpeOptionsException(String message, Throwable cause) {
            super(message, cause);
        }

        DpeOptionsException(Throwable cause) {
            super(cause);
        }
    }
}
