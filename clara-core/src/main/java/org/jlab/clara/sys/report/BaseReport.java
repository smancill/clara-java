/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.ClaraUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gurjyan
 * @version 4.x
 */
public class BaseReport {

    protected final String name;
    protected final String author;
    protected final String lang;
    protected final String description;
    protected final String startTime;

    private final AtomicInteger requestCount = new AtomicInteger();

    public BaseReport(String name, String author, String description) {
        this.name = name;
        this.author = author;
        this.lang = ClaraLang.JAVA.toString();
        this.description = description;
        this.startTime = ClaraUtil.getCurrentTime();
    }

    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getStartTime() {
        return startTime;
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public void incrementRequestCount() {
        requestCount.getAndIncrement();
    }
}
