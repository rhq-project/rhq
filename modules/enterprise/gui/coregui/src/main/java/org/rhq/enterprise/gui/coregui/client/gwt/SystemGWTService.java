/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.composite.SystemSettings;

/**
 * Provides information about the server as well as a way to reconfigure parts of the server.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public interface SystemGWTService extends RemoteService {

    ProductInfo getProductInfo() throws RuntimeException;

    ServerDetails getServerDetails() throws RuntimeException;

    String getSessionTimeout() throws RuntimeException;

    SystemSettings getSystemSettings() throws RuntimeException;

    void setSystemSettings(SystemSettings settings) throws RuntimeException;

    /**
     * @return metadata properties about the agent download available on the server.
     */
    HashMap<String, String> getAgentVersionProperties() throws RuntimeException;

    /**
     * Returns the name and download URL (key and value respectively) of all connector downloads
     * available on the server. The URL is relative to the server's host and port (i.e. only
     * the path of the URL is returned).
     *
     * @return information about connectors that can be downloaded from the server
     */
    HashMap<String, String> getConnectorDownloads() throws RuntimeException;

    /**
     * Returns the name and download URL (key and value respectively) of all data-migrator downloads
     * available on the server. The URL is relative to the server's host and port (i.e. only
     * the path of the URL is returned).
     *
     * @return information about data-migrators that can be downloaded from the server
     */
    HashMap<String, String> getMigratorDownloads() throws RuntimeException;

    /**
     * Returns the name and download URL (key and value respectively) of all CLI alert scripts
     * that are available on the server for download. These are just scripts that users can
     * then use to create actual cli scripts for alert notifications. These scripts will usually
     * require some tweaking to be usable as alert scripts.
     *
     * @return info about the available CLI alert scripts
     * @throws RuntimeException
     */
    HashMap<String, String> getCliAlertScriptDownloads() throws RuntimeException;

    /**
     * Returns the name and download URL (key and value respectively) of all script modules
     * that are available on the server for download. These are provided by RHQ for other
     * scripts to peruse using the module-loading mechanism of particular language.
     *
     * @return info about the available script modules
     * @throws RuntimeException
     */
    HashMap<String, String> getScriptModulesDownloads() throws RuntimeException;

    /**
     * @return metadata properties about the CLI download available on the server.
     */
    HashMap<String, String> getClientVersionProperties() throws RuntimeException;

    /**
     * Returns the name and download URL (key and value respectively) of the
     * standalone bundle deployer tool available on the server for download.
     * The URL is relative to the server's host and port (i.e. only
     * the path of the URL is returned).
     *
     * The bundle deployer is a standalone tool that bundle authors/developers can use
     * to test their bundles prior to uploading to RHQ and deploying to managed platforms.
     *
     * @return information about the bundle deployer tool that can be downloaded from the server
     */
    HashMap<String, String> getBundleDeployerDownload() throws RuntimeException;

    Boolean isLdapAuthorizationEnabled() throws RuntimeException;

    /**
     * GWT-RPC service  asynchronous (client-side) interface
     * @see SystemGWTService
     */
    void dumpToLog();
}
