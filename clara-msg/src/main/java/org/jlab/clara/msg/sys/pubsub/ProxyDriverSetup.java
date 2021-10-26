/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.pubsub;

import org.jlab.clara.msg.sys.utils.Environment;
import org.jlab.clara.msg.sys.utils.ThreadUtils;
import org.zeromq.ZMQ.Socket;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Setup of an connection to a proxy.
 */
public final class ProxyDriverSetup {

    /** The default timeout to wait for a connection confirmation. */
    public static final int CONNECTION_TIMEOUT = 1000;

    /** The default timeout to wait for a subscription confirmation. */
    public static final int SUBSCRIPTION_TIMEOUT = 1000;


    /**
     * Creates a new setup builder.
     *
     * @return the builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * Helps creating the setup for the connection(s).
     */
    public static final class Builder {

        private Consumer<Socket> preConnection;
        private Runnable postConnection;

        private Consumer<Socket> preSubscription;
        private Runnable postSubscription;

        private long connectionTimeout;
        private long subscriptionTimeout;

        private boolean checkConnection;
        private boolean checkSubscription;

        private Builder() {
            final long postConSleep = Environment.getLong("CLARA_POST_CONNECTION_SLEEP", 0);
            final long postSubSleep = Environment.getLong("CLARA_POST_SUBSCRIPTION_SLEEP", 10);

            preConnection = (s) -> { };
            postConnection = () -> ThreadUtils.sleep(postConSleep);

            preSubscription = (s) -> { };
            postSubscription = () -> ThreadUtils.sleep(postSubSleep);

            connectionTimeout = Environment.getLong("CLARA_CONNECTION_TIMEOUT",
                                                    CONNECTION_TIMEOUT);
            subscriptionTimeout = Environment.getLong("CLARA_SUBSCRIPTION_TIMEOUT",
                                                      SUBSCRIPTION_TIMEOUT);

            checkConnection = !Environment.isDefined("CLARA_NO_CHECK_CONNECTION");
            checkSubscription = !Environment.isDefined("CLARA_NO_CHECK_SUBSCRIPTION");
        }

        /**
         * Sets the action to run before connecting the socket.
         *
         * @param preConnection a consumer that acts on the socket
         *        before it is connected
         * @return this builder
         */
        public Builder withPreConnection(Consumer<Socket> preConnection) {
            Objects.requireNonNull(preConnection, "null pre-connection setup");
            this.preConnection = preConnection;
            return this;
        }

        /**
         * Sets the action to run after connecting the socket.
         *
         * @param postConnection a runnable that runs after the socket was
         *                       connected
         * @return this builder
         */
        public Builder withPostConnection(Runnable postConnection) {
            Objects.requireNonNull(postConnection, "null post-connection setup");
            this.postConnection = postConnection;
            return this;
        }


        /**
         * Sets the action to run before subscribing the socket.
         *
         * @param preSubscription a consumer that acts on the socket
         *        before it is subscribed
         * @return this builder
         */
        public Builder withPreSubscription(Consumer<Socket> preSubscription) {
            Objects.requireNonNull(preSubscription, "null pre-subscription setup");
            this.preSubscription = preSubscription;
            return this;
        }

        /**
         * Sets the action to run after subscribing the socket.
         *
         * @param postSubscription a runnable that runs after the socket was
         *                         subscribed
         * @return this builder
         */
        public Builder withPostSubscription(Runnable postSubscription) {
            Objects.requireNonNull(postSubscription, "null post-subscription setup");
            this.postSubscription = postSubscription;
            return this;
        }

        /**
         * Sets a timeout to wait for a confirmation than the socket was
         * connected.
         *
         * @param timeout the time to wait, in milliseconds
         * @return this builder
         */
        public Builder withConnectionTimeout(long timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("invalid timeout: " + timeout);
            }
            this.subscriptionTimeout = timeout;
            return this;
        }

        /**
         * Sets a timeout to wait for a confirmation than the socket was
         * subscribed.
         *
         * @param timeout the time to wait, in milliseconds
         * @return this builder
         */
        public Builder withSubscriptionTimeout(long timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("invalid timeout: " + timeout);
            }
            this.connectionTimeout = timeout;
            return this;
        }

        /**
         * Sets if the connection must be validated with control messages.
         *
         * @param flag if true, the connection will be checked
         * @return this builder
         */
        public Builder checkConnection(boolean flag) {
            this.checkConnection = flag;
            return this;
        }

        /**
         * Sets if the subscription must be validated with control messages.
         *
         * @param flag if true, the connection will be checked
         * @return this builder
         */
        public Builder checkSubscription(boolean flag) {
            this.checkSubscription = flag;
            return this;
        }

        /**
         * Creates the setup.
         *
         * @return the created connection setup
         */
        public ProxyDriverSetup build() {
            return new ProxyDriverSetup(preConnection,
                                        postConnection,
                                        preSubscription,
                                        postSubscription,
                                        connectionTimeout,
                                        subscriptionTimeout,
                                        checkConnection,
                                        checkSubscription);
        }
    }


    private final Consumer<Socket> preConnection;
    private final Runnable postConnection;

    private final Consumer<Socket> preSubscription;
    private final Runnable postSubscription;

    private final long connectionTimeout;
    private final long subscriptionTimeout;

    private final boolean checkConnection;
    private final boolean checkSubscription;


    // checkstyle.off: ParameterNumber
    private ProxyDriverSetup(Consumer<Socket> preConnection,
                             Runnable postConnection,
                             Consumer<Socket> preSubscription,
                             Runnable postSubscription,
                             long connectionTimeout,
                             long subscriptionTimeout,
                             boolean checkConnection,
                             boolean checkSubscription) {
        this.preConnection = preConnection;
        this.postConnection = postConnection;
        this.preSubscription = preSubscription;
        this.postSubscription = postSubscription;
        this.connectionTimeout = connectionTimeout;
        this.subscriptionTimeout = subscriptionTimeout;
        this.checkConnection = checkConnection;
        this.checkSubscription = checkSubscription;
    }
    // checkstyle.on: ParameterNumber

    /**
     * Runs the pre-connection action on the given socket.
     *
     * @param socket the socket to be consumed
     */
    public void preConnection(Socket socket) {
        preConnection.accept(socket);
    }

    /**
     * Runs the post-connection action.
     */
    public void postConnection() {
        postConnection.run();
    }

    /**
     * Runs the pre-subscription action on the given socket.
     *
     * @param socket the socket to be consumed
     */
    public void preSubscription(Socket socket) {
        preSubscription.accept(socket);
    }

    /**
     * Runs the post-subscription action.
     */
    public void postSubscription() {
        postSubscription.run();
    }

    /**
     * Gets the timeout for checking the connection.
     *
     * @return the timeout to wait for a control message confirming the
     *         connection.
     */
    public long connectionTimeout() {
        return connectionTimeout;
    }


    /**
     * Gets the timeout for checking the subscription.
     *
     * @return the timeout to wait for a control message confirming the
     *         connection.
     */
    public long subscriptionTimeout() {
        return subscriptionTimeout;
    }

    /**
     * Gets if the connection must be checked.
     *
     * @return true if the connection must be validated with control messages.
     */
    public boolean checkConnection() {
        return checkConnection;
    }

    /**
     * Gets if the subscription must be checked.
     *
     * @return true if the subscription must be validated with control messages.
     */
    public boolean checkSubscription() {
        return checkSubscription;
    }
}
