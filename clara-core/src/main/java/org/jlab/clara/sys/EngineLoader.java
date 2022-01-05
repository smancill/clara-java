/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineDataType;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Clara dynamic class loader.
 *
 * @author gurjyan
 * @version 4.x
 * @since 2/9/15
 */
class EngineLoader {

    private final ClassLoader classLoader;

    EngineLoader(ClassLoader cl) {
        classLoader = cl;
    }

    public Engine load(String className) throws ClaraException {
        try {
            Class<?> aClass = classLoader.loadClass(className);
            Object aInstance = aClass.getDeclaredConstructor().newInstance();
            if (aInstance instanceof Engine engine) {
                validateEngine(engine);
                return engine;
            } else {
                throw new ClaraException("not a Clara engine: " + className);
            }
        } catch (ClassNotFoundException e) {
            throw new ClaraException("class not found: " + className);
        } catch (NoSuchMethodException | SecurityException
                | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new ClaraException("could not create instance: " + className, e);
        }
    }

    private void validateEngine(Engine engine) throws ClaraException {
        validateDataTypes(engine.getInputDataTypes(), "input data types");
        validateDataTypes(engine.getOutputDataTypes(), "output data types");
        validateString(engine.getDescription(), "description");
        validateString(engine.getVersion(), "version");
        validateString(engine.getAuthor(), "author");
    }

    private void validateString(String value, String field) throws ClaraException {
        if (value == null || value.isEmpty()) {
            throw new ClaraException("missing engine " + field);
        }
    }

    private void validateDataTypes(Set<EngineDataType> types, String field) throws ClaraException {
        if (types == null || types.isEmpty()) {
            throw new ClaraException("missing engine " + field);
        }
        for (EngineDataType dt : types) {
            if (dt == null) {
                throw new ClaraException("null data type on engine " + field);
            }
        }
    }
}
