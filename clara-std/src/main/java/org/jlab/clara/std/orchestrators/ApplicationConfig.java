/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.jlab.clara.base.ServiceName;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

class ApplicationConfig {

    static final String READER = "reader";
    static final String WRITER = "writer";

    private static final String IO_CONFIG = "io-services";
    private static final String GLOBAL_CONFIG = "global";
    private static final String SERVICE_CONFIG = "services";

    private final JSONObject configData;
    private final Map<String, Object> model;

    private static final Configuration FTL_CONFIG = new Configuration(Configuration.VERSION_2_3_25);

    static {
        FTL_CONFIG.setDefaultEncoding("UTF-8");
        FTL_CONFIG.setNumberFormat("computer");
        FTL_CONFIG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        FTL_CONFIG.setLogTemplateExceptions(false);
    }

    ApplicationConfig(JSONObject configData) {
        this(configData, new HashMap<>());
    }

    ApplicationConfig(JSONObject configData, Map<String, Object> model) {
        this.configData = configData;
        this.model = model;
    }

    JSONObject reader() {
        return getIO(READER);
    }

    JSONObject writer() {
        return getIO(WRITER);
    }

    private JSONObject getIO(String key) {
        var config = new JSONObject();
        if (configData.has(IO_CONFIG)) {
            var ioConfig = configData.getJSONObject(IO_CONFIG);
            if (ioConfig.has(key)) {
                add(config, ioConfig, key);
            }
        }
        return config;
    }

    JSONObject get(ServiceName service) {
        var config = new JSONObject();
        if (configData.has(GLOBAL_CONFIG)) {
            add(config, configData, GLOBAL_CONFIG);
        }
        if (configData.has(SERVICE_CONFIG)) {
            var servicesConfig = configData.getJSONObject(SERVICE_CONFIG);
            if (servicesConfig.has(service.name())) {
                add(config, servicesConfig, service.name());
            }
        }
        return config;
    }

    private void add(JSONObject target, JSONObject parent, String serviceKey) {
        var config = parent.getJSONObject(serviceKey);
        for (var key : config.keySet()) {
            Object value = config.get(key);
            if (value instanceof String) {
                Object result = processTemplate(key, (String) value);
                if (result != null) {
                    target.put(key, result);
                }
            } else {
                target.put(key, value);
            }
        }
    }

    private String processTemplate(String key, String value) {
        try {
            var tpl = new Template(key, new StringReader(value), FTL_CONFIG);
            var writer = new StringWriter();
            tpl.process(model, writer);
            return writer.toString();
        } catch (ParseException e) {
            var error = String.format("\"%s\" template is not valid: %s", key, value);
            throw new OrchestratorConfigException(error);
        } catch (TemplateException e) {
            var error = String.format("\"%s\" template cannot not be evaluated: %s", key, value);
            throw new OrchestratorConfigException(error);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
