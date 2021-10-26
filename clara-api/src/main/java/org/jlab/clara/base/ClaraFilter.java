/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
