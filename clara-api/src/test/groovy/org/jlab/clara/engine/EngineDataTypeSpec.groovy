/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine

import spock.lang.Specification

import java.nio.ByteBuffer
import java.nio.ByteOrder

class EngineDataTypeSpec extends Specification {

    def "Serialize an integer value"() {
        given: "the default integer serializer"
        var serializer = EngineDataType.INT32.serializer()

        when: "serializing and deserializing an integer"
        var buffer = serializer.write(18)
        var result = serializer.read(buffer) as Integer

        then: "the result has the same value than the original"
        result == 18
    }

    def "Serialize a floating-point value"() {
        given: "the default double serializer"
        var serializer = EngineDataType.FLOAT.serializer()

        when: "serializing and deserializing a double"
        var buffer = serializer.write(78.98f)
        var result = serializer.read(buffer) as Float

        then: "the result has the same value than the original"
        result == 78.98f
    }

    def "Serialize a string value"() {
        given: "the default string serializer"
        var serializer = EngineDataType.STRING.serializer()

        when: "serializing and deserializing a string"
        var buffer = serializer.write("high-energy physics")
        var result = serializer.read(buffer) as String

        then: "the result has the same value than the original"
        result == "high-energy physics"
    }

    def "Serialize raw bytes"() {
        given: "the default raw bytes serializer"
        var serializer = EngineDataType.BYTES.serializer()

        and: "a byte buffer"
        var array = [0x0, 0x1, 0x2, 0x1, 0x0] as byte[]
        var data = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)

        when: "serializing and deserializing a byte buffer"
        var buffer = serializer.write(data)
        var result = serializer.read(buffer) as ByteBuffer

        then: "the result is the same object than the original"
        result === data

        and: "the original byte order is kept"
        result.order() == ByteOrder.LITTLE_ENDIAN
    }
}
