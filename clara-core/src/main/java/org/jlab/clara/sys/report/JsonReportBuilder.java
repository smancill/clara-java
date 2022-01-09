/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.core.ClaraConstants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author gurjyan
 * @version 4.x
 */
public class JsonReportBuilder implements ExternalReport {

    @Override
    public String generateReport(DpeReport dpeReport) {
        var snapshotTime = ClaraUtil.getCurrentTime();

        var dpeRuntime = new JSONObject();
        dpeRuntime.put("name", dpeReport.getHost());
        dpeRuntime.put("snapshot_time", snapshotTime);
        dpeRuntime.put("cpu_usage", dpeReport.getCpuUsage());
        dpeRuntime.put("memory_usage", dpeReport.getMemoryUsage());
        dpeRuntime.put("load", dpeReport.getLoad());

        var containersRuntimeArray = new JSONArray();
        for (var containerReport : dpeReport.getContainers()) {
            var containerRuntime = new JSONObject();
            containerRuntime.put("name", containerReport.getName());
            containerRuntime.put("snapshot_time", snapshotTime);

            var containerRequests = 0L;

            var servicesRuntimeArray = new JSONArray();
            for (var serviceReport : containerReport.getServices()) {
                var serviceRuntime = new JSONObject();

                var serviceRequests = serviceReport.getRequestCount();
                containerRequests += serviceRequests;

                serviceRuntime.put("name", serviceReport.getName());
                serviceRuntime.put("snapshot_time", snapshotTime);
                serviceRuntime.put("n_requests", serviceRequests);
                serviceRuntime.put("n_failures", serviceReport.getFailureCount());
                serviceRuntime.put("shm_reads", serviceReport.getShrmReads());
                serviceRuntime.put("shm_writes", serviceReport.getShrmWrites());
                serviceRuntime.put("bytes_recv", serviceReport.getBytesReceived());
                serviceRuntime.put("bytes_sent", serviceReport.getBytesSent());
                serviceRuntime.put("exec_time", serviceReport.getExecutionTime());

                servicesRuntimeArray.put(serviceRuntime);
            }

            containerRuntime.put("n_requests", containerRequests);
            containerRuntime.put("services", servicesRuntimeArray);
            containersRuntimeArray.put(containerRuntime);
        }

        dpeRuntime.put("containers", containersRuntimeArray);

        var dpeRegistration = new JSONObject();
        dpeRegistration.put("name", dpeReport.getHost());
        dpeRegistration.put("session", dpeReport.getSession());
        dpeRegistration.put("description", dpeReport.getDescription());
        dpeRegistration.put("language", dpeReport.getLang());
        dpeRegistration.put("clara_home", dpeReport.getClaraHome());
        dpeRegistration.put("n_cores", dpeReport.getCoreCount());
        dpeRegistration.put("memory_size", dpeReport.getMemorySize());
        dpeRegistration.put("start_time", dpeReport.getStartTime());

        var containersRegistrationArray = new JSONArray();
        for (var containerReport : dpeReport.getContainers()) {
            var containerRegistration = new JSONObject();
            containerRegistration.put("name", containerReport.getName());
            containerRegistration.put("language", containerReport.getLang());
            containerRegistration.put("author", containerReport.getAuthor());
            containerRegistration.put("start_time", containerReport.getStartTime());

            var servicesRegistrationArray = new JSONArray();
            for (var serviceReport : containerReport.getServices()) {
                var serviceRegistration = new JSONObject();
                serviceRegistration.put("name", serviceReport.getName());
                serviceRegistration.put("class_name", serviceReport.getClassName());
                serviceRegistration.put("author", serviceReport.getAuthor());
                serviceRegistration.put("version", serviceReport.getVersion());
                serviceRegistration.put("description", serviceReport.getDescription());
                serviceRegistration.put("language", serviceReport.getLang());
                serviceRegistration.put("pool_size", serviceReport.getPoolSize());
                serviceRegistration.put("start_time", serviceReport.getStartTime());

                servicesRegistrationArray.put(serviceRegistration);
            }

            containerRegistration.put("services", servicesRegistrationArray);
            containersRegistrationArray.put(containerRegistration);

        }

        dpeRegistration.put("containers", containersRegistrationArray);

        var dpeJsonReport = new JSONObject();
        dpeJsonReport.put(ClaraConstants.RUNTIME_KEY, dpeRuntime);
        dpeJsonReport.put(ClaraConstants.REGISTRATION_KEY, dpeRegistration);

        return dpeJsonReport.toString();
    }
}
