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
package org.rhq.enterprise.gui.installer.client.gwt;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;

/**
 * @author John Mazzitelli
 */
public interface InstallerGWTService extends RemoteService {

    /**
     * Returns a list of all registered servers in the database.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return list of all existing servers registered in the database.
     * @throws Exception
     */
    ArrayList<String> getServerNames(String connectionUrl, String username, String password) throws Exception;

    /**
     * Returns details on a specific server that is registered in the database.
     * 
     * @param connectionUrl
     * @param username
     * @param password
     * @param serverName the name of the server whose details are to be retrieved
     * @return the details of the server or null if the server does not exist
     * @throws Exception
     */
    ServerDetails getServerDetails(String connectionUrl, String username, String password, String serverName)
        throws Exception;

    /**
     * Tests to see if there is already a schema installed.
     * 
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>true</code> if there appears to be a schema installed in the database already.
     * @throws Exception
     */
    boolean isDatabaseSchemaExist(String connectionUrl, String username, String password) throws Exception;

    /**
     * Tests that the database can be connected to with the given URL and credentials.
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>null</code> if the connection succeeded; this will be an error message if failed
     * @throws Exception
     */
    String testConnection(String connectionUrl, String username, String password) throws Exception;

    /**
     * Returns the rhq-server.properties values in a map.
     * @return server properties
     * @throws Exception
     */
    HashMap<String, String> getServerProperties() throws Exception;

    /**
     * Saves the rhq-server.properties with the given values.
     * @param serverProperties
     * @throws Exception
     */
    void saveServerProperties(HashMap<String, String> serverProperties) throws Exception;

    /**
     * Returns the version string for the app server itself (e.g. "7.1.2.Final").
     * @return version string of app server
     * @throws Exception
     */
    String getAppServerVersion() throws Exception;

    /**
     * Returns the general type of operating system the server is running on (e.g. "Linux").
     * @return os type name
     * @throws Exception
     */
    String getOperatingSystem() throws Exception;
}
