/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.base.core.ClaraConstants;

/**
 * The name of a Clara DPE.
 * <p>
 * The canonical name for a DPE has the following structure:
 * <pre>
 * {@literal <host_address>_<language>}
 * {@literal <host_address>%<port>_<language>}
 * </pre>
 * Example:
 * <pre>
 * {@literal 10.1.1.1_java}
 * </pre>
 */
public class DpeName implements ClaraName {

    private final ClaraAddress address;
    private final ClaraLang language;
    private final String name;

    /**
     * Identify a DPE with host and language.
     * The default port will be used.
     *
     * @param host the host address where the DPE is running
     * @param lang the language of the DPE
     */
    public DpeName(String host, ClaraLang lang) {
        address = new ClaraAddress(host, ClaraUtil.getDefaultPort(lang.toString()));
        language = lang;
        name = host + ClaraConstants.LANG_SEP + lang;
    }

    /**
     * Identify a DPE with host, port and language.
     *
     * @param host the host address where the DPE is running
     * @param port the port used by the DPE
     * @param lang the language of the DPE
     */
    public DpeName(String host, int port, ClaraLang lang) {
        address = new ClaraAddress(host, port);
        language = lang;
        name = host + ClaraConstants.PORT_SEP
             + port + ClaraConstants.LANG_SEP
             + lang;
    }

    /**
     * Identify a DPE with a canonical name.
     *
     * @param canonicalName the canonical name of the DPE
     */
    public DpeName(String canonicalName) {
        if (!ClaraUtil.isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Invalid canonical name: " + canonicalName);
        }
        var host = ClaraUtil.getDpeHost(canonicalName);
        var port = ClaraUtil.getDpePort(canonicalName);
        this.address = new ClaraAddress(host, port);
        this.language = ClaraLang.fromString(ClaraUtil.getDpeLang(canonicalName));
        this.name = canonicalName;
    }

    @Override
    public String canonicalName() {
        return name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ClaraLang language() {
        return language;
    }

    @Override
    public ClaraAddress address() {
        return address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DpeName other = (DpeName) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
