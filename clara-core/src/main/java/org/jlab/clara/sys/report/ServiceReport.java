/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.engine.Engine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author gurjyan
 * @version 4.x
 */
public class ServiceReport extends BaseReport {

    private final String engineName;
    private final String className;
    private final String version;
    private final String session;
    private final int poolSize;

    private final AtomicInteger failureCount = new AtomicInteger();
    private final AtomicInteger shrmReads = new AtomicInteger();
    private final AtomicInteger shrmWrites = new AtomicInteger();
    private final AtomicLong bytesReceived = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();
    private final AtomicLong executionTime = new AtomicLong();

    public ServiceReport(ClaraComponent comp, Engine engine, String session) {
        super(comp.getCanonicalName(), engine.getAuthor(), engine.getDescription());
        this.engineName = comp.getEngineName();
        this.className = comp.getEngineClass();
        this.version = engine.getVersion();
        this.session = session;
        this.poolSize = comp.getSubscriptionPoolSize();
    }

    public String getEngineName() {
        return engineName;
    }

    public String getClassName() {
        return className;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public void incrementFailureCount() {
        failureCount.getAndIncrement();
    }

    public int getShrmReads() {
        return shrmReads.get();
    }

    public void incrementShrmReads() {
        shrmReads.getAndIncrement();
    }

    public int getShrmWrites() {
        return shrmWrites.get();
    }

    public void incrementShrmWrites() {
        shrmWrites.getAndIncrement();
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }

    public void addBytesReceived(long bytes) {
        bytesReceived.getAndAdd(bytes);
    }

    public long getBytesSent() {
        return bytesSent.get();
    }

    public void addBytesSent(long bytes) {
        bytesSent.getAndAdd(bytes);
    }

    public long getExecutionTime() {
        return executionTime.get();
    }

    public void addExecutionTime(long deltaTime) {
        executionTime.getAndAdd(deltaTime);
    }

    public String getVersion() {
        return version;
    }

    public String getSession() {
        return session;
    }

    public int getPoolSize() {
        return poolSize;
    }
}
