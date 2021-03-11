/*
 * Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved.
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

package org.jlab.clara.std.orchestrators

import org.jlab.clara.base.ClaraLang
import org.jlab.clara.base.DpeName

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
            dpes = [(DPE1.name.language()): DPE1]
        }

        AppBuilder withServices(ServiceInfo... services) {
            this.services = services
            this
        }

        AppBuilder withMonitoring(ServiceInfo... services) {
            this.monitoring = services
            this
        }

        AppBuilder withDpes(DpeInfo... dpes) {
            dpes.each {
                this.dpes[it.name.language()] = it
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
