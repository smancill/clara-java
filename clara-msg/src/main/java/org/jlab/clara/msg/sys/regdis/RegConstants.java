/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

public final class RegConstants {

    public static final int REGISTRATION_TIMEOUT = 3000;
    public static final int DISCOVERY_TIMEOUT = 3000;

    public static final String REGISTER_PUBLISHER = "registerPublisher";
    public static final String REGISTER_SUBSCRIBER = "registerSubscriber";

    public static final String REMOVE_PUBLISHER = "removePublisherRegistration";
    public static final String REMOVE_SUBSCRIBER = "removeSubscriberRegistration";
    public static final String REMOVE_ALL_REGISTRATION = "removeAllRegistration";

    public static final String FIND_PUBLISHER = "findPublisher";
    public static final String FIND_SUBSCRIBER = "findSubscriber";

    public static final String FILTER_PUBLISHER = "filterPublisher";
    public static final String FILTER_SUBSCRIBER = "filterSubscriber";

    public static final String EXACT_PUBLISHER = "exactPublisher";
    public static final String EXACT_SUBSCRIBER = "exactSubscriber";

    public static final String ALL_PUBLISHER = "allPublisher";
    public static final String ALL_SUBSCRIBER = "allSubscriber";

    public static final String UNDEFINED = "undefined";
    public static final String SUCCESS = "success";

    private RegConstants() { }
}
