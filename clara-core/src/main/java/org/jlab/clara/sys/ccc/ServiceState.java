/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.ccc;

/**
 * Defines service-name and state pair.
 *
 * @author gurjyan
 * @version 1.x
 * @since 5/21/15
 */
public class ServiceState {

    private final String name;
    private String state;

    public ServiceState(String name, String state) {
        this.name = name;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ServiceState other = (ServiceState) obj;

        if (!name.equals(other.name)) {
            return false;
        }
        if (!state.equals(other.state)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceState{" + "name='" + name + "', state='" + state + "'}";
    }
}
