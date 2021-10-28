/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.data.RegRecord;
import org.json.JSONObject;

/**
 * The standard filters to select Clara DPEs, containers or services.
 */
public final class ClaraFilters {

    private ClaraFilters() { }

    /**
     * Returns a filter to select all DPEs in the Clara cloud.
     * The filter will select every running DPE, of any language.
     *
     * @return a filter for all DPEs
     */
    public static DpeFilter allDpes() {
        return dpes();
    }

    /**
     * Returns a filter to select all containers in the Clara cloud.
     * The filter will select every deployed container, in every DPE, of any language.
     *
     * @return a filter for all containers
     */
    public static ContainerFilter allContainers() {
        return containers();
    }

    /**
     * Returns a filter to select all services in the Clara cloud.
     * The filter will select every deployed service, in every container of every DPE,
     * of any language.
     *
     * @return a filter for all services
     */
    public static ServiceFilter allServices() {
        return services();
    }

    /**
     * Returns a filter to select a specific DPE.
     * <p>
     * Example: the DPE {@code 10.2.9.100_java}.
     *
     * @param dpe the selected DPE
     * @return a filter for a single DPE
     */
    static DpeFilter dpe(DpeName dpe) {
        var topic = Topic.build("dpe", dpe.canonicalName());
        return new DpeFilter(withTopic(topic));
    }

    /**
     * Returns a filter to select all the DPEs of the given host.
     * A host can contain multiple DPEs of different languages.
     * The filter will select all DPEs running in the specified host.
     * <p>
     * Example: all the DPEs on the host {@code 10.2.9.100}.
     *
     * @param host the selected host
     * @return a filter for all DPEs in the host
     */
    public static DpeFilter dpesByHost(String host) {
        return dpes(host);
    }

    /**
     * Returns a filter to select all the DPEs of the given language.
     * The filter will select every running DPE of the specified language.
     * <p>
     * Example: all the {@code java} DPEs.
     *
     * @param lang the language to filter
     * @return a filter for all DPEs of the given language
     */
    public static DpeFilter dpesByLanguage(ClaraLang lang) {
        var filter = dpes();
        filter.addRegFilter(r -> sameLang(r, lang));
        return filter;
    }

    /**
     * Returns a filter to select a specific container.
     * <p>
     * Example: the container {@code 10.2.9.100_java:master}.
     *
     * @param container the selected container
     * @return a filter for a single container
     */
    static ContainerFilter container(ContainerName container) {
        var topic = Topic.build("container", container.canonicalName());
        var filter = new ContainerFilter(withTopic(topic));
        filter.addJsonFilter(o -> sameName(o, container));
        return filter;
    }

    /**
     * Returns a filter to select all the containers of the given host.
     * A host can contain multiple DPEs of different languages.
     * The filter will select all containers deployed on every DPE running
     * in the specified host.
     * <p>
     * Example: all the containers on the host {@code 10.2.9.100}.
     *
     * @param host the selected host
     * @return a filter for all containers in the host
     */
    public static ContainerFilter containersByHost(String host) {
        return containers(host);
    }

    /**
     * Returns a filter to select all the containers of the given DPE.
     * A host can contain multiple DPEs of different languages.
     * The filter will select every container deployed on the specified DPE.
     * <p>
     * Example: all the containers on the DPE {@code 10.2.9.100_cpp}.
     *
     * @param dpeName the selected DPE
     * @return a filter for all containers in the DPE
     */
    public static ContainerFilter containersByDpe(DpeName dpeName) {
        var filter = containers(dpeName.address().host());
        filter.addRegFilter(r -> sameDpe(r, dpeName));
        return filter;
    }

    /**
     * Returns a filter to select all the containers of the given language.
     * The filter will select all containers deployed on every running DPE of
     * the specified language.
     * <p>
     * Example: all the {@code cpp} containers.
     *
     * @param lang the language to filter
     * @return a filter for all containers of the given language
     */
    public static ContainerFilter containersByLanguage(ClaraLang lang) {
        var filter = containers();
        filter.addRegFilter(r -> sameLang(r, lang));
        return filter;
    }

    /**
     * Returns a filter to select all the containers of the given name.
     * The filter will select every container deployed on any running DPE whose
     * name matches the specified name. The match must be exact.
     * <p>
     * Example: all containers named {@code master}.
     *
     * @param name the container name to filter
     * @return a filter for all containers with the given name
     */
    public static ContainerFilter containersByName(String name) {
        var filter = containers();
        filter.addRegFilter(r -> ClaraUtil.getContainerName(name(r)).equals(name));
        filter.addJsonFilter(o -> ClaraUtil.getContainerName(name(o)).equals(name));
        return filter;
    }

    /**
     * Returns a filter to select a specific service.
     * <p>
     * Example: the service {@code 10.2.9.100_java:master:SqrRoot}.
     *
     * @param name the selected service
     * @return a filter for a single service
     */
    static ServiceFilter service(ServiceName name) {
        var topic = Topic.wrap(name.canonicalName());
        var filter = new ServiceFilter(withTopic(topic));
        filter.addJsonFilter(o -> sameName(o, name));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given host.
     * A host can contain multiple DPEs of different languages.
     * The filter will select all services deployed on every DPE running
     * in the specified host.
     * <p>
     * Example: all the services on the host {@code 10.2.9.100}.
     *
     * @param host the selected host
     * @return a filter for all services in the host
     */
    public static ServiceFilter servicesByHost(String host) {
        return services(host);
    }

    /**
     * Returns a filter to select all the services of the given DPE.
     * A host can contain multiple DPEs of different languages.
     * The filter will select every service deployed on the specified DPE.
     * <p>
     * Example: all the services on the DPE {@code 10.2.9.100_cpp}.
     *
     * @param dpeName the selected DPE
     * @return a filter for all services in the DPE
     */
    public static ServiceFilter servicesByDpe(DpeName dpeName) {
        var filter = services(dpeName.address().host());
        filter.addRegFilter(r -> sameDpe(r, dpeName));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given container.
     * The filter will select every service deployed on the specified container.
     * <p>
     * Example: all the services on the container {@code 10.2.9.100_cpp:master}.
     *
     * @param container the selected container
     * @return a filter for all services in the container
     */
    public static ServiceFilter servicesByContainer(ContainerName container) {
        var filter = services(container.address().host());
        filter.addRegFilter(r -> sameContainer(name(r), container));
        filter.addJsonFilter(o -> sameContainer(name(o), container));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given language.
     * The filter will select all services deployed on every running DPE of
     * the specified language.
     * <p>
     * Example: all the {@code java} services.
     *
     * @param lang the language to filter
     * @return a filter for all services of the given language
     */
    public static ServiceFilter servicesByLanguage(ClaraLang lang) {
        var filter = services();
        filter.addRegFilter(r -> sameLang(r, lang));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given name.
     * The filter will select every service deployed on any running DPE whose
     * engine matches the specified name. The match must be exact.
     * <p>
     * Example: all services named {@code SqrRoot}.
     *
     * @param name the engine name to filter
     * @return a filter for all services of the given name
     */
    public static ServiceFilter servicesByName(String name) {
        var filter = services();
        filter.addRegFilter(r -> ClaraUtil.getEngineName(name(r)).equals(name));
        filter.addJsonFilter(o -> ClaraUtil.getEngineName(name(o)).equals(name));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given author.
     * The filter will select every service deployed on any running DPE whose
     * author matches the specified name. The match must be exact.
     * <p>
     * Example: all services developed by {@code John Doe}.
     *
     * @param authorName the author name to filter
     * @return a filter for all services by the given author
     */
    public static ServiceFilter servicesByAuthor(String authorName) {
        var filter = services();
        filter.addJsonFilter(o -> o.getString("author").equals(authorName));
        return filter;
    }

    /**
     * Returns a filter to select all the services of the given description.
     * The filter will select every service deployed on any running DPE whose
     * description matches the specified regular expression.
     * <p>
     * Example: all services with the regex in its description.
     *
     * @param regex the engine name to filter
     * @return a filter for all services by a matching description
     */
    public static ServiceFilter servicesByDescription(String regex) {
        var filter = services();
        filter.addJsonFilter(o -> o.getString("description").matches(regex));
        return filter;
    }

    private static DpeFilter dpes() {
        return new DpeFilter(RegQuery.subscribers().withDomain("dpe"));
    }

    private static DpeFilter dpes(String host) {
        var filter = new DpeFilter(RegQuery.subscribers().withHost(host));
        filter.addRegFilter(r -> r.topic().domain().equals("dpe"));
        return filter;
    }

    private static ContainerFilter containers() {
        return new ContainerFilter(RegQuery.subscribers().withDomain("container"));
    }


    private static ContainerFilter containers(String host) {
        var filter = new ContainerFilter(RegQuery.subscribers().withHost(host));
        filter.addRegFilter(r -> r.topic().domain().equals("container"));
        return filter;
    }

    private static ServiceFilter services() {
        var filter = new ServiceFilter(RegQuery.subscribers().all());
        filter.addRegFilter(ClaraFilters::isService);
        return filter;
    }

    private static ServiceFilter services(String host) {
        var filter = new ServiceFilter(RegQuery.subscribers().withHost(host));
        filter.addRegFilter(ClaraFilters::isService);
        return filter;
    }

    private static RegQuery withTopic(Topic topic) {
        return RegQuery.subscribers().withSame(topic);
    }

    private static String name(RegRecord data) {
        return data.name();
    }

    private static String name(JSONObject data) {
        return data.getString("name");
    }

    private static boolean isService(RegRecord record) {
        String domain = record.topic().domain();
        return !domain.equals("dpe") && !domain.equals("container");
    }

    private static boolean sameName(JSONObject data, ClaraName component) {
        return name(data).equals(component.canonicalName());
    }

    private static boolean sameDpe(RegRecord data, DpeName dpe) {
        return ClaraUtil.getDpeName(name(data)).equals(dpe.canonicalName());
    }

    private static boolean sameLang(RegRecord data, ClaraLang lang) {
        return ClaraUtil.getDpeLang(name(data)).equals(lang.toString());
    }

    private static boolean sameContainer(String canonicalName, ContainerName container) {
        return ClaraUtil.getContainerCanonicalName(canonicalName).equals(container.canonicalName());
    }
}
