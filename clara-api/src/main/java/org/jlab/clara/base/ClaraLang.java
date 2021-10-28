/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

/**
 * The supported languages for Clara components.
 */
public enum ClaraLang {

    /**
     * Identifies components written in the Java language.
     */
    JAVA("java"),

    /**
     * Identifies components written in the Python language.
     */
    PYTHON("python"),

    /**
     * Identifies components written in the C++ language.
     */
    CPP("cpp");

    private final String name;

    ClaraLang(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the enum value for the given language string.
     *
     * @param lang a supported language name (java, cpp, python)
     * @return the enum value for the language
     */
    public static ClaraLang fromString(String lang) {
        return ClaraLang.valueOf(lang.toUpperCase());
    }
}
