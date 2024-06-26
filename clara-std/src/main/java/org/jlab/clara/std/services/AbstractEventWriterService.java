/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.util.FileUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * An abstract writer service that writes all received events into the
 * configured output file.
 *
 * @param <Writer> the class for the user-defined writer of the given data-type
 */
public abstract class AbstractEventWriterService<Writer> extends AbstractService {

    private static final String CONF_ACTION = "action";
    private static final String CONF_FILENAME = "file";

    private static final String CONF_ACTION_OPEN = "open";
    private static final String CONF_ACTION_CLOSE = "close";
    private static final String CONF_ACTION_SKIP = "skip";

    private static final String OUTPUT_NEXT = "next-rec";
    private static final String EVENT_SKIP = "skip";

    private static final String NO_NAME = "";
    private static final String NO_FILE = "No open file";

    private String fileName = NO_NAME;
    private boolean skipEvents = false;

    private String openError = NO_FILE;
    private int eventCounter;

    /** The writer object. */
    protected Writer writer;
    private final Object writerLock = new Object();


    @Override
    public EngineData configure(EngineData input) {
        final var startTime = System.currentTimeMillis();
        var mimeType = input.getMimeType();
        if (mimeType.equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            var data = (String) input.getData();
            var config = new JSONObject(data);
            if (config.has(CONF_ACTION)) {
                var action = config.getString(CONF_ACTION);
                if (action.equals(CONF_ACTION_OPEN)) {
                    if (config.has(CONF_FILENAME)) {
                        openFile(config);
                    } else {
                        logger.error("config: missing '{}' parameter", CONF_FILENAME);
                    }
                } else if (action.equals(CONF_ACTION_CLOSE)) {
                    if (config.has(CONF_FILENAME)) {
                        closeFile(config);
                    } else {
                        logger.error("config: missing '{}' parameter", CONF_FILENAME);
                    }
                } else if (action.equals(CONF_ACTION_SKIP)) {
                    skipAll();
                } else {
                    logger.error("config: wrong '{}' parameter value = {}", CONF_ACTION, action);
                }
            } else {
                logger.error("config: missing '{}' parameter", CONF_ACTION);
            }
        } else {
            logger.error("config: wrong mime-type '{}'", mimeType);
        }
        logger.info("config time: {} [ms]", System.currentTimeMillis() - startTime);
        return null;
    }


    private void openFile(JSONObject config) {
        synchronized (writerLock) {
            if (writer != null) {
                writeAndClose();
            }
            fileName = config.getString(CONF_FILENAME);
            logger.info("request to open file {}", fileName);
            try {
                var outputFile = Path.of(fileName);
                var outputDir = outputFile.getParent();
                if (outputDir != null) {
                    FileUtils.createDirectories(outputDir);
                }
                writer = createWriter(outputFile, config);
                eventCounter = 0;
                logger.info("opened file {}", fileName);
            } catch (IOException | EventWriterException e) {
                logger.error("could not open file {}", fileName, e);
                fileName = null;
                eventCounter = 0;
            }

            skipEvents = false;
        }
    }


    private void closeFile(JSONObject config) {
        synchronized (writerLock) {
            fileName = config.getString(CONF_FILENAME);
            logger.info("request to close file {}", fileName);
            if (writer != null) {
                writeAndClose();
            } else {
                logger.error("file {} not open", fileName);
            }
            openError = NO_FILE;
            fileName = null;
            eventCounter = 0;
        }
    }


    private void writeAndClose() {
        if (eventCounter > 0) {
            closeWriter();
        }
        logger.info("closed file {}", fileName);
        writer = null;
    }


    private void skipAll() {
        logger.info("request to skip events");
        synchronized (writerLock) {
            if (writer == null) {
                skipEvents = true;
                logger.info("skipping all events");
            } else {
                logger.error("file {} is already open", fileName);
            }
        }
    }


    /**
     * Creates a new writer and opens the given output file.
     *
     * @param file the path to the output file
     * @param opts extra options for the writer
     * @return a new writer ready to writer events to the output file
     * @throws EventWriterException if the writer could not be created
     */
    protected abstract Writer createWriter(Path file, JSONObject opts) throws EventWriterException;

    /**
     * Closes the writer and its output file.
     */
    protected abstract void closeWriter();


    @Override
    public EngineData execute(EngineData input) {
        var output = new EngineData();

        var mimeType = input.getMimeType();
        if (!mimeType.equalsIgnoreCase(getDataType().mimeType())) {
            ServiceUtils.setError(output, String.format("Wrong input type '%s'", mimeType));
            return output;
        }

        if (skipEvents || input.getDescription().equals(EVENT_SKIP)) {
            output.setData(EngineDataType.STRING.mimeType(), OUTPUT_NEXT);
            output.setDescription("event skipped");
            return output;
        }

        synchronized (writerLock) {
            if (writer == null) {
                ServiceUtils.setError(output, openError);
            } else {
                try {
                    writeEvent(input.getData());
                    eventCounter++;
                    output.setData(EngineDataType.STRING.mimeType(), OUTPUT_NEXT);
                    output.setDescription("event saved");

                } catch (EventWriterException e) {
                    var error = String.format("Error saving event to file %s%n%n%s",
                                              fileName, ClaraUtil.reportException(e));
                    ServiceUtils.setError(output, error);
                }
            }
        }

        return output;
    }


    @Override
    public EngineData executeGroup(Set<EngineData> inputs) {
        return null;
    }


    /**
     * Writes an event to the output file.
     * The event should be a Java object with the same type as the one defined
     * by the Clara engine data-type returned by {@link #getDataType()}.
     *
     * @param event the event to be written
     * @throws EventWriterException if the file could not be read
     */
    protected abstract void writeEvent(Object event) throws EventWriterException;

    /**
     * Gets the Clara engine data-type for the type of the events.
     * The data-type will be used to deserialize the events when the engine data
     * is received from services across the network.
     *
     * @return the data-type of the events
     */
    protected abstract EngineDataType getDataType();


    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(getDataType(), EngineDataType.JSON);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ClaraUtil.buildDataTypes(EngineDataType.STRING);
    }

    @Override
    public void reset() {
        synchronized (writerLock) {
            if (writer != null) {
                writeAndClose();
            }
        }
    }

    @Override
    public void destroy() {
        synchronized (writerLock) {
            if (writer != null) {
                writeAndClose();
            }
        }
    }
}
