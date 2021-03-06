/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.data.RegInfo;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.data.RegRecord;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;
import org.jlab.clara.msg.sys.regdis.RegDriver;
import org.jlab.clara.msg.sys.regdis.RegFactory;
import org.zeromq.ZMQException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * The main pub/sub actor.
 * <p>
 * Actors send messages to each other using pub/sub communications
 * through a cloud of proxies.
 * Registrar services provide registration and discoverability of actors.
 * <p>
 * An actor has a <em>name</em> for identification, a <em>default proxy</em> intended for
 * long-term publication/subscription of messages, and a <em>default registrar</em>
 * where it can register and discover other long-term actors.
 * Unless otherwise specified, the local node and the standard ports will be
 * used for both default proxy and registrar.
 * <p>
 * Publishers set specific <em>topics</em> for their messages, and subscribers define
 * topics of interest to filter which messages they want to receive.
 * A <em>domain-specific callback</em> defined by the subscriber will be executed every
 * time a message is received. This callback must be thread-safe,
 * and it can also be used to send responses or new messages.
 * <p>
 * In order to publish or subscribe to messages, a <em>connection</em> to a proxy must
 * be obtained. The actor owns and keeps a <em>pool of available connections</em>,
 * creating new ones as needed. When no address is specified, the <em>default
 * proxy</em> will be used. The connections can be returned to the pool of available
 * connections, to avoid creating too many new connections. All connections will
 * be closed when the actor is destroyed.
 * <p>
 * Multi-threaded publication of messages is fully supported, but every thread
 * must use its own connection. Subscriptions of messages always run in their
 * own background thread. It is recommended to always obtain and release the
 * necessary connections inside the thread that uses them. The <em>connect</em> methods
 * will ensure that each thread gets a different connection.
 * <p>
 * Publishers must send messages through the same <em>proxy</em> than the
 * subscribers for the messages to be delivered. Normally, this proxy will be
 * the <em>default proxy</em> of a long-term subscriber with many dynamic publishers, or
 * the <em>default proxy</em> of a long-term publisher with many dynamic subscribers.
 * To have many publishers sending messages to many subscribers, they all must
 * <em>agree</em> in the proxy. It is possible to use several proxies, but multiple
 * publications and subscriptions will be needed, and it may get complicated.
 * Applications have great flexibility to organize their
 * communications, but it is better to deploy simple topologies.
 * <p>
 * Actors can register as publishers and/or subscribers with <em>registrar services</em>,
 * so other actors can discover them if they share the topic of interest.
 * Using the registration and discovery methods is always thread-safe.
 * The registrar service must be common to the actors, running in a known node.
 * If no address is specified, the <em>default registrar</em> will be used.
 * Note that the registration will always set the <em>default proxy</em> as the proxy
 * through which the actor is publishing/subscribed to messages.
 * If registration for different proxies is needed, multiple actors should be
 * used, each one with an appropriate default proxy.
 *
 * @see Message
 * @see Topic
 */
public class Actor implements AutoCloseable {

    /** The identifier of this actor. */
    protected final String myName;

    /** The generated unique ID of this actor. */
    protected final String myId;

    // actor setup
    private final ActorSetup setup;

    // thread pool
    private final ThreadPoolExecutor threadPool;

    private final ConnectionManager connectionManager;

    // map of active subscriptions
    private final ConcurrentMap<String, Subscription> mySubscriptions;
    private final CallbackMode callbackMode;

    private final ResponseListener syncPubListener;

    /**
     * Creates an actor with default settings.
     * The local node and the standard ports will be used for both
     * default proxy and registrar, and the callback thread-pool will use the
     * {@link ActorSetup#DEFAULT_POOL_SIZE default pool size}.
     *
     * @param name the name of this actor
     * @see ProxyAddress
     * @see RegAddress
     */
    public Actor(String name) {
        this(name,
             new ProxyAddress(),
             new RegAddress(),
             ActorSetup.DEFAULT_POOL_SIZE);
    }

    /**
     * Creates an actor with default settings.
     * The local node and the standard ports will be used for both
     * default proxy and registrar.
     *
     * @param name the name of this actor
     * @param poolSize the initial size of the callback thread-pool
     * @see ProxyAddress
     * @see RegAddress
     */
    public Actor(String name, int poolSize) {
        this(name,
             new ProxyAddress(),
             new RegAddress(),
             poolSize);
    }

    /**
     * Creates an actor specifying the default registrar to be used.
     * The local node and the standard ports will be used for the default proxy,
     * and the callback thread-pool will use the
     * {@link ActorSetup#DEFAULT_POOL_SIZE default pool size}.
     *
     * @param name the name of an actor
     * @param defaultRegistrar the address to the default registrar
     */
    public Actor(String name, RegAddress defaultRegistrar) {
        this(name,
             new ProxyAddress(),
             defaultRegistrar,
             ActorSetup.DEFAULT_POOL_SIZE);
    }

    /**
     * Creates an actor specifying the default registrar to be used.
     * The local node and the standard ports will be used for the default proxy.
     *
     * @param name the name of this actor
     * @param defaultRegistrar the address to the default registrar
     * @param poolSize the initial size of the callback thread-pool
     * @see ProxyAddress
     */
    public Actor(String name, RegAddress defaultRegistrar, int poolSize) {
        this(name,
             new ProxyAddress(),
             defaultRegistrar,
             poolSize);
    }

    /**
     * Creates an actor specifying the default proxy and registrar to be used.
     *
     * @param name the name of this actor
     * @param defaultProxy the address to the default proxy
     * @param defaultRegistrar the address to the default registrar
     * @param poolSize the size of the callback thread pool
     */
    public Actor(String name,
                 ProxyAddress defaultProxy,
                 RegAddress defaultRegistrar,
                 int poolSize) {
        this(name, ActorSetup.newBuilder()
                             .withProxy(defaultProxy)
                             .withRegistrar(defaultRegistrar)
                             .withPoolSize(poolSize)
                             .build());
    }

    /**
     * Creates an actor with the given setup.
     *
     * @param name the name of this actor
     * @param setup the setup of this actor
     */
    public Actor(String name, ActorSetup setup) {
        this(name, setup, new ConnectionFactory(Context.getInstance()));
    }

    /**
     * Full constructor.
     *
     * @param name the name of this actor
     * @param setup the setup of this actor
     * @param factory the connection factory for this actor
     */
    protected Actor(String name,
                    ActorSetup setup,
                    ConnectionFactory factory) {
        // We need to have a name for an actor
        this.myName = name;
        this.myId = ActorUtils.encodeIdentity(setup.registrarAddress().toString(), name);
        this.setup = setup;

        // create fixed size thread pool
        this.threadPool = ActorUtils.newThreadPool(setup.poolSize(), name);
        this.threadPool.setRejectedExecutionHandler(new RejectedCallbackHandler());

        // create the connection pool
        this.connectionManager = new ConnectionManager(factory, setup.connectionSetup());

        // create the responses listener
        this.syncPubListener = new ResponseListener(myId, factory);
        this.syncPubListener.start();

        // create the map of running subscriptions
        this.mySubscriptions = new ConcurrentHashMap<>();
        this.callbackMode = setup.subscriptionMode();
    }

    /**
     * Unsubscribes all running subscriptions,
     * terminates all running callbacks and closes all connections.
     */
    public void destroy() {
        final int infiniteLinger = -1;
        destroy(infiniteLinger);
    }

    /**
     * Unsubscribes all running subscriptions,
     * terminates all running callbacks and closes all connections.
     *
     * @param linger the ZMQ linger period when closing the sockets
     * @see <a href="http://api.zeromq.org/3-2:zmq-setsockopt">ZMQ_LINGER</a>
     */
    public void destroy(int linger) {
        unsubscribeAll();
        terminateCallbacks();
        syncPubListener.stop();
        connectionManager.destroy(linger);
    }

    /**
     * Unsubscribes all running subscriptions,
     * terminates all running callbacks and closes all connections.
     */
    @Override
    public void close() {
        destroy();
    }

    /**
     * Obtains a connection to the default proxy.
     * If there is no available connection, a new one will be created.
     * <p>
     * Creating new connections takes some time, and the first published
     * messages may be lost. The {@link #cacheConnection()} method can be used
     * to create connections before using them .
     *
     * @return a connection to the proxy
     * @throws ClaraMsgException if a new connection could not be created
     */
    public Connection getConnection() throws ClaraMsgException {
        return getConnection(setup.proxyAddress());
    }

    /**
     * Obtains a connection to the specified proxy.
     * If there is no available connection, a new one will be created.
     * <p>
     * Creating new connections takes some time, and the first published
     * messages may be lost. The {@link #cacheConnection(ProxyAddress)}
     * method can be used to create connections before using them .
     *
     * @param address the address of the proxy
     * @return a connection to the proxy
     * @throws ClaraMsgException if a new connection could not be created
     */
    public Connection getConnection(ProxyAddress address) throws ClaraMsgException {
        return new Connection(connectionManager,
                                  connectionManager.getProxyConnection(address));
    }

    /**
     * Creates and stores a connection to the default proxy in the internal
     * connection pool.
     * Useful to ensure that there is a connection ready when {@link
     * #getConnection()} is called.
     *
     * @throws ClaraMsgException if the new connection could not be created
     */
    public void cacheConnection() throws ClaraMsgException {
        cacheConnection(setup.proxyAddress());
    }

    /**
     * Creates and stores a connection to the specified proxy in the internal
     * connection pool.
     * Useful to ensure that there is a connection ready when {@link
     * #getConnection(ProxyAddress)} is called.
     *
     * @param address the address of the proxy
     * @throws ClaraMsgException if the new connection could not be created
     */
    public void cacheConnection(ProxyAddress address) throws ClaraMsgException {
        ProxyDriver driver = connectionManager.createProxyConnection(address);
        connectionManager.releaseProxyConnection(driver);
    }

    /**
     * Destroys the given connection.
     *
     * @param connection the connection to be destroyed
     */
    public void destroyConnection(Connection connection) {
        connection.destroy();
    }

    /**
     * Publishes a message through the default proxy connection.
     *
     * @param msg the message to be published
     * @throws ClaraMsgException if the request failed
     */
    public void publish(Message msg) throws ClaraMsgException {
        try (Connection connection = getConnection()) {
            publish(connection, msg);
        }
    }

    /**
     * Publishes a message through the specified proxy.
     *
     * @param address the address to the proxy
     * @param msg the message to be published
     * @throws ClaraMsgException if the request failed
     */
    public void publish(ProxyAddress address, Message msg) throws ClaraMsgException {
        try (Connection connection = getConnection(address)) {
            publish(connection, msg);
        }
    }

    /**
     * Publishes a message through the specified proxy connection.
     *
     * @param connection the connection to the proxy
     * @param msg the message to be published
     * @throws ClaraMsgException if the request failed
     */
    public void publish(Connection connection, Message msg) throws ClaraMsgException {
        // just make sure that receiver knows that this is not a sync request.
        // need this in case we reuse messages.
        msg.getMetaData().clearReplyTo();

        connection.publish(msg);
    }

    /**
     * Publishes a message through the default proxy connection and blocks
     * waiting for a response.
     *
     * The subscriber must publish the response to the topic given by the
     * {@code replyto} metadata field, through the same proxy.
     *
     * This method will throw if a response is not received before the timeout
     * expires.
     *
     * @param msg the message to be published
     * @param timeout the length of time to wait a response, in milliseconds
     * @return the response message
     * @throws ClaraMsgException if the message could not be published
     * @throws TimeoutException if a response is not received in the given time
     */
    public Message syncPublish(Message msg, long timeout)
            throws ClaraMsgException, TimeoutException {
        try (Connection connection = getConnection()) {
            return syncPublish(connection, msg, timeout);
        }
    }

    /**
     * Publishes a message through the specified proxy and blocks
     * waiting for a response.
     *
     * The subscriber must publish the response to the topic given by the
     * {@code replyto} metadata field, through the same proxy.
     *
     * This method will throw if a response is not received before the timeout
     * expires.
     *
     * @param address the address to the proxy
     * @param msg the message to be published
     * @param timeout the length of time to wait a response, in milliseconds
     * @return the response message
     * @throws ClaraMsgException if the message could not be published
     * @throws TimeoutException if a response is not received in the given time
     */
    public Message syncPublish(ProxyAddress address, Message msg, long timeout)
            throws ClaraMsgException, TimeoutException {
        try (Connection connection = getConnection(address)) {
            return syncPublish(connection, msg, timeout);
        }
    }

    /**
     * Publishes a message through the specified proxy connection and blocks
     * waiting for a response.
     *
     * The subscriber must publish the response to the topic given by the
     * {@code replyto} metadata field, through the same proxy.
     *
     * This method will throw if a response is not received before the timeout
     * expires.
     *
     * @param connection the connection to the proxy
     * @param msg the message to be published
     * @param timeout the length of time to wait a response, in milliseconds
     * @return the response message
     * @throws ClaraMsgException if the message could not be published
     * @throws TimeoutException if a response is not received in the given time
     */
    public Message syncPublish(Connection connection, Message msg, long timeout)
            throws ClaraMsgException, TimeoutException {
        // address/topic where the subscriber should send the result
        String returnAddress = ActorUtils.getUniqueReplyTo(myId);

        // set the return address as replyTo in the Message
        msg.getMetaData().setReplyTo(returnAddress);

        try {
            // subscribe to the returnAddress
            syncPubListener.register(connection.getAddress());

            // it must be the internal publish, to keep the replyTo field
            connection.publish(msg);

            // wait for the response
            return syncPubListener.waitMessage(returnAddress, timeout);
        } finally {
            msg.getMetaData().clearReplyTo();
        }
    }

    /**
     * Subscribes to a topic of interest through the default proxy.
     * A background thread will be started to receive the messages.
     *
     * @param topic the topic to select messages
     * @param callback the user action to run when a message is received
     * @throws ClaraMsgException if the subscription could not be created
     * @return the subscription handler
     */
    public Subscription subscribe(Topic topic,
                                  Callback callback) throws ClaraMsgException {
        return subscribe(setup.proxyAddress(), topic, callback);
    }

    /**
     * Subscribes to a set of topics of interest through the default proxy.
     * A background thread will be started to receive the messages.
     *
     * @param topics the topics to select messages
     * @param callback the user action to run when a message is received
     * @throws ClaraMsgException if the subscription could not be created
     * @return the subscription handler
     */
    public Subscription subscribe(Set<Topic> topics,
                                  Callback callback) throws ClaraMsgException {
        return subscribe(setup.proxyAddress(), topics, callback);
    }

    /**
     * Subscribes to a topic of interest through the specified proxy.
     * A background thread will be started to receive the messages.
     *
     * @param address the address to the proxy
     * @param topic the topic to select messages
     * @param callback the user action to run when a message is received
     * @throws ClaraMsgException if the subscription could not be created
     * @return the subscription handler
     */
    public Subscription subscribe(ProxyAddress address,
                                  Topic topic,
                                  Callback callback) throws ClaraMsgException {
        return subscribe(address, new HashSet<>(Arrays.asList(topic)), callback);
    }

    /**
     * Subscribes to a set of topics of interest through the specified proxy.
     * A background thread will be started to receive the messages.
     *
     * @param address the address to the proxy
     * @param topics the topics to select messages
     * @param callback the user action to run when a message is received
     * @throws ClaraMsgException if the subscription could not be created
     * @return the subscription handler
     */
    public Subscription subscribe(ProxyAddress address,
                                  Set<Topic> topics,
                                  Callback callback) throws ClaraMsgException {
        // get a connection to the proxy
        ProxyDriver connection = connectionManager.createProxySubscriber(address);
        try {
            // define a unique name for the subscription
            String name = "sub-" + myName + "-" + connection.getAddress() + "-" + topics.hashCode();

            // start the subscription, if it does not exist yet
            Subscription sHandle = mySubscriptions.get(name);
            if (sHandle == null) {
                sHandle = createSubscription(name, connection, topics, callback);
                sHandle.start(setup.connectionSetup());
                Subscription result = mySubscriptions.putIfAbsent(name, sHandle);
                if (result == null) {
                    return sHandle;
                }
                sHandle.stop();
            }
            throw new IllegalStateException("subscription already exists");
        } catch (Exception e) {
            connection.close();
            throw e;
        }
    }

    private Subscription createSubscription(String name,
                                            ProxyDriver connection,
                                            Set<Topic> topics,
                                            Callback callback) {
        switch (callbackMode) {
            case MULTI_THREAD:
                return new Subscription(name, connection, topics) {
                    @Override
                    public void handle(Message inputMsg) throws ClaraMsgException {
                        threadPool.submit(() -> callback.callback(inputMsg));
                    }
                };

            case SINGLE_THREAD:
                return new Subscription(name, connection, topics) {
                    @Override
                    public void handle(Message inputMsg) throws ClaraMsgException {
                        callback.callback(inputMsg);
                    }
                };

            default:
                throw new IllegalArgumentException("invalid callback mode: " + callbackMode);
        }
    }

    /**
     * Stops the given subscription. This will not cancel the callbacks of the
     * subscription that are still pending or running in the internal
     * threadpool.
     *
     * @param handle an active subscription
     */
    public void unsubscribe(Subscription handle) {
        handle.stop();
        mySubscriptions.remove(handle.getName());
    }

    /**
     * Stops all subscriptions. This will not stop the callbacks that are still
     * pending or running in the internal threadpool.
     * <p>
     * Usually, {@link #close()} takes cares of stopping all running
     * subscriptions and callbacks. Use this method when you want to run some
     * actions between stopping the subscriptions and closing the actor
     * (like publishing a shutdown report). Otherwise just use {@link #close()}.
     */
    protected final void unsubscribeAll() {
        mySubscriptions.values().forEach(Subscription::stop);
        mySubscriptions.clear();
    }

    /**
     * Finishes all running and pending callbacks, and rejects all new ones.
     * Blocks until the callbacks have been completed, or a timeout occurs, or
     * the current thread is interrupted, whichever happens first.
     * <p>
     * This will not stop the subscriptions, but they will not be able to
     * execute new callbacks. To terminate all subscriptions, use {@link
     * #unsubscribeAll}.
     * <p>
     * Usually, {@link #close()} takes cares of stopping all running
     * subscriptions and terminating callbacks. Use this method when you want to
     * run some actions between finishing the callbacks and closing the actor,
     * (like publishing a shutdown report).
     * Otherwise just use {@link #close()}.
     */
    protected final void terminateCallbacks() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("callback pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Registers this actor on the <i>default</i> registrar service.
     * The actor will be registered as communicating through messages
     * of the given topic, using the default proxy.
     * Waits up to {@value ActorSetup#REGISTRATION_TIMEOUT}
     * milliseconds for a status response.
     *
     * @param info the parameters of the registration
     *             (publisher or subscriber, topic of interest, description)
     * @throws ClaraMsgException if the registration failed
     */
    public void register(RegInfo info) throws ClaraMsgException {
        register(info, setup.registrarAddress());
    }

    /**
     * Registers this actor on the specified registrar service.
     * The actor will be registered as communicating through messages
     * of the given topic, using the default proxy.
     * Waits up to {@value ActorSetup#REGISTRATION_TIMEOUT}
     * milliseconds for a status response.
     *
     * @param info the parameters of the registration
     *             (publisher or subscriber, topic of interest, description)
     * @param address the address of the registrar service
     * @throws ClaraMsgException if the registration failed
     */
    public void register(RegInfo info, RegAddress address) throws ClaraMsgException {
        register(info, address, ActorSetup.REGISTRATION_TIMEOUT);
    }

    /**
     * Registers this actor on the specified registrar service.
     * The actor will be registered as communicating through messages
     * of the given topic, using the default proxy.
     * Waits up to {@code timeout} milliseconds for a status response.
     *
     * @param info the parameters of the registration
     *             (publisher or subscriber, topic of interest, description)
     * @param address the address of the registrar service
     * @param timeout milliseconds to wait for a response
     * @throws ClaraMsgException if the registration failed
     */
    public void register(RegInfo info, RegAddress address, long timeout)
            throws ClaraMsgException {
        RegDriver regDriver = connectionManager.getRegistrarConnection(address);
        try {
            RegData.Builder reg = createRegistration(info);
            reg.setDescription(info.description());
            regDriver.addRegistration(myName, reg.build(), timeout);
            connectionManager.releaseRegistrarConnection(regDriver);
        } catch (ZMQException | ClaraMsgException e) {
            regDriver.close();
            throw e;
        }
    }

    /**
     * Removes this actor from the <i>default</i> registrar service.
     * The actor will be removed from the registered actors communicating
     * through messages of the given topic.
     * Waits up to {@value ActorSetup#REGISTRATION_TIMEOUT}
     * milliseconds for a status response.
     *
     * @param info the parameters used to register the actor
     *             (publisher or subscriber, the topic of interest)
     * @throws ClaraMsgException if the request failed
     */
    public void deregister(RegInfo info) throws ClaraMsgException {
        deregister(info, setup.registrarAddress());
    }

    /**
     * Removes this actor from the specified registrar service.
     * The actor will be removed from the registered actors communicating
     * through messages of the given topic.
     * Waits up to {@value ActorSetup#REGISTRATION_TIMEOUT}
     * milliseconds for a status response.
     *
     * @param info the parameters used to register the actor
     *             (publisher or subscriber, the topic of interest)
     * @param address the address of the registrar service
     * @throws ClaraMsgException if the request failed
     */
    public void deregister(RegInfo info, RegAddress address) throws ClaraMsgException {
        deregister(info, address, ActorSetup.REGISTRATION_TIMEOUT);
    }

    /**
     * Removes this actor from the specified registrar service.
     * The actor will be removed from the registered actors communicating
     * through messages of the given topic.
     * Waits up to {@code timeout} milliseconds for a status response.
     *
     * @param info the parameters used to register the actor
     *             (publisher or subscriber, the topic of interest)
     * @param address the address of the registrar service
     * @param timeout milliseconds to wait for a response
     * @throws ClaraMsgException if the request failed
     */
    public void deregister(RegInfo info, RegAddress address, long timeout)
            throws ClaraMsgException {
        RegDriver regDriver = connectionManager.getRegistrarConnection(address);
        try {
            RegData.Builder reg = createRegistration(info);
            regDriver.removeRegistration(myName, reg.build(), timeout);
            connectionManager.releaseRegistrarConnection(regDriver);
        } catch (ZMQException | ClaraMsgException e) {
            regDriver.close();
            throw e;
        }
    }

    /**
     * Searches the <i>default</i> registrar service for actors that match the given query.
     * A registered actor will be selected only if it matches all the parameters
     * of interest defined by the query. The registrar service will then reply
     * the registration data of all the matching actors.
     * Waits up to {@value ActorSetup#DISCOVERY_TIMEOUT}
     * milliseconds for a response.
     *
     * @param query the registration parameters to determine if an actor
     *              should be selected (publisher or subscriber, topic of interest)
     * @return a set with the registration data of the matching actors, if any
     * @throws ClaraMsgException if the request failed
     */
    public Set<RegRecord> discover(RegQuery query) throws ClaraMsgException {
        return discover(query, setup.registrarAddress());
    }

    /**
     * Searches the specified registrar service for actors that match the given query.
     * A registered actor will be selected only if it matches all the parameters
     * of interest defined by the query. The registrar service will then reply
     * the registration data of all the matching actors.
     * Waits up to {@value ActorSetup#DISCOVERY_TIMEOUT}
     * milliseconds for a response.
     *
     * @param query the registration parameters to determine if an actor
     *              should be selected (publisher or subscriber, topic of interest)
     * @param address the address of the registrar service
     * @return a set with the registration data of the matching actors, if any
     * @throws ClaraMsgException if the request failed
     */
    public Set<RegRecord> discover(RegQuery query, RegAddress address)
            throws ClaraMsgException {
        return discover(query, address, ActorSetup.DISCOVERY_TIMEOUT);
    }

    /**
     * Searches the specified registrar service for actors that match the given query.
     * A registered actor will be selected only if it matches all the parameters
     * of interest defined by the query. The registrar service will then reply
     * the registration data of all the matching actors.
     * Waits up to {@code timeout} milliseconds for a response.
     *
     * @param query the registration parameters to determine if an actor
     *              should be selected (publisher or subscriber, topic of interest)
     * @param address the address of the registrar service
     * @param timeout milliseconds to wait for a response
     * @return a set with the registration data of the matching actors, if any
     * @throws ClaraMsgException if the request failed
     */
    public Set<RegRecord> discover(RegQuery query, RegAddress address, long timeout)
            throws ClaraMsgException {
        RegDriver regDriver = connectionManager.getRegistrarConnection(address);
        try {
            RegData.Builder reg = query.data();
            Set<RegData> result;
            switch (query.category()) {
                case MATCHING:
                    result = regDriver.findRegistration(myName, reg.build(), timeout);
                    break;
                case FILTER:
                    result = regDriver.filterRegistration(myName, reg.build(), timeout);
                    break;
                case EXACT:
                    result = regDriver.sameRegistration(myName, reg.build(), timeout);
                    break;
                case ALL:
                    result = regDriver.allRegistration(myName, reg.build(), timeout);
                    break;
                default:
                    throw new IllegalArgumentException("invalid query type: " + query.category());
            }
            connectionManager.releaseRegistrarConnection(regDriver);

            return result.stream().map(RegRecord::new).collect(Collectors.toSet());
        } catch (ZMQException | ClaraMsgException e) {
            regDriver.close();
            throw e;
        }
    }

    /**
     * Returns the name of this actor.
     *
     * @return a string with the name
     */
    public String getName() {
        return myName;
    }

    /**
     * Returns the address of the default proxy used by this actor.
     *
     * @return the address of the default proxy
     */
    public ProxyAddress getDefaultProxyAddress() {
        return setup.proxyAddress();
    }

    /**
     * Returns the address of the default registrar used by this actor.
     *
     * @return the address of the default registrar
     */
    public RegAddress getDefaultRegistrarAddress() {
        return setup.registrarAddress();
    }

    /**
     * Returns the size of the callback thread pool.
     *
     * @return the pool size to run callbacks
     */
    public int getPoolSize() {
        return threadPool.getMaximumPoolSize();
    }

    private RegData.Builder createRegistration(RegInfo info) {
        return RegFactory.newRegistration(myName, setup.proxyAddress(),
                                          info.type(), info.topic());
    }


    private final class RejectedCallbackHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("Rejected callback execution for subscribed message.");
        }
    }
}
