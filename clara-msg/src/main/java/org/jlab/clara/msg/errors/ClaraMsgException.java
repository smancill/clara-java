/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.errors;

/**
 * Base exception class.
 */
public class ClaraMsgException extends Exception {

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     */
    public ClaraMsgException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ClaraMsgException(String message, Throwable cause) {
        super(message, cause);
    }
}
