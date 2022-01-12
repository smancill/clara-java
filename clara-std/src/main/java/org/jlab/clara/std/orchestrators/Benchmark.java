/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ServiceName;
import org.jlab.clara.base.ServiceRuntimeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Benchmark {

    final Map<ServiceInfo, Runtime> runtimeStats = new HashMap<>();

    Benchmark(ApplicationInfo application) {
        var services = allServices(application);
        services.forEach(s -> runtimeStats.put(s, new Runtime()));
    }

    private List<ServiceInfo> allServices(ApplicationInfo application) {
        var services = new ArrayList<ServiceInfo>();
        services.add(application.getReaderService());
        services.addAll(application.getDataProcessingServices());
        services.add(application.getWriterService());
        return services;
    }

    void initialize(Set<ServiceRuntimeData> data) {
        data.forEach(s -> {
            Runtime r = runtimeStats.get(key(s.name()));
            if (r != null) {
                r.initialTime = s.executionTime();
            }
        });
    }

    void update(Set<ServiceRuntimeData> data) {
        data.forEach(s -> {
            Runtime r = runtimeStats.get(key(s.name()));
            if (r != null) {
                r.totalTime = s.executionTime();
            }
        });
    }

    long time(ServiceInfo service) {
        Runtime r = runtimeStats.get(service);
        if (r != null) {
            return r.totalTime - r.initialTime;
        }
        throw new OrchestratorException("Invalid runtime report: missing " + service.name());
    }

    private static ServiceInfo key(ServiceName service) {
        return new ServiceInfo("", service.container().name(), service.name(), service.language());
    }

    private static class Runtime {
        long initialTime = 0;
        long totalTime = 0;
    }
}
