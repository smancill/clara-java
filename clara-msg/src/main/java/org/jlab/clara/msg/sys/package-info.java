/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Main server classes to connect and discover actors.
 * <p>
 * The {@link org.jlab.clara.msg.sys.Proxy proxy servers} communicate actors
 * by forwarding published messages to all the subscribers connected to the
 * proxy. Actors subscribed to other proxies would not receive the messages,
 * i.e. subscribers must connect to the same proxy where publishers are
 * publishing messages in order to receive them.
 * <p>
 * The {@link org.jlab.clara.msg.sys.Registrar registrar server} can be used
 * as a registration database for long-running publishers and subscribers, where
 * actors can discover other actors with the same topics of interest.
 */
package org.jlab.clara.msg.sys;
