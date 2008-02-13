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
package org.rhq.enterprise.server.util;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jboss.cache.aop.PojoCacheMBean;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertConditionManagerBean;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerBean;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerBean;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerBean;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerBean;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.jms.AlertConditionConsumerBean;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerBean;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerBean;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerBean;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerBean;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerBean;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.content.ChannelManagerBean;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerBean;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerBean;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerBean;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerBean;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerBean;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.core.EmailManagerBean;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossBean;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerBean;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerBean;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.perspective.PerspectiveManagerBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.resource.ProductVersionManagerBean;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceBossBean;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerBean;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerBean;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerBean;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerBean;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerBean;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerBean;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerBean;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.system.SystemManagerBean;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.test.AccessBean;
import org.rhq.enterprise.server.test.AccessLocal;
import org.rhq.enterprise.server.test.CoreTestBean;
import org.rhq.enterprise.server.test.CoreTestLocal;
import org.rhq.enterprise.server.test.DiscoveryTestBean;
import org.rhq.enterprise.server.test.DiscoveryTestLocal;
import org.rhq.enterprise.server.test.MeasurementTestBean;
import org.rhq.enterprise.server.test.MeasurementTestLocal;
import org.rhq.enterprise.server.test.ResourceGroupTestBean;
import org.rhq.enterprise.server.test.ResourceGroupTestBeanLocal;
import org.rhq.enterprise.server.test.SubjectRoleTestBean;
import org.rhq.enterprise.server.test.SubjectRoleTestBeanLocal;

/**
 * Methods that allow POJO objects to obtain references to EJB/JPA objects. These convienence methods attempt to
 * minimize the amount of exception handling required by throwing runtime exceptions for the cases where there are no
 * easy recovery options.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public final class LookupUtil {
    private static boolean embeddedDeployment;

    static {
        embeddedDeployment = Boolean.valueOf(System.getProperty("embeddedDeployment", "false"));
    }

    /**
     * Prevent instantiation.
     */
    private LookupUtil() {
        // only static access ...
    }

    /**
     * Creates and returns an EntityManager that allows you to perform JPA operations.
     *
     * @return a unique entity manager object
     *
     * @throws RuntimeException if failed to get the entity manager for some reason
     */
    public static EntityManager getEntityManager() {
        try {
            InitialContext context = new InitialContext();
            EntityManagerFactory factory = (EntityManagerFactory) context.lookup(RHQConstants.ENTITY_MANAGER_JNDI_NAME);
            return factory.createEntityManager();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create an entity manager", e);
        }
    }

    public static CachedConditionProducerBean getActiveConditionProducer() {
        return lookupLocal(CachedConditionProducerBean.class);
    }

    public static AlertConditionConsumerBean getActiveConditionConsumer() {
        return lookupLocal(AlertConditionConsumerBean.class);
    }

    public static AgentManagerLocal getAgentManager() {
        return lookupLocal(AgentManagerBean.class);
    }

    public static AlertConditionCacheManagerLocal getAlertConditionCacheManager() {
        return lookupLocal(AlertConditionCacheManagerBean.class);
    }

    public static AlertConditionManagerLocal getAlertConditionManager() {
        return lookupLocal(AlertConditionManagerBean.class);
    }

    public static AlertDefinitionManagerLocal getAlertDefinitionManager() {
        return lookupLocal(AlertDefinitionManagerBean.class);
    }

    public static AlertManagerLocal getAlertManager() {
        return lookupLocal(AlertManagerBean.class);
    }

    public static AlertNotificationManagerLocal getAlertNotificationManager() {
        return lookupLocal(AlertNotificationManagerBean.class);
    }

    public static AlertTemplateManagerLocal getAlertTemplateManager() {
        return lookupLocal(AlertTemplateManagerBean.class);
    }

    public static AuthorizationManagerLocal getAuthorizationManager() {
        return lookupLocal(AuthorizationManagerBean.class);
    }

    public static AvailabilityManagerLocal getAvailabilityManager() {
        return lookupLocal(AvailabilityManagerBean.class);
    }

    public static CallTimeDataManagerLocal getCallTimeDataManager() {
        return lookupLocal(CallTimeDataManagerBean.class);
    }

    public static ConfigurationManagerLocal getConfigurationManager() {
        return lookupLocal(ConfigurationManagerBean.class);
    }

    public static ContentManagerLocal getContentManager() {
        return lookupLocal(ContentManagerBean.class);
    }

    public static ContentUIManagerLocal getContentUIManager() {
        return lookupLocal(ContentUIManagerBean.class);
    }

    public static DiscoveryBossLocal getDiscoveryBoss() {
        return lookupLocal(DiscoveryBossBean.class);
    }

    public static EmailManagerLocal getEmailManagerBean() {
        return lookupLocal(EmailManagerBean.class);
    }

    public static GroupDefinitionManagerLocal getGroupDefinitionManager() {
        return lookupLocal(GroupDefinitionManagerBean.class);
    }

    public static MeasurementDefinitionManagerLocal getMeasurementDefinitionManager() {
        return lookupLocal(MeasurementDefinitionManagerBean.class);
    }

    public static MeasurementScheduleManagerLocal getMeasurementScheduleManager() {
        return lookupLocal(MeasurementScheduleManagerBean.class);
    }

    public static MeasurementDataManagerLocal getMeasurementDataManager() {
        return lookupLocal(MeasurementDataManagerBean.class);
    }

    public static MeasurementCompressionManagerLocal getMeasurementCompressionManager() {
        return lookupLocal(MeasurementCompressionManagerBean.class);
    }

    public static MeasurementProblemManagerLocal getMeasurementProblemManager() {
        return lookupLocal(MeasurementProblemManagerBean.class);
    }

    public static MeasurementBaselineManagerLocal getMeasurementBaselineManager() {
        return lookupLocal(MeasurementBaselineManagerBean.class);
    }

    public static OperationManagerLocal getOperationManager() {
        return lookupLocal(OperationManagerBean.class);
    }

    public static ConfigurationMetadataManagerLocal getConfigurationMetadataManager() {
        return lookupLocal(ConfigurationMetadataManagerBean.class);
    }

    public static ContentSourceMetadataManagerLocal getContentSourceMetadataManager() {
        return lookupLocal(ContentSourceMetadataManagerBean.class);
    }

    public static ContentSourceManagerLocal getContentSourceManager() {
        return lookupLocal(ContentSourceManagerBean.class);
    }

    public static ChannelManagerLocal getChannelManagerLocal() {
        return lookupLocal(ChannelManagerBean.class);
    }

    public static ResourceMetadataManagerLocal getResourceMetadataManager() {
        return lookupLocal(ResourceMetadataManagerBean.class);
    }

    public static ResourceBossLocal getResourceBoss() {
        return lookupLocal(ResourceBossBean.class);
    }

    public static ResourceGroupManagerLocal getResourceGroupManager() {
        return lookupLocal(ResourceGroupManagerBean.class);
    }

    public static ResourceManagerLocal getResourceManager() {
        return lookupLocal(ResourceManagerBean.class);
    }

    public static ResourceFactoryManagerLocal getResourceFactoryManager() {
        return lookupLocal(ResourceFactoryManagerBean.class);
    }

    public static ResourceTypeManagerLocal getResourceTypeManager() {
        return lookupLocal(ResourceTypeManagerBean.class);
    }

    public static RoleManagerLocal getRoleManager() {
        return lookupLocal(RoleManagerBean.class);
    }

    public static SchedulerLocal getSchedulerBean() {
        return lookupLocal(SchedulerBean.class);
    }

    public static SubjectManagerLocal getSubjectManager() {
        return lookupLocal(SubjectManagerBean.class);
    }

    public static SubjectRoleTestBeanLocal getSubjectRoleTestBean() {
        return lookupLocal(SubjectRoleTestBean.class);
    }

    public static SystemManagerLocal getSystemManager() {
        return lookupLocal(SystemManagerBean.class);
    }

    public static PerspectiveManagerLocal getPerspectiveManager() {
        return lookupLocal(PerspectiveManagerBean.class);
    }

    public static ProductVersionManagerLocal getProductVersionManager() {
        return lookupLocal(ProductVersionManagerBean.class);
    }

    public static CachedConditionProducerLocal getCachedConditionProducerLocal() {
        return lookupLocal(CachedConditionProducerBean.class);
    }

    //--------------------------------------------
    // The TEST services
    //--------------------------------------------

    public static AccessLocal getAccessLocal() {
        return lookupLocal(AccessBean.class);
    }

    public static CoreTestLocal getCoreTest() {
        return lookupLocal(CoreTestBean.class);
    }

    public static DiscoveryTestLocal getDiscoveryTest() {
        return lookupLocal(DiscoveryTestBean.class);
    }

    public static MeasurementTestLocal getMeasurementTest() {
        return lookupLocal(MeasurementTestBean.class);
    }

    public static ResourceGroupTestBeanLocal getResourceGroupTestBean() {
        return lookupLocal(ResourceGroupTestBean.class);
    }

    public static CoreServerMBean getCoreServer() {
        MBeanServer jBossMBeanServer = MBeanServerLocator.locate();
        CoreServerMBean jonServer = (CoreServerMBean) MBeanProxyExt.create(CoreServerMBean.class,
            CoreServerMBean.OBJECT_NAME, jBossMBeanServer);
        return jonServer;
    }

    private static <T> String getLocalJNDIName(@NotNull
    Class<? super T> beanClass) {
        return (embeddedDeployment ? "" : (RHQConstants.EAR_NAME + "/")) + beanClass.getSimpleName() + "/local";
    }

    /**
     * Returns the JNDI name of the remote interface for the given EJB class.
     *
     * @param  beanClass the EJB class whose remote interface JNDI name is to be returned
     *
     * @return JNDI name that the remote interface is registered as
     */
    private static <T> String getRemoteJNDIName(@NotNull
    Class<? super T> beanClass) {
        return (embeddedDeployment ? "" : (RHQConstants.EAR_NAME + "/")) + beanClass.getSimpleName() + "/remote";
    }

    @SuppressWarnings("unchecked")
    private static <T> T lookupLocal(Class<? super T> type) {
        try {
            return (T) lookup(getLocalJNDIName(type));
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup local interface to EJB " + type, e);
        }
    }

    @SuppressWarnings( { "unchecked", "unused" })
    private static <T> T lookupRemote(Class<? super T> type) {
        try {
            return (T) lookup(getRemoteJNDIName(type));
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup remote interface to EJB " + type, e);
        }
    }

    /**
     * Looks up the given JNDI name in an initial context and returns the object that was found.
     *
     * @param  name JNDI name to lookup
     *
     * @return the object that was found
     *
     * @throws NamingException when resource not found
     */
    private static Object lookup(String name) throws NamingException {
        return new InitialContext().lookup(name);
    }

    public static PojoCacheMBean getAlertCache() {
        MBeanServer jBossMBeanServer = MBeanServerLocator.locate();
        PojoCacheMBean pcMBean;
        ObjectName cacheName = ObjectNameFactory.create("rhq.cache:subsystem=alerts,service=cache");
        pcMBean = (PojoCacheMBean) MBeanProxyExt.create(PojoCacheMBean.class, cacheName, jBossMBeanServer);
        return pcMBean;
    }
}