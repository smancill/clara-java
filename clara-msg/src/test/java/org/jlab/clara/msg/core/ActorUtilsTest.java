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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ActorUtilsTest {

    @Test
    public void uniqueReplyToGenerator() throws Exception {
        ActorUtils.setUniqueReplyToGenerator(0);

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000000"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000001"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000002"));

        Thread t1 = new Thread(() -> {
            for (int i = 3; i < 900000; i++) {
                ActorUtils.getUniqueReplyTo("subject");
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 90000; i++) {
                ActorUtils.getUniqueReplyTo("subject");
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1990000"));

        for (int i = 1; i < 10000; i++) {
            ActorUtils.getUniqueReplyTo("subject");
        }

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000000"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000001"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000002"));
    }

    @Test
    public void overflowReplyToGenerator() throws Exception {
        ActorUtils.setUniqueReplyToGenerator(Integer.MAX_VALUE);

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1483647")); //0x7fff_ffff
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1483648")); //0x8000_0000
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1483649")); //0x8000_0001

        for (int i = 0; i < 1000000 - 483650; i++) {
            ActorUtils.getUniqueReplyTo("subject");
        }

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000000"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000001"));
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000002"));

        ActorUtils.setUniqueReplyToGenerator(-1);

        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1967295")); //0xffff_ffff
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000000")); //0x0000_0000
        assertThat(ActorUtils.getUniqueReplyTo("subject"), is("ret:subject:1000001")); //0x0000_0001
    }

    @Test
    public void encodeIdentity() throws Exception {
        String encode = ActorUtils.encodeIdentity(ActorUtils.localhost(), "test_actor");

        assertThat(encode.length(), is(8));
    }

    @Test
    public void serializeAsBytesAndDeserialize() throws Exception {
        List<String> orig = Arrays.asList("led zeppelin", "pink floyd", "black sabbath");

        byte[] data = ActorUtils.serializeToBytes(orig);
        @SuppressWarnings("unchecked")
        List<String> clone = (List<String>) ActorUtils.deserialize(data);

        assertThat(clone, is(orig));
    }

    @Test
    public void serializeAsByteStringAndDeserialize() throws Exception {
        List<String> orig = Arrays.asList("led zeppelin", "pink floyd", "black sabbath");

        ByteString data = ActorUtils.serializeToByteString(orig);
        @SuppressWarnings("unchecked")
        List<String> clone = (List<String>) ActorUtils.deserialize(data);

        assertThat(clone, is(orig));
    }
}
