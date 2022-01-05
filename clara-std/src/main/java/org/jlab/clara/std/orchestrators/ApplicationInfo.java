/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ApplicationInfo {

    static final String STAGE = "stage";
    static final String READER = "reader";
    static final String WRITER = "writer";

    private final Map<String, ServiceInfo> ioServices;
    private final List<ServiceInfo> dataServices;
    private final List<ServiceInfo> monServices;
    private final Set<ClaraLang> languages;

    ApplicationInfo(Map<String, ServiceInfo> ioServices,
                    List<ServiceInfo> dataServices,
                    List<ServiceInfo> monServices) {
        this.ioServices = copyServices(ioServices);
        this.dataServices = copyServices(dataServices, true);
        this.monServices = copyServices(monServices, false);
        this.languages = parseLanguages();
    }

    private static Map<String, ServiceInfo> copyServices(Map<String, ServiceInfo> ioServices) {
        if (ioServices.get(STAGE) == null) {
            throw new IllegalArgumentException("missing stage service");
        }
        if (ioServices.get(READER) == null) {
            throw new IllegalArgumentException("missing reader service");
        }
        if (ioServices.get(WRITER) == null) {
            throw new IllegalArgumentException("missing writer service");
        }
        return new HashMap<>(ioServices);
    }

    private static List<ServiceInfo> copyServices(List<ServiceInfo> chain, boolean checkEmpty) {
        if (chain == null) {
            throw new IllegalArgumentException("null chain of services");
        }
        if (checkEmpty && chain.isEmpty()) {
            throw new IllegalArgumentException("empty chain of services");
        }
        return new ArrayList<>(chain);
    }

    private Set<ClaraLang> parseLanguages() {
        return allServices().map(ServiceInfo::lang).collect(Collectors.toSet());
    }

    private Stream<ServiceInfo> allServices() {
        return stream(ioServices.values(), dataServices, monServices);
    }

    @SafeVarargs
    private Stream<ServiceInfo> stream(Collection<ServiceInfo>... values) {
        return Stream.of(values).flatMap(Collection::stream);
    }

    ServiceInfo getStageService() {
        return ioServices.get(STAGE);
    }

    ServiceInfo getReaderService() {
        return ioServices.get(READER);
    }

    ServiceInfo getWriterService() {
        return ioServices.get(WRITER);
    }

    List<ServiceInfo> getInputOutputServices() {
        return List.of(getStageService(), getReaderService(), getWriterService());
    }

    List<ServiceInfo> getDataProcessingServices() {
        return dataServices;
    }

    List<ServiceInfo> getMonitoringServices() {
        return monServices;
    }

    Set<ServiceInfo> getServices() {
        return stream(dataServices, monServices).collect(Collectors.toSet());
    }

    Set<ServiceInfo> getAllServices() {
        return allServices().collect(Collectors.toSet());
    }

    Set<ClaraLang> getLanguages() {
        return languages;
    }
}
