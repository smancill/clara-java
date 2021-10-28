/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.pubsub.ProxyDriverSetup;
import org.jlab.clara.msg.sys.regdis.RegConstants;

import java.util.Objects;

/**
 * Setup of an actor.
 */
public final class ActorSetup extends ConnectionSetup {

    /** The default size for the callback thread pool. */
    public static final int DEFAULT_POOL_SIZE = 2;

    /** The default timeout to wait for a registration request response. */
    public static final int REGISTRATION_TIMEOUT = RegConstants.REGISTRATION_TIMEOUT;

    /** The default timeout to wait for a discovery request response. */
    public static final int DISCOVERY_TIMEOUT = RegConstants.DISCOVERY_TIMEOUT;


    /**
     * Creates a builder to set options for an actor.
     *
     * @return a new ActorSetup builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * Helps creating the setup for an actor.
     * All parameters not set will be initialized to their default values.
     */
    public static final class Builder extends ConnectionSetup.Builder<Builder> {

        private RegAddress registrarAddress = new RegAddress();
        private CallbackMode subscriptionMode = CallbackMode.MULTI_THREAD;
        private int poolSize = DEFAULT_POOL_SIZE;

        /**
         * Sets the address of the default registrar.
         * This address will be used by the registration API when no
         * address is given.
         *
         * @param address the address to the default registrar
         * @return this builder
         */
        public Builder withRegistrar(RegAddress address) {
            Objects.requireNonNull(address, "null registrar address");
            this.registrarAddress = address;
            return this;
        }

        /**
         * Sets the size of the callback thread-pool.
         *
         * @param poolSize the size of the thread-pool that process the callbacks.
         * @return this builder
         */
        public Builder withPoolSize(int poolSize) {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("invalid pool size: " + poolSize);
            }
            this.poolSize = poolSize;
            return this;
        }

        /**
         * Sets the callback mode for all started subscriptions.
         * This setup will be applied every time a subscription is started.
         *
         * @param mode the callback mode
         * @return this builder
         */
        public Builder withSubscriptionMode(CallbackMode mode) {
            Objects.requireNonNull(mode, "null subscription mode");
            this.subscriptionMode = mode;
            return this;
        }

        /**
         * Creates the setup for an actor.
         *
         * @return the actor setup
         */
        public ActorSetup build() {
            return new ActorSetup(proxyAddress,
                                  registrarAddress,
                                  subscriptionMode,
                                  conSetup.build(),
                                  poolSize);
        }

        @Override
        Builder getThis() {
            return this;
        }
    }


    private final RegAddress registrarAddress;
    private final CallbackMode subscriptionMode;
    private final int poolSize;

    private ActorSetup(ProxyAddress proxyAddress,
                       RegAddress registrarAddress,
                       CallbackMode subscriptionMode,
                       ProxyDriverSetup connectionSetup,
                       int poolSize) {
        super(proxyAddress, connectionSetup);
        this.registrarAddress = registrarAddress;
        this.subscriptionMode = subscriptionMode;
        this.poolSize = poolSize;
    }

    /**
     * Gets the address to the default registrar.
     *
     * @return the address of the default registrar
     */
    public RegAddress registrarAddress() {
        return registrarAddress;
    }

    /**
     * Gets the subscription mode to process callbacks.
     *
     * @return the subscription mode
     */
    public CallbackMode subscriptionMode() {
        return subscriptionMode;
    }

    /**
     * Gets the size of the callback thread-pool.
     *
     * @return the pool size
     */
    public int poolSize() {
        return poolSize;
    }
}
