/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine;

import org.jlab.clara.base.core.DataUtil.EngineDataAccessor;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;

/**
 * Engine data passed in/out to the service engine.
 */
public class EngineData {

    private Object data;
    private MetaData.Builder metadata;

    /**
     * Creates an empty engine data object.
     * The user-data must be set with {@link #setData}.
     */
    public EngineData() {
        this.metadata = MetaData.newBuilder();
    }

    private EngineData(Object data, MetaData.Builder metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    private MetaData.Builder getMetadata() {
        return metadata;
    }

    /**
     * Gets the user-data.
     * The value must be cast to its proper Java class.
     * Use {@link #getMimeType} to get information about the type of the data.
     *
     * @return the user-data or null if not set
     */
    public Object getData() {
        return data;
    }

    /**
     * Gets the mime-type string for the user-data.
     * The mime-type acts as a clue for which Java class must be used when
     * casting the user-data.
     *
     * @return a string with the mime-type or empty if not set
     */
    public String getMimeType() {
        return metadata.getDataType();
    }

    /**
     * Sets a new string data for this object.
     *
     * @param data the string with the user-data
     */
    public void setData(String data) {
        setData(EngineDataType.STRING.mimeType(), data);
    }

    /**
     * Sets a new user-data for this object.
     * <p>
     * The Java class of the user-data must correspond to the given
     * {@link EngineDataType dataType}, which must also be supported by the
     * orchestrator or engine, to serialize the data if necessary.
     *
     * @param dataType the Clara data-type for the user-data
     * @param data the object with the user-data
     */
    public void setData(EngineDataType dataType, Object data) {
        setData(dataType.mimeType(), data);
    }

    /**
     * Sets a new user-data for this object.
     * <p>
     * The mime-type string and the Java class of the user-data
     * must correspond to an existing {@link EngineDataType} supported by
     * the orchestrator or engine, to serialize the data if necessary.
     *
     * @param mimeType the mime-type for the user-data
     * @param data the object with the user-data
     */
    public void setData(String mimeType, Object data) {
        this.data = data;
        this.metadata.setDataType(mimeType);
    }

    /**
     * Gets the description of the data and/or status.
     * Each engine can set a description to provide extra information about the
     * result of a request.
     *
     * @return a string with the description or empty if not set
     */
    public String getDescription() {
        return metadata.getDescription();
    }

    /**
     * Sets a description for the data.
     * It can provide further details about the user-data,
     * or the cause for the error status, etc.
     *
     * @param description a description for the data
     */
    public void setDescription(String description) {
        metadata.setDescription(description);
    }

    /**
     * Gets the status for the data.
     * Useful to check if the result of an engine execution
     * was a warning or an error.
     * <p>
     * The default status is always {@link EngineStatus#INFO info}.
     *
     * @return the status of the data
     */
    public EngineStatus getStatus() {
        return switch (metadata.getStatus()) {
            case INFO -> EngineStatus.INFO;
            case WARNING -> EngineStatus.WARNING;
            case ERROR -> EngineStatus.ERROR;
        };
    }

    /**
     * Gets the optional severity for the status of the data.
     * <p>
     * The default severity for a new status is 1.
     *
     * @return the value of the severity (engine-specific)
     */
    public int getStatusSeverity() {
        return metadata.getSeverityId();
    }

    /**
     * Sets a new status for this data.
     * Useful to set the result of an engine execution request
     * as a warning or error.
     *
     * @param status the new status
     */
    public void setStatus(EngineStatus status) {
        setStatus(status, 1);
    }

    /**
     * Sets a new status for this data, with custom severity.
     * Useful to set the result of an engine execution request
     * as a warning or error.
     * <p>
     * Each engine defines the interpretation of the severity values.
     *
     * @param status the new status
     * @param severity the custom severity as a positive integer
     */
    public void setStatus(EngineStatus status, int severity) {
        if (severity <= 0) {
            throw new IllegalArgumentException("severity must be positive: " + severity);
        }
        switch (status) {
            case INFO -> metadata.setStatus(MetaData.Status.INFO);
            case WARNING -> metadata.setStatus(MetaData.Status.WARNING);
            case ERROR -> metadata.setStatus(MetaData.Status.ERROR);
            default -> throw new IllegalStateException("Unknown status " + status);
        }
        metadata.setSeverityId(severity);
    }

    /**
     * Gets the canonical name of the engine that returned this data, if any.
     * <p>
     * This value will be set only when the data is the result of an engine
     * request, and it can be obtained by the next service in a composition or
     * by the monitoring orchestrators.
     *
     * @return the canonical name of the engine or empty
     *         if not created by an engine
     */
    public String getEngineName() {
        return metadata.getAuthor();
    }

    /**
     * Gets the version of the engine that returned this data, if any.
     * <p>
     * This value will be set only when the data is the result of an engine
     * request, and it can be obtained by the next service in a composition or
     * by the monitoring orchestrators.
     *
     * @return the version of the engine or empty if not created by an engine
     */
    public String getEngineVersion() {
        return metadata.getVersion();
    }

    /**
     * Gets the ID for the request this data is part of.
     * <p>
     * This value should be set by the orchestrator or data-source engine
     * sending a new request (a single request to an engine, or the first
     * request of a new application composition request).
     *
     * @return the ID of the request
     */
    public int getCommunicationId() {
        return metadata.getCommunicationId();
    }

    /**
     * Sets an ID for the request this data is part of.
     * <p>
     * Orchestrators and data-source engines should set a proper communication
     * ID to identify unique requests.
     * Engines should avoid changing the communication ID in the middle of an
     * application composition request.
     *
     * @param value the communication ID
     */
    public void setCommunicationId(int value) {
        metadata.setCommunicationId(value);
    }

    /**
     * Gets the composition for the request this data is part of.
     * <p>
     * The composition is set when an orchestrator publishes the data, i.e.,
     * only the engines receiving the data (or the orchestrators receiving the
     * results) can observe its value.
     *
     * @return a string with the composition
     */
    public String getComposition() {
        return metadata.getComposition();
    }

    /**
     * Gets the state of the execution result set by the engine, if any.
     * <p>
     * This value will be set only when the data is the result of an engine,
     * and it is used in composition requests to route the data to the next
     * service in the composition.
     *
     * @return the state set by the engine or empty
     */
    public String getExecutionState() {
        return metadata.getSenderState();
    }

    /**
     * Sets an execution state for this data.
     * Engines should set a state for the result of processing a specific
     * request, and it should be one of the defined states by the engine.
     * <p>
     * States define the flow the output data in a composition request,
     * where results are routed to the next services based on its state.
     *
     * @param state the state for the data
     */
    public void setExecutionState(String state) {
        metadata.setSenderState(state);
    }

    /**
     * Gets the time that took the engine to process a request and return this data.
     * If set, this is the time spent by a successful request to the
     * {@link Engine#configure configure} or {@link Engine#execute execute}
     * method of the engine that created this data.
     * <p>
     * This value will be set only when the data is the result of an engine
     * request, and it can be obtained by the next service in a composition or
     * by the monitoring orchestrators.
     *
     * @return the execution time of the request or 0 if not a result of a request
     */
    public long getExecutionTime() {
        return metadata.getExecutionTime();
    }

    @Override
    public String toString() {
        return "EngineData["
                + "mime-type=" + getMimeType()
                + " data=" + getMimeType()
                + " status=" + getStatus()
                + " description" + getDescription()
                + ']';
    }


    static {
        EngineDataAccessor.setDefault(new Accessor());
    }

    private static class Accessor extends EngineDataAccessor {

        @Override
        protected MetaData.Builder getMetadata(EngineData data) {
            return data.getMetadata();
        }

        @Override
        protected EngineData build(Object data, MetaData.Builder metadata) {
            return new EngineData(data, metadata);
        }
    }
}
