/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.core.MessageUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MimeType;
import org.jlab.clara.msg.data.RegRecord;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.util.report.JsonUtils;
import org.json.JSONObject;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Queries to the Clara registration/runtime database.
 */
public final class ClaraQueries {

    private ClaraQueries() { }


    private static class WrappedException extends RuntimeException {

        final Throwable cause;

        WrappedException(Throwable cause) {
            this.cause = cause;
        }
    }


    /**
     * A query to Clara registration.
     *
     * @param <D> The specific subclass
     * @param <T> The type returned by the query
     */
    abstract static class BaseQuery<D extends BaseQuery<D, T>, T> {

        protected final ClaraBase base;
        protected final ClaraComponent frontEnd;
        protected final ClaraFilter filter;

        protected BaseQuery(ClaraBase base, ClaraComponent frontEnd, ClaraFilter filter) {
            this.base = base;
            this.frontEnd = frontEnd;
            this.filter = filter;
        }

        /**
         * Sends the query and waits for a response.
         *
         * @param wait the amount of time units to wait for a response
         * @param unit the unit of time
         * @throws ClaraException if the query could not be sent or received
         * @throws TimeoutException if the response is not received
         * @return the result of the query
         */
        public T syncRun(long wait, TimeUnit unit) throws ClaraException, TimeoutException {
            try {
                if (wait <= 0) {
                    throw new IllegalArgumentException("Invalid timeout: " + wait);
                }
                var timeout = unit.toMillis(wait);
                var start = System.currentTimeMillis();
                var registration = queryRegistrar(timeout);
                var end = System.currentTimeMillis();
                return collect(registration, timeout - (end - start));
            } catch (ClaraMsgException e) {
                throw new ClaraException("Cannot send query", e);
            } catch (WrappedException e) {
                throw new ClaraException("Cannot send query ", e.cause);
            }
        }

        private Stream<RegRecord> queryRegistrar(long timeout) throws ClaraMsgException {
            return base.discover(filter.regQuery(), ClaraBase.getRegAddress(frontEnd), timeout)
                       .stream()
                       .filter(filter.regFilter());
        }

        protected abstract T collect(Stream<RegRecord> registration, long timeout);

        @SuppressWarnings("unchecked")
        protected D self() {
            return (D) this;
        }
    }


    @FunctionalInterface
    interface DpeReportParser {
        Stream<JSONObject> parseComponents(JSONObject dpeReport, String sectionKey);
    }


    abstract static class DpeQuery<D extends DpeQuery<D, T, R>, T, R>
            extends BaseQuery<D, R> {

        private final DpeReportParser parseReport;
        private final Function<JSONObject, T> parseData;

        private final String reportKey;

        protected DpeQuery(ClaraBase base,
                           ClaraComponent frontEnd,
                           ClaraFilter filter,
                           DpeReportParser parseReport,
                           Function<JSONObject, T> parseData,
                           String reportKey) {
            super(base, frontEnd, filter);
            this.parseReport = parseReport;
            this.parseData = parseData;
            this.reportKey = reportKey;
        }

        protected R collect(Stream<RegRecord> registration, long timeout) {
            return collect(queryDpes(registration, timeout));
        }

        protected abstract R collect(Stream<T> data);

        protected Stream<T> queryDpes(Stream<RegRecord> registration, long timeout) {
            return dpeNames(registration)
                    .flatMap(d -> queryDpe(d, timeout))
                    .map(parseData);
        }

        private Stream<DpeName> dpeNames(Stream<RegRecord> registration) {
            return registration.map(RegRecord::name)
                         .map(ClaraUtil::getDpeName)
                         .distinct()
                         .map(DpeName::new);
        }

        private Stream<JSONObject> queryDpe(DpeName dpe, long timeout) {
            try {
                var address = dpe.address().proxyAddress();
                var query = msg(dpe);
                var response = base.syncPublish(address, query, timeout);
                var mimeType = response.getMimeType();
                if (mimeType.equals(MimeType.STRING)) {
                    var data = new String(response.getData());
                    return filterQuery(new JSONObject(data));
                }
                return Stream.empty();
            } catch (TimeoutException | ClaraMsgException e) {
                throw new WrappedException(e);
            }
        }

        private Message msg(DpeName dpe) {
            var topic = Topic.build("dpe", dpe.canonicalName());
            return MessageUtil.buildRequest(topic, ClaraConstants.REPORT_JSON);
        }

        private Stream<JSONObject> filterQuery(JSONObject dpeReport) {
            // Optimize in case there is no need to filter the reports
            if (!filter.hasJsonFilter()) {
                return parseReport.parseComponents(dpeReport, reportKey);
            }

            // Filters use registration data in order to select components
            var registrationKey = ClaraConstants.REGISTRATION_KEY;
            var filteredRegistration = parseReport.parseComponents(dpeReport, registrationKey)
                    .filter(filter.jsonFilter());

            // If the query result requires registration data
            if (reportKey.equals(registrationKey)) {
                return filteredRegistration;
            }

            // Else the query result requires runtime data
            var filteredNames = filteredRegistration
                    .map(o -> o.getString("name"))
                    .collect(Collectors.toSet());

            return parseReport.parseComponents(dpeReport, reportKey)
                    .filter(o -> filteredNames.contains(o.getString("name")));
        }
    }


    /**
     * A query to get the names of the registered Clara components.
     *
     * @param <T> The name class of the components
     */
    public static class CanonicalNameQuery<T> extends DpeQuery<CanonicalNameQuery<T>, T, Set<T>> {

        private final Function<String, T> parseName;

        CanonicalNameQuery(ClaraBase base,
                           ClaraComponent frontEnd,
                           ClaraFilter filter,
                           DpeReportParser parseReport,
                           Function<String, T> parseName) {
            super(base, frontEnd, filter,
                  parseReport, j -> parseName.apply(j.getString("name")),
                  ClaraConstants.REGISTRATION_KEY);
            this.parseName = parseName;
        }

        @Override
        protected Set<T> collect(Stream<RegRecord> registration, long timeout) {
            if (filter.hasJsonFilter()) {
                return collect(queryDpes(registration, timeout));
            }
            return collect(registration.map(RegRecord::name).map(parseName));
        }

        @Override
        protected Set<T> collect(Stream<T> data) {
            return data.collect(Collectors.toSet());
        }
    }


    /**
     * A query to check if a Clara component is registered.
     *
     * @param <T> The name class of the component
     */
    public static class DiscoveryQuery<T> extends BaseQuery<DiscoveryQuery<T>, Boolean> {

        private final Function<String, T> parseName;

        DiscoveryQuery(ClaraBase base,
                       ClaraComponent frontEnd,
                       ClaraFilter filter,
                       Function<String, T> parseName) {
            super(base, frontEnd, filter);
            this.parseName = parseName;
        }

        @Override
        protected Boolean collect(Stream<RegRecord> registration, long timeout) {
            return registration.map(RegRecord::name).map(parseName).findFirst().isPresent();
        }
    }


    /**
     * A query to get the registration data of the registered Clara components.
     *
     * @param <T> The registration data class of the components
     */
    public static class RegistrationQuery<T> extends DpeQuery<RegistrationQuery<T>, T, Set<T>> {

        RegistrationQuery(ClaraBase base,
                          ClaraComponent frontEnd,
                          ClaraFilter filter,
                          DpeReportParser parseReport,
                          Function<JSONObject, T> parseData) {
            super(base, frontEnd, filter, parseReport, parseData, ClaraConstants.REGISTRATION_KEY);
        }

        @Override
        protected Set<T> collect(Stream<T> data) {
            return data.collect(Collectors.toSet());
        }
    }


    /**
     * A query to get the registration data of a specific Clara component.
     *
     * @param <T> The registration data class of the component
     */
    public static class RegistrationData<T>
            extends DpeQuery<RegistrationData<T>, T, Optional<T>> {

        RegistrationData(ClaraBase base,
                         ClaraComponent frontEnd,
                         ClaraFilter filter,
                         DpeReportParser parseReport,
                         Function<JSONObject, T> parseData) {
            super(base, frontEnd, filter, parseReport, parseData, ClaraConstants.REGISTRATION_KEY);
        }

        @Override
        protected Optional<T> collect(Stream<T> data) {
            return data.findFirst();
        }
    }


    /**
     * A query to get the runtime data of the registered Clara components.
     *
     * @param <T> The runtime data class of the components
     */
    public static final class RuntimeQuery<T>
            extends DpeQuery<RuntimeQuery<T>, T, Set<T>> {

        RuntimeQuery(ClaraBase base,
                     ClaraComponent frontEnd,
                     ClaraFilter filter,
                     DpeReportParser parseReport,
                     Function<JSONObject, T> parseData) {
            super(base, frontEnd, filter, parseReport, parseData, ClaraConstants.RUNTIME_KEY);
        }

        @Override
        protected Set<T> collect(Stream<T> data) {
            return data.collect(Collectors.toSet());
        }
    }


    /**
     * A query to get the runtime data of a specific Clara component.
     *
     * @param <T> The runtime data class of the component
     */
    public static final class RuntimeData<T>
            extends DpeQuery<RuntimeData<T>, T, Optional<T>> {

        RuntimeData(ClaraBase base,
                    ClaraComponent frontEnd,
                    ClaraFilter filter,
                    DpeReportParser parseReport,
                    Function<JSONObject, T> parseData) {
            super(base, frontEnd, filter, parseReport, parseData, ClaraConstants.RUNTIME_KEY);
        }

        @Override
        protected Optional<T> collect(Stream<T> data) {
            return data.findFirst();
        }
    }


    /**
     * Builds a request to query the Clara registration and runtime database.
     */
    public static class ClaraQueryBuilder {

        private final ClaraBase base;
        private final ClaraComponent frontEnd;

        ClaraQueryBuilder(ClaraBase base, ClaraComponent frontEnd) {
            this.base = base;
            this.frontEnd = frontEnd;
        }

        /**
         * Creates a query to get the names of the selected DPEs.
         *
         * @param filter a filter to select DPEs
         * @return the query to get the names of the registered DPEs that match the filter
         */
        public CanonicalNameQuery<DpeName> canonicalNames(DpeFilter filter) {
            return new CanonicalNameQuery<>(base, frontEnd, filter,
                                            JsonUtils::dpeStream, DpeName::new);
        }

        /**
         * Creates a query to get the names of the selected containers.
         *
         * @param filter a filter to select containers
         * @return the query to get the names of the registered containers that match the filter
         */
        public CanonicalNameQuery<ContainerName> canonicalNames(ContainerFilter filter) {
            return new CanonicalNameQuery<>(base, frontEnd, filter,
                                            JsonUtils::containerStream, ContainerName::new);
        }

        /**
         * Creates a query to get the names of the selected services.
         *
         * @param filter a filter to select services
         * @return the query to get the names of the registered services that match the filter
         */
        public CanonicalNameQuery<ServiceName> canonicalNames(ServiceFilter filter) {
            return new CanonicalNameQuery<>(base, frontEnd, filter,
                                            JsonUtils::serviceStream, ServiceName::new);
        }

        /**
         * Creates a query to check if the given DPE is registered.
         *
         * @param name the name of the selected DPE
         * @return the query to check if the DPE is registered
         */
        public DiscoveryQuery<DpeName> discover(DpeName name) {
            return new DiscoveryQuery<>(base, frontEnd, ClaraFilters.dpe(name),
                                        DpeName::new);
        }


        /**
         * Creates a query to check if the given container is registered.
         *
         * @param name the name of the selected container
         * @return the query to check if the container is registered
         */
        public DiscoveryQuery<ContainerName> discover(ContainerName name) {
            return new DiscoveryQuery<>(base, frontEnd, ClaraFilters.container(name),
                                        ContainerName::new);
        }


        /**
         * Creates a query to check if the given service is registered.
         *
         * @param name the name of the selected service
         * @return the query to check if the service is registered
         */
        public DiscoveryQuery<ServiceName> discover(ServiceName name) {
            return new DiscoveryQuery<>(base, frontEnd, ClaraFilters.service(name),
                                        ServiceName::new);
        }


        /**
         * Creates a query to get the registration data of the selected DPEs.
         *
         * @param filter a filter to select DPEs
         * @return the query to get the registration data of the registered DPEs
         *         that match the filter
         */
        public RegistrationQuery<DpeRegistrationData> registrationData(DpeFilter filter) {
            return new RegistrationQuery<>(base, frontEnd, filter,
                                           JsonUtils::dpeStream, DpeRegistrationData::new);
        }


        /**
         * Creates a query to get the registration data of the selected containers.
         *
         * @param filter a filter to select containers
         * @return the query to get the registration data of the registered containers
         *         that match the filter
         */
        public RegistrationQuery<ContainerRegistrationData>
                registrationData(ContainerFilter filter) {
            return new RegistrationQuery<>(base, frontEnd, filter,
                                           JsonUtils::containerStream,
                                           ContainerRegistrationData::new);
        }

        /**
         * Creates a query to get the registration data of the selected services.
         *
         * @param filter a filter to select services
         * @return the query to get the registration data of the registered services
         *         that match the filter
         */
        public RegistrationQuery<ServiceRegistrationData> registrationData(ServiceFilter filter) {
            return new RegistrationQuery<>(base, frontEnd, filter,
                                           JsonUtils::serviceStream,
                                           ServiceRegistrationData::new);
        }

        /**
         * Creates a query to get the registration data of a specific DPE.
         *
         * @param name the name of the selected DPE
         * @return the query to get the registration data of the given DPE
         */
        public RegistrationData<DpeRegistrationData> registrationData(DpeName name) {
            return new RegistrationData<>(base, frontEnd, ClaraFilters.dpe(name),
                                          JsonUtils::dpeStream, DpeRegistrationData::new);
        }

        /**
         * Creates a query to get the registration data of a specific container.
         *
         * @param name the name of the selected container
         * @return the query to get the registration data of the given container
         */
        public RegistrationData<ContainerRegistrationData> registrationData(ContainerName name) {
            return new RegistrationData<>(base, frontEnd, ClaraFilters.container(name),
                                          JsonUtils::containerStream,
                                          ContainerRegistrationData::new);
        }

        /**
         * Creates a query to get the registration data of a specific service.
         *
         * @param name the name of the selected service
         * @return the query to get the registration data of the given service
         */
        public RegistrationData<ServiceRegistrationData> registrationData(ServiceName name) {
            return new RegistrationData<>(base, frontEnd, ClaraFilters.service(name),
                                          JsonUtils::serviceStream,
                                          ServiceRegistrationData::new);
        }


        /**
         * Creates a query to get the runtime data of the selected DPEs.
         *
         * @param filter a filter to select DPEs
         * @return the query to get the runtime data of the registered DPEs
         *         that match the filter
         */
        public RuntimeQuery<DpeRuntimeData> runtimeData(DpeFilter filter) {
            return new RuntimeQuery<>(base, frontEnd, filter,
                                      JsonUtils::dpeStream, DpeRuntimeData::new);
        }

        /**
         * Creates a query to get the runtime data of the selected containers.
         *
         * @param filter a filter to select containers
         * @return the query to get the runtime data of the registered containers
         *         that match the filter
         */
        public RuntimeQuery<ContainerRuntimeData> runtimeData(ContainerFilter filter) {
            return new RuntimeQuery<>(base, frontEnd, filter,
                                      JsonUtils::containerStream,
                                      ContainerRuntimeData::new);
        }

        /**
         * Creates a query to get the runtime data of the selected services.
         *
         * @param filter a filter to select services
         * @return the query to get the runtime data of the registered services
         *         that match the filter
         */
        public RuntimeQuery<ServiceRuntimeData> runtimeData(ServiceFilter filter) {
            return new RuntimeQuery<>(base, frontEnd, filter,
                                      JsonUtils::serviceStream,
                                      ServiceRuntimeData::new);
        }

        /**
         * Creates a query to get the runtime data of a specific DPE.
         *
         * @param name the name of the selected DPE
         * @return the query to get the runtime data of the given DPE
         */
        public RuntimeData<DpeRuntimeData> runtimeData(DpeName name) {
            return new RuntimeData<>(base, frontEnd, ClaraFilters.dpe(name),
                                     JsonUtils::dpeStream, DpeRuntimeData::new);
        }

        /**
         * Creates a query to get the runtime data of a specific container.
         *
         * @param name the name of the selected container
         * @return the query to get the runtime data of the given container
         */
        public RuntimeData<ContainerRuntimeData> runtimeData(ContainerName name) {
            return new RuntimeData<>(base, frontEnd, ClaraFilters.container(name),
                                     JsonUtils::containerStream,
                                     ContainerRuntimeData::new);
        }

        /**
         * Creates a query to get the runtime data of a specific service.
         *
         * @param name the name of the selected service
         * @return the query to get the runtime data of the given service
         */
        public RuntimeData<ServiceRuntimeData> runtimeData(ServiceName name) {
            return new RuntimeData<>(base, frontEnd, ClaraFilters.service(name),
                                     JsonUtils::serviceStream,
                                     ServiceRuntimeData::new);
        }
    }
}
