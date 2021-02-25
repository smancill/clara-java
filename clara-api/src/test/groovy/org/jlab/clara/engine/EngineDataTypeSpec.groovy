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

package org.jlab.clara.engine

import org.jlab.clara.msg.data.PlainDataProto.PayloadData
import org.jlab.clara.msg.data.PlainDataProto.PlainData
import spock.lang.Specification

import java.nio.ByteBuffer
import java.nio.ByteOrder

class EngineDataTypeSpec extends Specification {

    def "Serialize an integer value"() {
        given: "the default integer serializer"
        var serializer = EngineDataType.SINT32.serializer()

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

    def "Serialize an integer array"() {
        given: "the default integer array serializer"
        var serializer = EngineDataType.ARRAY_SINT32.serializer()

        when: "serializing and deserializing an integer array"
        var data = [4, 5, 6] as Integer[]
        var buffer = serializer.write(data)
        var result = serializer.read(buffer) as Integer[]

        then: "the result has the same value than the original"
        result == data
    }

    def "Serialize a floating-point array"() {
        given: "the default floating-point array serializer"
        var serializer = EngineDataType.ARRAY_FLOAT.serializer()

        when: "serializing and deserializing a floating-point array"
        var data = [4.1, 5.7] as Float[]
        var buffer = serializer.write(data)
        var result = serializer.read(buffer) as Float[]

        then: "the result has the same value than the original"
        result == data
    }

    def "Serialize a string array"() {
        given: "the default string array serializer"
        var serializer = EngineDataType.ARRAY_STRING.serializer()

        when: "serializing and deserializing a string array"
        var data = ["proton", "electron"] as String[]
        var buffer = serializer.write(data)
        var result = serializer.read(buffer) as String[]

        then: "the result has the same value than the original"
        result == data
    }

    def "Serialize a native data object"() {
        given: "the default native data serializer"
        var serializer = EngineDataType.NATIVE_DATA.serializer()

        and: "a native data object"
        var data = PlainData.newBuilder()
            .setFLSINT32(56)
            .setDOUBLE(5.6)
            .addSTRINGA("pion")
            .addSTRINGA("muon")
            .addSTRINGA("neutrino")
            .build()

        when: "serializing and deserializing a native data objet"
        var buffer = serializer.write(data)
        var result = serializer.read(buffer) as PlainData

        then: "the result has the same value than the original"
        result == data
    }

    def "Serialize a native payload object"() {
        given: "the default native payload serializer"
        var serializer = EngineDataType.NATIVE_PAYLOAD.serializer()

        and: "a native payload object"
        var data1 = PlainData.newBuilder()
            .addDOUBLEA(1)
            .addDOUBLEA(4.5)
            .addDOUBLEA(5.8)
            .build()
        var data2 = PlainData.newBuilder()
            .addFLOATA(4.3f)
            .addFLOATA(4.5f)
            .addFLOATA(5.8f)
            .build()
        var payload = PayloadData.newBuilder()
            .addItem(PayloadData.Item.newBuilder().setData(data1).setName("doubles"))
            .addItem(PayloadData.Item.newBuilder().setData(data2).setName("floats"))
            .build()

        when: "serializing and deserializing a native payload objet"
        var buffer = serializer.write(payload)
        var result = serializer.read(buffer) as PayloadData

        then: "the result has the same value than the original"
        result == payload
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
