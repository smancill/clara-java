/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.Composition;
import org.jlab.clara.base.ContainerName;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.ServiceName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class WorkerApplication {

    private final ApplicationInfo application;
    private final Map<ClaraLang, DpeInfo> dpes;


    WorkerApplication(ApplicationInfo application, DpeInfo dpe) {
        this.application = application;
        this.dpes = new HashMap<>();
        this.dpes.put(dpe.name().language(), dpe);
    }


    WorkerApplication(ApplicationInfo application, Map<ClaraLang, DpeInfo> dpes) {
        this.application = application;
        this.dpes = new HashMap<>(dpes);
    }


    public Set<ClaraLang> languages() {
        return application.getLanguages();
    }


    public ServiceName stageService() {
        return toName(application.getStageService());
    }


    public ServiceName readerService() {
        return toName(application.getReaderService());
    }


    public ServiceName writerService() {
        return toName(application.getWriterService());
    }


    public List<ServiceName> processingServices() {
        return application.getDataProcessingServices().stream()
                .map(this::toName)
                .collect(Collectors.toList());
    }


    public List<ServiceName> monitoringServices() {
        return application.getMonitoringServices().stream()
                .map(this::toName)
                .collect(Collectors.toList());
    }


    public List<ServiceName> services() {
        return application.getServices().stream()
                .map(this::toName)
                .collect(Collectors.toList());
    }


    public Composition composition() {
        var dataServices = processingServices();

        // main chain
        var composition = new StringBuilder();
        composition.append(readerService());
        for (var service : dataServices) {
            composition.append("+").append(service);
        }
        composition.append("+").append(writerService());
        composition.append("+").append(readerService());
        composition.append(";");

        var monServices = monitoringServices();
        if (!monServices.isEmpty()) {
            // monitoring chain
            composition.append(dataServices.get(dataServices.size() - 1));
            for (var service : monServices) {
                composition.append("+").append(service);
            }
            composition.append(";");
        }

        return new Composition(composition.toString());
    }


    private ServiceName toName(ServiceInfo service) {
        var dpe = dpes.get(service.lang());
        if (dpe == null) {
            var error = String.format("Missing %s DPE for service %s",
                                      service.lang(), service.name());
            throw new IllegalStateException(error);
        }
        return new ServiceName(dpe.name(), service.cont(), service.name());
    }


    Stream<DeployInfo> getInputOutputServicesDeployInfo() {
        return application.getInputOutputServices().stream()
                          .map(s -> new DeployInfo(toName(s), s.classpath(), 1));
    }


    Stream<DeployInfo> getProcessingServicesDeployInfo() {
        var maxCores = maxCores();
        return application.getDataProcessingServices().stream()
                          .distinct()
                          .map(s -> new DeployInfo(toName(s), s.classpath(), maxCores));
    }


    Stream<DeployInfo> getMonitoringServicesDeployInfo() {
        return application.getMonitoringServices().stream()
                          .distinct()
                          .map(s -> new DeployInfo(toName(s), s.classpath(), 1));
    }


    Map<DpeName, Set<ServiceName>> allServices() {
        return application.getAllServices().stream()
                          .map(this::toName)
                          .collect(Collectors.groupingBy(ServiceName::dpe, Collectors.toSet()));
    }


    Map<DpeName, Set<ContainerName>> allContainers() {
        return application.getAllServices().stream()
                          .map(this::toName)
                          .map(ServiceName::container)
                          .collect(Collectors.groupingBy(ContainerName::dpe, Collectors.toSet()));
    }


    Set<DpeName> dpes() {
        return dpes.values().stream()
                   .map(DpeInfo::name)
                   .collect(Collectors.toSet());
    }


    public int maxCores() {
        return dpes.values().stream()
                   .mapToInt(DpeInfo::cores)
                   .min()
                   .orElseThrow(() -> new IllegalStateException("Empty list of DPEs"));
    }


    public String hostName() {
        var firstDpe = dpes.values().iterator().next();
        return firstDpe.name().address().host();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dpes.hashCode();
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
        WorkerApplication other = (WorkerApplication) obj;
        if (!dpes.equals(other.dpes)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        String dpeNames = dpes.values().stream()
                .map(DpeInfo::name)
                .map(DpeName::canonicalName)
                .collect(Collectors.joining(","));
        return "[" + dpeNames + "]";
    }
}
