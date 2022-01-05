/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;

/**
 * Stores the general properties of a service.
 * <p>
 * Currently, these properties are:
 * <ul>
 * <li>name (ex: {@code ECReconstruction})
 * <li>the full classpath (ex: {@code org.jlab.clas12.ec.services.ECReconstruction})
 * <li>the container where the service should be deployed (ex: {@code ec-cont})
 * <li>the language of the service
 * </ul>
 * Note that this class doesn't represent a deployed service in a DPE, but a
 * template that keeps the name and container of the service. Orchestrators should
 * use the data of this class combined with the values in {@link DpeInfo} to
 * fully identify individual deployed services (i.e. the canonical name).
 */
record ServiceInfo(String classpath, String cont, String name, ClaraLang lang) {

    ServiceInfo {
        if (classpath == null) {
            throw new IllegalArgumentException("Null service classpath name");
        }
        if (cont == null) {
            throw new IllegalArgumentException("Null container name");
        }
        if (name == null) {
            throw new IllegalArgumentException("Null service name");
        }
        if (lang == null) {
            throw new IllegalArgumentException("Null service language");
        }
    }
}
