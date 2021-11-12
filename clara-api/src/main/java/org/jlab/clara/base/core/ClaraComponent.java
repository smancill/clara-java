/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base.core;

// checkstyle.off: ParameterNumber
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.msg.core.ActorSetup;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.net.ProxyAddress;

import java.text.MessageFormat;

/**
 *  Clara component. This is used to define
 *  service, container, DPE and orchestrator components.
 *
 * @author gurjyan
 * @since 4.x
 */
public final class ClaraComponent {

    public static final String DPE_NAME_REGEX;
    public static final String CONTAINER_NAME_REGEX;
    public static final String ENGINE_NAME_REGEX;

    public static final String SERVICE_NAME_REGEX;
    public static final String CANONICAL_NAME_REGEX;

    static {
        final String ip = "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}";

        final String port = "[0-9]+";
        final String lang = "(?:"
                + ClaraConstants.JAVA_LANG + "|"
                + ClaraConstants.CPP_LANG + "|"
                + ClaraConstants.PYTHON_LANG + ")";

        final String portPart = ClaraConstants.PORT_SEP + port;
        final String langPart = ClaraConstants.LANG_SEP + lang;

        DPE_NAME_REGEX = ip + "(?:" + portPart + ")?" + langPart;
        CONTAINER_NAME_REGEX = "[\\w-]+";
        ENGINE_NAME_REGEX = "\\w+";

        SERVICE_NAME_REGEX = MessageFormat.format("{1}{0}{2}{0}{3}",
                Topic.SEPARATOR, DPE_NAME_REGEX, CONTAINER_NAME_REGEX, ENGINE_NAME_REGEX);

        CANONICAL_NAME_REGEX = MessageFormat.format("({1})(?:{0}({2})(?:{0}({3}))?)?",
                Topic.SEPARATOR, DPE_NAME_REGEX, CONTAINER_NAME_REGEX, ENGINE_NAME_REGEX);
    }

    private Topic topic;

    private String dpeLang;
    private String dpeHost;
    private int dpePort;

    private String dpeCanonicalName;
    private String containerName;
    private String engineName;
    private String engineClass;

    private String canonicalName;
    private String description;
    private String initialState;

    private int subscriptionPoolSize;

    private boolean isOrchestrator = false;
    private boolean isDpe = false;
    private boolean isContainer = false;
    private boolean isService = false;

    private ClaraComponent(String dpeLang, String dpeHost, int dpePort,
                           String container, String engine, String engineClass,
                           int subscriptionPoolSize, String description,
                           String initialState) {
        this.dpeLang = dpeLang;
        this.subscriptionPoolSize = subscriptionPoolSize;
        this.dpeHost = dpeHost;
        this.dpePort = dpePort;
        if (dpePort == ClaraUtil.getDefaultPort(dpeLang)) {
            this.dpeCanonicalName = dpeHost + ClaraConstants.LANG_SEP + dpeLang;
        } else {
            this.dpeCanonicalName = dpeHost + ClaraConstants.PORT_SEP
                                  + dpePort + ClaraConstants.LANG_SEP
                                  + dpeLang;
        }
        this.containerName = container;
        this.engineName = engine;
        this.engineClass = engineClass;
        if (engine != null && !engine.equalsIgnoreCase(Topic.ANY)) {
            topic = Topic.build(dpeCanonicalName, containerName, engineName);
            canonicalName = topic.toString();
        } else if (container != null && !container.equalsIgnoreCase(Topic.ANY)) {
            topic = Topic.build(ClaraConstants.CONTAINER, dpeCanonicalName, containerName);
            canonicalName = Topic.build(dpeCanonicalName, containerName).toString();
        } else {
            topic = Topic.build(ClaraConstants.DPE, dpeCanonicalName);
            canonicalName = Topic.build(dpeCanonicalName).toString();
        }
        this.description = description;
        this.initialState = initialState;
    }


    /**
     * Creates and returns Clara orchestrator.
     *
     * @param name                 of the orchestrator
     * @param dpeHost              host of the PDE to communicate with
     * @param dpePort              port of the DPE to communicate with
     * @param dpeLang              language of the DPE
     * @param subscriptionPoolSize pool size for the
     *                             orchestrator to be used for subscriptions
     * @param description          textual description of this orchestrator
     * @return orchestrator {@link org.jlab.clara.base.core.ClaraComponent} object
     */
    public static ClaraComponent orchestrator(String name,
                                              String dpeHost,
                                              int dpePort,
                                              String dpeLang,
                                              int subscriptionPoolSize,
                                              String description) {
        ClaraComponent a = new ClaraComponent(dpeLang,
                dpeHost,
                dpePort,
                Topic.ANY,
                Topic.ANY,
                ClaraConstants.UNDEFINED,
                subscriptionPoolSize,
                description,
                ClaraConstants.UNDEFINED);
        a.setCanonicalName(name);
        a.isOrchestrator = true;
        return a;
    }

    /**
     * Creates and returns Clara orchestrator. Uses default DPE port.
     *
     * @param name                 of the orchestrator
     * @param dpeHost              host of the PDE to communicate with
     * @param dpeLang              language of the DPE
     * @param subscriptionPoolSize pool size for the
     *                             orchestrator to be used for subscriptions
     * @param description          textual description of this orchestrator
     * @return the orchestrator component
     */
    public static ClaraComponent orchestrator(String name, String dpeHost, String dpeLang,
                                              int subscriptionPoolSize, String description) {
        return orchestrator(name,
                            dpeHost, ClaraUtil.getDefaultPort(dpeLang), dpeLang,
                            subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara orchestrator. Default port of the DP and Java lang is used.
     *
     * @param name                 of the orchestrator
     * @param dpeHost              host of the PDE to communicate with
     * @param subscriptionPoolSize pool size for the
     *                             orchestrator to be used for subscriptions
     * @param description          textual description of this orchestrator
     * @return the orchestrator component
     */
    public static ClaraComponent orchestrator(String name, String dpeHost,
                                              int subscriptionPoolSize, String description) {
        return orchestrator(name,
                            dpeHost, ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                            subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara orchestrator. DPE on the local host, with
     * the default port and Java lang is used.
     *
     * @param name of the orchestrator
     * @param subscriptionPoolSize pool size for the
     *                             orchestrator to be used for subscriptions
     * @param description textual description of this orchestrator
     * @return the orchestrator component
     */
    public static ClaraComponent orchestrator(String name,
                                              int subscriptionPoolSize,
                                              String description) {
        return orchestrator(name,
                            ClaraUtil.localhost(),
                            ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                            subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara DPE component.
     *
     * @param dpeHost              host where the DPE will run
     * @param dpePort              port of the DPE will use
     * @param dpeLang              language of the DPE
     * @param subscriptionPoolSize pool size for the
     *                             DPE to be used for subscriptions
     * @param description          textual description of the DPE
     * @return the DPE component
     */
    public static ClaraComponent dpe(String dpeHost, int dpePort, String dpeLang,
                                     int subscriptionPoolSize, String description) {
        ClaraComponent a = new ClaraComponent(dpeLang,
                dpeHost,
                dpePort,
                Topic.ANY,
                Topic.ANY,
                ClaraConstants.UNDEFINED,
                subscriptionPoolSize, description,
                ClaraConstants.UNDEFINED);
        a.isDpe = true;
        return a;
    }

    /**
     * Creates and returns Clara DPE component. The default DPE port is used.
     *
     * @param dpeHost host where the DPE will run
     * @param dpeLang language of the DPE
     * @param subscriptionPoolSize pool size for the
     *                             DPE to be used for subscriptions
     * @param description textual description of the DPE
     * @return the DPE component
     */
    public static ClaraComponent dpe(String dpeHost, String dpeLang,
                                     int subscriptionPoolSize, String description) {
        return dpe(dpeHost, ClaraUtil.getDefaultPort(dpeLang), dpeLang,
                   subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara DPE component. The default DPE port and Java lang is used.
     *
     * @param dpeHost host where the DPE will run
     * @param subscriptionPoolSize pool size for the
     *                             DPE to be used for subscriptions
     * @param description textual description of the DPE
     * @return the DPE component
     */
    public static ClaraComponent dpe(String dpeHost,
                                     int subscriptionPoolSize,
                                     String description) {
        return dpe(dpeHost, ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                   subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara DPE component.
     * The local host, default DPE port and Java lang is used.
     *
     * @param subscriptionPoolSize pool size for the
     *                             DPE to be used for subscriptions
     * @param description textual description of the DPE
     * @return the DPE component
     */
    public static ClaraComponent dpe(int subscriptionPoolSize, String description) {
        return dpe(ClaraUtil.localhost(), ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                   subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara DPE component.
     * DPE default settings are used
     *
     * @return the DPE component
     */
    public static ClaraComponent dpe() {
        return dpe(ClaraUtil.localhost(), ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                   1, ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara DPE component from the Clara component
     * canonical name. DPE default pool-size = 1 is used.
     *
     * @param canonicalName The canonical name of a component
     * @param description textual description of the DPE
     * @return the DPE component
     */
    public static ClaraComponent dpe(String canonicalName, String description) {
        if (!ClaraUtil.isCanonicalName(canonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + canonicalName);
        }
        return dpe(ClaraUtil.getDpeHost(canonicalName),
                   ClaraUtil.getDpePort(canonicalName),
                   ClaraUtil.getDpeLang(canonicalName),
                   1, description);
    }

    /**
     * Creates and returns Clara DPE component from the Clara component
     * canonical name. DPE default pool-size = 1 is used, leaving description
     * of the DPE undefined.
     *
     * @param canonicalName The canonical name of a component
     * @return the DPE component
     */
    public static ClaraComponent dpe(String canonicalName) {
        return dpe(canonicalName, ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Container component.
     *
     * @param dpeHost              host of the DPE where container is/(will be) deployed
     * @param dpePort              port of the DPE where container is/(will be) deployed
     * @param dpeLang              language of the DPE
     * @param container            the name of the container
     * @param subscriptionPoolSize pool size for the
     *                             container to be used for subscriptions
     * @param description          textual description of the container
     * @return the container component
     */
    public static ClaraComponent container(String dpeHost, int dpePort, String dpeLang,
                                           String container,
                                           int subscriptionPoolSize, String description) {
        ClaraComponent a = new ClaraComponent(dpeLang,
                dpeHost,
                dpePort,
                container,
                Topic.ANY,
                ClaraConstants.UNDEFINED,
                subscriptionPoolSize, description,
                ClaraConstants.UNDEFINED);
        a.isContainer = true;
        return a;
    }

    /**
     * Creates and returns Clara Container component. The default DPE port is used.
     *
     * @param dpeHost host of the DPE where container is/(will be) deployed
     * @param dpeLang language of the DPE
     * @param container the name of the container
     * @param subscriptionPoolSize pool size for the
     *                             container to be used for subscriptions
     * @param description textual description of the container
     * @return the container component
     */
    public static ClaraComponent container(String dpeHost, String dpeLang,
                                           String container, int subscriptionPoolSize,
                                           String description) {
        return container(dpeHost, ClaraUtil.getDefaultPort(dpeLang), dpeLang,
                         container, subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara Container component. The default DPE port and Java lang is used.
     *
     * @param dpeHost host of the DPE where container is/(will be) deployed
     * @param container the name of the container
     * @param subscriptionPoolSize pool size for the
     *                             container to be used for subscriptions
     * @param description textual description of the container
     * @return the container component
     */
    public static ClaraComponent container(String dpeHost, String container,
                                           int subscriptionPoolSize, String description) {
        return container(dpeHost, ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                         container, subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara Container component. The DPE running on a local host,
     * default DPE port and Java lang is used.
     *
     * @param container the name of the container
     * @param subscriptionPoolSize pool size for the
     *                             container to be used for subscriptions
     * @param description textual description of the container
     * @return the container component
     */
    public static ClaraComponent container(String container,
                                           int subscriptionPoolSize,
                                           String description) {
        return container(ClaraUtil.localhost(),
                         ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                         container,
                         subscriptionPoolSize, description);
    }

    /**
     * Creates and returns Clara Container component, using the container canonical name.
     * Default subscriptions pool-size = 1 is used.
     *
     * @param containerCanonicalName the canonical name of the container
     * @param description textual description of the container
     * @return the container component
     */
    public static ClaraComponent container(String containerCanonicalName, String description) {
        if (!ClaraUtil.isCanonicalName(containerCanonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + containerCanonicalName);
        }
        return container(ClaraUtil.getDpeHost(containerCanonicalName),
                         ClaraUtil.getDpePort(containerCanonicalName),
                         ClaraUtil.getDpeLang(containerCanonicalName),
                         ClaraUtil.getContainerName(containerCanonicalName),
                         ActorSetup.DEFAULT_POOL_SIZE,
                         description);
    }

    /**
     * Creates and returns Clara Container component, using the container canonical name.
     * Default subscriptions pool-size = 1 is used.
     *
     * @param containerCanonicalName the canonical name of the container
     * @return the container component
     */
    public static ClaraComponent container(String containerCanonicalName) {
        return container(containerCanonicalName, ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Service component.
     *
     * @param dpeHost host of the DPE where service is/(will be) deployed
     * @param dpePort port of the DPE where service is/(will be) deployed
     * @param dpeLang language of the DPE
     * @param container the name of the container of the service
     * @param engine the name of the service engine
     * @param engineClass engine full class name (package name)
     * @param subscriptionPoolSize pool size for the
     *                             service to be used for subscriptions
     * @param description textual description of the service
     * @param initialState the initial state of the service
     * @return the service component
     */
    public static ClaraComponent service(String dpeHost, int dpePort, String dpeLang,
                                         String container, String engine, String engineClass,
                                         int subscriptionPoolSize, String description,
                                         String initialState) {
        ClaraComponent a = new ClaraComponent(dpeLang,
                dpeHost,
                dpePort,
                container,
                engine,
                engineClass,
                subscriptionPoolSize,
                description,
                initialState);
        a.isService = true;
        return a;
    }

    /**
     * Creates and returns Clara Service component. Default pool-size=1 is used.
     * The description of the service is undefined.
     *
     * @param dpeHost host of the DPE where service is/(will be) deployed
     * @param dpePort port of the DPE where service is/(will be) deployed
     * @param dpeLang language of the DPE
     * @param container the name of the container of the service
     * @param engine the name of the service engine
     * @return the service component
     */
    public static ClaraComponent service(String dpeHost, int dpePort, String dpeLang,
                                         String container, String engine) {
        return service(dpeHost, dpePort, dpeLang,
                       container, engine, ClaraConstants.UNDEFINED,
                       1, ClaraConstants.UNDEFINED,
                       ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Service component.
     * DPE default port and default pool-size=1 is used.
     * The description of the service is undefined.
     *
     * @param dpeHost host of the DPE where service is/(will be) deployed
     * @param dpeLang language of the DPE
     * @param container the name of the container of the service
     * @param engine the name of the service engine
     * @return the service component
     */
    public static ClaraComponent service(String dpeHost, String dpeLang,
                                         String container, String engine) {
        return service(dpeHost, ClaraUtil.getDefaultPort(dpeLang), dpeLang,
                       container, engine, ClaraConstants.UNDEFINED,
                       1, ClaraConstants.UNDEFINED,
                       ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Service component. DPE running on a local host with the
     * default port and default pool-size=1 is used. The description of the service is undefined.
     *
     * @param container the name of the container of the service
     * @param engine the name of the service engine
     * @return the service component
     */
    public static ClaraComponent service(String container, String engine) {
        return service(ClaraUtil.localhost(), ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                       container, engine, ClaraConstants.UNDEFINED,
                       1, ClaraConstants.UNDEFINED,
                       ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Service component. DPE running on a local host with the
     * default port used. The description of the service is undefined.
     *
     * @param container the name of the container of the service
     * @param engine the name of the service engine
     * @param poolSize pool size for the service subscriptions
     * @return the service component
     */
    public static ClaraComponent service(String container, String engine, int poolSize) {
        return service(ClaraUtil.localhost(), ClaraConstants.JAVA_PORT, ClaraConstants.JAVA_LANG,
                       container, engine, ClaraConstants.UNDEFINED,
                       poolSize, ClaraConstants.UNDEFINED,
                       ClaraConstants.UNDEFINED);
    }

    /**
     * Creates and returns Clara Service component, using the service canonical name.
     * Default subscriptions pool-size = 1 is used. The description of the service is undefined.
     *
     * @return the service component
     */
    public static ClaraComponent service(String serviceCanonicalName) {
        if (!ClaraUtil.isCanonicalName(serviceCanonicalName)) {
            throw new IllegalArgumentException("Not a canonical name: " + serviceCanonicalName);
        }
        return service(ClaraUtil.getDpeHost(serviceCanonicalName),
                       ClaraUtil.getDpePort(serviceCanonicalName),
                       ClaraUtil.getDpeLang(serviceCanonicalName),
                       ClaraUtil.getContainerName(serviceCanonicalName),
                       ClaraUtil.getEngineName(serviceCanonicalName),
                       ClaraConstants.UNDEFINED,
                       ActorSetup.DEFAULT_POOL_SIZE, ClaraConstants.UNDEFINED,
                       ClaraConstants.UNDEFINED);
    }

    /**
     * Returns the topic of the Clara component, i.e. the topic of the subscriber.
     * Note that all Clara components are registered as subscribers.
     *
     * @return {@link Topic} object
     */
    public Topic getTopic() {
        return topic;
    }

    public String getDpeLang() {
        return dpeLang;
    }

    public String getDpeHost() {
        return dpeHost;
    }

    public int getDpePort() {
        return dpePort;
    }

    /**
     * Returns DPE canonical, constructed as "dpeHost % dpePort _ dpeLang".
     *
     * @return DPE canonical name
     */
    public String getDpeCanonicalName() {
        return dpeCanonicalName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineClass() {
        return engineClass;
    }

    /**
     * Sets the engine class which is the package name of the class.
     *
     * @param engineClass package name of the class
     */
    public void setEngineClass(String engineClass) {
        this.engineClass = engineClass;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Note: candidate to be deprecated. Do not use to define the
     * canonical names for DPE, container or service.
     *
     * The canonical name of a Clara component is defined internally,
     * yet this method is used to set the name of an orchestrator, which
     * considers to be non-critical.
     *
     * @param canonicalName canonical name
     */
    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public int getSubscriptionPoolSize() {
        return subscriptionPoolSize;
    }

    /**
     * Sets the subscription pool-size of the component.
     *
     * @param subscriptionPoolSize pool size
     */
    public void setSubscriptionPoolSize(int subscriptionPoolSize) {
        this.subscriptionPoolSize = subscriptionPoolSize;
    }

    public boolean isOrchestrator() {
        return isOrchestrator;
    }

    public boolean isDpe() {
        return isDpe;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public boolean isService() {
        return isService;
    }

    /**
     * Returns the DPE proxy address.
     *
     * @return {@link ProxyAddress} object
     */
    public ProxyAddress getProxyAddress() {
        return new ProxyAddress(getDpeHost(), getDpePort());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInitialState() {
        return initialState;
    }

    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    @Override
    public String toString() {
        return canonicalName;
    }
}
