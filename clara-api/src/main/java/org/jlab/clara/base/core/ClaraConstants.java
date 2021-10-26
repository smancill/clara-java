/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base.core;

/**
 * CLARA internal constants.
 *
 * @author gurjyan
 * @version 4.x
 */
public final class ClaraConstants {

    private ClaraConstants() { }

    public static final int JAVA_PORT = 7771;
    public static final int CPP_PORT = 7781;
    public static final int PYTHON_PORT = 7791;
    public static final int REG_PORT_SHIFT = 4;

    public static final int MONITOR_PORT = 9000;

    public static final String DPE = "dpe";
    public static final String SESSION = "claraSession";
    public static final String START_DPE = "startDpe";
    public static final String STOP_DPE = "stopDpe";
    public static final String STOP_REMOTE_DPE = "stopRemoteDpe";
    public static final String DPE_EXIT = "dpeExit";
    public static final String PING_DPE = "pingDpe";
    public static final String PING_REMOTE_DPE = "pingRemoteDpe";
    public static final String DPE_ALIVE = "dpeAlive";
    public static final String DPE_REPORT = "dpeReport";
    public static final String MONITOR_REPORT = "ring";

    public static final String CONTAINER = "container";
    public static final String STATE_CONTAINER = "getContainerState";
    public static final String START_CONTAINER = "startContainer";
    public static final String START_REMOTE_CONTAINER = "startRemoteContainer";
    public static final String STOP_CONTAINER = "stopContainer";
    public static final String STOP_REMOTE_CONTAINER = "stopRemoteContainer";
    public static final String CONTAINER_DOWN = "containerIsDown";
    public static final String REMOVE_CONTAINER = "removeContainer";

    public static final String STATE_SERVICE = "getServiceState";
    public static final String START_SERVICE = "startService";
    public static final String START_REMOTE_SERVICE = "startRemoteService";
    public static final String STOP_SERVICE = "stopService";
    public static final String STOP_REMOTE_SERVICE = "stopRemoteService";
    public static final String DEPLOY_SERVICE = "deployService";
    public static final String REMOVE_SERVICE = "removeService";

    public static final String SERVICE_REPORT_INFO = "serviceReportInfo";
    public static final String SERVICE_REPORT_DONE = "serviceReportDone";
    public static final String SERVICE_REPORT_DATA = "serviceReportData";
    public static final String SERVICE_REPORT_RING = "serviceReportRing";

    public static final String SET_FRONT_END = "setFrontEnd";
    public static final String SET_FRONT_END_REMOTE = "setFrontEndRemote";

    public static final String SET_SESSION = "setSession";

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String REPORT_REGISTRATION = "reportRegistration";
    public static final String REPORT_RUNTIME = "reportRuntime";
    public static final String REPORT_JSON = "reportJson";

    public static final String REGISTRATION_KEY = "DPERegistration";
    public static final String RUNTIME_KEY = "DPERuntime";

    public static final String SHARED_MEMORY_KEY = "clara/shmkey";

    public static final String MAPKEY_SEP = "#";
    public static final String DATA_SEP = "?";
    public static final String LANG_SEP = "_";
    public static final String PORT_SEP = "%";

    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";
    public static final String DONE = "done";
    public static final String DATA = "data";

    public static final int BENCHMARK = 10000;

    public static final String JAVA_LANG = "java";
    public static final String PYTHON_LANG = "python";
    public static final String CPP_LANG = "cpp";

    public static final String UNDEFINED = "undefined";

    public static final String ENV_MONITOR_FE = "CLARA_MONITOR_FE";
}
