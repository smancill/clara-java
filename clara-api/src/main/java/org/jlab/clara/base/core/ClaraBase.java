/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base.core;

import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.ActorSetup;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Connection;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Subscription;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegInfo;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.data.RegRecord;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.util.EnvUtils;
import org.jlab.clara.util.report.ReportType;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 *  Clara base class providing methods build services,
 *  service container and orchestrator.
 *
 * @author gurjyan
 * @since 4.x
 */
public class ClaraBase extends Actor {

    private final String claraHome;
    // reference to this component description
    private final ClaraComponent me;

    // reference to the front end DPE
    private ClaraComponent frontEnd;

    /**
     * A Clara component that can send and receives messages.
     *
     * @param me        definition of the component
     * @param frontEnd  definition of the front-end
     */
    public ClaraBase(ClaraComponent me, ClaraComponent frontEnd) {
        super(me.getCanonicalName(), setup(me, frontEnd));
        this.me = me;
        this.frontEnd = frontEnd;
        this.claraHome = EnvUtils.claraHome();
    }

    private static ActorSetup setup(ClaraComponent me, ClaraComponent frontEnd) {
        ActorSetup.Builder builder = ActorSetup.newBuilder()
                        .withProxy(me.getProxyAddress())
                        .withRegistrar(getRegAddress(frontEnd))
                        .withPoolSize(me.getSubscriptionPoolSize())
                        .withPreConnectionSetup(s -> {
                            s.setRcvHWM(0);
                            s.setSndHWM(0);
                        })
                        .withPostConnectionSetup(() -> ActorUtils.sleep(100));
        if (me.isOrchestrator()) {
            builder.checkSubscription(false);
        }
        return builder.build();
    }

    /**
     * @return the path to the Clara_home defined
     * by means of the CLARA_HOME env variable.
     */
    public String getClaraHome() {
        return claraHome;
    }

    /**
     * Returns the definition of this component.
     */
    public ClaraComponent getMe() {
        return me;
    }

    /**
     * Returns the description of this component.
     */
    public String getDescription() {
        return me.getDescription();
    }

    /**
     * Stores a connection to the default proxy in the connection pool.
     *
     * @throws ClaraException if a connection could not be created or connected
     */
    public void cacheLocalConnection() throws ClaraException {
        try {
            cacheConnection();
        } catch (ClaraMsgException e) {
            throw new ClaraException("could not connect to local proxy", e);
        }
    }

    /**
     * Sends a message to the address of the given Clara component.
     *
     * @param component the component that shall receive the message
     * @param msg the message to be published
     * @throws ClaraMsgException if the message could not be sent
     */
    public void send(ClaraComponent component, Message msg)
            throws ClaraMsgException {
        msg.getMetaData().setSender(myName);
        publish(component.getProxyAddress(), msg);
    }

    /**
     * Sends a string to the given Clara component.
     *
     * @param component the component that shall receive the message
     * @param requestText string of the message
     * @throws ClaraMsgException if the message could not be sent
     */
    public void send(ClaraComponent component, String requestText)
            throws ClaraMsgException {
        Message msg = MessageUtil.buildRequest(component.getTopic(), requestText);
        send(component, msg);
    }

    /**
     * Sends a message using the specified connection.
     *
     * @param con the connection that shall be used to publish the message
     * @param msg the message to be published
     * @throws ClaraMsgException if the message could not be sent
     */
    public void send(Connection con, Message msg)
            throws ClaraMsgException {
        msg.getMetaData().setSender(myName);
        publish(con, msg);
    }

    /**
     * Sends a message to the address of this Clara component.
     *
     * @param msg the message to be published
     * @throws ClaraMsgException if the message could not be sent
     */
    public void send(Message msg)
            throws ClaraMsgException {
        send(me, msg);
    }

    /**
     * Sends a text message to this Clara component.
     *
     * @param msgText string of the message
     * @throws ClaraMsgException if the message could not be sent
     */
    public void send(String msgText)
            throws ClaraMsgException {
        send(me, msgText);
    }

    /**
     * Synchronous sends a message to the address of the given Clara component.
     *
     * @param component the component that shall receive the message
     * @param msg the message to be published
     * @param timeout in milliseconds
     * @throws ClaraMsgException if the message could not be sent
     * @throws TimeoutException if a response was not received
     */
    public Message syncSend(ClaraComponent component, Message msg, long timeout)
            throws ClaraMsgException, TimeoutException {
        msg.getMetaData().setSender(myName);
        return syncPublish(component.getProxyAddress(), msg, timeout);
    }

    /**
     * Synchronous sends a string to the given Clara component.
     *
     * @param component the component that shall receive the message
     * @param requestText string of the message
     * @param timeout in milli seconds
     * @throws ClaraMsgException if the message could not be sent
     * @throws TimeoutException if a response was not received
     */
    public Message syncSend(ClaraComponent component, String requestText, long timeout)
            throws ClaraMsgException, TimeoutException {
        Message msg = MessageUtil.buildRequest(component.getTopic(), requestText);
        return syncSend(component, msg, timeout);
    }

    /**
     * Listens for messages published to the given component.
     *
     * @param component a component defining the topic of interest
     * @param callback the callback action
     * @return a handler to the subscription
     * @throws ClaraException if the subscription could not be started
     */
    public Subscription listen(ClaraComponent component, Callback callback)
            throws ClaraException {
        return listen(me, component.getTopic(), callback);
    }

    /**
     * Listens for messages of given topic published to the address of the given
     * component.
     *
     * @param component a component defining the address to connect
     * @param topic topic of interest
     * @param callback the callback action
     * @return a handler to the subscription
     * @throws ClaraException if the subscription could not be started
     */
    public Subscription listen(ClaraComponent component, Topic topic, Callback callback)
            throws ClaraException {
        try {
            return subscribe(component.getProxyAddress(), topic, callback);
        } catch (ClaraMsgException e) {
            throw new ClaraException("could not subscribe to " + topic);
        }
    }

    /**
     * Listens for messages of given topic published to the address of this
     * component.
     *
     * @param topic topic of interest
     * @param callback the callback action
     * @return a handler to the subscription
     * @throws ClaraException if the subscription could not be started
     */
    public Subscription listen(Topic topic, Callback callback)
            throws ClaraException {
        return listen(me, topic, callback);
    }

    /**
     * Stops listening to a subscription defined by the handler.
     *
     * @param handle the subscription handler
     */
    public void stopListening(Subscription handle) {
        unsubscribe(handle);
    }

    /**
     * Terminates all callbacks.
     */
    public void stopCallbacks() {
        terminateCallbacks();
    }

    /**
     * Registers this component with the front-end as subscriber to the given topic.
     *
     * @param topic the subscribed topic
     * @param description a description of the component
     * @throws ClaraException if registration failed
     */
    public void register(Topic topic, String description) throws ClaraException {
        RegAddress regAddress = getRegAddress(frontEnd);
        try {
            register(RegInfo.subscriber(topic, description), regAddress);
        } catch (ClaraMsgException e) {
            throw new ClaraException("could not register with front-end = " + regAddress, e);
        }
    }

    /**
     * Remove the registration of this component from the front-end as
     * subscriber to the given topic.
     *
     * @param topic the subscribed topic
     * @throws ClaraException if removing the registration failed
     */
    public void removeRegistration(Topic topic) throws ClaraException {
        RegAddress regAddress = getRegAddress(frontEnd);
        try {
            deregister(RegInfo.subscriber(topic), regAddress);
        } catch (ClaraMsgException e) {
            throw new ClaraException("could not deregister from front-end = " + regAddress, e);
        }
    }

    /**
     * Retrieves Clara actor registration information from the registrar service.
     *
     * @param regHost registrar server host
     * @param regPort registrar server port
     * @param topic   the canonical name of an actor: {@link Topic}
     * @return set of {@link org.jlab.clara.msg.data.RegDataProto.RegData} objects
     * @throws IOException
     * @throws ClaraMsgException
     */
    public Set<RegRecord> discover(String regHost, int regPort, Topic topic)
            throws IOException, ClaraMsgException {
        RegAddress regAddress = new RegAddress(regHost, regPort);
        return discover(RegQuery.subscribers(topic), regAddress, 1000);
    }

    /**
     * Retrieves Clara actor registration information from the registrar service,
     * assuming registrar is running using the default port.
     *
     * @param regHost registrar server host
     * @param topic   the canonical name of an actor: {@link Topic}
     * @return set of {@link org.jlab.clara.msg.data.RegDataProto.RegData} objects
     * @throws IOException
     * @throws ClaraMsgException
     */
    public Set<RegRecord> discover(String regHost, Topic topic)
            throws IOException, ClaraMsgException {
        RegAddress regAddress = new RegAddress(regHost);
        return discover(RegQuery.subscribers(topic), regAddress);
    }

    /**
     * Retrieves Clara actor registration information from the registrar service,
     * assuming registrar is running on a local host, using the default port.
     *
     * @param topic the canonical name of an actor: {@link Topic}
     * @return set of {@link org.jlab.clara.msg.data.RegDataProto.RegData} objects
     * @throws IOException
     * @throws ClaraMsgException
     */
    public Set<RegRecord> discover(Topic topic)
            throws IOException, ClaraMsgException {
        return discover(RegQuery.subscribers(topic));
    }

    public static RegAddress getRegAddress(ClaraComponent fe) {
        return new RegAddress(fe.getDpeHost(), fe.getDpePort() + ClaraConstants.REG_PORT_SHIFT);
    }

    /**
     * Sync asks DPE to report.
     *
     * @param component dpe as a {@link ClaraComponent#dpe()} object
     * @param timeout sync request timeout
     * @return message {@link Message}
     *         back from a dpe.
     * @throws IOException
     * @throws ClaraMsgException
     * @throws TimeoutException
     */
    public Message pingDpe(ClaraComponent component, int timeout)
            throws IOException, ClaraMsgException, TimeoutException {

        if (component.isDpe()) {
            String data = MessageUtil.buildData(ReportType.INFO.getValue());
            Topic topic = component.getTopic();
            Message msg = MessageUtil.buildRequest(topic, data);
            return syncSend(component, msg, timeout);
        }
        return null;
    }


    /**
     * Returns the reference to the front-end DPE.
     *
     * @return {@link org.jlab.clara.base.core.ClaraComponent} object
     */
    public ClaraComponent getFrontEnd() {
        return frontEnd;
    }

    /**
     * Sets a DPE Clara component as a front-end.
     *
     * @param frontEnd {@link org.jlab.clara.base.core.ClaraComponent} object
     */
    public void setFrontEnd(ClaraComponent frontEnd) {
        this.frontEnd = frontEnd;
    }
}
