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
import java.util.Map;
import java.util.HashMap;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.*;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerBean;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerBean;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerBean;
import org.rhq.enterprise.server.report.DataAccessBean;
import org.rhq.enterprise.server.report.DataAccessLocal;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerBean;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerBean;
import org.rhq.enterprise.server.alert.*;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerBean;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.core.EmailManagerBean;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerBean;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerBean;
import org.rhq.enterprise.server.measurement.*;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerBean;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerBean;
import org.rhq.enterprise.server.content.*;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerBean;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossBean;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.event.EventManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerBean;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerBean;
import org.rhq.enterprise.server.test.SubjectRoleTestBeanLocal;
import org.rhq.enterprise.server.test.SubjectRoleTestBean;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.perspective.PerspectiveManagerBean;

/**
 * A remote access client with transparent proxies to RHQ servers.
 *
 * @author Greg Hinkle
 */
public class RHQRemoteClient {
    // Default locator values
    private String transport = "servlet";
    private String host = "localhost";
    private int port = 7080;

    private Client remotingClient = null;
    private Map<String, Object> allServices;

    public RHQRemoteClient(String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }



    private void init() {
        try {
            // create InvokerLocator with the url type string
            // indicating the target remoting server to call upon.
            String locatorURI = transport + "://" + host + ":" + port + "/jboss-remoting-servlet-invoker/ServerInvokerServlet";
            InvokerLocator locator = new InvokerLocator(locatorURI);
            System.out.println("Calling remoting server with locator uri of: " + locatorURI);

            remotingClient = new Client(locator);
//        remotingClient.setSubsystem("EJB3");
            remotingClient.setSubsystem("AOP");
            remotingClient.connect();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public boolean isConnected() {
        return this.remotingClient.isConnected();
    }

    /*

    public static void main(String[] args) throws Throwable {


        String[] foo = new String[0];
        System.out.println(foo.getClass().getName());

        SubjectManagerLocal subjectManager = RHQRemoteClient.getSubjectManager();

        Subject subject = subjectManager.login("rhqadmin", "rhqadmin");

        System.out.println("Login successful: " + subject);

        ResourceManagerLocal resourceManager = RHQRemoteClient.getResourceManager();
        Resource res = resourceManager.getResourceTree(500050, true);

        System.out.println("res: " + res);

    }*/

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public AgentManagerLocal getAgentManager() {
        return RHQRemoteClientProxy.getProcessor(this, AgentManagerBean.class, AgentManagerLocal.class);
    }

    public AlertConditionCacheManagerLocal getAlertConditionCacheManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertConditionCacheManagerBean.class, AlertConditionCacheManagerLocal.class);
    }

    public AlertConditionManagerLocal getAlertConditionManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertConditionManagerBean.class, AlertConditionManagerLocal.class);
    }

    public AlertDefinitionManagerLocal getAlertDefinitionManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertDefinitionManagerBean.class, AlertDefinitionManagerLocal.class);
    }

    public AlertManagerLocal getAlertManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertManagerBean.class, AlertManagerLocal.class);
    }

    public AlertNotificationManagerLocal getAlertNotificationManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertNotificationManagerBean.class, AlertNotificationManagerLocal.class);
    }

    public AlertTemplateManagerLocal getAlertTemplateManager() {
        return RHQRemoteClientProxy.getProcessor(this, AlertTemplateManagerBean.class, AlertTemplateManagerLocal.class);
    }

    public AuthorizationManagerLocal getAuthorizationManager() {
        return RHQRemoteClientProxy.getProcessor(this, AuthorizationManagerBean.class, AuthorizationManagerLocal.class);
    }

    public AvailabilityManagerLocal getAvailabilityManager() {
        return RHQRemoteClientProxy.getProcessor(this, AvailabilityManagerBean.class, AvailabilityManagerLocal.class);
    }

    public CallTimeDataManagerLocal getCallTimeDataManager() {
        return RHQRemoteClientProxy.getProcessor(this, CallTimeDataManagerBean.class, CallTimeDataManagerLocal.class);
    }

    public ConfigurationManagerLocal getConfigurationManager() {
        return RHQRemoteClientProxy.getProcessor(this, ConfigurationManagerBean.class, ConfigurationManagerLocal.class);
    }

    public ContentManagerLocal getContentManager() {
        return RHQRemoteClientProxy.getProcessor(this, ContentManagerBean.class, ContentManagerLocal.class);
    }

    public ContentUIManagerLocal getContentUIManager() {
        return RHQRemoteClientProxy.getProcessor(this, ContentUIManagerBean.class, ContentUIManagerLocal.class);
    }

    public DiscoveryBossLocal getDiscoveryBoss() {
        return RHQRemoteClientProxy.getProcessor(this, DiscoveryBossBean.class, DiscoveryBossLocal.class);
    }

    public EmailManagerLocal getEmailManagerBean() {
        return RHQRemoteClientProxy.getProcessor(this, EmailManagerBean.class, EmailManagerLocal.class);
    }

    public EventManagerLocal getEventManager() {
        return RHQRemoteClientProxy.getProcessor(this, EventManagerBean.class, EventManagerLocal.class);
    }

    public GroupDefinitionManagerLocal getGroupDefinitionManager() {
        return RHQRemoteClientProxy.getProcessor(this, GroupDefinitionManagerBean.class, GroupDefinitionManagerLocal.class);
    }

    public MeasurementDefinitionManagerLocal getMeasurementDefinitionManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementDefinitionManagerBean.class, MeasurementDefinitionManagerLocal.class);
    }

    public MeasurementScheduleManagerLocal getMeasurementScheduleManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementScheduleManagerBean.class, MeasurementScheduleManagerLocal.class);
    }

    public MeasurementDataManagerLocal getMeasurementDataManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementDataManagerBean.class, MeasurementDataManagerLocal.class);
    }

    public MeasurementCompressionManagerLocal getMeasurementCompressionManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementCompressionManagerBean.class, MeasurementCompressionManagerLocal.class);
    }

    public MeasurementProblemManagerLocal getMeasurementProblemManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementProblemManagerBean.class, MeasurementProblemManagerLocal.class);
    }

    public MeasurementBaselineManagerLocal getMeasurementBaselineManager() {
        return RHQRemoteClientProxy.getProcessor(this, MeasurementBaselineManagerBean.class, MeasurementBaselineManagerLocal.class);
    }

    public OperationManagerLocal getOperationManager() {
        return RHQRemoteClientProxy.getProcessor(this, OperationManagerBean.class, OperationManagerLocal.class);
    }

    public ConfigurationMetadataManagerLocal getConfigurationMetadataManager() {
        return RHQRemoteClientProxy.getProcessor(this, ConfigurationMetadataManagerBean.class, ConfigurationMetadataManagerLocal.class);
    }

    public ContentSourceMetadataManagerLocal getContentSourceMetadataManager() {
        return RHQRemoteClientProxy.getProcessor(this, ContentSourceMetadataManagerBean.class, ContentSourceMetadataManagerLocal.class);
    }

    public ContentSourceManagerLocal getContentSourceManager() {
        return RHQRemoteClientProxy.getProcessor(this, ContentSourceManagerBean.class, ContentSourceManagerLocal.class);
    }

    public ChannelManagerLocal getChannelManagerLocal() {
        return RHQRemoteClientProxy.getProcessor(this, ChannelManagerBean.class, ChannelManagerLocal.class);
    }

    public ResourceMetadataManagerLocal getResourceMetadataManager() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceMetadataManagerBean.class, ResourceMetadataManagerLocal.class);
    }

    public ResourceBossLocal getResourceBoss() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceBossBean.class, ResourceBossLocal.class);
    }

    public ResourceGroupManagerLocal getResourceGroupManager() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceGroupManagerBean.class, ResourceGroupManagerLocal.class);
    }

    public ResourceManagerLocal getResourceManager() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceManagerBean.class, ResourceManagerLocal.class);
    }

    /*public ResourceManagerRemote getResourceManagerRemote() {
        return lookupRemote(ResourceManagerBean.class,);
    }
    */

    public DataAccessLocal getDataAccess() {
        return RHQRemoteClientProxy.getProcessor(this, DataAccessBean.class, DataAccessLocal.class);
    }

    public ResourceFactoryManagerLocal getResourceFactoryManager() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceFactoryManagerBean.class, ResourceFactoryManagerLocal.class);
    }

    public ResourceTypeManagerLocal getResourceTypeManager() {
        return RHQRemoteClientProxy.getProcessor(this, ResourceTypeManagerBean.class, ResourceTypeManagerLocal.class);
    }

    public RoleManagerLocal getRoleManager() {
        return RHQRemoteClientProxy.getProcessor(this, RoleManagerBean.class, RoleManagerLocal.class);
    }

    public SchedulerLocal getSchedulerBean() {
        return RHQRemoteClientProxy.getProcessor(this, SchedulerBean.class, SchedulerLocal.class);
    }

    public SubjectManagerLocal getSubjectManager() {
        return RHQRemoteClientProxy.getProcessor(this, SubjectManagerBean.class, SubjectManagerLocal.class);
    }

    /*public SubjectManagerRemote getSubjectManagerRemote() {
        return lookupRemote(SubjectManagerBean.class);
    }
*/
    public SubjectRoleTestBeanLocal getSubjectRoleTestBean() {
        return RHQRemoteClientProxy.getProcessor(this, SubjectRoleTestBean.class, SubjectRoleTestBeanLocal.class);
    }

    public SystemManagerLocal getSystemManager() {
        return RHQRemoteClientProxy.getProcessor(this, SystemManagerBean.class, SystemManagerLocal.class);
    }

    public PerspectiveManagerLocal getPerspectiveManager() {
        return RHQRemoteClientProxy.getProcessor(this, PerspectiveManagerBean.class, PerspectiveManagerLocal.class);
    }

    public ProductVersionManagerLocal getProductVersionManager() {
        return RHQRemoteClientProxy.getProcessor(this, ProductVersionManagerBean.class, ProductVersionManagerLocal.class);
    }

    public CachedConditionProducerLocal getCachedConditionProducerLocal() {
        return RHQRemoteClientProxy.getProcessor(this, CachedConditionProducerBean.class, CachedConditionProducerLocal.class);
    }


    public Client getRemotingClient() {
        return remotingClient;
    }

    public Map<String, Object> getAllServices() {
        if (this.allServices == null) {

            this.allServices = new HashMap<String, Object>();

            for (String serviceName : SERVICE_NAMES) {
                try {
                    Method m = this.getClass().getMethod("get" + serviceName);
                    this.allServices.put(serviceName, m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Couldn't load service " + serviceName + " due to missing class " + e.getMessage());
                }
            }
        }

        return allServices;
    }

    public static final String[] SERVICE_NAMES = new String[]
            {
                    "AgentManager",
                    "AlertConditionCacheManager",
                    "AlertConditionManager",
                    "AlertDefinitionManager",
                    "AlertManager",
                    "AlertNotificationManager",
                    "AlertTemplateManager",
                    "AuthorizationManager",
                    "AvailabilityManager",
                    "CallTimeDataManager",
                    "ConfigurationManager",
                    "ContentManager",
                    "ContentUIManager",
                    "DiscoveryBoss",
                    "EmailManagerBean",
                    "EventManager",
                    "GroupDefinitionManager",
                    "MeasurementDefinitionManager",
                    "MeasurementScheduleManager",
                    "MeasurementDataManager",
                    "MeasurementCompressionManager",
                    "MeasurementProblemManager",
                    "MeasurementBaselineManager",
                    "OperationManager",
                    "ConfigurationMetadataManager",
                    "ContentSourceMetadataManager",
                    "ContentSourceManager",
                    "ChannelManagerLocal",
                    "ResourceMetadataManager",
                    "ResourceBoss",
                    "ResourceGroupManager",
                    "ResourceManager",
                    "DataAccess",
                    "ResourceFactoryManager",
                    "ResourceTypeManager",
                    "RoleManager",
                    "SchedulerBean",
                    "SubjectManager",
                    "SubjectRoleTestBean",
                    "SystemManager",
                    "PerspectiveManager",
                    "ProductVersionManager",

            };
}