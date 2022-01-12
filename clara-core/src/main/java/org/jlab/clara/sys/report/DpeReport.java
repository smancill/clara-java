/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.util.EnvUtils;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gurjyan
 * @version 4.x
 */
public class DpeReport extends BaseReport {

    private final String host;
    private final String claraHome;
    private final String session;

    private final int coreCount;
    private int poolSize;
    private final long memorySize;

    private final String aliveData;

    private final Map<String, ContainerReport> containers = new ConcurrentHashMap<>();

    public DpeReport(ClaraBase base, String session) {
        super(base.getName(), EnvUtils.userName(), base.getDescription());

        this.host = name;
        this.claraHome = base.getClaraHome();
        this.session = session;

        this.coreCount = Runtime.getRuntime().availableProcessors();
        this.memorySize = Runtime.getRuntime().maxMemory();

        this.aliveData = aliveData();
    }

    private String aliveData() {
        var data =  new JSONObject();
        data.put("name", name);
        data.put("n_cores", coreCount);
        data.put("clara_home", claraHome);
        return data.toString();
    }

    public String getHost() {
        return host;
    }

    public String getClaraHome() {
        return claraHome;
    }

    public String getSession() {
        return session;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public double getCpuUsage() {
        return SystemStats.getCpuUsage();
    }

    public long getMemoryUsage() {
        return SystemStats.getMemoryUsage();
    }

    public double getLoad() {
        return SystemStats.getSystemLoad();
    }

    public Collection<ContainerReport> getContainers() {
        return containers.values();
    }

    public ContainerReport addContainer(ContainerReport cr) {
        return containers.putIfAbsent(cr.getName(), cr);
    }

    public ContainerReport removeContainer(ContainerReport cr) {
        return containers.remove(cr.getName());
    }

    public void removeAllContainers() {
        containers.clear();
    }

    public String getAliveData() {
        return aliveData;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
