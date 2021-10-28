/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys;

import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;

/**
 * A wrapper that creates a proxy if one is not running yet.
 * But if there is a proxy running already, prefer that one.
 */
public class ProxyWrapper implements AutoCloseable {

    private final Context context = Context.newContext();
    private final Proxy proxy;

    public ProxyWrapper() {
        proxy = tryMakeProxy();
    }

    private Proxy tryMakeProxy() {
        try {
            var proxy = new Proxy(context);
            proxy.start();
            ActorUtils.sleep(250);
            return proxy;
        } catch (ClaraMsgException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        context.destroy();
        if (proxy != null) {
            proxy.shutdown();
        }
    }
}
