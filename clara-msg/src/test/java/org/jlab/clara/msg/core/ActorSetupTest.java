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

import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.RegAddress;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ActorSetupTest {

    @Test
    public void defaultValues() throws Exception {
        ActorSetup setup = ActorSetup.newBuilder().build();

        assertThat(setup.proxyAddress(), is(new ProxyAddress()));
        assertThat(setup.registrarAddress(), is(new RegAddress()));
        assertThat(setup.poolSize(), is(ActorSetup.DEFAULT_POOL_SIZE));
        assertThat(setup.subscriptionMode(), is(CallbackMode.MULTI_THREAD));
    }


    @Test
    public void customValues() throws Exception {
        ActorSetup setup = ActorSetup.newBuilder()
                                     .withProxy(new ProxyAddress("10.1.1.10"))
                                     .withRegistrar(new RegAddress("10.1.1.1"))
                                     .withPoolSize(5)
                                     .withSubscriptionMode(CallbackMode.SINGLE_THREAD)
                                     .build();

        assertThat(setup.proxyAddress(), is(new ProxyAddress("10.1.1.10")));
        assertThat(setup.registrarAddress(), is(new RegAddress("10.1.1.1")));
        assertThat(setup.poolSize(), is(5));
        assertThat(setup.subscriptionMode(), is(CallbackMode.SINGLE_THREAD));
    }
}
