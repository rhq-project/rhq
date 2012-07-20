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
package org.rhq.enterprise.gui.installer.server.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerDetails {
    private final Log LOG = LogFactory.getLog(ServerDetails.class);

    public static final String DEFAULT_AFFINITY_GROUP = "";
    public static final int DEFAULT_ENDPOINT_PORT = 7080;
    public static final int DEFAULT_ENDPOINT_SECURE_PORT = 7443;

    private String name;
    private String endpointAddress;
    private int endpointPort;
    private int endpointSecurePort;
    private String affinityGroup;

    public ServerDetails(String name, String endpointAddress, int port, int securePort, String affinityGroup) {
        this.name = name;
        this.endpointAddress = endpointAddress;
        this.endpointPort = port;
        this.endpointSecurePort = securePort;
        this.affinityGroup = affinityGroup;
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
        return (String.valueOf(this.endpointPort));
    }

    public void setEndpointPortString(String endpointPort) {
        try {
            this.endpointPort = Integer.valueOf(endpointPort).intValue();
        } catch (NumberFormatException e) {
            LOG.debug("Failed to set port with invalid number: " + endpointPort);
        }
    }

    public int getEndpointSecurePort() {
        return endpointSecurePort;
    }

    public void setEndpointSecurePort(int endpointSecurePort) {
        this.endpointSecurePort = endpointSecurePort;
    }

    public String getEndpointSecurePortString() {
        return (String.valueOf(this.endpointSecurePort));
    }

    public void setEndpointSecurePortString(String endpointSecurePort) {
        try {
            this.endpointSecurePort = Integer.valueOf(endpointSecurePort).intValue();
        } catch (NumberFormatException e) {
            LOG.debug("Failed to set secure port with invalid number: " + endpointSecurePort);
        }
    }

    public String getAffinityGroup() {
        return affinityGroup;
    }

    public void setAffinityGroup(String affinityGroup) {
        if ((null != affinityGroup) && (!"".equals(affinityGroup.trim()))) {
            this.affinityGroup = affinityGroup;
        }
    }

    @Override
    public String toString() {
        return "[name=" + name + " address=" + endpointAddress + " port=" + endpointPort + " secureport="
            + endpointSecurePort + " affinitygroup=" + affinityGroup + "]";
    }
}
