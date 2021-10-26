/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

class BenchmarkPrinter {

    private final Benchmark benchmark;

    private long totalTime = 0;
    private long totalRequests = 0;

    BenchmarkPrinter(Benchmark benchmark, long totalRequests) {
        this.benchmark = benchmark;
        this.totalRequests = totalRequests;
    }

    void printBenchmark(ApplicationInfo application) {
        Logging.info("Benchmark results:");
        printService(application.getReaderService(), "READER");
        for (ServiceInfo service : application.getDataProcessingServices()) {
            printService(service, service.name);
        }
        printService(application.getWriterService(), "WRITER");
        printTotal();
    }

    private void printService(ServiceInfo service, String label) {
        long time = benchmark.time(service);
        totalTime += time;
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
