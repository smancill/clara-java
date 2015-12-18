/*
 * Copyright (C) 2015. Jefferson Lab, CLARA framework (JLAB). All Rights Reserved.
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * Author Vardan Gyurjyan
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.jlab.clara.examples.engines;

import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.util.CConstants;
import org.jlab.clara.util.ClaraUtil;

import java.util.Set;

/**
 * <p>
 *     User engine class example
 * </p>
 *
 * @author gurjyan
 * @version 1.x
 * @since 2/9/15
 */
public class E3 implements Engine {

    private long nr = 0;
    private long t1;
    private long t2;

    @Override
    public EngineData execute(EngineData x) {
        if (nr == 0) {
            t1 = System.currentTimeMillis();
        }
        nr = nr + 1;
        if (nr >= CConstants.BENCHMARK) {
            t2 = System.currentTimeMillis();
            long dt = t2 - t1;
            double pt = (double) dt / (double) nr;
            long pr = (nr * 1000) / dt;
            System.out.println("E3 processing time = " + pt + " ms");
            System.out.println("E3 rate = " + pr + " Hz");
            nr = 0;
        }
        return x;
    }

    @Override
    public EngineData executeGroup(Set<EngineData> x) {
        System.out.println("E3 engine group execute...");
        return x.iterator().next();
    }

    @Override
    public EngineData configure(EngineData x) {
        System.out.println("E3 engine configure...");
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
        return "Sample service E3";
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
