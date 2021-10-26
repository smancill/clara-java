/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

/**
 * A factory for new shell builtin commands.
 */
@FunctionalInterface
public interface CommandFactory {

    /**
     * Creates a new builtin command.
     *
     * @param context the shell session
     * @return the builtin command
     */
    Command create(Context context);
}
