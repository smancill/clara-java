/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;


/**
 * A wrapper over a 0MQ context to handle connection sockets.
 * <p>
 * A global singleton can be obtained with {@link #getInstance()}.
 * This context is shared by all actors in the same JVM process,
 * and it must be destroyed at the end of the process,
 * after all actors have been destroyed.
 * <p>
 * New contexts can be created with {@link #newContext()},
 * for cases when the global context cannot be used
 * (i.e. the context should be destroyed before exiting the application)
 */
public final class Context implements AutoCloseable {

    private static final Context ourInstance = new Context();  // nocheck: ConstantName

    // ZMQ.Context is used for all context operations, but some jeromq APIs
    // may require a shadow ZContext and since 0.4.x a shadow context cannot be
    // created from an existing ZMQ.Context anymore, so this needs to be a
    // ZContext.
    private final ZContext ctx;

    private Context() {
        ctx = new ZContext(1);
    }

    /**
     * Returns the global singleton context.
     *
     * @return the global context
     */
    public static Context getInstance() {
        return ourInstance;
    }

    /**
     * Creates a new context.
     *
     * @return the created context
     */
    public static Context newContext() {
        return new Context();
    }

    /**
     * Gets the size of the 0MQ thread pool to handle I/O operations.
     *
     * @return the number of I/O threads used by the context
     */
    public int getIOThreads() {
        return ctx.getContext().getIOThreads();
    }

    /**
     * Set the size of the 0MQ thread pool to handle I/O operations.
     *
     * @param ioThreads the number of I/O threads
     */
    public void setIOThreads(int ioThreads) {
        ctx.getContext().setIOThreads(ioThreads);
    }

    /**
     * Gets the maximum number of sockets allowed on the context.
     *
     * @return the maximum number of sockets that can be created
     *         with the context
     */
    public int getMaxSockets() {
        return ctx.getContext().getMaxSockets();
    }

    /**
     * Sets the maximum number of sockets allowed on the context.
     *
     * @param maxSockets the maximum number of sockets that can be created
     *        with the context
     */
    public void setMaxSockets(int maxSockets) {
        ctx.getContext().setMaxSockets(maxSockets);
    }

    /**
     * Returns the internal wrapped 0MQ context.
     *
     * @return the wrapped 0MQ context
     */
    public ZMQ.Context getContext() {
        return ctx.getContext();
    }

    /**
     * Destroys the context.
     * All connections must be already closed otherwise this will hang.
     */
    public void destroy() {
        ctx.destroy();
    }

    /**
     * Destroys the context.
     * All connections must be already closed otherwise this will hang.
     */
    @Override
    public void close() {
        destroy();
    }
}
