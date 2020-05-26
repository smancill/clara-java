/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
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
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.msg.data.MimeType;
import org.jlab.clara.msg.data.PlainDataProto.PlainData;
import org.junit.jupiter.api.Test;

public class MessageTest {

    private final Topic testTopic = Topic.wrap("test_topic");

    @Test
    public void createFromPrimitive() throws Exception {
        Message msg;

        msg = Message.createFrom(testTopic, 460);

        assertThat(msg.getMimeType(), is(MimeType.SFIXED32));
        assertThat(Message.parseData(msg), is(460));
        assertThat(Message.parseData(msg, Integer.class), is(460));

        msg = Message.createFrom(testTopic, 2000.5);

        assertThat(msg.getMimeType(), is(MimeType.DOUBLE));
        assertThat(Message.parseData(msg), is(2000.5));
        assertThat(Message.parseData(msg, Double.class), is(2000.5));

        msg = Message.createFrom(testTopic, "test_data");

        assertThat(msg.getMimeType(), is(MimeType.STRING));
        assertThat(Message.parseData(msg), is("test_data"));
        assertThat(Message.parseData(msg, String.class), is("test_data"));
    }

    @Test
    public void createFromArray() throws Exception {
        Message msg;

        msg = Message.createFrom(testTopic, new Integer[] {3, 4, 5});
        Integer[] ivalue = new Integer[] {3, 4, 5};

        assertThat(msg.getMimeType(), is(MimeType.ARRAY_SFIXED32));
        assertThat(Message.parseData(msg), is(ivalue));
        assertThat(Message.parseData(msg, Integer[].class), is(ivalue));

        msg = Message.createFrom(testTopic, new Double[] {300.2, 4000.7, 58.8});
        Double[] dvalue = new Double[] {300.2, 4000.7, 58.8};

        assertThat(msg.getMimeType(), is(MimeType.ARRAY_DOUBLE));
        assertThat(Message.parseData(msg), is(dvalue));
        assertThat(Message.parseData(msg, Double[].class), is(dvalue));

        msg = Message.createFrom(testTopic, new String[] {"test_data", "test_value" });
        String[] svalue = new String[] {"test_data", "test_value" };

        assertThat(msg.getMimeType(), is(MimeType.ARRAY_STRING));
        assertThat(Message.parseData(msg), is(svalue));
        assertThat(Message.parseData(msg, String[].class), is(svalue));
    }

    @Test
    public void createFromJavaObject() throws Exception {
        List<String> orig = Arrays.asList("led zeppelin", "pink floyd", "black sabbath");

        Message msg = Message.createFrom(testTopic, orig);

        assertThat(msg.getData(), is(ActorUtils.serializeToBytes(orig)));

        assertThat(Message.parseData(msg), is(orig));
        assertThat(Message.parseData(msg, Object.class), is(orig));
    }

    @Test
    public void createWithoutByteOrder() throws Exception {
        byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3, 0xa, 0xb};
        MetaData.Builder meta = MetaData.newBuilder();
        meta.setDataType("test/binary");

        Message msg = new Message(testTopic, meta, data);

        assertThat(msg.hasDataOrder(), is(false));
        assertThat(msg.getDataOrder(), is(ByteOrder.BIG_ENDIAN));
    }

    @Test
    public void createAndSetByteOrder() throws Exception {
        byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3, 0xa, 0xb};
        MetaData.Builder meta = MetaData.newBuilder();
        meta.setByteOrder(MetaData.Endian.Little);
        meta.setDataType("test/binary");

        Message msg = new Message(testTopic, meta, data);

        assertThat(msg.hasDataOrder(), is(true));
        assertThat(msg.getDataOrder(), is(ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    public void createSimpleResponse() throws Exception {
        byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3, 0xa, 0xb};
        MetaData.Builder meta = MetaData.newBuilder();
        meta.setReplyTo("return_123");
        meta.setDataType("test/binary");

        Message msg = new Message(testTopic, meta, data);
        Message res = Message.createResponse(msg);

        assertThat(res.getTopic().toString(), is("return_123"));
        assertThat(res.getData(), is(msg.getData()));
        assertThat(res.getMetaData().getDataType(), is("test/binary"));
        assertFalse(res.getMetaData().hasReplyTo());
    }

    @Test
    public void createDataResponse() throws Exception {
        byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3, 0xa, 0xb};
        MetaData.Builder meta = MetaData.newBuilder();
        meta.setReplyTo("return_123");
        meta.setDataType("test/binary");

        Message msg = new Message(testTopic, meta, data);
        Message res = Message.createResponse(msg, 1000);

        assertThat(res.getTopic().toString(), is("return_123"));
        assertThat(PlainData.parseFrom(res.getData()).getFLSINT32(), is(1000));
        assertThat(res.getMetaData().getDataType(), is(MimeType.SFIXED32));
        assertFalse(res.getMetaData().hasReplyTo());
    }
}
