/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.core.ClaraBase;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.sys.Registrar;
import org.jlab.clara.sys.RequestParser.RequestException;
import org.jlab.clara.util.EnvUtils;

import java.util.Optional;

class FrontEnd {

    private final ClaraBase base;

    private final Context context;
    private final Registrar registrar;

    FrontEnd(ClaraComponent frontEnd)
            throws ClaraException {
        try {
            // create the registrar
            context = Context.newContext();
            registrar = new Registrar(context, ClaraBase.getRegAddress(frontEnd));

            // create the actor
            base = new ClaraBase(frontEnd, frontEnd);
        } catch (ClaraMsgException e) {
            throw new ClaraException("Could not create front-end", e);
        }
    }


    public void start() throws ClaraException {
        // start registrar service
        registrar.start();

        // subscribe to forwarding requests
        Topic topic = Topic.build(ClaraConstants.DPE,
                                          base.getFrontEnd().getCanonicalName());
        base.listen(topic, new GatewayCallback());
        base.register(topic, base.getMe().getDescription());

        ActorUtils.sleep(100);
    }


    public void stop() {
        context.destroy();
        registrar.shutdown();
        base.destroy();
    }


    private void startDpe(RequestParser parser, MetaData.Builder meta)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void stopDpe(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void setFrontEnd(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void pingDpe(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void startContainer(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void stopContainer(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void startService(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    private void stopService(RequestParser parser, MetaData.Builder metadata)
            throws RequestException, ClaraException {
        // TODO implement this
    }


    /**
     * DPE callback.
     * <p>
     * The topic of this subscription is:
     * topic = CConstants.DPE + ":" + dpeCanonicalName
     * <p>
     * The following are accepted message data:
     * <li>
     *     CConstants.START_DPE ?
     *     dpeHost ? dpePort ? dpeLang ? poolSize ? regHost ? regPort ? description
     * </li>
     * <li>
     *     CConstants.STOP_REMOTE_DPE ?
     *     dpeHost ? dpePort ? dpeLang
     * </li>
     * <li>
     *     CConstants.SET_FRONT_END_REMOTE ?
     *     dpeHost ? dpePort ? dpeLang ? frontEndHost ? frontEndPort ? frontEndLang
     * </li>
     * <li>
     *     CConstants.PING_REMOTE_DPE ?
     *     dpeHost ? dpePort ? dpeLang
     * </li>
     * <li>
     *     CConstants.START_REMOTE_CONTAINER ?
     *     dpeHost ? dpePort ? dpeLang ? containerName ? poolSize ? description
     * </li>
     * <li>
     *     CConstants.STOP_REMOTE_CONTAINER ?
     *     dpeHost ? dpePort ? dpeLang ? containerName
     * </li>
     * <li>
     *     CConstants.START_REMOTE_SERVICE ?
     *     dpeHost ? dpePort ? dpeLang ? containerName ? engineName ? engineClass ?
     *     poolSize ? description ? initialState
     * </li>
     * <li>
     *     CConstants.STOP_REMOTE_SERVICE ?
     *     dpeHost ? dpePort ? dpeLang ? containerName ? engineName
     * </li>
     */
    private class GatewayCallback implements Callback {

        @Override
        public void callback(Message msg) {
            MetaData.Builder metadata = msg.getMetaData();
            try {
                RequestParser parser = RequestParser.build(msg);
                String cmd = parser.nextString();

                switch (cmd) {
                    case ClaraConstants.START_DPE -> startDpe(parser, metadata);
                    case ClaraConstants.STOP_REMOTE_DPE -> stopDpe(parser, metadata);
                    case ClaraConstants.SET_FRONT_END_REMOTE -> setFrontEnd(parser, metadata);
                    case ClaraConstants.PING_REMOTE_DPE -> pingDpe(parser, metadata);
                    case ClaraConstants.START_REMOTE_CONTAINER -> startContainer(parser, metadata);
                    case ClaraConstants.STOP_REMOTE_CONTAINER -> stopContainer(parser, metadata);
                    case ClaraConstants.START_REMOTE_SERVICE -> startService(parser, metadata);
                    case ClaraConstants.STOP_REMOTE_SERVICE -> stopService(parser, metadata);
                    default -> { }
                }
            } catch (RequestException e) {
                e.printStackTrace();
            } catch (ClaraException e) {
                e.printStackTrace();
            }
        }
    }


    static Optional<DpeName> getMonitorFrontEnd() {
        try {
            return EnvUtils.get(ClaraConstants.ENV_MONITOR_FE).map(DpeName::new);
        } catch (IllegalArgumentException e) {
            Logging.error("Cannot use $%s: %s", ClaraConstants.ENV_MONITOR_FE, e.getMessage());
            return Optional.empty();
        }
    }
}
