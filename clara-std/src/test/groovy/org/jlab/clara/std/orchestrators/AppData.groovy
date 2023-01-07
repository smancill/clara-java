/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import groovy.transform.TypeChecked
import org.jlab.clara.base.ClaraLang
import org.jlab.clara.base.DpeName

@TypeChecked
final class AppData {

    public static final int CORES = 5

    public static final String CONT1 = "master"
    public static final String CONT2 = "slave"

    public static final ServiceInfo S1 = service(CONT1, "S1", ClaraLang.JAVA)
    public static final ServiceInfo R1 = service(CONT1, "R1", ClaraLang.JAVA)
    public static final ServiceInfo W1 = service(CONT1, "W1", ClaraLang.JAVA)

    public static final ServiceInfo J1 = service(CONT1, "J1", ClaraLang.JAVA)
    public static final ServiceInfo J2 = service(CONT1, "J2", ClaraLang.JAVA)
    public static final ServiceInfo J3 = service(CONT1, "J3", ClaraLang.JAVA)

    public static final ServiceInfo K1 = service(CONT2, "K1", ClaraLang.JAVA)
    public static final ServiceInfo K2 = service(CONT2, "K2", ClaraLang.JAVA)

    public static final ServiceInfo C1 = service(CONT1, "C1", ClaraLang.CPP)
    public static final ServiceInfo C2 = service(CONT1, "C2", ClaraLang.CPP)

    public static final ServiceInfo P1 = service(CONT2, "P1", ClaraLang.PYTHON)

    public static final DpeInfo DPE1 = dpe("10.1.1.10_java")
    public static final DpeInfo DPE2 = dpe("10.1.1.10_cpp")
    public static final DpeInfo DPE3 = dpe("10.1.1.10_python")

    private AppData() { }

    static final class AppBuilder {

        private List<ServiceInfo> services
        private List<ServiceInfo> monitoring

        private Map<ClaraLang, DpeInfo> dpes

        private AppBuilder() {
            services = [J1, J2, J3]
            monitoring = []
            dpes = [(DPE1.name().language()): DPE1]
        }

        AppBuilder withServices(ServiceInfo... services) {
            this.services = services.toList()
            this
        }

        AppBuilder withMonitoring(ServiceInfo... services) {
            this.monitoring = services.toList()
            this
        }

        AppBuilder withDpes(DpeInfo... dpes) {
            dpes.each {
                this.dpes[it.name().language()] = it
            }
            this
        }

        WorkerApplication build() {
            var app = new ApplicationInfo(ioServices(), services, monitoring)
            new WorkerApplication(app, dpes)
        }
    }

    static AppBuilder builder() {
        new AppBuilder()
    }

    static ApplicationInfo newAppInfo(ServiceInfo... services) {
        new ApplicationInfo(ioServices(), services as List, [])
    }

    private static Map<String, ServiceInfo> ioServices() {
        [
            (ApplicationInfo.STAGE): S1,
            (ApplicationInfo.READER): R1,
            (ApplicationInfo.WRITER): W1,
        ]
    }

    private static ServiceInfo service(String cont, String engine, ClaraLang lang) {
        new ServiceInfo("org.test." + engine, cont, engine, lang)
    }

    static DpeInfo dpe(String name) {
        new DpeInfo(new DpeName(name), CORES, "")
    }
}
