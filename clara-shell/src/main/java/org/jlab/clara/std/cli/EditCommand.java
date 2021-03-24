/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import java.nio.file.Path;
import java.util.function.Function;

class EditCommand extends BaseCommand {

    EditCommand(Context context) {
        super(context, "edit", "Edit data processing conditions");

        addArgument("services", "Edit services composition.",
            c -> c.getPath(Config.SERVICES_FILE));
        addArgument("files", "Edit input file list.",
            c -> c.getPath(Config.FILES_LIST));
    }

    void addArgument(String name, String description, Function<Config, Path> fileArg) {
        addSubCommand(newArgument(name, description, fileArg));
    }

    static CommandFactory newArgument(String name,
                                      String description,
                                      Function<Config, Path> fileArg) {
        return session -> new AbstractCommand(session, name, description) {
            @Override
            public int execute(String[] args) {
                return CommandUtils.editFile(fileArg.apply(session.config()));
            }
        };
    }
}
