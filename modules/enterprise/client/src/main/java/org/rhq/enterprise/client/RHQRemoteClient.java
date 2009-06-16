/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ChannelManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * A remote access client with transparent proxies to RHQ servers.
 *
 * @author Greg Hinkle, Simeon Pinder
 */
public class RHQRemoteClient {
    // Default locator values
    private String transport = "servlet";
    private String host = "localhost";
    private int port = 7080;
    private boolean loggedIn = false;

    public void setLoggedIn(boolean value) {
        this.loggedIn = value;
    }

    private Map<String, Object> allServices;
    private Subject subject = null;

    public RHQRemoteClient(String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }

    public boolean isConnected() {
        return this.loggedIn;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private RoleManagerRemote roleManagerRemote = null;
    private ContentManagerRemote contentManagerRemote = null;
    private SubjectManagerRemote subjectManagerRemote = null;
    private OperationManagerRemote operationManagerRemote = null;
    private ChannelManagerRemote channelManagerRemote = null;
    private ConfigurationManagerRemote configurationManagerRemote = null;
    private ResourceManagerRemote resourceManagerRemote = null;
    private ClientMain clireference;

    public RoleManagerRemote getRoleManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "RoleManager" + "Bean", RoleManagerRemote.class);
    }

    public ContentManagerRemote getContentManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "ContentManager" + "Bean", ContentManagerRemote.class);
    }

    public SubjectManagerRemote getSubjectManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "SubjectManager" + "Bean", SubjectManagerRemote.class);
    }

    public OperationManagerRemote getOperationManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "OperationManager" + "Bean", OperationManagerRemote.class);
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "ChannelManager" + "Bean", ChannelManagerRemote.class);
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setChannelManagerRemote(ChannelManagerRemote channelManagerRemote) {
        this.channelManagerRemote = channelManagerRemote;
    }

    public ConfigurationManagerRemote getConfigurationManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "ConfigurationManager" + "Bean",
            ConfigurationManagerRemote.class);
    }

    public ResourceManagerRemote getResourceManagerRemote() {
        return RHQRemoteClientProxy.getProcessor(this, "ResourceManager" + "Bean", ResourceManagerRemote.class);
    }

    public static final String[] SERVICE_NAMES = new String[] { "RoleManagerRemote", "ContentManagerRemote",
        "SubjectManagerRemote", "OperationManagerRemote", "ChannelManagerRemote", "ConfigurationManagerRemote",
        "ResourceManagerRemote" };

    public Map<String, Object> getAllServices() {
        if (this.allServices == null) {

            this.allServices = new HashMap<String, Object>();

            for (String serviceName : SERVICE_NAMES) {
                try {
                    Method m = this.getClass().getMethod("get" + serviceName);
                    this.allServices.put(serviceName, m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Couldn't load service " + serviceName + " due to missing class "
                        + e.getMessage());
                }
            }
        }

        return allServices;
    }

    /**
     * Called after host and port have been changed/set on login.
     */
    public void reinitialize() {
        try {
            init();
        } catch (Exception ex) {
            System.out.println("Exception reinitalizing with new host :" + ex.getMessage());
        }

    }

    public void setCliReference(ClientMain main) {
        this.clireference = main;
    }

    public Client getRemotingClient() {
        return remotingClient;
    }

    private Client remotingClient = null;

    private void init() {
        try {
            // create InvokerLocator with the url type string
            // indicating the target remoting server to call upon.
            String locatorURI = transport + "://" + host + ":" + port
                + "/jboss-remoting-servlet-invoker/ServerInvokerServlet";
            InvokerLocator locator = new InvokerLocator(locatorURI);

            remotingClient = new Client(locator);
            remotingClient.setSubsystem("REMOTEAPI");
            remotingClient.connect();
        } catch (Exception e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }
    }

}