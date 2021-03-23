/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract reader service that reads events from the configured input file.
 *
 * @param <Reader> the class for the user-defined reader of the given data-type
 */
public abstract class AbstractEventReaderService<Reader> extends AbstractService {

    private static final String CONF_ACTION = "action";
    private static final String CONF_FILENAME = "file";

    private static final String CONF_ACTION_OPEN = "open";
    private static final String CONF_ACTION_CLOSE = "close";

    private static final String CONF_EVENTS_SKIP = "skip";
    private static final String CONF_EVENTS_MAX = "max";

    private static final String REQUEST_NEXT = "next";
    private static final String REQUEST_NEXT_REC = "next-rec";
    private static final String REQUEST_ORDER = "order";
    private static final String REQUEST_COUNT = "count";

    private static final String NO_NAME = "";
    private static final String NO_FILE = "No open file";
    private static final String END_OF_FILE = "End of file";

    private static final int EOF_NOT_FROM_WRITER = 0;
    private static final int EOF_WAITING_REC = -1;

    private String fileName = NO_NAME;
    private String openError = NO_FILE;

    /** The reader object. */
    protected Reader reader;
    private final Object readerLock = new Object();

    private int currentEvent;
    private int lastEvent;
    private int eventCount;

    private Set<Integer> processingEvents = new HashSet<>();
    private int eofRequestCount;


    @Override
    public EngineData configure(EngineData input) {
        final var startTime = System.currentTimeMillis();
        var mimeType = input.getMimeType();
        if (mimeType.equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            var data = (String) input.getData();
            var config = new JSONObject(data);
            if (config.has(CONF_ACTION) && config.has(CONF_FILENAME)) {
                var action = config.getString(CONF_ACTION);
                if (action.equals(CONF_ACTION_OPEN)) {
                    openFile(config);
                } else if (action.equals(CONF_ACTION_CLOSE)) {
                    closeFile(config);
                } else {
                    logger.error("config: wrong '{}' parameter value = {}", CONF_ACTION, action);
                }
            } else {
                logger.error("config: missing '{}' or '{}' parameters", CONF_ACTION, CONF_FILENAME);
            }
        } else {
            logger.error("config: wrong mime-type {}", mimeType);
        }
        logger.info("config time: {} [ms]", System.currentTimeMillis() - startTime);
        return null;
    }


    private void openFile(JSONObject config) {
        synchronized (readerLock) {
            if (reader != null) {
                closeFile();
            }
            fileName = config.getString(CONF_FILENAME);
            logger.info("request to open file {}", fileName);
            try {
                reader = createReader(Path.of(fileName), config);
                setLimits(config);
                logger.info("opened file {}", fileName);
            } catch (EventReaderException e) {
                logger.error("could not open file {}", fileName, e);
                fileName = null;
            }
        }
    }


    private void setLimits(JSONObject config) throws EventReaderException {
        eventCount = readEventCount();
        var skipEvents = getValue(config, CONF_EVENTS_SKIP, 0, 0, eventCount);
        if (skipEvents != 0) {
            logger.info("config: skip first {} events", skipEvents);
        }
        currentEvent = skipEvents;

        var remEvents = eventCount - skipEvents;
        var maxEvents = getValue(config, CONF_EVENTS_MAX, remEvents, 0, remEvents);
        if (maxEvents != remEvents) {
            logger.info("config: read {} events%n", maxEvents);
        }
        lastEvent = skipEvents + maxEvents;

        processingEvents.clear();
        eofRequestCount = 0;
    }


    private int getValue(JSONObject config, String key, int defaultVal, int minVal, int maxVal) {
        if (config.has(key)) {
            try {
                var value = config.getInt(key);
                if (value >= minVal && value <= maxVal) {
                    return value;
                }
                logger.error("config: invalid value for '{}': {}", key, value);
            } catch (JSONException e) {
                logger.error("config: {}", e.getMessage());
            }
        }
        return defaultVal;
    }


    private void closeFile(JSONObject config) {
        synchronized (readerLock) {
            fileName = config.getString(CONF_FILENAME);
            logger.info("request to close file {}", fileName);
            if (reader != null) {
                closeFile();
            } else {
                logger.error("file {} not open", fileName);
            }
            openError = NO_FILE;
            fileName = null;
        }
    }


    private void closeFile() {
        closeReader();
        reader = null;
        logger.info("closed file {}", fileName);
    }


    /**
     * Creates a new reader and opens the given input file.
     *
     * @param file the path to the input file
     * @param opts extra options for the reader
     * @return a new reader ready to read events from the input file
     * @throws EventReaderException if the reader could not be created
     */
    protected abstract Reader createReader(Path file, JSONObject opts) throws EventReaderException;

    /**
     * Closes the reader and its input file.
     */
    protected abstract void closeReader();


    @Override
    public EngineData execute(EngineData input) {
        var output = new EngineData();

        var mimeType = input.getMimeType();
        if (mimeType.equalsIgnoreCase(EngineDataType.STRING.mimeType())) {
            var request = (String) input.getData();
            if (request.equals(REQUEST_NEXT) || request.equals(REQUEST_NEXT_REC)) {
                getNextEvent(input, output);
            } else if (request.equals(REQUEST_ORDER)) {
                logger.info("execute request {}", REQUEST_ORDER);
                getFileByteOrder(output);
            } else if (request.equals(REQUEST_COUNT)) {
                logger.info("execute request {}", REQUEST_COUNT);
                getEventCount(output);
            } else {
                ServiceUtils.setError(output, String.format("Wrong input data = '%s'", request));
            }
        } else {
            var error = String.format("Wrong input type '%s'", mimeType);
            ServiceUtils.setError(output, error);
        }

        return output;
    }


    private boolean isReconstructionRequest(EngineData input) {
        var requestType = (String) input.getData();
        return requestType.equalsIgnoreCase(REQUEST_NEXT_REC);
    }


    private void getNextEvent(EngineData input, EngineData output) {
        synchronized (readerLock) {
            var fromRec = isReconstructionRequest(input);
            if (fromRec) {
                processingEvents.remove(input.getCommunicationId());
            }
            if (reader == null) {
                ServiceUtils.setError(output, openError, 1);
            } else if (currentEvent < lastEvent) {
                returnNextEvent(output);
            } else {
                ServiceUtils.setError(output, END_OF_FILE, 1);
                if (fromRec) {
                    if (processingEvents.isEmpty()) {
                        eofRequestCount++;
                        ServiceUtils.setError(output, END_OF_FILE, eofRequestCount + 1);
                        output.setData(EngineDataType.SFIXED32.mimeType(), eofRequestCount);
                    } else {
                        output.setData(EngineDataType.SFIXED32.mimeType(), EOF_WAITING_REC);
                    }
                } else {
                    output.setData(EngineDataType.SFIXED32.mimeType(), EOF_NOT_FROM_WRITER);
                }
            }
        }
    }


    private void returnNextEvent(EngineData output) {
        try {
            var event = readEvent(currentEvent);
            output.setData(getDataType().toString(), event);
            output.setDescription("data");
            processingEvents.add(currentEvent);
        } catch (EventReaderException e) {
            var error = String.format("Error requesting event %d from file %s%n%n%s",
                                      currentEvent, fileName, ClaraUtil.reportException(e));
            ServiceUtils.setError(output, error, 1);
        } finally {
            output.setCommunicationId(currentEvent);
            currentEvent++;
        }
    }


    private void getFileByteOrder(EngineData output) {
        synchronized (readerLock) {
            if (reader == null) {
                ServiceUtils.setError(output, openError, 1);
            } else {
                try {
                    output.setData(EngineDataType.STRING.mimeType(), readByteOrder().toString());
                    output.setDescription("byte order");
                } catch (EventReaderException e) {
                    var error = String.format("Error requesting byte-order from file %s%n%n%s",
                                              fileName, ClaraUtil.reportException(e));
                    ServiceUtils.setError(output, error, 1);
                }
            }
        }
    }


    private void getEventCount(EngineData output) {
        synchronized (readerLock) {
            if (reader == null) {
                ServiceUtils.setError(output, openError, 1);
            } else {
                output.setData(EngineDataType.SFIXED32.mimeType(), eventCount);
                output.setDescription("event count");
            }
        }
    }


    @Override
    public EngineData executeGroup(Set<EngineData> inputs) {
        return null;
    }


    /**
     * Gets the total number of events that can be read from the input file.
     *
     * @return the total number of events in the file
     * @throws EventReaderException if the file could not be read
     */
    protected abstract int readEventCount() throws EventReaderException;

    /**
     * Gets the byte order of the events stored in the input file.
     *
     * @return the byte order of the events in the file
     * @throws EventReaderException if the file could not be read
     */
    protected abstract ByteOrder readByteOrder() throws EventReaderException;

    /**
     * Reads an event from the input file.
     * The event should be a Java object with the same type as the one defined
     * by the Clara engine data-type returned by {@link #getDataType()}.
     *
     * @param eventNumber the index of the event in the file (starts from zero)
     * @return the read event as a Java object
     * @throws EventReaderException if the file could not be read
     */
    protected abstract Object readEvent(int eventNumber) throws EventReaderException;

    /**
     * Gets the Clara engine data-type for the type of the events.
     * The data-type will be used to serialize the events when the engine data
     * result needs to be sent to services over the network.
     *
     * @return the data-type of the events
     */
    protected abstract EngineDataType getDataType();


    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(
                EngineDataType.JSON,
                EngineDataType.STRING);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ClaraUtil.buildDataTypes(
                getDataType(),
                EngineDataType.STRING,
                EngineDataType.SFIXED32);
    }


    @Override
    public void reset() {
        synchronized (readerLock) {
            if (reader != null) {
                closeFile();
            }
        }
    }


    @Override
    public void destroy() {
        synchronized (readerLock) {
            if (reader != null) {
                closeFile();
            }
        }
    }
}
