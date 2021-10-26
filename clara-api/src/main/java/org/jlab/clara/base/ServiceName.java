/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import static org.jlab.clara.msg.core.Topic.SEPARATOR;

/**
 * Unique identifier of a CLARA service.
 * <p>
 * The canonical name for a service has the following structure:
 * <pre>
 * {@literal <container_canonical_name>:<engine_name>}
 * </pre>
 * Example:
 * <pre>
 * {@literal 10.1.1.1_java:master:SqrRoot}
 * </pre>
 *
 * @see ContainerName
 */
public class ServiceName implements ClaraName {

    private final ContainerName container;
    private final String engine;
    private final String canonicalName;

    /**
     * Identify a service with its container and engine name.
     *
     * @param container the name of the service container
     * @param engine the name of the service engine
     */
    public ServiceName(ContainerName container, String engine) {
        this.container = container;
        this.engine = engine;
        this.canonicalName = container.canonicalName() + SEPARATOR + engine;
    }

    /**
     * Identify a service with its DPE, container name and engine name.
     *
     * @param dpe the name of the DPE
     * @param container the name of the service container
     * @param engine the name of the service engine
     */
    public ServiceName(DpeName dpe, String container, String engine) {
        this(new ContainerName(dpe, container), engine);
    }

    /**
     * Identify a service with its host, language, container and engine name.
     *
     * @param host the host address of the DPE
     * @param lang the language of the DPE
     * @param container the name of the service container
     * @param engine the name of the service engine
     */
    public ServiceName(String host, ClaraLang lang, String container, String engine) {
        this(new ContainerName(new DpeName(host, lang), container), engine);
    }

    /**
     * Identify a service with its canonical name.
     *
     * @param canonicalName the canonical name of the service
     */
    public ServiceName(String canonicalName) {
        if (!ClaraUtil.isServiceName(canonicalName)) {
            throw new IllegalArgumentException("Invalid service name: " + canonicalName);
        }
        this.container = new ContainerName(ClaraUtil.getContainerCanonicalName(canonicalName));
        this.canonicalName = canonicalName;
        this.engine = ClaraUtil.getEngineName(canonicalName);
    }

    @Override
    public String canonicalName() {
        return canonicalName;
    }

    @Override
    public String name() {
        return engine;
    }

    @Override
    public ClaraAddress address() {
        return container.address();
    }

    @Override
    public ClaraLang language() {
        return container.language();
    }

    /**
     * Gets the canonical name of the DPE for this service.
     *
     * @return the DPE name
     */
    public DpeName dpe() {
        return container.dpe();
    }

    /**
     * Gets the canonical name of the container for this service.
     *
     * @return the container name
     */
    public ContainerName container() {
        return container;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + canonicalName.hashCode();
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
        ServiceName other = (ServiceName) obj;
        return canonicalName.equals(other.canonicalName);
    }

    @Override
    public String toString() {
        return canonicalName;
    }
}
