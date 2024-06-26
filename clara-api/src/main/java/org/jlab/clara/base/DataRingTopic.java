/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.util.ArgUtils;

/**
 * The address of a Clara data-ring.
 */
public class DataRingTopic {

    private final String state;
    private final String session;
    private final String engine;

    /**
     * A topic to listen all events of the given state.
     *
     * @param state the output state of interest
     */
    public DataRingTopic(String state) {
        this(state, Topic.ANY, Topic.ANY);
    }

    /**
     * A topic to listen all events of the given state and session.
     *
     * @param state the output state of interest
     * @param session the data-processing session of interest
     */
    public DataRingTopic(String state, String session) {
        this(state, session, Topic.ANY);
    }

    /**
     * A topic to listen all events of the given state, session and engine.
     *
     * @param state the output state of interest
     * @param session the data-processing session of interest
     * @param engine the name of the engine of interest
     */
    public DataRingTopic(String state, String session, String engine) {
        this.state = ArgUtils.requireNonEmpty(state, "state");
        this.session = ArgUtils.requireNonNull(session, "session");
        this.engine = ArgUtils.requireNonEmpty(engine, "engine");

        if (state.equals(Topic.ANY)) {
            throw new IllegalArgumentException("state is not defined");
        }
        if (session.equals(Topic.ANY) && !engine.equals(Topic.ANY)) {
            throw new IllegalArgumentException("session is not defined");
        }
    }

    // checkstyle.off: Javadoc
    public String state() {
        return state;
    }

    public String session() {
        return session;
    }

    public String engine() {
        return engine;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + engine.hashCode();
        result = prime * result + session.hashCode();
        result = prime * result + state.hashCode();
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataRingTopic other = (DataRingTopic) obj;
        if (!engine.equals(other.engine)) {
            return false;
        }
        if (!session.equals(other.session)) {
            return false;
        }
        if (!state.equals(other.state)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("DataRingTopic[");
        sb.append("state='").append(state).append('\'');
        if (!session.equals(Topic.ANY)) {
            sb.append(", session='").append(session).append('\'');
        }
        if (!engine.equals(Topic.ANY)) {
            sb.append(", engine='").append(engine).append('\'');
        }
        sb.append(']');
        return sb.toString();
    }

    String topic() {
        var sb = new StringBuilder();
        sb.append(state);
        if (!session.equals(Topic.ANY)) {
            sb.append(Topic.SEPARATOR).append(session);
        }
        if (!engine.equals(Topic.ANY)) {
            sb.append(Topic.SEPARATOR).append(engine);
            if (!engine.endsWith(Topic.ANY)) {
                sb.append('*');
            }
        }
        return sb.toString();
    }
}
