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

package org.jlab.clara.base.core

import org.jlab.clara.msg.core.Topic
import spock.lang.Specification

class ClaraComponentSpec extends Specification {

    def "Create a Java DPE component"() {
        given:
        var component = ClaraComponent.dpe("10.2.9.1_java")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_java"
            topic == Topic.wrap("dpe:10.2.9.1_java")
            dpeCanonicalName == "10.2.9.1_java"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.JAVA_LANG
            dpePort == ClaraConstants.JAVA_PORT
        }
    }

    def "Create a Java container component"() {
        given:
        var component = ClaraComponent.container("10.2.9.1_java:master")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_java:master"
            topic == Topic.wrap("container:10.2.9.1_java:master")

            dpeCanonicalName == "10.2.9.1_java"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.JAVA_LANG
            dpePort == ClaraConstants.JAVA_PORT

            containerName == "master"
        }
    }

    def "Create a Java service component"() {
        given:
        var component = ClaraComponent.service("10.2.9.1_java:master:E1")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_java:master:E1"
            topic == Topic.wrap("10.2.9.1_java:master:E1")

            dpeCanonicalName == "10.2.9.1_java"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.JAVA_LANG
            dpePort == ClaraConstants.JAVA_PORT

            containerName == "master"
            engineName == "E1"
        }
    }

    def "Create a C++ DPE component"() {
        given:
        var component = ClaraComponent.dpe("10.2.9.1_cpp")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_cpp"
            topic == Topic.wrap("dpe:10.2.9.1_cpp")

            dpeCanonicalName == "10.2.9.1_cpp"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.CPP_LANG
            dpePort == ClaraConstants.CPP_PORT
        }
    }

    def "Create a C++ container component"() {
        given:
        var component = ClaraComponent.container("10.2.9.1_cpp:master")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_cpp:master"
            topic == Topic.wrap("container:10.2.9.1_cpp:master")

            dpeCanonicalName == "10.2.9.1_cpp"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.CPP_LANG
            dpePort == ClaraConstants.CPP_PORT

            containerName == "master"
        }
    }

    def "Create a C++ service component"() {
        given:
        var component = ClaraComponent.service("10.2.9.1_cpp:master:E1")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_cpp:master:E1"
            topic == Topic.wrap("10.2.9.1_cpp:master:E1")

            dpeCanonicalName == "10.2.9.1_cpp"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.CPP_LANG
            dpePort == ClaraConstants.CPP_PORT

            containerName == "master"
            engineName == "E1"
        }
    }

    def "Create a Python DPE component"() {
        given:
        var component = ClaraComponent.dpe("10.2.9.1_python")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_python"
            topic == Topic.wrap("dpe:10.2.9.1_python")

            dpeCanonicalName == "10.2.9.1_python"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.PYTHON_LANG
            dpePort == ClaraConstants.PYTHON_PORT
        }
    }

    def "Create a Python container component"() {
        given:
        var component = ClaraComponent.container("10.2.9.1_python:master")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_python:master"
            topic == Topic.wrap("container:10.2.9.1_python:master")

            dpeCanonicalName == "10.2.9.1_python"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.PYTHON_LANG
            dpePort == ClaraConstants.PYTHON_PORT

            containerName == "master"
        }
    }

    def "Create a Python service component"() {
        given:
        var component = ClaraComponent.service("10.2.9.1_python:master:E1")

        expect:
        with(component) {
            canonicalName == "10.2.9.1_python:master:E1"
            topic == Topic.wrap("10.2.9.1_python:master:E1")

            dpeCanonicalName == "10.2.9.1_python"
            dpeHost == "10.2.9.1"
            dpeLang == ClaraConstants.PYTHON_LANG
            dpePort == ClaraConstants.PYTHON_PORT

            containerName == "master"
            engineName == "E1"
        }
    }

    def "Create a component with custom port"() {
        given:
        var component = ClaraComponent.dpe("10.2.9.1%9999_java")

        expect:
        with(component) {
            canonicalName == "10.2.9.1%9999_java"
            topic == Topic.wrap("dpe:10.2.9.1%9999_java")

            dpeCanonicalName == "10.2.9.1%9999_java"
            dpeHost == "10.2.9.1"
            dpeLang == "java"
            dpePort == 9999
        }
    }
}
