/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.installer.client.shared;

import java.io.Serializable;

/**
 * Provides details about the server if it is already a known one (by "known" meaning it is
 * in the database already).
 * 
 * @author John Mazzitelli
 */
public class ServerDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_AFFINITY_GROUP = "";
    public static final int DEFAULT_ENDPOINT_PORT = 7080;
    public static final int DEFAULT_ENDPOINT_SECURE_PORT = 7443;

    private String name;
    private String endpointAddress;
    private int endpointPort;
    private int endpointSecurePort;

    protected ServerDetails() {
        // for GWT
    }

    public ServerDetails(String name, String endpointAddress, int port, int securePort) {
        this.name = name;
        this.endpointAddress = endpointAddress;
        this.endpointPort = port;
        this.endpointSecurePort = securePort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if ((null != name) && (!"".equals(name.trim()))) {
            this.name = name;
        }
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public void setEndpointAddress(String endpointAddress) {
        if ((null != endpointAddress) && (!"".equals(endpointAddress.trim()))) {
            this.endpointAddress = endpointAddress;
        }
    }

    public int getEndpointPort() {
        return endpointPort;
    }

    public void setEndpointPort(int endpointPort) {
        this.endpointPort = endpointPort;
    }

    public String getEndpointPortString() {
        return (String.valueOf(endpointPort));
    }

    public void setEndpointPortString(String endpointPort) {
        this.endpointPort = Integer.valueOf(endpointPort).intValue();
    }

    public int getEndpointSecurePort() {
        return endpointSecurePort;
    }

    public void setEndpointSecurePort(int endpointSecurePort) {
        this.endpointSecurePort = endpointSecurePort;
    }

    public String getEndpointSecurePortString() {
        return (String.valueOf(endpointSecurePort));
    }

    public void setEndpointSecurePortString(String endpointSecurePort) {
        this.endpointSecurePort = Integer.valueOf(endpointSecurePort).intValue();
    }

    @Override
    public String toString() {
        return "[name=" + name + ", address=" + endpointAddress + ", port=" + endpointPort + ", secureport="
            + endpointSecurePort + "]";
    }
}
