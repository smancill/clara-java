/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

class BenchmarkPrinter {

    private final Benchmark benchmark;
    private final ApplicationInfo application;

    private final long totalRequests;
    private long totalTime;

    BenchmarkPrinter(Benchmark benchmark, ApplicationInfo application, long totalRequests) {
        this.benchmark = benchmark;
        this.application = application;
        this.totalRequests = totalRequests;
        this.totalTime = 0;
    }

    void printBenchmark() {
        Logging.info("Benchmark results:");
        printService("READER", time(application.getReaderService()));
        for (var service : application.getDataProcessingServices()) {
            printService(service.name(), time(service));
        }
        printService("WRITER", time(application.getWriterService()));
        printTotal();
    }

    private long time(ServiceInfo service) {
        long time = benchmark.time(service);
        totalTime += time;
        return time;
    }

    private void printService(String label, long time) {
        print(label, time, totalRequests);
    }

    private void printTotal() {
        print("TOTAL", totalTime, totalRequests);
    }

    private void print(String name, long time, long requests) {
        double timePerEvent = (time / (double) requests) / 1e3;
        Logging.info("  %-12.12s   %6d events    total time = %8.2f s    "
                + "average event time = %7.2f ms",
                name, requests, time / 1e6,
                timePerEvent);
    }
}
