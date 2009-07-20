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
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.exception.LoginException;

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
        AvailabilityManager(AvailabilityManagerRemote.class), //
        CallTimeDataManager(CallTimeDataManagerRemote.class), // 
        ChannelManager(ChannelManagerRemote.class), //
        ConfigurationManager(ConfigurationManagerRemote.class), //
        //ContentHelperManager(ContentHelperRemote.class), //
        ContentManager(ContentManagerRemote.class), //
        DataAccess(DataAccessRemote.class), //
        EventManager(EventManagerRemote.class), //
        MeasurementBaselineManager(MeasurementBaselineManagerRemote.class), //
        MeasurementDataManager(MeasurementDataManagerRemote.class), //
        MeasurementDefinitionManager(MeasurementDefinitionManagerRemote.class), // 
        MeasurementScheduleManager(MeasurementScheduleManagerRemote.class), //
        OperationManager(OperationManagerRemote.class), //
        ResourceManager(ResourceManagerRemote.class), //
        ResourceGroupManager(ResourceGroupManagerRemote.class), //
        ResourceTypeManager(ResourceTypeManagerRemote.class), //
        RoleManager(RoleManagerRemote.class), //
        SubjectManager(SubjectManagerRemote.class),
        //        RemoteInstallManager(RemoteInstallManagerRemote.class),
        ;

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

    /** Map K=ManagerName V=RemoteProxy */
    private Map<String, Object> managers;
    private Subject subject = null;

    public RemoteClient(String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }


    public Subject login(String user, String password) throws LoginException {
        this.subject = getSubjectManagerRemote().login(user, password);
        return subject;
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

    public AvailabilityManagerRemote getAvailabilityManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.AvailabilityManager);
    }

    public CallTimeDataManagerRemote getCallTimeDataManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.CallTimeDataManager);
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ChannelManager);
    }

    public ConfigurationManagerRemote getConfigurationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ConfigurationManager);
    }

    /*
    public ContentHelperRemote getContentHelperRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ContentHelperManager);
    }
    */

    public ContentManagerRemote getContentManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ContentManager);
    }

    public DataAccessRemote getDataAccessRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.DataAccess);
    }

    public EventManagerRemote getEventManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.EventManager);
    }

    public MeasurementBaselineManagerRemote getMeasurementBaselineManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementBaselineManager);
    }

    public MeasurementDataManagerRemote getMeasurementDataManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementDataManager);
    }

    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementDefinitionManager);
    }

    public MeasurementScheduleManagerRemote getMeasurementScheduleManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.MeasurementScheduleManager);
    }

    public OperationManagerRemote getOperationManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.OperationManager);
    }

    public ResourceManagerRemote getResourceManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceManager);
    }

    public ResourceGroupManagerRemote getResourceGroupManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceGroupManager);
    }

    public ResourceTypeManagerRemote getResourceTypeManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.ResourceTypeManager);
    }

    public RoleManagerRemote getRoleManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.RoleManager);
    }

    public SubjectManagerRemote getSubjectManagerRemote() {
        return RemoteClientProxy.getProcessor(this, Manager.SubjectManager);
    }

    //    public RemoteInstallManagerRemote getRemoteInstallManagerRemote() {
    //        return RemoteClientProxy.getProcessor(this, Manager.RemoteInstallManager);
    //    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * 
     * @return Map K=manager name V=remote proxy
     */
    public Map<String, Object> getManagers() {
        if (this.managers == null) {

            this.managers = new HashMap<String, Object>();

            for (Manager manager : Manager.values()) {
                try {
                    Method m = this.getClass().getMethod("get" + manager.remoteName());
                    this.managers.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Failed to load manager " + manager + " due to missing class: " + e);
                }
            }
        }

        return managers;
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