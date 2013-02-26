/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.server.installer;

/**
 * Provides settings for the installer itself.
 *
 * @author John Mazzitelli
 */
public class InstallerConfiguration {
    private String managementHost = "127.0.0.1";
    private int managementPort = 9999; // this is the default AS port
    private boolean forceInstall = false;

    public InstallerConfiguration() {
    }

    // copy-constructor
    public InstallerConfiguration(InstallerConfiguration orig) {
        this.managementHost = orig.managementHost;
        this.managementPort = orig.managementPort;
    }

    public String getManagementHost() {
        return managementHost;
    }

    public void setManagementHost(String host) {
        if (host == null || host.length() == 0) {
            host = "127.0.0.1";
        }
        this.managementHost = host;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int port) {
        if (port <= 0) {
            port = 9999;
        }
        this.managementPort = port;
    }

    public boolean isForceInstall() {
        return this.forceInstall;
    }

    public void setForceInstall(boolean flag) {
        this.forceInstall = flag;
    }
}
