/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

enum OrchestratorConfigMode {

    FILE("file"),
    DATASET("dataset");

    private final String name;

    OrchestratorConfigMode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static OrchestratorConfigMode fromString(String lang) {
        return OrchestratorConfigMode.valueOf(lang.toUpperCase());
    }
}
