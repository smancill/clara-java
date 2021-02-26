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

package org.jlab.clara.base;

import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.data.RegRecord;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

abstract class ClaraFilter {

    static final String TYPE_DPE = "dpe";
    static final String TYPE_CONTAINER = "container";
    static final String TYPE_SERVICE = "service";

    private final RegQuery regQuery;
    private final String type;

    private final List<Predicate<RegRecord>> regFilters = new ArrayList<>();
    private final List<Predicate<JSONObject>> jsonFilters = new ArrayList<>();

    ClaraFilter(RegQuery query, String type) {
        this.regQuery = query;
        this.type = type;
    }

    void addRegFilter(Predicate<RegRecord> predicate) {
        regFilters.add(predicate);
    }

    void addJsonFilter(Predicate<JSONObject> predicate) {
        jsonFilters.add(predicate);
    }

    RegQuery regQuery() {
        return regQuery;
    }

    Predicate<RegRecord> regFilter() {
        return regFilters.stream().reduce(Predicate::and).orElse(t -> true);
    }

    Predicate<JSONObject> jsonFilter() {
        return jsonFilters.stream().reduce(Predicate::and).orElse(t -> true);
    }

    boolean hasJsonFilter() {
        return !jsonFilters.isEmpty();
    }

    @Override
    public String toString() {
        return type;
    }
}
