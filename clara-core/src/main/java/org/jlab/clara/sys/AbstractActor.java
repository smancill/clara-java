/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
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

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.MessageUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Subscription;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.msg.errors.ClaraMsgException;

import java.util.concurrent.atomic.AtomicBoolean;

abstract class AbstractActor {

    static final AtomicBoolean isShutDown = new AtomicBoolean(); // nocheck: ConstantName
    static final AtomicBoolean isFrontEnd = new AtomicBoolean(); // nocheck: ConstantName

    final ClaraBase base;

    private boolean running = false;
    private final Object lock = new Object();

    AbstractActor(ClaraComponent component, ClaraComponent fe) {
        this.base = new ClaraBase(component, fe);
    }

    public void start() throws ClaraException {
        synchronized (lock) {
            initialize();
            startMsg();
            running = true;
        }
    }

    public void stop() {
        synchronized (lock) {
            end();
            base.close();
            if (running) {
                running = false;
                stopMsg();
            }
        }
    }

    /**
     * Initializes the CLARA actor.
     */
    abstract void initialize() throws ClaraException;

    /**
     * Runs before closing the actor.
     */
    abstract void end();

    abstract void startMsg();

    abstract void stopMsg();

    /**
     * Listens for messages of given topic published to the address of this component,
     * and registers as a subscriber with the front-end.
     *
     * @param topic topic of interest
     * @param callback the callback action
     * @param description a description for the registration
     * @return a handler to the subscription
     * @throws ClaraException if the subscription could not be started or
     *                        if the registration failed
     */
    Subscription startRegisteredSubscription(Topic topic,
                                             Callback callback,
                                             String description) throws ClaraException {
        Subscription sub = base.listen(topic, callback);
        try {
            base.register(topic, description);
        } catch (Exception e) {
            base.unsubscribe(sub);
            throw e;
        }
        return sub;
    }

    void sendResponse(Message msg, MetaData.Status status, String data) {
        try {
            Message repMsg = MessageUtil.buildRequest(msg.getReplyTopic(), data);
            repMsg.getMetaData().setAuthor(base.getName());
            repMsg.getMetaData().setStatus(status);
            base.send(repMsg);
        } catch (ClaraMsgException e) {
            e.printStackTrace();
        }
    }


    static boolean shouldDeregister() {
        return !(isShutDown.get() && isFrontEnd.get());
    }

    static class WrappedException extends RuntimeException {

        private final ClaraException cause;

        WrappedException(ClaraException cause) {
            this.cause = cause;
        }

        @Override
        public ClaraException getCause() {
            return cause;
        }
    }

    static void throwWrapped(ClaraException t) {
        throw new WrappedException(t);
    }
}
