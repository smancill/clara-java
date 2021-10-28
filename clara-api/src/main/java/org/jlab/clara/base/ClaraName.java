/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

/**
 * Identifier of a Clara component.
 */
public interface ClaraName {

    /**
     * Returns the canonical name of this Clara component.
     *
     * @return a string with the canonical name
     */
    String canonicalName();

    /**
     * Returns the specific name of this Clara component.
     * This is the last part of the canonical name.
     *
     * @return a string with the name part of the canonical name
     */
    String name();

    /**
     * Returns the language of this Clara component.
     *
     * @return the language
     */
    ClaraLang language();

    /**
     * Returns the address of the proxy used by this Clara component.
     *
     * @return the address where the component is listening requests
     */
    ClaraAddress address();
}
