/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

/**
 * A problem in the event writer implementation.
 */
public class EventWriterException extends Exception {

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     */
    public EventWriterException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param cause the cause of the exception
     */
    public EventWriterException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public EventWriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
