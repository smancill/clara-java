/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Main classes for actors.
 * <p>
 * {@link org.jlab.clara.msg.core.Actor Actors} use
 * {@link org.jlab.clara.msg.core.Connection connections} to publish and
 * subscribe {@link org.jlab.clara.msg.core.Message messages} to specific
 * {@link org.jlab.clara.msg.core.Topic topics}.
 * When subscribing, a {@link org.jlab.clara.msg.core.Callback callback}
 * must be provided to process the received messages. Each actor process its
 * registered callbacks in a background thread pool.
 * <p>
 * Actors are data-agnostic, i.e., the data is always stored and published in
 * the message as a binary byte array. Serialization and interpretation of the
 * data must be user-provided.
 * <p>
 * A {@link org.jlab.clara.msg.sys.Proxy proxy server} must be running and both
 * publisher(s) and subscriber(s) must use a connection to the same proxy for
 * the messages to be delivered.
 * Actors can register with a {@link org.jlab.clara.msg.sys.Registrar
 * registrar server} to be discovered by other actors.
 */
package org.jlab.clara.msg.core;
