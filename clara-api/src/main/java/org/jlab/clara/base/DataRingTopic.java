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

import org.jlab.clara.msg.core.xMsgTopic;
import org.jlab.clara.util.ArgUtils;

/**
 * The address of a CLARA data-ring.
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
        this(state, xMsgTopic.ANY, xMsgTopic.ANY);
    }

    /**
     * A topic to listen all events of the given state and session.
     *
     * @param state the output state of interest
     * @param session the data-processing session of interest
     */
    public DataRingTopic(String state, String session) {
        this(state, session, xMsgTopic.ANY);
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

        if (state.equals(xMsgTopic.ANY)) {
            throw new IllegalArgumentException("state is not defined");
        }
        if (session.equals(xMsgTopic.ANY) && !engine.equals(xMsgTopic.ANY)) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("DataRingTopic[");
        sb.append("state='").append(state).append('\'');
        if (!session.equals(xMsgTopic.ANY)) {
            sb.append(", session='").append(session).append('\'');
        }
        if (!engine.equals(xMsgTopic.ANY)) {
            sb.append(", engine='").append(engine).append('\'');
        }
        sb.append(']');
        return sb.toString();
    }

    String topic() {
        StringBuilder sb = new StringBuilder();
        sb.append(state);
        if (!session.equals(xMsgTopic.ANY)) {
            sb.append(xMsgTopic.SEPARATOR).append(session);
        }
        if (!engine.equals(xMsgTopic.ANY)) {
            sb.append(xMsgTopic.SEPARATOR).append(engine);
            if (!engine.endsWith(xMsgTopic.ANY)) {
                sb.append('*');
            }
        }
        return sb.toString();
    }
}
