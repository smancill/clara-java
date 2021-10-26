/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.util.EnvUtils;

class Proxy {

    private final Context context;
    private final org.jlab.clara.msg.sys.Proxy proxy;

    Proxy(ClaraComponent dpe) throws ClaraException {
        try {
            context = Context.newContext();
            proxy = new org.jlab.clara.msg.sys.Proxy(context, dpe.getProxyAddress());
            if (EnvUtils.get("CLARA_PROXY_DEBUG").isPresent()) {
                proxy.verbose();
            }
        } catch (ClaraMsgException e) {
            throw new ClaraException("Could not create proxy", e);
        }
    }

    public void start() {
        proxy.start();
        ActorUtils.sleep(100);
    }

    public void stop() {
        context.destroy();
        proxy.shutdown();
    }
}
