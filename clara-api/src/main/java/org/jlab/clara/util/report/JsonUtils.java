/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.util.report;

import org.jlab.clara.base.core.ClaraConstants;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class JsonUtils {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(ClaraConstants.DATE_FORMAT);

    private JsonUtils() { }

    public static JSONObject readJson(String resource) {
        var stream = JsonUtils.class.getResourceAsStream(resource);
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            var content =  reader.lines().collect(Collectors.joining("\n"));
            return new JSONObject(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JSONObject getContainer(JSONObject dpe, int c) {
        return dpe.getJSONArray("containers")
                  .getJSONObject(c);
    }

    public static JSONObject getService(JSONObject dpe, int c, int s) {
        return dpe.getJSONArray("containers").getJSONObject(c)
                  .getJSONArray("services").getJSONObject(s);
    }

    public static Stream<JSONObject> dpeStream(JSONObject dpeReport, String type) {
        return Stream.of(dpeReport.getJSONObject(type));
    }

    public static Stream<JSONObject> containerStream(JSONObject dpeReport, String type) {
        return containerStream(dpeReport.getJSONObject(type));
    }

    public static Stream<JSONObject> containerStream(JSONObject dpe) {
        return arrayStream(dpe, "containers");
    }

    public static Stream<JSONObject> serviceStream(JSONObject dpeReport, String type) {
        return containerStream(dpeReport, type).flatMap(JsonUtils::serviceStream);
    }

    public static Stream<JSONObject> serviceStream(JSONObject container) {
        return arrayStream(container, "services");
    }

    public static Stream<JSONObject> arrayStream(JSONObject json, String key) {
        var array = json.getJSONArray(key);
        return IntStream.range(0, array.length())
                        .mapToObj(array::getJSONObject);
    }

    public static LocalDateTime getDate(JSONObject json, String key) {
        return LocalDateTime.parse(json.getString(key), FORMATTER);
    }
}
