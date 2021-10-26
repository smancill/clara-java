/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

/**
 * Specifies the mode used by subscriptions to run their callback on received
 * messages.
 */
public enum CallbackMode {
    /** The callbacks will run in a single thread. */
    SINGLE_THREAD,

    /** The callbacks will run in a thread pool. */
    MULTI_THREAD
}
