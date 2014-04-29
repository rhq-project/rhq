/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.install.remote;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.install.remote.AgentInstallInfo;
import org.rhq.core.domain.install.remote.CustomAgentInstallData;
import org.rhq.core.domain.install.remote.RemoteAccessInfo;

/**
 * Provides an interface to remotely install an RHQ Agent over SSH.
 *
 * @author Greg Hinkle
 */
@Remote
public interface RemoteInstallManagerRemote {
    /**
     * Checks to see if an agent is installed in the given directory.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param agentInstallPath the directory to check
     *
     * @return true if an agent is installed in the given install path, false if not
     */
    boolean agentInstallCheck(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath);

    /**
     * Installs the agent update binary distribution file to the given parent
     * directory. Note that the agent's install directory will be a child of
     * the given parent directory, with that child install directory usually
     * named "rhq-agent".
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param customData contains custom install details such as where the agent install directory will be
     *
     * @return info containing the results of the installation
     * @since 4.11
     */
    AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, CustomAgentInstallData customData);

    /**
     * @param subject
     * @param remoteAccessInfo
     * @param parentPath
     * @return info containing the results of the installation
     * @deprecated use {@link #installAgent(Subject, RemoteAccessInfo, CustomAgentInstallData)}
     */
    @Deprecated
    AgentInstallInfo installAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

    /**
     * Uninstalls the actual distribution files of an installed agent. You must give a valid agent name
     * in the remote access info and that agent name must have its install location in the database for
     * this to work. We purposefully do not allow you to give just any path to uninstall as that could
     * have bad implications such as wiping out large portions of the disk as a mistake.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials including the name of the agent.
     * @return the results of the uninstall - this will be null if there was no install
     *         information for the given agent (and thus nothing was uninstalled).
     * @since 4.11
     */
    String uninstallAgent(Subject subject, RemoteAccessInfo remoteAccessInfo);

    /**
     * Starts the agent located in the given installation directory.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param agentInstallPath where the agent is installed
     *
     * @return results of the start command
     */
    String startAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath);

    /**
     * Stops the agent located in the given installation directory.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param agentInstallPath where the agent is installed
     *
     * @return results of the stop command
     */
    String stopAgent(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath);

    /**
     * Determines the running status of the agent located in the given installation directory.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param agentInstallPath where the agent is installed
     *
     * @return results of the status command
     */
    String agentStatus(Subject subject, RemoteAccessInfo remoteAccessInfo, String agentInstallPath);

    /**
     * Given a root parent path to check, this will scan all subdirectories (recursively)
     * to try to find where the agent is installed (if it is installed at all). If parentPath
     * is null or empty, the more common locations where agents are normally installed will be
     * scanned. Returns the path to the first location where an agent is probably installed;
     * <code>null</code> is returned if it does not look like the agent is installed anywhere
     * under the given parent path (or in any of the common locations, if parent path is null).
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param parentPath the parent directory whose children files/directories are scanned
     *
     * @return the probable location of an installed agent; null if no agent install was found
     */
    String findAgentInstallPath(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);

    /**
     * Returns the given parent directory's child files/directories.
     *
     * @param subject the RHQ user making the request
     * @param remoteAccessInfo the remote machine information and remote user SSH credentials
     * @param parentPath the parent directory whose children files/directories are returned
     *
     * @return names of the parent's child files/directories
     */
    String[] remotePathDiscover(Subject subject, RemoteAccessInfo remoteAccessInfo, String parentPath);
}
