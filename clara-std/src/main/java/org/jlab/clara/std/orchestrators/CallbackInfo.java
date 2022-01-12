/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.DataRingTopic;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.util.ArgUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

final class CallbackInfo {

    private CallbackInfo() { }


    static class BaseCallbackInfo {

        final String classPath;

        BaseCallbackInfo(String classpath) {
            this.classPath = ArgUtils.requireNonEmpty(classpath, "classpath");
        }

        void tryClose(Object object) {
            if (object instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    Logging.error("could not close instance of: " + classPath + ": " + e.getMessage());
                }
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + classPath.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof BaseCallbackInfo other)) {
                return false;
            }
            if (!classPath.equals(other.classPath)) {
                return false;
            }
            return true;
        }
    }


    record RingTopic(String state, String session, String engine) { }


    interface RingListener {

        void listen(@CheckForNull String session,
                    @Nonnull DpeReportHandler handler) throws ClaraException;

        void listen(@CheckForNull DataRingTopic topic,
                    @Nonnull EngineReportHandler handler) throws ClaraException;
    }


    static class RingCallbackInfo extends BaseCallbackInfo {

        final RingTopic topic;

        @Override
        public String toString() {
            return "RingCallbackInfo [topic=" + topic + ", classPath=" + classPath + "]";
        }

        RingCallbackInfo(String classpath, RingTopic topic) {
            super(classpath);
            this.topic = ArgUtils.requireNonNull(topic, "topic");
        }

        AutoCloseable loadCallback(RingListener listener) throws ClaraException {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                Class<?> klass = classLoader.loadClass(classPath);
                Object object = klass.getDeclaredConstructor().newInstance();
                try {
                    if (object instanceof EngineReportHandler handler) {
                        listener.listen(getEngineReportTopic(), handler);
                        return handler;
                    } else if (object instanceof DpeReportHandler handler) {
                        listener.listen(getDpeReportTopic(), handler);
                        return handler;
                    } else {
                        throw new ClaraException("invalid monitoring class: " + classPath);
                    }
                } catch (Exception e) {
                    tryClose(object);
                    throw e;
                }
            } catch (ClassNotFoundException e) {
                throw new ClaraException("class not found: " + classPath);
            } catch (NoSuchMethodException | SecurityException
                    | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new ClaraException("could not create instance: " + classPath, e);
            }
        }

        DataRingTopic getEngineReportTopic() {
            if (topic.state == null) {
                return null;
            }
            if (topic.session == null) {
                return new DataRingTopic(topic.state);
            }
            if (topic.engine == null) {
                return new DataRingTopic(topic.state, topic.session);
            }
            return new DataRingTopic(topic.state, topic.session, topic.engine);
        }

        String getDpeReportTopic() {
            return topic.session;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + topic.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (!(obj instanceof RingCallbackInfo other)) {
                return false;
            }
            if (!topic.equals(other.topic)) {
                return false;
            }
            return true;
        }
    }
}
