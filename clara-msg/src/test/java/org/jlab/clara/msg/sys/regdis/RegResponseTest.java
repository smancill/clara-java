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

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.jlab.clara.msg.sys.regdis.RegDataFactory.newRegistration;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegResponseTest {

    private RegData.Builder data1;
    private RegData.Builder data2;

    @BeforeEach
    public void setup() {
        RegData.OwnerType type = RegData.OwnerType.SUBSCRIBER;
        data1 = newRegistration("asimov", "10.2.9.1", type, "writer.scifi:books");
        data2 = newRegistration("bradbury", "10.2.9.1", type, "writer.scifi:books");
    }


    @Test
    public void createSuccessResponse() throws Exception {
        RegResponse sendResponse = new RegResponse("foo:bar", "registration_fe");
        RegResponse recvResponse = new RegResponse(sendResponse.msg());

        assertThat(recvResponse.topic(), is("foo:bar"));
        assertThat(recvResponse.sender(), is("registration_fe"));
        assertThat(recvResponse.status(), is(RegConstants.SUCCESS));
        assertThat(recvResponse.data(), is(empty()));
    }


    @Test
    public void createErrorResponse() throws Exception {
        String error = "could not handle request";
        RegResponse sendResponse = new RegResponse("foo:bar", "registration_fe", error);

        ClaraMsgException ex = assertThrows(ClaraMsgException.class, () ->
                new RegResponse(sendResponse.msg()));
        assertThat(ex.getMessage(), containsString(error));
    }


    @Test
    public void createDataResponse() throws Exception {
        Set<RegData> data = new HashSet<>(Arrays.asList(data1.build(), data2.build()));
        RegResponse sendResponse = new RegResponse("foo:bar", "registration_fe", data);
        RegResponse recvResponse = new RegResponse(sendResponse.msg());

        assertThat(recvResponse.topic(), is("foo:bar"));
        assertThat(recvResponse.sender(), is("registration_fe"));
        assertThat(recvResponse.status(), is(RegConstants.SUCCESS));
        assertThat(recvResponse.data(), is(data));
    }


    @Test
    public void failWithMalformedMessage() throws Exception {
        ZMsg msg = new ZMsg();
        msg.addString("foo:bar");
        msg.addString("foo_service");

        ClaraMsgException ex = assertThrows(ClaraMsgException.class, () -> new RegResponse(msg));
        assertThat(ex.getMessage(), is("invalid registrar server response format"));
    }


    @Test
    public void failWithMalformedData() throws Exception {
        byte[] bb = data1.build().toByteArray();
        ZMsg msg = new ZMsg();
        msg.addString("foo:bar");
        msg.addString("foo_service");
        msg.addString(RegConstants.SUCCESS);
        msg.add(data2.build().toByteArray());
        msg.add(Arrays.copyOf(bb, bb.length - 10));

        ClaraMsgException ex = assertThrows(ClaraMsgException.class, () -> new RegResponse(msg));
        assertThat(ex.getMessage(), is("could not parse registrar server response"));
    }
}
