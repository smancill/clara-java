/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.jlab.clara.base.BaseOrchestrator;
import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.DataRingAddress;
import org.jlab.clara.base.DataRingTopic;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.std.orchestrators.CallbackInfo.RingListener;
import org.jlab.clara.util.EnvUtils;
import org.jlab.clara.util.OptUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Listen to reports published to the Clara data-ring.
 */
public class MonitorOrchestrator implements AutoCloseable {

    // TODO: use Topic.ANY without adding a dependency to clara-msg
    private static final String ANY = "*";

    private final BaseOrchestrator orchestrator;

    public static void main(String[] args) throws Exception {
        var cl = new CommandLineBuilder();
        try {
            cl.parse(args);
            if (cl.hasHelp()) {
                System.out.println(cl.usage());
                System.exit(0);
            }
            var parser = new OrchestratorConfigParser(cl.setupFile());
            var ringCallbacks = parser.parseDataRingCallbacks();
            if (ringCallbacks.isEmpty()) {
                System.err.println("Error: no callbacks found in " + cl.setupFile());
                System.exit(1);
            }

            var monitor = new MonitorOrchestrator();
            var handlers = new ConcurrentLinkedQueue<AutoCloseable>();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                monitor.close();
                closeHandlers(handlers);
            }));

            var ringListener = asListener(monitor);
            for (var callback : ringCallbacks) {
                var handler = callback.loadCallback(ringListener);
                handlers.add(handler);
            }
            Logging.info("Waiting reports...");
        } catch (CommandLineException e) {
            System.err.println("error: " + e.getMessage());
            System.err.println(cl.usage());
            System.exit(1);
        } catch (OrchestratorConfigException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (ClaraException e) {
            System.err.println("Error: " + e.getMessage());
            Logging.error("Exiting...");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Create a new monitor orchestrator to a default Clara data-ring in the localhost.
     */
    public MonitorOrchestrator() {
        this(getDataRing());
    }


    /**
     * Create a new monitor orchestrator to the given Clara data-ring.
     *
     * @param address the address of the Clara data-ring
     */
    public MonitorOrchestrator(DataRingAddress address) {
        orchestrator = new BaseOrchestrator(getRingAsDpe(address), getPoolSize());
    }


    private static DataRingAddress getDataRing() {
        return EnvUtils.get(ClaraConstants.ENV_MONITOR_FE)
                .map(DpeName::new)
                .map(DataRingAddress::new)
                .orElseGet(() -> new DataRingAddress(ClaraUtil.localhost()));
    }


    private static int getPoolSize() {
        var cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 8) {
            return 12;
        }
        if (cores <= 16) {
            return 24;
        }
        return 32;
    }


    private static DpeName getRingAsDpe(DataRingAddress address) {
        return new DpeName(address.host(), address.port(), ClaraLang.JAVA);
    }


    /**
     * Listen DPE reports.
     *
     * @param handler DPE report handler
     * @throws ClaraException if the subscription could not be started
     */
    public void listenDpeReports(DpeReportHandler handler)
            throws ClaraException {
        orchestrator.listen()
                .dpeReport()
                .parseJson()
                .start(handler::handleReport);
        Logging.info("Subscribed to all DPE reports");
    }


    /**
     * Listen DPE reports.
     *
     * @param session DPE session
     * @param handler DPE report handler
     * @throws ClaraException if the subscription could not be started
     */
    public void listenDpeReports(String session, DpeReportHandler handler)
            throws ClaraException {
        orchestrator.listen()
                .dpeReport(session)
                .parseJson()
                .start(handler::handleReport);
        Logging.info("Subscribed to DPE reports with session = \"%s\"", session);
    }


    /**
     * Listen engine reports.
     *
     * @param handler data-ring event handler
     * @throws ClaraException if the subscription could not be started
     */
    public void listenEngineReports(EngineReportHandler handler)
            throws ClaraException {
        orchestrator.listen()
                .dataRing()
                .withDataTypes(handler.dataTypes())
                .start(handler::handleEvent);
        Logging.info("Subscribed to all service reports");
    }


    /**
     * Listen engine reports.
     *
     * @param ringTopic data-ring topic
     * @param handler data-ring event handler
     * @throws ClaraException if the subscription could not be started
     */
    public void listenEngineReports(DataRingTopic ringTopic, EngineReportHandler handler)
            throws ClaraException {
        orchestrator.listen()
                .dataRing(ringTopic)
                .withDataTypes(handler.dataTypes())
                .start(handler::handleEvent);
        Logging.info("Subscribed to service reports with %s", getTopicLog(ringTopic));
    }


    private String getTopicLog(DataRingTopic topic) {
        var sb = new StringBuilder();
        sb.append("state = ").append('"').append(topic.state()).append('"');
        if (!topic.session().equals(ANY)) {
            sb.append("  session = ").append('"').append(topic.session()).append('"');
        }
        if (!topic.engine().equals(ANY)) {
            sb.append("  engine = ").append('"').append(topic.engine()).append('"');
        }
        return sb.toString();
    }


    @Override
    public void close() {
        orchestrator.close();
    }


    private static RingListener asListener(MonitorOrchestrator orchestrator) {
        return new RingListener() {

            @Override
            public void listen(DataRingTopic topic, EngineReportHandler handler)
                    throws ClaraException {
                if (topic == null) {
                    orchestrator.listenEngineReports(handler);
                } else {
                    orchestrator.listenEngineReports(topic, handler);
                }
            }

            @Override
            public void listen(String session, DpeReportHandler handler) throws ClaraException {
                if (session == null) {
                    orchestrator.listenDpeReports(handler);
                } else {
                    orchestrator.listenDpeReports(session, handler);
                }
            }
        };
    }


    private static void closeHandlers(Queue<AutoCloseable> handlers) {
        for (var handler : handlers) {
            try {
                handler.close();
            } catch (Exception e) {
                Logging.error("could not close handler: " + e.getMessage());
            }
        }
    }


    static class CommandLineException extends RuntimeException {

        CommandLineException(String message) {
            super(message);
        }

        CommandLineException(Throwable cause) {
            super(cause);
        }

        @Override
        public String getMessage() {
            Throwable cause = getCause();
            if (cause != null) {
                return cause.getMessage();
            }
            return super.getMessage();
        }
    }


    static class CommandLineBuilder {

        private final OptionSpec<Path> arguments;

        private final OptionParser parser;
        private OptionSet options;

        CommandLineBuilder() {
            parser = new OptionParser();
            arguments = parser.nonOptions().withValuesConvertedBy(OptUtils.PATH_CONVERTER);

            parser.acceptsAll(List.of("h", "help")).forHelp();
        }

        public void parse(String[] args) {
            try {
                options = parser.parse(args);
                if (hasHelp()) {
                    return;
                }
                var numArgs = options.nonOptionArguments().size();
                if (numArgs == 0) {
                    throw new CommandLineException("missing arguments");
                }
                if (numArgs > 1) {
                    throw new CommandLineException("invalid number of arguments");
                }
            } catch (OptionException e) {
                throw new CommandLineException(e);
            }
        }

        public boolean hasHelp() {
            return options.has("help");
        }

        public Path setupFile() {
            var argsList = arguments.values(options);
            return argsList.get(0);
        }

        public String usage() {
            var wrapper = "clara-monitor";
            return String.format("usage: %s [options] <setup.yaml>", wrapper);
        }
    }
}
