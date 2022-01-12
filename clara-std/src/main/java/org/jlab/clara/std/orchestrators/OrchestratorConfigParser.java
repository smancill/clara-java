/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.std.orchestrators.CallbackInfo.RingCallbackInfo;
import org.jlab.clara.std.orchestrators.CallbackInfo.RingTopic;
import org.jlab.clara.util.EnvUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class to read configuration for the standard orchestrators.
 * <p>
 * Currently, the user can set:
 * <ul>
 * <li>A default container name and language for the services
 * <li>The set of I/O services (required)
 * <li>The list of processing services (required)
 * <li>The list of mime-types used by the services (required)
 * <li>The global configuration for all services
 * </ul>
 *
 * The <i>services</i> description is provided in a YAML file,
 * which format is the following:
 * <pre>
 * container: default # Optional: change default container, otherwise it is $USER
 * io-services:
 *   reader:
 *     class: org.jlab.clas12.ana.ReaderService
 *     name: ReaderService
 *   reader:
 *     class: org.jlab.clas12.ana.WriterService
 *     name: WriterService
 * services:
 *   - class: org.jlab.clas12.ana.ServiceA
 *     name: ServiceA
 *   - class: org.jlab.clas12.rec.ServiceB
 *     name: ServiceB
 *     container: containerB # Optional: change container for this service
 *   - class: service_c
 *     name: ServiceC
 *     lang: cpp # a C++ service
 * mime-types:
 *   - binary/data-hipo
 * config:
 *   param1: "some_string"
 *   param2:
 *      key1: 31
 *      key2: 50
 * </pre>
 * By default, all processing and I/O services will be deployed in a
 * container named as the {@code $USER} running the orchestrator. This can be
 * changed by including a {@code container} key with the desired container name.
 * The container can be overwritten for individual services too. There is no
 * need to include I/O services in this file. They are controlled by the
 * orchestrators.
 */
public class OrchestratorConfigParser {

    private static final String DEFAULT_CONTAINER = EnvUtils.userName();

    private static final String SERVICES_KEY = "services";

    private final JSONObject config;

    /**
     * Creates a parser for the given configuration file.
     *
     * @param configFilePath the path to the configuration file
     */
    public OrchestratorConfigParser(String configFilePath) {
        try (InputStream input = new FileInputStream(configFilePath)) {
            var yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            this.config = new JSONObject(config);
        } catch (FileNotFoundException e) {
            throw error("could not open configuration file", e);
        } catch (IOException e) {
            throw error(e);
        } catch (ClassCastException | YAMLException e) {
            throw error("invalid YAML configuration file", e);
        }
    }


    static String getDefaultContainer() {
        return DEFAULT_CONTAINER;
    }


    /**
     * Returns the languages of all services defined in the application.
     *
     * @return a set with the languages of the services
     */
    public Set<ClaraLang> parseLanguages() {
        var app = new ApplicationInfo(
                parseInputOutputServices(),
                parseDataProcessingServices(),
                parseMonitoringServices());
        return app.getLanguages();
    }


    /**
     * Returns the mime-types required to receive messages from the services.
     * <p>
     * The orchestrator will create fake {@link org.jlab.clara.engine.EngineDataType
     * EngineDataType} objects for each mime-type. These will not deserialize
     * the user-data contained in the messages, they will be used to access just
     * the metadata.
     *
     * @return the mime-types of the data returned by the services
     */
    public Set<String> parseDataTypes() {
        var types = new HashSet<String>();
        var mimeTypes = config.optJSONArray("mime-types");
        if (mimeTypes != null) {
            for (int i = 0; i < mimeTypes.length(); i++) {
                try {
                    types.add(mimeTypes.getString(i));
                } catch (JSONException e) {
                    throw error("invalid array of mime-types");
                }
            }
        }
        return types;
    }


    Map<String, ServiceInfo> parseInputOutputServices() {
        var services = new HashMap<String, ServiceInfo>();
        var io = config.optJSONObject("io-services");
        if (io == null) {
            throw error("missing I/O services");
        }

        Consumer<String> getTypes = key -> {
            var data = io.optJSONObject(key);
            if (data == null) {
                throw error("missing " + key + " I/O service");
            }
            services.put(key, parseService(data));
        };
        getTypes.accept(ApplicationInfo.READER);
        getTypes.accept(ApplicationInfo.WRITER);

        services.put(ApplicationInfo.STAGE, getStageService());

        return services;
    }


    private ServiceInfo getStageService() {
        return new ServiceInfo("org.jlab.clara.std.services.DataManager",
                               parseDefaultContainer(), "DataManager", ClaraLang.JAVA);
    }


    List<ServiceInfo> parseDataProcessingServices() {
        var sl = config.optJSONArray(SERVICES_KEY);
        if (sl != null) {
            return parseServices(sl);
        }
        return parseServices("data-processing", true);
    }


    List<ServiceInfo> parseMonitoringServices() {
        if (config.optJSONArray(SERVICES_KEY) != null) {
            return new ArrayList<>();
        }
        return parseServices("monitoring", false);
    }


    private List<ServiceInfo> parseServices(String key, boolean required) {
        var ss = config.optJSONObject(SERVICES_KEY);
        if (ss == null) {
            throw error("missing list of services");
        }
        if (!ss.has(key)) {
            if (required) {
                throw error("missing list of " + key + " services");
            }
            return new ArrayList<>();
        }
        var so = ss.optJSONObject(key);
        if (so == null) {
            throw error("invalid list of " + key + " services");
        }
        var sl = so.optJSONArray("chain");
        if (sl == null) {
            throw error("invalid list of " + key + " services");
        }
        return parseServices(sl);
    }


    private List<ServiceInfo> parseServices(JSONArray array) {
        var services = new ArrayList<ServiceInfo>();
        for (int i = 0; i < array.length(); i++) {
            var service = parseService(array.getJSONObject(i));
            if (services.contains(service)) {
                throw error(String.format("duplicated service  name = '%s' container = '%s'",
                                          service.name(), service.cont()));
            }
            services.add(service);
        }
        return services;
    }


    List<RingCallbackInfo> parseDataRingCallbacks() {
        var callbacks = new ArrayList<RingCallbackInfo>();
        var co = config.optJSONObject("callbacks");
        if (co == null) {
            throw error("missing callbacks");
        }

        callbacks.addAll(parseDataRingCallbacks(co, "service-reports", this::engineRingTopic));
        callbacks.addAll(parseDataRingCallbacks(co, "dpe-reports", this::dpeRingTopic));

        return callbacks;
    }


    private List<RingCallbackInfo> parseDataRingCallbacks(
            JSONObject parent,
            String callbacksKey,
            Function<JSONObject, RingTopic> topicParser) {
        var callbacks = new ArrayList<RingCallbackInfo>();
        var cbArray = parent.optJSONArray(callbacksKey);
        if (cbArray != null) {
            for (int i = 0; i < cbArray.length(); i++) {
                var cbObj = cbArray.getJSONObject(i);
                var classPath = cbObj.optString("class");
                if (classPath.isEmpty()) {
                    throw error("missing class of callback");
                }
                var topic = parseRingTopic(cbObj, topicParser);
                callbacks.add(new RingCallbackInfo(classPath, topic));
            }
        }
        return callbacks;
    }


    private RingTopic parseRingTopic(JSONObject data,
                                     Function<JSONObject, RingTopic> parser) {
        if (!data.has("topic")) {
            return new RingTopic(null, null, null);
        }
        var topic = data.optJSONObject("topic");
        var cb = data.optString("class");
        if (topic == null) {
            throw error("invalid topic for callback: " + cb);
        }
        try {
            return parser.apply(topic);
        } catch (OrchestratorConfigException e) {
            throw error("invalid topic for callback " + cb + ": " + e.getMessage());
        }
    }


    private RingTopic engineRingTopic(JSONObject topic) {
        var state = parseRingTopicPart(topic, "state");
        var session = parseRingTopicPart(topic, "session");
        var engine = parseRingTopicPart(topic, "engine");
        if (state == null || state.isEmpty()) {
            throw error("missing state");
        }
        if (session == null && engine != null) {
            throw error("missing session");
        }
        if (engine != null && engine.isEmpty()) {
            throw error("missing engine");
        }
        return new RingTopic(state, session, engine);
    }


    private RingTopic dpeRingTopic(JSONObject topic) {
        if (topic.has("state")) {
            throw error("state is not supported");
        }
        if (topic.has("engine")) {
            throw error("engine is not supported");
        }
        var session = parseRingTopicPart(topic, "session");
        return new RingTopic(null, session, null);
    }


    private String parseRingTopicPart(JSONObject topic, String key) {
        String part = null;
        if (topic.has(key)) {
            part = parseString(topic, key);
            if (part == null) {
                throw error("invalid " + key);
            }
        }
        return part;
    }


    JSONObject parseConfiguration() {
        if (config.has("configuration")) {
            return config.getJSONObject("configuration");
        }
        return new JSONObject();
    }


    OrchestratorConfigMode parseConfigurationMode() {
        if (config.has("configuration_mode")) {
            var mode = config.getString("configuration_mode");
            try {
                return OrchestratorConfigMode.fromString(mode);
            } catch (IllegalArgumentException e) {
                throw error("invalid value for \"configuration_mode\": " + mode);
            }
        }
        return OrchestratorConfigMode.DATASET;
    }


    private String parseDefaultContainer() {
        return config.optString("container", DEFAULT_CONTAINER);
    }


    private String parseDefaultLanguage() {
        return config.optString("lang", ClaraLang.JAVA.toString());
    }


    private ServiceInfo parseService(JSONObject data) {
        var name = data.optString("name");
        var classPath = data.optString("class");
        var container = data.optString("container", parseDefaultContainer());
        var lang = ClaraLang.fromString(data.optString("lang", parseDefaultLanguage()));
        if (name.isEmpty() || classPath.isEmpty()) {
            throw error("missing name or class of service");
        }
        return new ServiceInfo(classPath, container, name, lang);
    }


    static DpeInfo getDefaultDpeInfo(String hostName) {
        var dpeIp = hostAddress(hostName);
        var dpeName = new DpeName(dpeIp, ClaraLang.JAVA);
        return new DpeInfo(dpeName, 0, EnvUtils.claraHome());
    }


    static DpeName localDpeName() {
        return new DpeName(hostAddress("localhost"), ClaraLang.JAVA);
    }


    static String hostAddress(String host) {
        try {
            return ClaraUtil.toHostAddress(host);
        } catch (UncheckedIOException e) {
            throw error("node name not known: " + host);
        }
    }


    static List<String> readInputFiles(String inputFilesList) {
        try {
            var pattern = Pattern.compile("^\\s*#.*$");
            var files = Files.lines(Paths.get(inputFilesList))
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !pattern.matcher(line).matches())
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                throw error("empty list of input files from " + inputFilesList);
            }
            return files;
        } catch (IOException e) {
            throw error("could not open file", e);
        } catch (UncheckedIOException e) {
            throw error("could not read list of input files from " + inputFilesList);
        }
    }


    private static String parseString(JSONObject json, String key) {
        try {
            return json.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }


    private static OrchestratorConfigException error(String msg) {
        return new OrchestratorConfigException(msg);
    }


    private static OrchestratorConfigException error(Throwable cause) {
        return new OrchestratorConfigException(cause);
    }


    private static OrchestratorConfigException error(String msg, Throwable cause) {
        return new OrchestratorConfigException(msg, cause);
    }
}
