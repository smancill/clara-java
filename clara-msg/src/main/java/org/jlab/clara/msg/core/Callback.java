/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

/**
 * A user-defined action to process subscribed messages.
 */
public interface Callback {

    /**
     * Runs the user-action on a received message.
     * This method can be executed concurrently in several threads.
     *
     * @param msg a received message.
     */
    void callback(Message msg);
}
