/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
