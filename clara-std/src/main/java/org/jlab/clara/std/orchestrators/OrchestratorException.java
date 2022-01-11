/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraUtil;

/**
 * An error in the orchestrator.
 */
public class OrchestratorException extends RuntimeException {

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     */
    public OrchestratorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param cause the cause of the exception
     */
    public OrchestratorException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception.
     *
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public OrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessage());
        for (Throwable t : ClaraUtil.getThrowableList(getCause())) {
            sb.append(": ").append(t.getMessage());
        }
        return sb.toString();
    }
}
