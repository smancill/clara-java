/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.examples.engines;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;

import java.util.Set;

/**
 * User engine class example.
 *
 * @author gurjyan
 * @version 1.x
 * @since 2/9/15
 */
public class E4 implements Engine {

    private long nr = 0;
    private long t1;
    private long t2;

    @Override
    public EngineData execute(EngineData x) {
        if (nr == 0) {
            t1 = System.currentTimeMillis();
        }
        nr = nr + 1;
        if (nr >= ClaraConstants.BENCHMARK) {
            t2 = System.currentTimeMillis();
            long dt = t2 - t1;
            double pt = (double) dt / (double) nr;
            long pr = (nr * 1000) / dt;
            System.out.println("E4 processing time = " + pt + " ms");
            System.out.println("E4 rate = " + pr + " Hz");
            nr = 0;
        }
        return x;
    }

    @Override
    public EngineData executeGroup(Set<EngineData> x) {
        System.out.println("E4 engine group execute...");
        return x.iterator().next();
    }

    @Override
    public EngineData configure(EngineData x) {
        System.out.println("E4 engine configure...");
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
        return "Sample service E4";
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
