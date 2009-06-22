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
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ChannelManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;

/**
 * A remote access client with transparent proxies to RHQ servers.
 *
 * @author Greg Hinkle
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */
public class RemoteClient {

    public enum Manager {
        AlertManager(AlertManagerRemote.class), //
        AlertDefinitionManager(AlertDefinitionManagerRemote.class), //
        ChannelManager(ChannelManagerRemote.class), //
        ConfigurationManager(ConfigurationManagerRemote.class), //
        ContentManager(ContentManagerRemote.class), //
        OperationManager(OperationManagerRemote.class), //
        ResourceManager(ResourceManagerRemote.class), //
        ResourceGroupManager(ResourceGroupManagerRemote.class), //
        RoleManager(RoleManagerRemote.class), //
        SubjectManager(SubjectManagerRemote.class);

        private Class<?> remote;
        private String remoteName;
        private String beanName;

        private Manager(Class<?> remote) {
            this.remote = remote;
            this.beanName = this.name() + "Bean";
            this.remoteName = this.name() + "Remote";
        }

        Class<?> remote() {
            return this.remote;
        }

        public String beanName() {
            return this.beanName;
        }

        public String remoteName() {
            return this.remoteName;
        }
    };

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

    public RemoteClient(String host, int port) {
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

    public AlertManagerRemote getAlertManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AlertManager);
    }

    public AlertDefinitionManagerRemote getAlertDefinitionManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AlertDefinitionManager);
    }

    public ConfigurationManagerRemote getConfigurationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ConfigurationManager);
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ChannelManager);
    }

    public ContentManagerRemote getContentManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ContentManager);
    }

    public OperationManagerRemote getOperationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.OperationManager);
    }

    public RoleManagerRemote getRoleManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.RoleManager);
    }

    public ResourceManagerRemote getResourceManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceManager);
    }

    public ResourceGroupManagerRemote getResourceGroupManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceGroupManager);
    }

    public SubjectManagerRemote getSubjectManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.SubjectManager);
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Map<String, Object> getAllManagers() {
        if (this.allServices == null) {

            this.allServices = new HashMap<String, Object>();

            for (Manager manager : Manager.values()) {
                try {
                    Method m = this.getClass().getMethod("get" + manager.remoteName());
                    this.allServices.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Failed to load manager " + manager + " due to missing class: " + e);
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

    public Client getRemotingClient() {
        return remotingClient;
    }

    private Client remotingClient = null;

    private void init() {
        try {
            // create InvokerLocator with the url type string indicating the target remoting server to call upon.
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