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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.jetbrains.annotations.NotNull;

import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;

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
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerBean;
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerLocal;
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
import org.rhq.enterprise.server.bundle.BundleManagerBean;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.cloud.AffinityGroupManagerBean;
import org.rhq.enterprise.server.cloud.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.cloud.CloudManagerBean;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.cloud.FailoverListManagerBean;
import org.rhq.enterprise.server.cloud.FailoverListManagerLocal;
import org.rhq.enterprise.server.cloud.PartitionEventManagerBean;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.cloud.StatusManagerBean;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerBean;
import org.rhq.enterprise.server.cloud.instance.CacheConsistencyManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerBean;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacade;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerBean;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.DynamicConfigurationPropertyBean;
import org.rhq.enterprise.server.configuration.DynamicConfigurationPropertyLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerBean;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.content.AdvisoryManagerBean;
import org.rhq.enterprise.server.content.AdvisoryManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerBean;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerBean;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.ContentUIManagerBean;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.content.DistributionManagerBean;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerBean;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerBean;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerBean;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.core.EmailManagerBean;
import org.rhq.enterprise.server.core.EmailManagerLocal;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.dashboard.DashboardManagerBean;
import org.rhq.enterprise.server.dashboard.DashboardManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossBean;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.entitlement.EntitlementManagerBean;
import org.rhq.enterprise.server.entitlement.EntitlementManagerLocal;
import org.rhq.enterprise.server.event.EventManagerBean;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerBean;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerBean;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerBean;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementViewManagerBean;
import org.rhq.enterprise.server.measurement.MeasurementViewManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerBean;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.perspective.PerspectiveManagerBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsBean;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.report.DataAccessManagerBean;
import org.rhq.enterprise.server.report.DataAccessManagerLocal;
import org.rhq.enterprise.server.resource.ProductVersionManagerBean;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerBean;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceBossBean;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerBean;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerBean;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerBean;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerBean;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerBean;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerBean;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionExpressionBuilderManagerBean;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionExpressionBuilderManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerBean;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerBean;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerBean;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerBean;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.search.SavedSearchManagerBean;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.subsystem.AlertSubsystemManagerBean;
import org.rhq.enterprise.server.subsystem.AlertSubsystemManagerLocal;
import org.rhq.enterprise.server.subsystem.ConfigurationSubsystemManagerBean;
import org.rhq.enterprise.server.subsystem.ConfigurationSubsystemManagerLocal;
import org.rhq.enterprise.server.subsystem.OperationHistorySubsystemManagerBean;
import org.rhq.enterprise.server.subsystem.OperationHistorySubsystemManagerLocal;
import org.rhq.enterprise.server.support.SupportManagerBean;
import org.rhq.enterprise.server.support.SupportManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerBean;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.tagging.TagManagerBean;
import org.rhq.enterprise.server.tagging.TagManagerLocal;
import org.rhq.enterprise.server.test.AccessBean;
import org.rhq.enterprise.server.test.AccessLocal;
import org.rhq.enterprise.server.test.AlertTemplateTestBean;
import org.rhq.enterprise.server.test.AlertTemplateTestLocal;
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
 * Methods that allow POJO objects to obtain references to EJB/JPA objects. These convenience methods attempt to
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
     * Returns the main data source that can be used to directly access the database.
     *
     * @return a transactional data source to connect to the database
     */
    public static DataSource getDataSource() {
        try {
            InitialContext context = new InitialContext();
            DataSource ds = (DataSource) context.lookup(RHQConstants.DATASOURCE_JNDI_NAME);
            return ds;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the data source", e);
        }
    }

    /**
     * Returns the transaction manager that you can use for your own managed transactions.
     * Use this sparingly and only inside code that is outside of any CMT-scoped objects.
     *
     * @return the transaction manager
     */
    public static TransactionManager getTransactionManager() {
        try {
            InitialContext context = new InitialContext();
            TransactionManager tm = (TransactionManager) context.lookup(RHQConstants.TRANSACTION_MANAGER_JNDI_NAME);
            return tm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the transaction manager", e);
        }
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

    public static GroupAlertDefinitionManagerLocal getGroupAlertDefinitionManager() {
        return lookupLocal(GroupAlertDefinitionManagerBean.class);
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

    public static AlertSubsystemManagerLocal getAlertSubsystemManager() {
        return lookupLocal(AlertSubsystemManagerBean.class);
    }

    public static AlertTemplateTestLocal getAlertTemplateTestBean() {
        return lookupLocal(AlertTemplateTestBean.class);
    }

    public static AuthorizationManagerLocal getAuthorizationManager() {
        return lookupLocal(AuthorizationManagerBean.class);
    }

    public static AvailabilityManagerLocal getAvailabilityManager() {
        return lookupLocal(AvailabilityManagerBean.class);
    }

    public static BundleManagerLocal getBundleManager() {
        return lookupLocal(BundleManagerBean.class);
    }

    public static CallTimeDataManagerLocal getCallTimeDataManager() {
        return lookupLocal(CallTimeDataManagerBean.class);
    }

    public static ConfigurationManagerLocal getConfigurationManager() {
        return lookupLocal(ConfigurationManagerBean.class);
    }

    public static DynamicConfigurationPropertyLocal getDynamicConfigurationProperty() {
        return lookupLocal(DynamicConfigurationPropertyBean.class);
    }

    public static ConfigurationSubsystemManagerLocal getConfigurationSubsystemManager() {
        return lookupLocal(ConfigurationSubsystemManagerBean.class);
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

    public static EntitlementManagerLocal getEntitlementManagerBean() {
        return lookupLocal(EntitlementManagerBean.class);
    }

    public static EntityManagerFacadeLocal getEntityManagerFacade() {
        return lookupLocal(EntityManagerFacade.class);
    }

    public static EventManagerLocal getEventManager() {
        return lookupLocal(EventManagerBean.class);
    }

    public static FailoverListManagerLocal getFailoverListManager() {
        return lookupLocal(FailoverListManagerBean.class);
    }

    public static GroupDefinitionManagerLocal getGroupDefinitionManager() {
        return lookupLocal(GroupDefinitionManagerBean.class);
    }

    public static GroupDefinitionExpressionBuilderManagerLocal getGroupDefinitionExpressionBuilderManager() {
        return lookupLocal(GroupDefinitionExpressionBuilderManagerBean.class);
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

    public static MeasurementChartsManagerLocal getMeasurementChartsManager() {
        return lookupLocal(MeasurementChartsManagerBean.class);
    }

    public static MeasurementCompressionManagerLocal getMeasurementCompressionManager() {
        return lookupLocal(MeasurementCompressionManagerBean.class);
    }

    public static MeasurementProblemManagerLocal getMeasurementProblemManager() {
        return lookupLocal(MeasurementProblemManagerBean.class);
    }

    public static MeasurementViewManagerLocal getMeasurementViewManager() {
        return lookupLocal(MeasurementViewManagerBean.class);
    }

    public static MeasurementBaselineManagerLocal getMeasurementBaselineManager() {
        return lookupLocal(MeasurementBaselineManagerBean.class);
    }

    public static MeasurementOOBManagerLocal getOOBManager() {
        return lookupLocal(MeasurementOOBManagerBean.class);
    }

    public static OperationManagerLocal getOperationManager() {
        return lookupLocal(OperationManagerBean.class);
    }

    public static OperationHistorySubsystemManagerLocal getOperationHistorySubsystemManager() {
        return lookupLocal(OperationHistorySubsystemManagerBean.class);
    }

    public static PartitionEventManagerLocal getPartitionEventManager() {
        return lookupLocal(PartitionEventManagerBean.class);
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

    public static RepoManagerLocal getRepoManagerLocal() {
        return lookupLocal(RepoManagerBean.class);
    }

    public static DistributionManagerLocal getDistributionManagerLocal() {
        return lookupLocal(DistributionManagerBean.class);
    }

    public static AdvisoryManagerLocal getAdvisoryManagerLocal() {
        return lookupLocal(AdvisoryManagerBean.class);
    }

    public static AffinityGroupManagerLocal getAffinityGroupManager() {
        return lookupLocal(AffinityGroupManagerBean.class);
    }

    public static CloudManagerLocal getCloudManager() {
        return lookupLocal(CloudManagerBean.class);
    }

    public static ClusterManagerLocal getClusterManager() {
        return lookupLocal(ClusterManagerBean.class);
    }

    public static ServerManagerLocal getServerManager() {
        return lookupLocal(ServerManagerBean.class);
    }

    public static CacheConsistencyManagerLocal getCacheConsistenyManager() {
        return lookupLocal(CacheConsistencyManagerBean.class);
    }

    public static RemoteInstallManagerLocal getRemoteInstallManager() {
        return lookupLocal(RemoteInstallManagerBean.class);
    }

    public static PluginManagerLocal getPluginManager() {
        return lookupLocal(PluginManagerBean.class);
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

    public static ResourceAvailabilityManagerLocal getResourceAvailabilityManager() {
        return lookupLocal(ResourceAvailabilityManagerBean.class);
    }

    public static ResourceFactoryManagerLocal getResourceFactoryManager() {
        return lookupLocal(ResourceFactoryManagerBean.class);
    }

    public static ResourceTypeManagerLocal getResourceTypeManager() {
        return lookupLocal(ResourceTypeManagerBean.class);
    }

    public static ResourceTypeManagerRemote getResourceTypeManagerRemote() {
        return lookupRemote(ResourceTypeManagerBean.class);
    }

    public static RoleManagerLocal getRoleManager() {
        return lookupLocal(RoleManagerBean.class);
    }

    public static SchedulerLocal getSchedulerBean() {
        return lookupLocal(SchedulerBean.class);
    }

    public static SavedSearchManagerLocal getSavedSearchManager() {
        return lookupLocal(SavedSearchManagerBean.class);
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

    public static SupportManagerLocal getSupportManager() {
        return lookupLocal(SupportManagerBean.class);
    }

    public static StatusManagerLocal getStatusManager() {
        return lookupLocal(StatusManagerBean.class);
    }

    public static ServerPluginsLocal getServerPlugins() {
        return lookupLocal(ServerPluginsBean.class);
    }

    public static TagManagerLocal getTagManager() {
        return lookupLocal(TagManagerBean.class);
    }

    public static DashboardManagerLocal getDashboardManagerLocal() {
        return lookupLocal(DashboardManagerBean.class);
    }

    public static CoreServerMBean getCoreServer() {
        MBeanServer jBossMBeanServer = MBeanServerLocator.locateJBoss();
        CoreServerMBean jonServer = (CoreServerMBean) MBeanProxyExt.create(CoreServerMBean.class,
            CoreServerMBean.OBJECT_NAME, jBossMBeanServer);
        return jonServer;
    }

    public static PluginDeploymentScannerMBean getPluginDeploymentScanner() {
        MBeanServer jBossMBeanServer = MBeanServerLocator.locateJBoss();
        PluginDeploymentScannerMBean scanner = (PluginDeploymentScannerMBean) MBeanProxyExt.create(
            PluginDeploymentScannerMBean.class, PluginDeploymentScannerMBean.OBJECT_NAME, jBossMBeanServer);
        return scanner;
    }

    public static ServerPluginServiceManagement getServerPluginService() {
        MBeanServer jBossMBeanServer = MBeanServerLocator.locateJBoss();
        ServerPluginServiceManagement service = (ServerPluginServiceManagement) MBeanProxyExt.create(
            ServerPluginServiceManagement.class, ServerPluginServiceManagement.OBJECT_NAME, jBossMBeanServer);
        return service;
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

    // Private Methods

    private static <T> String getLocalJNDIName(@NotNull Class<? super T> beanClass) {
        return (embeddedDeployment ? "" : (RHQConstants.EAR_NAME + "/")) + beanClass.getSimpleName() + "/local";
    }

    /**
     * Returns the JNDI name of the remote interface for the given EJB class.
     *
     * @param  beanClass the EJB class whose remote interface JNDI name is to be returned
     *
     * @return JNDI name that the remote interface is registered as
     */
    private static <T> String getRemoteJNDIName(@NotNull Class<? super T> beanClass) {
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

    @SuppressWarnings("unchecked")
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

    public static DataAccessManagerLocal getDataAccessManager() {
        return lookupLocal(DataAccessManagerBean.class);
    }

    public static LdapGroupManagerLocal getLdapGroupManager() {
        return lookupLocal(LdapGroupManagerBean.class);
    }

}
