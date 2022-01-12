/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogUtils {

    private LogUtils() { }

    public static Logger getConsoleLogger(String name) {
        var dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var logger = Logger.getLogger(name);
        var handler = new ConsoleHandler() {
            @Override
            protected void setOutputStream(OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        };
        handler.setLevel(Level.FINER);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("[%s] %s %s: %s%n",
                                     LocalDateTime.now().format(dateFormat),
                                     record.getLoggerName(),
                                     record.getLevel(),
                                     record.getMessage());
            }
        });
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.OFF);
        return logger;
    }

    public static Supplier<String> exceptionReporter(Exception e) {
        return () -> {
            try {
                var sw = new StringWriter();
                var pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                return sw.toString();
            } catch (Exception e2) {
                return "bad stack";
            }
        };
    }
}
