/*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.webservices;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.transfer.EntitlementCertificate;
import org.rhq.core.domain.content.transfer.SubscribedRepo;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationUpdateStillInProgressException;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.DistributionException;
import org.rhq.enterprise.server.content.DistributionManagerLocal;
import org.rhq.enterprise.server.content.EntitlementStuffManagerLocal;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;
import org.rhq.enterprise.server.jaxb.adapter.ResourceGroupAdapter;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.GroupOperationSchedule;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.support.SupportManagerLocal;
import org.rhq.enterprise.server.system.ServerVersion;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/** The purpose of this class is to aggregate all the EJB remote implementation into one
 *  class that can be annotated by JBossWS.  Each annotated SLSB causes a full WSDL compile and
 *  publish by JBossWS which is very costly in terms of time.  Deploy times went from 2 mins to 12 mins.
 *
 * @author Simeon Pinder
 *
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.webservices.WebservicesRemote", targetNamespace = ServerVersion.namespace)
@XmlSeeAlso( { PropertyDefinition.class, PropertyDefinitionSimple.class, PropertyDefinitionList.class,
    PropertyDefinitionMap.class })
public class WebservicesManagerBean implements WebservicesRemote {

    //Lookup the required beans as local references
    private AlertManagerLocal alertManager = LookupUtil.getAlertManager();
    private AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
    private AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
    private BundleManagerLocal bundleManager = LookupUtil.getBundleManager();
    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private ContentManagerLocal contentManager = LookupUtil.getContentManager();
    //removed as it is problematic for WS clients having XMLAny for Object.
    //    private DataAccessManagerLocal dataAccessManager = LookupUtil.getDataAccessManager();
    private DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
    private DistributionManagerLocal distributionManager = LookupUtil.getDistributionManagerLocal();
    private EventManagerLocal eventManager = LookupUtil.getEventManager();
    private MeasurementBaselineManagerLocal measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();
    private MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();
    private MeasurementDefinitionManagerLocal measurementDefinitionManager = LookupUtil
        .getMeasurementDefinitionManager();
    private MeasurementProblemManagerLocal measurementProblemManager = LookupUtil.getMeasurementProblemManager();
    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();
    private RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
    private ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private RoleManagerLocal roleManager = LookupUtil.getRoleManager();
    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
    private SupportManagerLocal supportManager = LookupUtil.getSupportManager();
    private SystemManagerLocal systemManager = LookupUtil.getSystemManager();
    private EntitlementStuffManagerLocal entitlementManager = LookupUtil.getEntitlementManager();

    //ALERTMANAGER: BEGIN ------------------------------------------
    public PageList<Alert> findAlertsByCriteria(Subject subject, AlertCriteria criteria) {
        return alertManager.findAlertsByCriteria(subject, criteria);
    }

    //ALERTMANAGER: END --------------------------------------------

    //ALERTDEFINITIONMANAGER: BEGIN --------------------------------

    public AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId) {
        return alertDefinitionManager.getAlertDefinition(subject, alertDefinitionId);
    }

    public PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return alertDefinitionManager.findAlertDefinitionsByCriteria(subject, criteria);
    }

    //ALERTDEFINITIONMANAGER: END ----------------------------------

    //AVAILABILITYMANAGER: BEGIN ----------------------------------
    public PageList<Availability> findAvailabilityForResource(Subject subject, int resourceId, PageControl pc) {
        return availabilityManager.findAvailabilityForResource(subject, resourceId, pc);
    }

    public Availability getCurrentAvailabilityForResource(Subject subject, int resourceId) {
        return availabilityManager.getCurrentAvailabilityForResource(subject, resourceId);
    }

    //AVAILABILITYMANAGER: END ----------------------------------

    //BUNDLEMANAGER: BEGIN ------------------------------------------

    public BundleFile addBundleFile(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, InputStream fileStream) throws Exception {
        return bundleManager.addBundleFile(subject, bundleVersionId, name, version, architecture, fileStream);
    }

    public BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes) throws Exception {
        return bundleManager
            .addBundleFileViaByteArray(subject, bundleVersionId, name, version, architecture, fileBytes);
    }

    public BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl) throws Exception {
        return bundleManager.addBundleFileViaURL(subject, bundleVersionId, name, version, architecture, bundleFileUrl);
    }

    public BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name,
        int packageVersionId) throws Exception {
        return bundleManager.addBundleFileViaPackageVersion(subject, bundleVersionId, name, packageVersionId);
    }

    public BundleDeployment createBundleDeployment(Subject subject, int bundleVersionId, int bundleDestinationId,
        String description, Configuration configuration) throws Exception {
        return bundleManager.createBundleDeployment(subject, bundleVersionId, bundleDestinationId, description,
            configuration);
    }

    public BundleDestination createBundleDestination(Subject subject, int bundleId, String name, String description,
        String deployDir, Integer groupId) throws Exception {
        return bundleManager.createBundleDestination(subject, bundleId, name, description, deployDir, groupId);
    }

    public BundleVersion createBundleVersionViaRecipe(Subject subject, String recipe) throws Exception {
        return bundleManager.createBundleVersionViaRecipe(subject, recipe);
    }

    public BundleVersion createBundleVersionViaFile(Subject subject, File distributionFile) throws Exception {
        return bundleManager.createBundleVersionViaFile(subject, distributionFile);
    }

    public BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl) throws Exception {
        return bundleManager.createBundleVersionViaURL(subject, distributionFileUrl);
    }

    public void deleteBundle(Subject subject, int bundleId) throws Exception {
        bundleManager.deleteBundle(subject, bundleId);
    }

    public void deleteBundleVersion(Subject subject, int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception {
        bundleManager.deleteBundleVersion(subject, bundleVersionId, deleteBundleIfEmpty);
    }

    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria) {
        return bundleManager.findBundleDeploymentsByCriteria(subject, criteria);
    }

    public PageList<BundleDestination> findBundleDestinationsByCriteria(Subject subject,
        BundleDestinationCriteria criteria) {
        return bundleManager.findBundleDestinationsByCriteria(subject, criteria);
    }

    public PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(Subject subject,
        BundleResourceDeploymentCriteria criteria) {
        return bundleManager.findBundleResourceDeploymentsByCriteria(subject, criteria);
    }

    public PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria) {
        return bundleManager.findBundlesByCriteria(subject, criteria);
    }

    public PageList<BundleFile> findBundleFilesByCriteria(Subject subject, BundleFileCriteria criteria) {
        return bundleManager.findBundleFilesByCriteria(subject, criteria);
    }

    public PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria) {
        return bundleManager.findBundleVersionsByCriteria(subject, criteria);
    }

    public PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(Subject subject,
        BundleCriteria criteria) {
        return bundleManager.findBundlesWithLatestVersionCompositesByCriteria(subject, criteria);
    }

    public List<BundleType> getAllBundleTypes(Subject subject) {
        return bundleManager.getAllBundleTypes(subject);
    }

    public BundleType getBundleType(Subject subject, String bundleTypeName) {
        return bundleManager.getBundleType(subject, bundleTypeName);
    }

    public Set<String> getBundleVersionFilenames(Subject subject, int bundleVersionId, boolean withoutBundleFileOnly)
        throws Exception {
        return bundleManager.getBundleVersionFilenames(subject, bundleVersionId, withoutBundleFileOnly);
    }

    public BundleDeployment scheduleBundleDeployment(Subject subject, int bundleDeploymentId, boolean isCleanDeployment)
        throws Exception {
        return bundleManager.scheduleBundleDeployment(subject, bundleDeploymentId, isCleanDeployment);
    }

    public BundleDeployment scheduleRevertBundleDeployment(Subject subject, int bundleDestinationId,
        String deploymentDescription, boolean isCleanDeployment) throws Exception {
        return bundleManager.scheduleRevertBundleDeployment(subject, bundleDestinationId, deploymentDescription,
            isCleanDeployment);
    }

    //BUNDLEMANAGER: END ----------------------------------  

    //CALLTIMEDATAMANAGER: BEGIN ----------------------------------
    public PageList<CallTimeDataComposite> findCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pc) {
        return callTimeDataManager.findCallTimeDataForResource(subject, scheduleId, beginTime, endTime, pc);
    }

    //CALLTIMEDATAMANAGER: END ----------------------------------

    //REPOMANAGER: BEGIN ----------------------------------
    public void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds) {
        repoManager.addPackageVersionsToRepo(subject, repoId, packageVersionIds);
    }

    public Repo createRepo(Subject subject, Repo repo) throws RepoException {
        return repoManager.createRepo(subject, repo);
    }

    public void deleteRepo(Subject subject, int repoId) {
        repoManager.deleteRepo(subject, repoId);
    }

    public PageList<Repo> findRepos(Subject subject, PageControl pc) {
        return repoManager.findRepos(subject, pc);
    }

    public PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return repoManager.findReposByCriteria(subject, criteria);
    }

    public PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc) {
        return repoManager.findPackageVersionsInRepo(subject, repoId, filter, pc);
    }

    public PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria) {
        return repoManager.findPackageVersionsInRepoByCriteria(subject, criteria);
    }

    public PageList<Resource> findSubscribedResources(Subject subject, int repoId, PageControl pc) {
        return repoManager.findSubscribedResources(subject, repoId, pc);
    }

    public Repo getRepo(Subject subject, int repoId) {
        return repoManager.getRepo(subject, repoId);
    }

    public RepoGroup createRepoGroup(Subject subject, RepoGroup repoGroup) throws RepoException {
        return repoManager.createRepoGroup(subject, repoGroup);
    }

    public void deleteRepoGroup(Subject subject, int repoGroupId) {
        repoManager.deleteRepoGroup(subject, repoGroupId);
    }

    public RepoGroup getRepoGroup(Subject subject, int repoGroupId) {
        return repoManager.getRepoGroup(subject, repoGroupId);
    }

    public RepoGroupType getRepoGroupTypeByName(Subject subject, String name) {
        return repoManager.getRepoGroupTypeByName(subject, name);
    }

    public void subscribeResourceToRepos(Subject subject, int resourceId, int[] repoIds) {
        repoManager.subscribeResourceToRepos(subject, resourceId, repoIds);
    }

    public void unsubscribeResourceFromRepos(Subject subject, int resourceId, int[] repoIds) {
        repoManager.unsubscribeResourceFromRepos(subject, resourceId, repoIds);
    }

    public Repo updateRepo(Subject subject, Repo repo) throws RepoException {
        return repoManager.updateRepo(subject, repo);
    }

    public PageList<PackageVersion> findPackageVersionsByCriteria(Subject subject, PackageVersionCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return repoManager.findPackageVersionsInRepoByCriteria(subject, criteria);
    }

    public int synchronizeRepos(Subject subject, Integer[] repoIds) throws Exception {
        return repoManager.synchronizeRepos(subject, repoIds);
    }

    //REPOMANAGER: END ----------------------------------

    //CONFIGURATIONMANAGER: BEGIN ----------------------------------
    public Configuration getConfiguration(Subject subject, int configurationId) {
        return configurationManager.getConfiguration(subject, configurationId);
    }

    public GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        return configurationManager.getGroupPluginConfigurationUpdate(subject, configurationUpdateId);
    }

    public GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate(Subject subject,
        int configurationUpdateId) {
        return configurationManager.getGroupResourceConfigurationUpdate(subject, configurationUpdateId);
    }

    public PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId) {
        return configurationManager.getLatestPluginConfigurationUpdate(subject, resourceId);
    }

    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId) {
        return configurationManager.getLatestResourceConfigurationUpdate(subject, resourceId);
    }

    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception {
        return configurationManager.getLiveResourceConfiguration(subject, resourceId, pingAgentFirst);
    }

    public @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getPluginConfiguration(Subject subject, int resourceId) {
        return configurationManager.getPluginConfiguration(subject, resourceId);
    }

    public ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        return configurationManager.getPluginConfigurationDefinitionForResourceType(subject, resourceTypeId);
    }

    public @XmlJavaTypeAdapter(ConfigurationAdapter.class)
    Configuration getResourceConfiguration(Subject subject, int resourceId) {
        return configurationManager.getResourceConfiguration(subject, resourceId);
    }

    public ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
        return configurationManager.getResourceConfigurationDefinitionForResourceType(subject, resourceTypeId);
    }

    public ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject subject,
        int resourceTypeId) {
        return configurationManager.getResourceConfigurationDefinitionWithTemplatesForResourceType(subject,
            resourceTypeId);
    }

    public boolean isGroupResourceConfigurationUpdateInProgress(Subject subject, int resourceGroupId) {
        return configurationManager.isGroupResourceConfigurationUpdateInProgress(subject, resourceGroupId);
    }

    public boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId) {
        return configurationManager.isResourceConfigurationUpdateInProgress(subject, resourceId);
    }

    public int scheduleGroupResourceConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> newResourceConfigurationMap) {
        return configurationManager.scheduleGroupResourceConfigurationUpdate(subject, compatibleGroupId,
            newResourceConfigurationMap);
    }

    public PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId,
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration)
        throws ResourceNotFoundException {
        return configurationManager.updatePluginConfiguration(subject, resourceId, newConfiguration);
    }

    public ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,
        @XmlJavaTypeAdapter(ConfigurationAdapter.class) Configuration newConfiguration)
        throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException {
        return configurationManager.updateResourceConfiguration(subject, resourceId, newConfiguration);
    }

    public ResourceConfigurationUpdate updateStructuredOrRawConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration, boolean fromStructured) throws ResourceNotFoundException,
        ConfigurationUpdateStillInProgressException {
        return configurationManager.updateStructuredOrRawConfiguration(subject, resourceId, newConfiguration,
            fromStructured);
    }

    public ConfigurationDefinition getPackageTypeConfigurationDefinition(Subject subject, int packageTypeId) {
        return configurationManager.getPackageTypeConfigurationDefinition(subject, packageTypeId);
    }

    public Configuration translateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean fromStructured) throws ResourceNotFoundException {
        return configurationManager.translateResourceConfiguration(subject, resourceId, configuration, fromStructured);
    }

    //CONFIGURATIONMANAGER: END ----------------------------------

    //CONTENTMANAGER: BEGIN ----------------------------------
    public PackageVersion createPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        Integer architectureId, byte[] packageBytes) {
        return contentManager.createPackageVersion(subject, packageName, packageTypeId, version, architectureId,
            packageBytes);
    }

    public void deletePackages(Subject subject, int resourceId, int[] installedPackageIds, String requestNotes) {
        contentManager.deletePackages(subject, resourceId, installedPackageIds, requestNotes);
    }

    public void deployPackages(Subject subject, int[] resourceIds, int[] packageVersionIds) {
        contentManager.deployPackages(subject, resourceIds, packageVersionIds);
    }

    public List<Architecture> findArchitectures(Subject subject) {
        return contentManager.findArchitectures(subject);
    }

    public PageList<InstalledPackage> findInstalledPackagesByCriteria(Subject subject, InstalledPackageCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return contentManager.findInstalledPackagesByCriteria(subject, criteria);
    }

    public List<PackageType> findPackageTypes(Subject subject, String resourceTypeName, String pluginName)
        throws ResourceTypeNotFoundException {
        return contentManager.findPackageTypes(subject, resourceTypeName, pluginName);
    }

    public InstalledPackage getBackingPackageForResource(Subject subject, int resourceId) {
        return contentManager.getBackingPackageForResource(subject, resourceId);
    }

    public byte[] getPackageBytes(Subject subject, int resourceId, int installedPackageId) {
        return contentManager.getPackageBytes(subject, resourceId, installedPackageId);
    }

    //CONTENTMANAGER: END ----------------------------------

    //    //DATAACCESSMANAGER: BEGIN ----------------------------------
    //    public List<Object[]> executeQuery(Subject subject, String query) {
    //        return dataAccessManager.executeQuery(subject, query);
    //    }
    //
    //    public List<Object[]> executeQueryWithPageControl(Subject subject, String query, PageControl pageControl) {
    //        return dataAccessManager.executeQueryWithPageControl(subject, query, pageControl);
    //    }

    //DATAACCESSMANAGER: END ----------------------------------

    //DISCOVERYBOSS: BEGIN ------------------------------------
    public void ignoreResources(Subject subject, Integer[] resourceIds) {
        discoveryBoss.ignoreResources(subject, resourceIds);
    }

    public void importResources(Subject subject, Integer[] resourceIds) {
        discoveryBoss.importResources(subject, resourceIds);
    }

    public void unignoreResources(Subject subject, Integer[] resourceIds) {
        discoveryBoss.unignoreResources(subject, resourceIds);
    }

    public Resource manuallyAddResource(Subject subject, int resourceTypeId, int parentResourceId,
        Configuration pluginConfiguration) throws Exception {
        return discoveryBoss.manuallyAddResource(subject, resourceTypeId, parentResourceId, pluginConfiguration);
    }

    //DISCOVERYBOSS: END ------------------------------------

    //DISTRIBUTION: START ------------------------------------

    public DistributionType getDistributionTypeByName(String name) {
        return distributionManager.getDistributionTypeByName(name);
    }

    public void deleteDistributionFilesByDistId(Subject subject, int distid) {
        distributionManager.deleteDistributionByDistId(subject, distid);
    }

    public List<DistributionFile> getDistributionFilesByDistId(int distid) {
        return distributionManager.getDistributionFilesByDistId(distid);
    }

    public Distribution getDistributionByPath(String basepath) {
        return distributionManager.getDistributionByPath(basepath);
    }

    public Distribution getDistributionByLabel(String kslabel) {
        return distributionManager.getDistributionByLabel(kslabel);
    }

    public void deleteDistributionByDistId(Subject subject, int distId) throws Exception {
        distributionManager.deleteDistributionByDistId(subject, distId);
    }

    public void deleteDistributionTypeByName(Subject subject, String name) {
        distributionManager.deleteDistributionTypeByName(subject, name);
    }

    public Distribution createDistribution(Subject subject, String kslabel, String basepath, DistributionType disttype)
        throws DistributionException {
        return distributionManager.createDistribution(subject, kslabel, basepath, disttype);
    }

    //DISTRIBUTION: END ------------------------------------

    //EVENTMANAGER: BEGIN ----------------------------------
    public PageList<Event> findEventsByCriteria(Subject subject, EventCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return eventManager.findEventsByCriteria(subject, criteria);
    }

    public EventSeverity[] getSeverityBuckets(Subject subject, int resourceId, long begin, long end, int numBuckets) {
        return eventManager.getSeverityBuckets(subject, resourceId, begin, end, numBuckets);
    }

    public EventSeverity[] getSeverityBucketsForAutoGroup(Subject subject, int parentResourceId, int resourceTypeId,
        long begin, long end, int numBuckets) {
        return eventManager.getSeverityBucketsForAutoGroup(subject, parentResourceId, resourceTypeId, begin, end,
            numBuckets);
    }

    public EventSeverity[] getSeverityBucketsForCompGroup(Subject subject, int resourceGroupId, long begin, long end,
        int numBuckets) {
        return eventManager.getSeverityBucketsForCompGroup(subject, resourceGroupId, begin, end, numBuckets);
    }

    //EVENTMANAGER: END ----------------------------------

    //MEASUREMENTBASELINEMANAGER: BEGIN ----------------------------------
    public List<MeasurementBaseline> findBaselinesForResource(Subject subject, int resourceId) {
        return measurementBaselineManager.findBaselinesForResource(subject, resourceId);
    }

    //MEASUREMENTBASELINEMANAGER: END ----------------------------------

    //MEASUREMENTDATAMANAGER: BEGIN ----------------------------------
    public List<MeasurementDataTrait> findCurrentTraitsForResource(Subject subject, int resourceId,
        DisplayType displayType) {
        return measurementDataManager.findCurrentTraitsForResource(subject, resourceId, displayType);
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForCompatibleGroup(Subject subject, int groupId,
        int definitionId, long beginTime, long endTime, int numPoints) {
        return measurementDataManager.findDataForCompatibleGroup(subject, groupId, definitionId, beginTime, endTime,
            numPoints);
    }

    public List<List<MeasurementDataNumericHighLowComposite>> findDataForResource(Subject subject, int resourceId,
        int[] definitionIds, long beginTime, long endTime, int numPoints) {
        return measurementDataManager.findDataForResource(subject, resourceId, definitionIds, beginTime, endTime,
            numPoints);
    }

    public Set<MeasurementData> findLiveData(Subject subject, int resourceId, int[] definitionIds) {
        return measurementDataManager.findLiveData(subject, resourceId, definitionIds);
    }

    public List<MeasurementDataTrait> findTraits(Subject subject, int resourceId, int definitionId) {
        return measurementDataManager.findTraits(subject, resourceId, definitionId);
    }

    public MeasurementAggregate getAggregate(Subject subject, int scheduleId, long startTime, long endTime) {
        return measurementDataManager.getAggregate(subject, scheduleId, startTime, endTime);
    }

    //MEASUREMENTDATAMANAGER: END ----------------------------------

    //MEASUREMENTDEFINITIONMANAGER: BEGIN ----------------------------------
    public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
        MeasurementDefinitionCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return measurementDefinitionManager.findMeasurementDefinitionsByCriteria(subject, criteria);
    }

    public MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId) {
        return measurementDefinitionManager.getMeasurementDefinition(subject, definitionId);
    }

    //MEASUREMENTDEFINITIONMANAGER: END ----------------------------------

    //MEASUREMENTPROBLEMMANAGER: BEGIN ----------------------------------
    public PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate, PageControl pc) {
        return measurementProblemManager.findProblemResources(subject, oldestDate, pc);
    }

    //MEASUREMENTPROBLEMMANAGER: END ----------------------------------

    //MEASUREMENTSCHEDULEMANAGER: BEGIN ----------------------------------
    public void disableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        measurementScheduleManager.disableSchedulesForResource(subject, resourceId, measurementDefinitionIds);
    }

    public void disableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        measurementScheduleManager.disableSchedulesForCompatibleGroup(subject, groupId, measurementDefinitionIds);
    }

    public void disableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds) {
        measurementScheduleManager.disableMeasurementTemplates(subject, measurementDefinitionIds);
    }

    public void enableSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds) {
        measurementScheduleManager.enableSchedulesForResource(subject, resourceId, measurementDefinitionIds);
    }

    public void enableSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds) {
        measurementScheduleManager.enableSchedulesForCompatibleGroup(subject, groupId, measurementDefinitionIds);
    }

    public void enableMeasurementTemplates(Subject subject, int[] measurementDefinitionIds) {
        measurementScheduleManager.enableMeasurementTemplates(subject, measurementDefinitionIds);
    }

    public void updateSchedule(Subject subject, MeasurementSchedule schedule) {
        measurementScheduleManager.updateSchedule(subject, schedule);
    }

    public void updateSchedulesForResource(Subject subject, int resourceId, int[] measurementDefinitionIds,
        long collectionInterval) {
        measurementScheduleManager.updateSchedulesForResource(subject, resourceId, measurementDefinitionIds,
            collectionInterval);
    }

    public void updateSchedulesForCompatibleGroup(Subject subject, int groupId, int[] measurementDefinitionIds,
        long collectionInterval) {
        measurementScheduleManager.updateSchedulesForCompatibleGroup(subject, groupId, measurementDefinitionIds,
            collectionInterval);
    }

    public void updateMeasurementTemplates(Subject subject, int[] measurementDefinitionIds, long collectionInterval) {
        measurementScheduleManager.updateMeasurementTemplates(subject, measurementDefinitionIds, collectionInterval);
    }

    public PageList<MeasurementSchedule> findSchedulesByCriteria(Subject subject, MeasurementScheduleCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return measurementScheduleManager.findSchedulesByCriteria(subject, criteria);
    }

    //MEASUREMENTSCHEDULEMANAGER: END ----------------------------------

    //OPERATIONMANAGER: BEGIN ----------------------------------
    public void cancelOperationHistory(Subject subject, int operationHistoryId, boolean ignoreAgentErrors) {
        operationManager.cancelOperationHistory(subject, operationHistoryId, ignoreAgentErrors);
    }

    public void deleteOperationHistory(Subject subject, int operationHistoryId, boolean purgeInProgress) {
        operationManager.deleteOperationHistory(subject, operationHistoryId, purgeInProgress);
    }

    public PageList<GroupOperationHistory> findGroupOperationHistoriesByCriteria(Subject subject,
        GroupOperationHistoryCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return operationManager.findGroupOperationHistoriesByCriteria(subject, criteria);
    }

    public List<OperationDefinition> findOperationDefinitionsByCriteria(Subject subject,
        OperationDefinitionCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return operationManager.findOperationDefinitionsByCriteria(subject, criteria);
    }

    public PageList<ResourceOperationHistory> findResourceOperationHistoriesByCriteria(Subject subject,
        ResourceOperationHistoryCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return operationManager.findResourceOperationHistoriesByCriteria(subject, criteria);
    }

    public List<GroupOperationSchedule> findScheduledGroupOperations(Subject subject, int groupId) throws Exception {
        return operationManager.findScheduledGroupOperations(subject, groupId);
    }

    public List<ResourceOperationSchedule> findScheduledResourceOperations(Subject subject, int resourceId)
        throws Exception {
        return operationManager.findScheduledResourceOperations(subject, resourceId);
    }

    public GroupOperationSchedule scheduleGroupOperation(Subject subject, int groupId, int[] executionOrderResourceIds,
        boolean haltOnFailure, String operationName, Configuration parameters, long delay, long repeatInterval,
        int repeatCount, int timeout, String description) throws ScheduleException {
        return operationManager.scheduleGroupOperation(subject, groupId, executionOrderResourceIds, haltOnFailure,
            operationName, parameters, delay, repeatInterval, repeatCount, timeout, description);
    }

    public ResourceOperationSchedule scheduleResourceOperation(Subject subject, int resourceId, String operationName,
        long delay, long repeatInterval, int repeatCount, int timeout,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration parameters, String description) throws ScheduleException {
        return operationManager.scheduleResourceOperation(subject, resourceId, operationName, delay, repeatInterval,
            repeatCount, timeout, parameters, description);
    }

    public void unscheduleGroupOperation(Subject subject, String jobId, int resourceGroupId) throws UnscheduleException {
        operationManager.unscheduleGroupOperation(subject, jobId, resourceGroupId);
    }

    public void unscheduleResourceOperation(Subject subject, String jobId, int resourceId) throws UnscheduleException {
        operationManager.unscheduleResourceOperation(subject, jobId, resourceId);
    }

    //OPERATIONMANAGER: END ----------------------------------

    //RESOURCEFACTORYMANAGER: BEGIN ----------------------------------
    public void createResource(Subject subject, int parentResourceId, int resourceTypeId, String resourceName,
        Configuration pluginConfiguration, Configuration resourceConfiguration) {
        resourceFactoryManager.createResource(subject, parentResourceId, resourceTypeId, resourceName,
            pluginConfiguration, resourceConfiguration);
    }

    public void createPackageBackedResource(Subject subject, int parentResourceId, int newResourceTypeId,
        String newResourceName,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration pluginConfiguration, String packageName, String packageVersion, Integer architectureId,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration deploymentTimeConfiguration, byte[] packageBits) {
        resourceFactoryManager.createPackageBackedResource(subject, parentResourceId, newResourceTypeId,
            newResourceName, pluginConfiguration, packageName, packageVersion, architectureId,
            deploymentTimeConfiguration, packageBits);
    }

    public void deleteResource(Subject subject, int resourceId) {
        resourceFactoryManager.deleteResource(subject, resourceId);
    }

    //RESOURCEFACTORYMANAGER: END ----------------------------------

    //RESOURCEMANAGER: BEGIN ----------------------------------
    public PageList<ResourceComposite> findResourceComposites(Subject subject, ResourceCategory category,
        String typeName, int parentResourceId, String searchString, PageControl pageControl) {
        return resourceManager.findResourceComposites(subject, category, typeName, parentResourceId, searchString,
            pageControl);
    }

    public List<Resource> findResourceLineage(Subject subject, int resourceId) {
        return resourceManager.findResourceLineage(subject, resourceId);
    }

    public PageList<Resource> findResourcesByCriteria(Subject subject, ResourceCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return resourceManager.findResourcesByCriteria(subject, criteria);
    }

    public PageList<Resource> findChildResources(Subject subject, int resourceId, PageControl pageControl) {
        return resourceManager.findChildResources(subject, resourceId, pageControl);
    }

    public Resource getParentResource(Subject subject, int resourceId) {
        return resourceManager.getParentResource(subject, resourceId);
    }

    public ResourceAvailability getLiveResourceAvailability(Subject subject, int resourceId) {
        return resourceManager.getLiveResourceAvailability(subject, resourceId);
    }

    public Resource getResource(Subject subject, int resourceId) {
        return resourceManager.getResource(subject, resourceId);
    }

    public List<Integer> uninventoryResources(Subject subject, int[] resourceIds) {
        return resourceManager.uninventoryResources(subject, resourceIds);
    }

    //RESOURCEMANAGER: END ----------------------------------

    //RESOURCEGROUPMANAGER: BEGIN ----------------------------------
    public void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds) {
        resourceGroupManager.addResourcesToGroup(subject, groupId, resourceIds);
    }

    public ResourceGroup createResourceGroup(Subject subject, //
        @XmlJavaTypeAdapter(ResourceGroupAdapter.class) ResourceGroup resourceGroup) {
        return resourceGroupManager.createResourceGroup(subject, resourceGroup);
    }

    public void deleteResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException {
        resourceGroupManager.deleteResourceGroup(subject, groupId);
    }

    public PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return resourceGroupManager.findResourceGroupsByCriteria(subject, criteria);
    }

    public PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc) {
        return resourceGroupManager.findResourceGroupsForRole(subject, roleId, pc);
    }

    public ResourceGroup getResourceGroup(Subject subject, int groupId) {
        return resourceGroupManager.getResourceGroup(subject, groupId);
    }

    public ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId) {
        return resourceGroupManager.getResourceGroupComposite(subject, groupId);
    }

    public void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds) {
        resourceGroupManager.removeResourcesFromGroup(subject, groupId, resourceIds);
    }

    public void setRecursive(Subject subject, int groupId, boolean isRecursive) {
        resourceGroupManager.setRecursive(subject, groupId, isRecursive);
    }

    public ResourceGroup updateResourceGroup(Subject subject, ResourceGroup newResourceGroup) {
        return resourceGroupManager.updateResourceGroup(subject, newResourceGroup);
    }

    //RESOURCEGROUPMANAGER: END ----------------------------------

    //RESOURCETYPEMANAGER: BEGIN ------------------------------------
    public PageList<ResourceType> findResourceTypesByCriteria(Subject subject, ResourceTypeCriteria criteria) {
        return resourceTypeManager.findResourceTypesByCriteria(subject, criteria);
    }

    public ResourceType getResourceTypeById(Subject subject, int resourceTypeId) throws ResourceTypeNotFoundException {
        return resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
    }

    public ResourceType getResourceTypeByNameAndPlugin(Subject subject, String name, String plugin) {
        return resourceTypeManager.getResourceTypeByNameAndPlugin(subject, name, plugin);
    }

    //RESOURCETYPEMANAGER: END ------------------------------------

    //ROLEMANAGER: BEGIN ----------------------------------
    public void addResourceGroupsToRole(Subject subject, int roleId, int[] pendingGroupIds) {
        roleManager.addResourceGroupsToRole(subject, roleId, pendingGroupIds);
    }

    public void addRolesToResourceGroup(Subject subject, int groupId, int[] roleIds) {
        roleManager.addRolesToResourceGroup(subject, groupId, roleIds);
    }

    public void addRolesToSubject(Subject subject, int subjectId, int[] roleIds) {
        roleManager.addRolesToSubject(subject, subjectId, roleIds);
    }

    public void addSubjectsToRole(Subject subject, int roleId, int[] subjectIds) {
        roleManager.addSubjectsToRole(subject, roleId, subjectIds);
    }

    public PageList<Role> findRolesByCriteria(Subject subject, RoleCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return roleManager.findRolesByCriteria(subject, criteria);
    }

    public PageList<Role> findSubjectAssignedRoles(Subject subject, int subjectId, PageControl pc) {
        return roleManager.findSubjectAssignedRoles(subject, subjectId, pc);
    }

    public PageList<Role> findSubjectUnassignedRoles(Subject subject, int subjectId, PageControl pc) {
        return roleManager.findSubjectUnassignedRoles(subject, subjectId, pc);
    }

    public Role getRole(Subject subject, int roleId) {
        return roleManager.getRole(subject, roleId);
    }

    public Role createRole(Subject subject, Role newRole) {
        return roleManager.createRole(subject, newRole);
    }

    public void deleteRoles(Subject subject, Integer[] roleIds) {
        roleManager.deleteRoles(subject, roleIds);
    }

    public Role updateRole(Subject subject, Role role) {
        return roleManager.updateRole(subject, role);
    }

    public void removeResourceGroupsFromRole(Subject subject, int roleId, int[] groupIds) {
        roleManager.removeResourceGroupsFromRole(subject, roleId, groupIds);
    }

    public void removeRolesFromResourceGroup(Subject subject, int groupId, int[] roleIds) {
        roleManager.removeRolesFromResourceGroup(subject, groupId, roleIds);
    }

    public void removeRolesFromSubject(Subject subject, int subjectId, int[] roleIds) {
        roleManager.removeRolesFromSubject(subject, subjectId, roleIds);
    }

    public void removeSubjectsFromRole(Subject subject, int roleId, int[] subjectIds) {
        roleManager.removeSubjectsFromRole(subject, roleId, subjectIds);
    }

    public void setAssignedResourceGroups(Subject subject, int roleId, int[] groupIds) {
        roleManager.setAssignedResourceGroups(subject, roleId, groupIds);
    }

    //ROLEMANAGER: END ----------------------------------

    //SUBJECT MANAGER: BEGIN ---------------------------------------
    public void changePassword(Subject subject, String username, String password) {
        subjectManager.changePassword(subject, username, password);
    }

    public void createPrincipal(Subject subject, String username, String password) throws SubjectException {
        subjectManager.createPrincipal(subject, username, password);
    }

    public Subject createSubject(Subject subject, Subject subjectToCreate) throws SubjectException {
        return subjectManager.createSubject(subject, subjectToCreate);
    }

    public void deleteSubjects(Subject subject, int[] subjectIds) {
        subjectManager.deleteSubjects(subject, subjectIds);
    }

    public PageList<Subject> findSubjectsByCriteria(Subject subject, SubjectCriteria criteria) {
        checkParametersPassedIn(subject, criteria);
        return subjectManager.findSubjectsByCriteria(subject, criteria);
    }

    public Subject getSubjectByName(String username) {
        return subjectManager.getSubjectByName(username);
    }

    public Subject getSubjectByNameAndSessionId(String username, int sessionId) throws Exception {
        return subjectManager.getSubjectByNameAndSessionId(username, sessionId);
    }

    public Subject login(String username, String password) throws LoginException {
        return subjectManager.login(username, password);
    }

    public void logout(Subject subject) {
        subjectManager.logout(subject);
    }

    public Subject updateSubject(Subject subject, Subject subjectToModify) {
        return subjectManager.updateSubject(subject, subjectToModify);
    }

    //SUBJECTMANAGER: END ------------------------------------

    //SUPPORTMANAGER: BEGIN ------------------------------------
    public URL getSnapshotReport(Subject subject, int resourceId, String name, String description) throws Exception {
        return supportManager.getSnapshotReport(subject, resourceId, name, description);
    }

    //SUPPORTMANAGER: END ------------------------------------

    //SYSTEMMANAGER: BEGIN ------------------------------------
    public ServerVersion getServerVersion(Subject subject) throws Exception {
        return systemManager.getServerVersion(subject);
    }

    //SYSTEMMANAGER: END ------------------------------------

    private void checkParametersPassedIn(Subject subject, Criteria criteria) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null.");
        }
        if (criteria == null) {
            throw new IllegalArgumentException("Criteria cannot be null.");
        }
    }

    public PageList<Distribution> findAssociatedDistributions(Subject subject, int repoId, PageControl pc) {
        return repoManager.findAssociatedDistributions(subject, repoId, pc);
    }

    public PageList<Advisory> findAssociatedAdvisory(Subject subject, int repoId, PageControl pc) {
        return repoManager.findAssociatedAdvisory(subject, repoId, pc);
    }

    public List<SubscribedRepo> findSubscriptions(Subject subject, int resourceId) {
        return repoManager.findSubscriptions(subject, resourceId);
    }

    public List<EntitlementCertificate> getCertificates(Subject subject, int resourceId) {
        return entitlementManager.getCertificates(subject, resourceId);
    }
}
