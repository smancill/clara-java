/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

/**
 * A problem in the event reader implementation.
 */
public class EventReaderException extends Exception {

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     */
    public EventReaderException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param cause the cause of the exception
     */
    public EventReaderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public EventReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
