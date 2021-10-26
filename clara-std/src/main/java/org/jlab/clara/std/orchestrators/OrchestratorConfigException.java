/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

/**
 * An error configuring the orchestrator.
 */
public class OrchestratorConfigException extends OrchestratorException {

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     */
    public OrchestratorConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param cause the cause of the exception
     */
    public OrchestratorConfigException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public OrchestratorConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
