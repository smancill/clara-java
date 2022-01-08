/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.utils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtils {

    private ThreadUtils() { }

    /**
     * Thread sleep wrapper.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // nothing
        }
    }

    /**
     * A new Thread that reports uncaught exceptions.
     */
    public static Thread newThread(String name, Runnable target) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(target, "target is null");
        Thread thread = new Thread(target);
        thread.setName(name);
        thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        return thread;
    }

    public static ThreadPoolExecutor newThreadPool(int maxThreads,
                                                   String namePrefix,
                                                   BlockingQueue<Runnable> workQueue) {
        return new FixedExecutor(maxThreads, maxThreads,
                                 0L, TimeUnit.MILLISECONDS,
                                 workQueue,
                                 new DefaultThreadFactory(namePrefix));
    }

    /**
     * A thread pool executor that prints the stackTrace of uncaught exceptions.
     */
    private static final class FixedExecutor extends ThreadPoolExecutor {

        private FixedExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                              TimeUnit unit, BlockingQueue<Runnable> workQueue,
                              ThreadFactory factory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, factory);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t == null && r instanceof Future<?> future) {
                try {
                    if (future.isDone()) {
                        future.get();
                    }
                } catch (CancellationException ce) {
                    t = ce;
                } catch (ExecutionException ee) {
                    t = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // ignore/reset
                }
            }
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    /**
     * A thread pool factory with custom thread names.
     */
    private static final class DefaultThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private DefaultThreadFactory(String name) {
            namePrefix = name + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
