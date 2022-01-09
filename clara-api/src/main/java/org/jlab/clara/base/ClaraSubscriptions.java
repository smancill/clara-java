/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.core.DataUtil;
import org.jlab.clara.base.core.MessageUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.engine.EngineStatus;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Subscription;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.util.ArgUtils;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Subscriptions for running Clara components.
 */
public class ClaraSubscriptions {

    /**
     * Starts and stops a Clara subscription.
     *
     * @param <D> The specific subclass
     * @param <C> The user callback
     */
    abstract static class BaseSubscription<D extends BaseSubscription<D, C>, C> {

        final ClaraBase base;
        final ClaraComponent frontEnd;
        final Topic topic;

        final Map<String, Subscription> subscriptions;

        BaseSubscription(ClaraBase base,
                         Map<String, Subscription> subscriptions,
                         ClaraComponent frontEnd,
                         Topic topic) {
            this.base = base;
            this.frontEnd = frontEnd;
            this.topic = topic;
            this.subscriptions = subscriptions;
        }

        /**
         * A background thread is started to receive messages from the service.
         * Every time a report is received, the provided callback will be executed.
         * The messages are received sequentially, but the callback may run
         * in extra background threads, so it must be thread-safe.
         *
         * @param callback the callback to be executed for every received message
         * @throws ClaraException if the subscription failed to start
         */
        public void start(C callback) throws ClaraException {
            String key = frontEnd.getDpeHost() + ClaraConstants.MAPKEY_SEP + topic;
            if (subscriptions.containsKey(key)) {
                throw new IllegalStateException("duplicated subscription to: " + frontEnd);
            }
            Callback wrapperCallback = wrap(callback);
            Subscription handler = base.listen(frontEnd, topic, wrapperCallback);
            subscriptions.put(key, handler);
        }

        public void stop() {
            String key = frontEnd.getDpeHost() + ClaraConstants.MAPKEY_SEP + topic;
            Subscription handler = subscriptions.remove(key);
            if (handler != null) {
                base.unsubscribe(handler);
            }
        }

        @SuppressWarnings("unchecked")
        D self() {
            return (D) this;
        }

        abstract Callback wrap(C callback);
    }


    /**
     * A subscription to listen for service reports (data, done, status).
     */
    public static class ServiceSubscription
            extends BaseSubscription<ServiceSubscription, EngineCallback> {

        private Set<EngineDataType> dataTypes;

        ServiceSubscription(ClaraBase base,
                            Map<String, Subscription> subscriptions,
                            Set<EngineDataType> dataTypes,
                            ClaraComponent frontEnd,
                            Topic topic) {
            super(base, subscriptions, frontEnd, topic);
            this.dataTypes = dataTypes;
        }

        /**
         * Overwrites the data types used for deserializing the data from the
         * service.
         *
         * @param dataTypes the custom data-type of the service reports
         * @return this object, so methods can be chained
         */
        public ServiceSubscription withDataTypes(Set<EngineDataType> dataTypes) {
            this.dataTypes = dataTypes;
            return this;
        }

        /**
         * Overwrites the data types used for deserializing the data from the
         * service.
         *
         * @param dataTypes the custom data-type of the service reports
         * @return this object, so methods can be chained
         */
        public ServiceSubscription withDataTypes(EngineDataType... dataTypes) {
            var newTypes = new HashSet<EngineDataType>();
            Collections.addAll(newTypes, dataTypes);
            this.dataTypes = newTypes;
            return this;
        }

        @Override
        Callback wrap(final EngineCallback userCallback) {
            return msg -> {
                try {
                    userCallback.callback(DataUtil.deserialize(msg, dataTypes));
                } catch (ClaraException e) {
                    System.out.println("Error receiving data to " + msg.getTopic());
                    e.printStackTrace();
                }
            };
        }
    }


    /**
     * A subscription to listen for JSON reports from the DPEs.
     */
    public static class JsonReportSubscription
            extends BaseSubscription<JsonReportSubscription, GenericCallback> {

        JsonReportSubscription(ClaraBase base,
                               Map<String, Subscription> subscriptions,
                               ClaraComponent frontEnd,
                               Topic topic) {
            super(base, subscriptions, frontEnd, topic);
        }

        @Override
        Callback wrap(final GenericCallback userCallback) {
            return msg -> {
                try {
                    var mimeType = msg.getMimeType();
                    if (mimeType.equals(EngineDataType.JSON.mimeType())) {
                        userCallback.callback(new String(msg.getData()));
                    } else {
                        throw new ClaraException("Unexpected mime-type: " + mimeType);
                    }
                } catch (ClaraException e) {
                    System.out.println("Error receiving data to " + msg.getTopic());
                    e.printStackTrace();
                }
            };
        }
    }


    /**
     * A subscription to listen for JSON reports from the DPEs.
     */
    public static class BaseDpeReportSubscription extends JsonReportSubscription {

        BaseDpeReportSubscription(ClaraBase base,
                                  Map<String, Subscription> subscriptions,
                                  ClaraComponent frontEnd, Topic topic) {
            super(base, subscriptions, frontEnd, topic);
        }

        /**
         * Parse the JSON report as {@link DpeRegistrationData} and
         * {@link DpeRuntimeData} objects.
         *
         * @return A subscription to listen for reports from the DPEs.
         */
        public DpeReportSubscription parseJson() {
            return new DpeReportSubscription(base, subscriptions, frontEnd, topic);
        }
    }


    /**
     * A subscription to listen for JSON reports from the DPEs.
     */
    public static class DpeReportSubscription
            extends BaseSubscription<DpeReportSubscription, DpeReportCallback> {

        DpeReportSubscription(ClaraBase base,
                               Map<String, Subscription> subscriptions,
                               ClaraComponent frontEnd,
                               Topic topic) {
            super(base, subscriptions, frontEnd, topic);
        }

        @Override
        Callback wrap(final DpeReportCallback userCallback) {
            return msg -> {
                try {
                    var mimeType = msg.getMimeType();
                    if (mimeType.equals(EngineDataType.JSON.mimeType())) {
                        var source = new String(msg.getData());
                        var data = new JSONObject(source);
                        var registration = data.getJSONObject(ClaraConstants.REGISTRATION_KEY);
                        var runtime = data.getJSONObject(ClaraConstants.RUNTIME_KEY);
                        userCallback.callback(new DpeRegistrationData(registration),
                                              new DpeRuntimeData(runtime));
                    } else {
                        throw new ClaraException("Unexpected mime-type: " + mimeType);
                    }
                } catch (ClaraException e) {
                    System.out.println("Error receiving data to " + msg.getTopic());
                    e.printStackTrace();
                }
            };
        }
    }


    /**
     * Builds a subscription to listen the different Clara service reports.
     */
    public static class ServiceSubscriptionBuilder {
        private final ClaraBase base;
        private final Map<String, Subscription> subscriptions;
        private final Set<EngineDataType> dataTypes;
        private final ClaraComponent frontEnd;
        private final ClaraName component;

        ServiceSubscriptionBuilder(ClaraBase base,
                                   Map<String, Subscription> subscriptions,
                                   Set<EngineDataType> dataTypes,
                                   ClaraComponent frontEnd,
                                   ClaraName service) {
            this.base = base;
            this.subscriptions = subscriptions;
            this.dataTypes = dataTypes;
            this.frontEnd = frontEnd;
            this.component = service;
        }

        /**
         * A subscription to the specified status reports of the selected service.
         * <p>
         * Services will publish status reports after every execution that results
         * on error or warning.
         *
         * @param status the status to be listened
         * @return a service subscription to listen the given status
         */
        public ServiceSubscription status(EngineStatus status) {
            return new ServiceSubscription(base, subscriptions, dataTypes, frontEnd,
                                           getTopic(status.toString(), component));
        }

        /**
         * A subscription to the "done" reports of the selected service.
         * <p>
         * Services will publish "done" reports if they are configured to do so
         * with a given event count. The messages will not contain the full
         * output result of the service, but just a few stats about the execution.
         *
         * @return a service subscription to listen "done" reports
         */
        public ServiceSubscription done() {
            return new ServiceSubscription(base, subscriptions, dataTypes, frontEnd,
                                           getTopic(ClaraConstants.DONE, component));
        }

        /**
         * A subscription to the data reports of the selected service.
         * <p>
         * Services will publish "data" reports if they are configured to do so
         * with a given event count. The messages will contain the full
         * output result of the service.
         *
         * @return a service subscription to listen data reports
         */
        public ServiceSubscription data() {
            return new ServiceSubscription(base, subscriptions, dataTypes, frontEnd,
                                           getTopic(ClaraConstants.DATA, component));
        }

        private Topic getTopic(String prefix, ClaraName service) {
            return MessageUtil.buildTopic(prefix, service.canonicalName());
        }
    }


    /**
     * Builds a subscription to listen the different Clara DPE reports.
     */
    public static class GlobalSubscriptionBuilder {
        private final ClaraBase base;
        private final Map<String, Subscription> subscriptions;
        private final Set<EngineDataType> dataTypes;
        private final ClaraComponent frontEnd;

        GlobalSubscriptionBuilder(ClaraBase base,
                               Map<String, Subscription> subscriptions,
                               Set<EngineDataType> dataTypes,
                               ClaraComponent frontEnd) {
            this.base = base;
            this.subscriptions = subscriptions;
            this.dataTypes = dataTypes;
            this.frontEnd = frontEnd;
        }

        /**
         * A subscription to the periodic alive message reported by
         * all running DPEs.
         *
         * @return a subscription to listen DPE alive reports
         */
        public JsonReportSubscription aliveDpes() {
            var topic = MessageUtil.buildTopic(ClaraConstants.DPE_ALIVE, "");
            return new JsonReportSubscription(base, subscriptions, frontEnd, topic);
        }

        /**
         * A subscription to the periodic alive message reported by
         * the running DPEs with the given session.
         * <p>
         * If the session is empty, only DPEs with no session will be listened.
         *
         * @param session the session to select with DPEs to monitor
         * @return a subscription to listen DPE alive reports
         */
        public JsonReportSubscription aliveDpes(String session) {
            if (session == null) {
                throw new IllegalArgumentException("null session argument");
            }
            var topic = buildMatchingTopic(ClaraConstants.DPE_ALIVE, session);
            return new JsonReportSubscription(base, subscriptions, frontEnd, topic);
        }

        /**
         * A subscription to the periodic runtime and registration reports of
         * all running DPEs.
         *
         * @return a subscription to listen DPE reports
         */
        public BaseDpeReportSubscription dpeReport() {
            var topic = MessageUtil.buildTopic(ClaraConstants.DPE_REPORT, "");
            return new BaseDpeReportSubscription(base, subscriptions, frontEnd, topic);
        }

        /**
         * A subscription to the periodic runtime and registration reports of
         * the running DPEs with the given session.
         * <p>
         * If the session is empty, only DPEs with no session will be listened.
         *
         * @param session the session to select with DPEs to monitor
         * @return a subscription to listen DPE reports
         */
        public BaseDpeReportSubscription dpeReport(String session) {
            ArgUtils.requireNonNull(session, "session");
            var topic = buildMatchingTopic(ClaraConstants.DPE_REPORT, session);
            return new BaseDpeReportSubscription(base, subscriptions, frontEnd, topic);
        }

        /**
         * A subscription for all events published to the Clara data-ring.
         *
         * @return a subscription to listen all events in the data-ring
         */
        public ServiceSubscription dataRing() {
            var topic = MessageUtil.buildTopic(ClaraConstants.MONITOR_REPORT, "");
            return new ServiceSubscription(base, subscriptions, dataTypes, frontEnd, topic);
        }

        /**
         * A subscription for events published with the given topic to the Clara
         * data-ring.
         *
         * @param ringTopic the data-ring topic to filter events
         * @return a subscription to listen events in the data-ring
         */
        public ServiceSubscription dataRing(DataRingTopic ringTopic) {
            ArgUtils.requireNonNull(ringTopic, "topic");
            var topic = buildMatchingTopic(ClaraConstants.MONITOR_REPORT, ringTopic.topic());
            return new ServiceSubscription(base, subscriptions, dataTypes, frontEnd, topic);
        }
    }


    private static Topic buildMatchingTopic(String prefix, String keyword) {
        if (keyword.endsWith("*")) {
            keyword = keyword.substring(0, keyword.length() - 1);
            return MessageUtil.buildTopic(prefix, keyword);
        }
        return MessageUtil.buildTopic(prefix, keyword, "");
    }
}
