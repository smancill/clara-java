/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.engine.EngineData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SharedMemory {

    /*
      key = <receiver-service>
      value = map where:
          key = <sender-service>:<communication-id>,
          value = EngineData object
    */
    private static final Map<String, Map<String, EngineData>>
            sharedData = new ConcurrentHashMap<>(); // nocheck: ConstantName

    private SharedMemory() {
    }

    static void putEngineData(String receiver, String sender, int id, EngineData data) {
        Map<String, EngineData> inputs = sharedData.get(receiver);
        if (inputs != null) {
            String key = sender + ":" + id;
            inputs.put(key, data);
        } else {
            throw new IllegalStateException("Receiver not registered: " + receiver);
        }
    }


    static EngineData getEngineData(String receiver, String sender, int id) {
        Map<String, EngineData> inputs = sharedData.get(receiver);
        EngineData data = null;
        if (inputs != null) {
            String key = sender + ":" + id;
            data = inputs.get(key);
            inputs.remove(key);
        }
        return data;
    }

    static void addReceiver(String receiver) {
        sharedData.put(receiver, new ConcurrentHashMap<>());
    }

    static void removeReceiver(String receiver) {
        sharedData.remove(receiver);
    }

    static boolean containsReceiver(String receiver) {
        return sharedData.containsKey(receiver);
    }
}
