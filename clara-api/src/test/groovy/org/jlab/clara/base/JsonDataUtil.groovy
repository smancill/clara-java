/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import groovy.transform.PackageScope
import org.jlab.clara.base.core.ClaraConstants
import org.jlab.clara.util.report.JsonUtils
import org.json.JSONObject

@PackageScope
class JsonDataUtil {
    static JSONObject parseRegistrationExample() {
        JsonUtils.readJson("/registration-data.json").getJSONObject(ClaraConstants.REGISTRATION_KEY)
    }

    static JSONObject parseRuntimeExample() {
        JsonUtils.readJson("/runtime-data.json").getJSONObject(ClaraConstants.RUNTIME_KEY)
    }
}
