/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.examples.engines;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;

import java.util.Set;

/**
 * Empty engine.
 *
 * @author gurjyan
 * @version 1.x
 * @since 2/9/17
 */
public class Empty implements Engine {

    @Override
    public EngineData execute(EngineData x) {
        return x;
    }

    @Override
    public EngineData executeGroup(Set<EngineData> x) {
        System.out.println("E1 engine group execute...");
        return x.iterator().next();
    }

    @Override
    public EngineData configure(EngineData x) {
        System.out.println("E1 engine configure...");
        return x;
    }

    @Override
    public Set<String> getStates() {
        return null;
    }

    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(EngineDataType.STRING);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ClaraUtil.buildDataTypes(EngineDataType.STRING);
    }

    @Override
    public String getDescription() {
        return "Sample service E1";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "Vardan Gyurgyan";
    }

    @Override
    public void reset() {

    }

    @Override
    public void destroy() {

    }
}
