/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

/**
 * Identifier of a CLARA component.
 */
public interface ClaraName {

    /**
     * Returns the canonical name of this CLARA component.
     *
     * @return a string with the canonical name
     */
    String canonicalName();

    /**
     * Returns the specific name of this CLARA component.
     * This is the last part of the canonical name.
     *
     * @return a string with the name part of the canonical name
     */
    String name();

    /**
     * Returns the language of this CLARA component.
     *
     * @return the language
     */
    ClaraLang language();

    /**
     * Returns the address of the proxy used by this CLARA component.
     *
     * @return the address where the component is listening requests
     */
    ClaraAddress address();
}
