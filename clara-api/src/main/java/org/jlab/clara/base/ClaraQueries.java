/*
 * Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
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
import org.jlab.clara.msg.net.ProxyAddress;
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
 * Queries to the CLARA registration/runtime database.
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
     * A query to CLARA registration.
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
                long timeout = (int) unit.toMillis(wait);
                long start = System.currentTimeMillis();
                Stream<RegRecord> regData = queryRegistrar(timeout);
                long end = System.currentTimeMillis();
                return collect(regData, timeout - (end - start));
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

        protected abstract T collect(Stream<RegRecord> regData, long timeout);

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

        private final String regKey;
        private final String dataKey;

        protected DpeQuery(ClaraBase base,
                           ClaraComponent frontEnd,
                           ClaraFilter filter,
                           DpeReportParser parseReport,
                           Function<JSONObject, T> parseData,
                           String dataKey) {
            super(base, frontEnd, filter);
            this.parseReport = parseReport;
            this.parseData = parseData;
            this.regKey = ClaraConstants.REGISTRATION_KEY;
            this.dataKey = dataKey;
        }

        protected Stream<T> query(Stream<RegRecord> regData, long timeout) {
            return dpeNames(regData)
                    .flatMap(d -> queryDpe(d, timeout))
                    .map(parseData);
        }

        private Stream<DpeName> dpeNames(Stream<RegRecord> record) {
            return record.map(RegRecord::name)
                         .map(ClaraUtil::getDpeName)
                         .distinct()
                         .map(DpeName::new);
        }

        private Stream<JSONObject> queryDpe(DpeName dpe, long timeout) {
            try {
                ProxyAddress address = dpe.address().proxyAddress();
                Message query = msg(dpe);
                Message response = base.syncPublish(address, query, timeout);
                String mimeType = response.getMimeType();
                if (mimeType.equals(MimeType.STRING)) {
                    String data = new String(response.getData());
                    return filterQuery(new JSONObject(data));
                }
                return Stream.empty();
            } catch (TimeoutException | ClaraMsgException e) {
                throw new WrappedException(e);
            }
        }

        private Message msg(DpeName dpe) {
            Topic topic = Topic.build("dpe", dpe.canonicalName());
            return MessageUtil.buildRequest(topic, ClaraConstants.REPORT_JSON);
        }

        private Stream<JSONObject> filterQuery(JSONObject report) {
            if (!filter.useDpe()) {
                return parseReport.parseComponents(report, dataKey);
            }
            Stream<JSONObject> regData = parseReport
                    .parseComponents(report, regKey)
                    .filter(filter.filter());
            if (regKey.equals(dataKey)) {
                return regData;
            }
            Set<String> names = regData.map(o -> o.getString("name"))
                                       .collect(Collectors.toSet());
            return parseReport.parseComponents(report, dataKey)
                              .filter(o -> names.contains(o.getString("name")));
        }
    }


    /**
     * A query to get the names of the registered CLARA components.
     *
     * @param <T> The name class of the components
     */
    public static class CanonicalNameQuery<T> extends DpeQuery<CanonicalNameQuery<T>, T, Set<T>> {

        private final Function<String, T> parseReg;

        CanonicalNameQuery(ClaraBase base,
                           ClaraComponent frontEnd,
                           ClaraFilter filter,
                           DpeReportParser parseReport,
                           Function<String, T> parseData) {
            super(base, frontEnd, filter,
                  parseReport, j -> parseData.apply(j.getString("name")),
                  ClaraConstants.REGISTRATION_KEY);
            this.parseReg = parseData;
        }

        @Override
        protected Set<T> collect(Stream<RegRecord> regData, long timeout) {
            if (filter.useDpe()) {
                return query(regData, timeout).collect(Collectors.toSet());
            }
            return regData.map(RegRecord::name).map(parseReg).collect(Collectors.toSet());
        }
    }


    /**
     * A query to check if a CLARA component is registered.
     *
     * @param <T> The name class of the component
     */
    public static class DiscoveryQuery<T> extends BaseQuery<DiscoveryQuery<T>, Boolean> {

        private final Function<String, T> parseReg;

        DiscoveryQuery(ClaraBase base,
                       ClaraComponent frontEnd,
                       ClaraFilter filter,
                       Function<String, T> parseReg) {
            super(base, frontEnd, filter);
            this.parseReg = parseReg;
        }

        @Override
        protected Boolean collect(Stream<RegRecord> regData, long timeout) {
            return regData.map(RegRecord::name).map(parseReg).findFirst().isPresent();
        }
    }


    /**
     * A query to get the registration data of the registered CLARA components.
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
        protected Set<T> collect(Stream<RegRecord> regData, long timeout) {
            return query(regData, timeout).collect(Collectors.toSet());
        }
    }


    /**
     * A query to get the registration data of a specific CLARA component.
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
        protected Optional<T> collect(Stream<RegRecord> regData, long timeout) {
            return query(regData, timeout).findFirst();
        }
    }


    /**
     * A query to get the runtime data of the registered CLARA components.
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
        protected Set<T> collect(Stream<RegRecord> regData, long timeout) {
            return query(regData, timeout).collect(Collectors.toSet());
        }
    }


    /**
     * A query to get the runtime data of a specific CLARA component.
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
        protected Optional<T> collect(Stream<RegRecord> regData, long timeout) {
            return query(regData, timeout).findFirst();
        }
    }


    /**
     * Builds a request to query the CLARA registration and runtime database.
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
