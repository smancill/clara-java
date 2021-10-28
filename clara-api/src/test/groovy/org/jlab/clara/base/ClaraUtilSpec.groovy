/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.base.core.ClaraConstants
import spock.lang.Rollup
import spock.lang.Shared
import spock.lang.Specification

@Rollup
class ClaraUtilSpec extends Specification {

    @Shared goodDpeNames = [
        "192.168.1.102_java",
        "192.168.1.102_cpp",
        "192.168.1.102_python",
        "192.168.1.102%20000_java",
        "192.168.1.102%16000_cpp",
        "192.168.1.102%9999_python",
    ]

    @Shared goodContainerNames = [
        "10.2.58.17_java:master",
        "10.2.58.17_cpp:container1",
        "10.2.58.17_python:User",
        "10.2.58.17%20000_python:User",
        "10.2.58.17_java:best_container",
        "10.2.58.17_cpp:with-hyphen",
    ]

    @Shared goodServiceNames = [
        "129.57.28.27_java:master:SimpleEngine",
        "129.57.28.27_cpp:container1:IntegrationEngine",
        "129.57.28.27_python:User:StatEngine",
        "129.57.28.27%20000_python:User:StatEngine",
        "129.57.28.27%9000_cpp:user-session:LogEngine",
    ]

    @Shared badDpeNames = [
        "192.168.1.102",
        "192.168.1.102%",
        "192_168_1_102_java",
        "192.168.1.102_erlang",
        "192.168.1.103:python",
        "192.168.1.103%aaa_python",
        "192 168 1 102 java",
        " 192.168.1.102_java",
    ]

    @Shared badContainerNames = [
        "10.2.9.9_java:",
        "10.2.9.9_java:name.part",
        "10.2.9.9_cpp:container:",
        "10.2.9.9_python:long,user",
        "10.2.58.17_python: User",
    ]

    @Shared badServiceNames = [
        "129.57.28.27_java:master:Simple:Engine",
        "129.57.28.27_cpp:container1:Integration...",
        "129.57.28.27_python:User:Stat,Engine",
        " 129.57.28.27_java:master:SimpleEngine",
        "129.57.28.27_java:master: SimpleEngine",
    ]

    def "Valid DPE names"() {
        expect:
        ClaraUtil.isDpeName(name)

        where:
        name << goodDpeNames
    }

    def "Invalid DPE names"() {
        expect:
        !ClaraUtil.isDpeName(name)

        where:
        name <<
            badDpeNames +
            goodContainerNames + badContainerNames +
            goodServiceNames + badServiceNames
    }

    def "Valid container names"() {
        expect:
        ClaraUtil.isContainerName(name)

        where:
        name << goodContainerNames
    }

    def "Invalid container names"() {
        expect:
        !ClaraUtil.isContainerName(name)

        where:
        name <<
            goodDpeNames + badDpeNames +
            badContainerNames +
            goodServiceNames + badServiceNames
    }

    def "Valid service names"() {
        expect:
        ClaraUtil.isServiceName(name)

        where:
        name << goodServiceNames
    }

    def "Invalid service names"() {
        expect:
        !ClaraUtil.isServiceName(name)

        where:
        name <<
            goodDpeNames + badDpeNames +
            goodContainerNames + badContainerNames +
            badServiceNames
    }

    def "Valid canonical names"() {
        expect:
        ClaraUtil.isCanonicalName(name)

        where:
        name << goodDpeNames + goodContainerNames + goodServiceNames
    }

    def "Invalid canonical names"() {
        expect:
        !ClaraUtil.isCanonicalName(name)

        where:
        name << badDpeNames + badContainerNames + badServiceNames
    }

    def "Get DPE host"() {
        expect:
        ClaraUtil.getDpeHost(name) == value

        where:
        name                  || value
        goodDpeNames[0]       || "192.168.1.102"
        goodContainerNames[0] || "10.2.58.17"
        goodServiceNames[0]   || "129.57.28.27"

        goodDpeNames[3]       || "192.168.1.102"
        goodContainerNames[3] || "10.2.58.17"
        goodServiceNames[3]   || "129.57.28.27"
    }

    def "Get DPE port"() {
        expect:
        ClaraUtil.getDpePort(name) == value

        where:
        name                  || value
        goodDpeNames[0]       || ClaraConstants.JAVA_PORT
        goodContainerNames[0] || ClaraConstants.JAVA_PORT
        goodServiceNames[0]   || ClaraConstants.JAVA_PORT

        goodDpeNames[1]       || ClaraConstants.CPP_PORT
        goodContainerNames[1] || ClaraConstants.CPP_PORT
        goodServiceNames[1]   || ClaraConstants.CPP_PORT

        goodDpeNames[2]       || ClaraConstants.PYTHON_PORT
        goodContainerNames[2] || ClaraConstants.PYTHON_PORT
        goodServiceNames[2]   || ClaraConstants.PYTHON_PORT

        goodDpeNames[3]       || 20000
        goodContainerNames[3] || 20000
        goodServiceNames[3]   || 20000
    }

    def "Get DPE lang"() {
        expect:
        ClaraUtil.getDpeLang(name) == value

        where:
        name                  || value
        goodDpeNames[0]       || ClaraConstants.JAVA_LANG
        goodContainerNames[0] || ClaraConstants.JAVA_LANG
        goodServiceNames[0]   || ClaraConstants.JAVA_LANG

        goodDpeNames[3]       || ClaraConstants.JAVA_LANG
        goodContainerNames[3] || ClaraConstants.PYTHON_LANG
        goodServiceNames[3]   || ClaraConstants.PYTHON_LANG
    }

    def "Get DPE name"() {
        expect:
        ClaraUtil.getDpeName(name) == value

        where:
        name                  || value
        goodDpeNames[0]       || "192.168.1.102_java"
        goodContainerNames[0] || "10.2.58.17_java"
        goodServiceNames[0]   || "129.57.28.27_java"

        goodDpeNames[3]       || "192.168.1.102%20000_java"
        goodContainerNames[3] || "10.2.58.17%20000_python"
        goodServiceNames[3]   || "129.57.28.27%20000_python"
    }

    def "Get container canonical name"() {
        expect:
        ClaraUtil.getContainerCanonicalName(name) == value

        where:
        name                  || value
        goodContainerNames[0] || "10.2.58.17_java:master"
        goodServiceNames[0]   || "129.57.28.27_java:master"
    }

    def "Get container name"() {
        expect:
        ClaraUtil.getContainerName(name) == value

        where:
        name                  || value
        goodContainerNames[0] || "master"
        goodServiceNames[0]   || "master"
    }

    def "Get engine name"() {
        expect:
        ClaraUtil.getEngineName(name) == value

        where:
        name                  || value
        goodServiceNames[0]   || "SimpleEngine"
    }

    def "Construct new DPE name"() {
        expect:
        dpeName.canonicalName() == canonicalName

        where:
        dpeName                                   || canonicalName
        new DpeName("10.2.58.17", ClaraLang.JAVA) || "10.2.58.17_java"
    }

    def "Construct new container name"() {
        expect:
        containerName.canonicalName() == "10.2.58.17_java:master"

        where:
        containerName << [
            new ContainerName(new DpeName("10.2.58.17", ClaraLang.JAVA), "master"),
            new ContainerName("10.2.58.17", ClaraLang.JAVA, "master"),
        ]
    }

    def "Construct new service name"() {
        expect:
        serviceName.canonicalName() == "10.2.58.17_java:cont:Engine"

        where:
        serviceName << [
            new ServiceName(new ContainerName("10.2.58.17", ClaraLang.JAVA, "cont"), "Engine"),
            new ServiceName("10.2.58.17", ClaraLang.JAVA, "cont", "Engine"),
        ]
    }

    def "Split long line keeps a single line when max length is enough"() {
        given:
        var line = "Call me Ishmael."
        var maxLen = line.length() + extraLen

        expect:
        ClaraUtil.splitIntoLines(line, prefix, maxLen) == result

        where:
        prefix | extraLen || result
        ""     | 0        || "Call me Ishmael."
        ""     | 10       || "Call me Ishmael."
        "    " | 0        || "    Call me Ishmael."
        "    " | 10       || "    Call me Ishmael."
    }

    def "Split long line into multiple lines of given max length"() {
        given:
        var text = "Moby Dick seeks thee not. It is thou, thou, that madly seekest him!"

        expect:
        ClaraUtil.splitIntoLines(text, prefix, 25) == result

        where:
        result << [
            """\
            Moby Dick seeks thee not.
            It is thou, thou, that
            madly seekest him!""".stripIndent(),
            """\
            >>>Moby Dick seeks thee not.
            >>>It is thou, thou, that
            >>>madly seekest him!""".stripIndent(),
        ]
        prefix << ["", ">>>"]
    }
}
