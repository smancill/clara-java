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
 * A wrapper that creates a registrar if one is not running yet.
 * But if there is a registrar running already, prefer that one.
 */
public class RegistrarWrapper implements AutoCloseable {

    private final Context context = Context.newContext();
    private final Registrar registrar;

    public RegistrarWrapper() {
        registrar = tryMakeRegistrar();
    }

    private Registrar tryMakeRegistrar() {
        try {
            var registrar = new Registrar(context);
            registrar.start();
            ActorUtils.sleep(100);
            return registrar;
        } catch (ClaraMsgException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        context.destroy();
        if (registrar != null) {
            registrar.shutdown();
        }
    }
}
