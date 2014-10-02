/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.resource;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.server.resource.disambiguation.DisambiguationUpdateStrategy;

/**
 * A manager that provides methods for creating, reading, updating, and deleting {@link Resource}s.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Local
public interface ResourceManagerLocal extends ResourceManagerRemote {
    /**
     * Create a new Resource. This method creates only the supplied Resource, it will *NOT* create
     * the parent or any childResources.
     *
     * @param user the user creating the resource
     * @param resource the resource to be created
     * @param parentId set to -1 to imply that this is a root resource, which has no parent
     *
     * @throws ResourceAlreadyExistsException if an equivalent resource already exists
     */
    void createResource(Subject user, Resource resource, int parentId) throws ResourceAlreadyExistsException;

    /**
     * This will uninventory the resource with the given ID along with all of its child resources. Existing
     * transactions will be suspended and the work will be done in a NEW transaction.
     *
     * @param  user       the user deleting the resource
     * @param  resourceId the ID of the resource to be deleted
     *
     * @return the list of all resources that were deleted - in effect, this will contain <code>resourceId</code> and
     *         its children's IDs
     */
    List<Integer> uninventoryResource(Subject user, int resourceId);

    /**
     * Internal use only. use with care, avoids authz checking overhead.
     *
     * @param resourceId
     * @return true if uninventoryMissing was enabled on the type and the resource was uninventoried, or if the resource
     * was not found. Otherwise false.
     */
    boolean handleMissingResourceInNewTransaction(int resourceId);

    /**
     * Internal use only. use with care, avoids authz checking overhead.
     *
     * @param resourceId
     * @return
     */
    List<Integer> uninventoryResourceInNewTransaction(int resourceId);

    /**
     * Deletes the given resource (but not its children) in a new transaction. This is normally used only within this
     * manager bean itself. Clients normally should call {@link #uninventoryResource(Subject, int)}. If you call this
     * method, make sure you have a specific reason for it; check to see if calling
     * {@link #uninventoryResource(Subject, int)} would not be more appropriate.
     *
     * @param user       the user deleting the resource
     * @param resourceId the ID of the resource to be deleted
     */
    void uninventoryResourceAsyncWork(Subject user, int resourceId);

    boolean bulkNativeQueryDeleteInNewTransaction(Subject subject, String nativeQueryString, List<Integer> resourceIds);

    boolean bulkNamedQueryDeleteInNewTransaction(Subject subject, String namedQuery, List<Integer> resourceIds);

    void unlinkStorageNodesInNewTx(List<Integer> resourceIds);

    List<Integer> getResourceDescendantsByTypeAndName(Subject user, int resourceId, Integer resourceTypeId, String name);

    /**
     * Changes the inventory status of the specified resource and optionally its descendants to the provided inventory
     * status if the user is authorized to do so.
     *
     * @param  user           user doing the resource status changing
     * @param  resource       the Resource to change
     * @param  newStatus      the new status to change it to
     * @param  setDescendants if true, all descendent resources will also have their status set
     *
     * @return an updated copy of the resource
     */
    Resource setResourceStatus(Subject user, Resource resource, InventoryStatus newStatus, boolean setDescendants);

    /**
     * @param user
     * @param resourceId
     * @return the resource
     * @throws ResourceNotFoundException if not found. Note, this is an ApplicationException.
     */
    @NotNull
    Resource getResourceById(Subject user, int resourceId);

    /**
     * Returns the parent of the Resource with the specified id, or null if the Resource does not have a parent (i.e. is
     * a platform).
     *
     * @param  resourceId the id of a {@link Resource} in inventory
     *
     * @return the parent of the Resource with the specified id, or null if the Resource does not have a parent
     */
    @Nullable
    Resource getParentResource(int resourceId);

    List<Integer> getResourceIdLineage(int resourceId);

    /**
     * Returns the lineage of the Resource with the specified id. The lineage is represented as a List of Resources,
     * with the first item being the root of the Resource's ancestry (or the Resource itself if it is a root Resource
     * (i.e. a platform)) and the last item being the Resource itself. Since the lineage includes the Resource itself,
     * the returned List will always contain at least one item.
     *
     * @param  resourceId the id of a {@link Resource} in inventory
     *
     * @return the lineage of the Resource with the specified id
     */
    @NotNull
    List<Resource> getResourceLineage(int resourceId);

    /**
     * Returns the direct lineage of a resource up to the top most resource in the hierarchy.
     * Composites are returned that indicate whether the resource is viewable or not by the user.
     *
     * @param resourceId id of resource
     * @return resource lineage
     */
    List<ResourceLineageComposite> getResourceLineage(Subject subject, int resourceId);

    /**
     * Returns the lineage of a resource plus all the siblings of the resources in the lineage. This is
     * useful for prepopulating all the resources visible in an expanded tree. Composites are returned
     * that indicate whether the resource is viewable or should be locked in the tree.
     *
     * @param resourceId id of resource
     * @return resource lineage and siblings
     */
    List<ResourceLineageComposite> getResourceLineageAndSiblings(Subject subject, int resourceId);

    /**
     * Looks up the root of a subtree in the inventory. This will generally find the platform on which
     * a resource is running.
     * @param resourceId the resource to find the root parent of
     * @return the root of the supplied resource's tree
     */
    @NotNull
    Resource getRootResourceForResource(int resourceId);

    /**
     * @param  user
     * @param  parent
     * @param  key
     *
     * @return the resource, or null if no such resource exists
     */
    @Nullable
    Resource getResourceByParentAndKey(Subject user, @Nullable
    Resource parent, String key, String plugin, String typeName);

    PageList<Resource> findResourceByParentAndInventoryStatus(Subject user, Resource parent, InventoryStatus status,
        PageControl pageControl);

    List<ResourceWithAvailability> findResourcesByParentAndType(Subject user, Resource parent, ResourceType type);

    List<Integer> findChildrenResourceIds(int parentResourceId, InventoryStatus status);

    PageList<Resource> findChildResourcesByCategoryAndInventoryStatus(Subject user, Resource parent,
        ResourceCategory category, InventoryStatus status, PageControl pageControl);

    PageList<Resource> findResourcesByCategory(Subject user, ResourceCategory category,
        InventoryStatus inventoryStatus, PageControl pageControl);

    PageList<ResourceComposite> findResourceComposites(Subject user, ResourceCategory category, String typeName,
        String pluginName, Resource parentResource, String searchString, boolean attachParentResource,
        PageControl pageControl);

    /**
     * Get a summary of counts, by category, of the user's viewable resources having the provided inventory status.
     *
     * @param user
     * @param status
     * @return A 3 element int array with counts for platform, service, service as a[0], a[1], a[2], respectively.
     */
    int[] getResourceCountSummary(Subject user, InventoryStatus status);

    int getResourceCountByCategory(Subject user, ResourceCategory category, InventoryStatus status);

    int getResourceCountByTypeAndIds(Subject user, ResourceType type, int[] resourceIds);

    List<Integer> findResourcesMarkedForAsyncDeletion(Subject user);

    /**
     * Gets a list of platforms that were recently added (committed) to inventory.
     *
     * @param  user
     * @param  ctime the oldest time (epoch mills) that a platform had to have been added for it to be returned
     * @param  maxItems the maximum number of items to return within the timeframe
     *
     * @return list of all platforms that were added since or at <code>ctime</code>
     */
    List<RecentlyAddedResourceComposite> findRecentlyAddedPlatforms(Subject user, long ctime, int maxItems);

    /**
     * Gets a list of servers that are children of the given platform that were recently added (committed) to inventory.
     *
     * @param  user
     * @param  ctime the oldest time (epoch mills) that a server had to have been added for it to be returned
     *
     * @return list of all servers (that are children of the given platforms) that were added since or at <code>
     *         ctime</code>
     */
    List<RecentlyAddedResourceComposite> findRecentlyAddedServers(Subject user, long ctime, int platformId);

    List<Integer> findExplicitResourceIdsByResourceGroup(int resourceGroupId);

    List<Integer> findImplicitResourceIdsByResourceGroup(int resourceGroupId);

    List<ResourceIdFlyWeight> findFlyWeights(int[] resourceIds);

    PageList<Resource> findExplicitResourcesByResourceGroup(Subject subject, ResourceGroup group,
        PageControl pageControl);

    PageList<Resource> findImplicitResourcesByResourceGroup(Subject user, ResourceGroup group, PageControl pageControl);

    PageList<ResourceWithAvailability> findExplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl);

    PageList<ResourceWithAvailability> findImplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl);

    PageList<Resource> findAvailableResourcesForResourceGroup(Subject user, int groupId, ResourceType type,
        ResourceCategory category, String nameFilter, int[] excludeIds, PageControl pageControl);

    PageList<Resource> findAvailableResourcesForRepo(Subject user, int repoId, String search,
        ResourceCategory category, PageControl pageControl);

    PageList<Resource> findAvailableResourcesForDashboardPortlet(Subject user, Integer typeId,
        ResourceCategory category, int[] excludeIds, PageControl pageControl);

    PageList<Resource> findResourceByIds(Subject subject, int[] resourceIds, boolean attachParentResource,
        PageControl pageControl);

    Resource getResourceTree(int rootResourceId, boolean recursive);

    /**
     * Get a Resource Composite for Resources limited by the given parameters
     *
     * @param  user           User executing the query
     * @param  category       Category this query should be limited to
     * @param  resourceTypeId the PK of the desired resource type or -1 if no limit
     * @param  parentResource the desired parent resource or null if no limit
     * @param  pageControl
     *
     * @return
     */
    PageList<ResourceComposite> findResourceCompositeForParentAndTypeAndCategory(Subject user,
        ResourceCategory category, int resourceTypeId, Resource parentResource, PageControl pageControl);

    /**
     * Returns the errors of the given type that have occurred for the given resource.
     *
     * @param  user       the user asking to see the errors
     * @param  resourceId the resource whose errors are to be returned
     * @param  errorType  the type of errors to return
     *
     * @return the list of resource errors
     */
    @NotNull
    List<ResourceError> findResourceErrors(Subject user, int resourceId, ResourceErrorType errorType);

    /**
     * Returns all the errors that have occurred for the given resource.
     *
     * @param  user       the user asking to see the errors
     * @param  resourceId the resource whose errors are to be returned
     *
     * @return the list of resource errors
     */
    @NotNull
    List<ResourceError> findResourceErrors(Subject user, int resourceId);

    /**
     * Indicates an error occurred on a resource. The given error will be associated with the resource found in the
     * error.
     *
     * @param resourceError encapsulates all information about the error
     */
    void addResourceError(ResourceError resourceError);

    /**
     * Deletes the given resource error, effectively removing it from its resource's list of errors. Requires the
     * specified user to possess the {@link org.rhq.core.domain.authz.Permission#MODIFY_RESOURCE MODIFY_RESOURCE}
     * permission for the Resource with which the error is associated.
     *
     * @param user the user deleting the resource error
     * @param resourceErrorId identifies the resource error to delete
     */
    void deleteResourceError(Subject user, int resourceErrorId);

    List<AutoGroupComposite> findChildrenAutoGroups(Subject user, int parentResourceId);

    @NotNull
    List<AutoGroupComposite> findChildrenAutoGroups(Subject user, int parentResourceId, int[] resourceTypeIds);

    AutoGroupComposite getResourceAutoGroup(Subject user, int resourceId);

    /**
     * INTERNAL.
     *
     * Our implementation of resource errors may lead to duplicates for error types which are supposed to get at most
     * one entry per resource. The purge job will call this method to get rid of the duplicates, if any.
     */
    void removeResourceErrorDuplicates();

    Map<Integer, InventoryStatus> getResourceStatuses(int rootResourceId, boolean descendants);

    /**
     * Gets the "health" of a set of resources, where the health composite gives you the resource's availability status
     * and the number of alerts it has emitted. Note that even though this takes a subject - it is assumed the caller is
     * authorized to see the given resources and no authz checks will be made. This method takes a subject in case, in
     * the future, we decide this implementation needs to perform authz checks.
     *
     * <p>This method is here to support the favorites portlet in the dashboard, where the favorites are stored as user
     * preferences - and they can't make it into the preferences unless the user had access to see those resources in
     * the first place, hence, no additional authorizations need to be made here.
     *
     * <p>
     *
     * @param  user
     * @param  resourceIds
     * @param  pc
     *
     * @return the health information on the resources
     */
    PageList<ResourceHealthComposite> findResourceHealth(Subject user, int[] resourceIds, PageControl pc);

    public List<AutoGroupComposite> findResourcesAutoGroups(Subject subject, int[] resourceIds);

    /**
     * Clears errors of type {@link ResourceErrorType}.INVALID_PLUGIN_CONFIGURATION
     * @param resourceId id of the resource
     */
    void clearResourceConfigError(int resourceId);

    /**
     * Clears errors of the given type.
     * @param subject the user that is making the request
     * @param resourceId id of the resource
     * @param resourceErrorType type of error to clear
     * @return the number of errors that were cleared
     */
    int clearResourceConfigErrorByType(Subject subject, int resourceId, ResourceErrorType resourceErrorType);

    /**
     * Returns the platform Resource associated with the specified Agent.
     *
     * @param agent an Agent
     *
     * @return the platform Resource associated with the specified Agent or null if the platform for the agent is not known yet.
     */
    Resource getPlatform(Agent agent);

    /**
     * Load the entire list of resources under an agent. Tries to do so in as few
     * queries as possible while prefetching the information necessary to create a tree
     * view of the platform inventory. This includes resource type and subcategory information
     * as well as current availability and structure.
     *
     * This method also returns placeholder "locked" ResourceFlyweight
     * objects for resources that a user should not have visibility to in order to keep the tree a
     * directed graph.
     *
     * @param user user accessing the tree
     * @param agentId the agent id of the platform to return inventory for
     * @param pageControl the filter for the resources
     * @return the list of all resources on a platform
     */
    List<ResourceFlyweight> findResourcesByAgent(Subject user, int agentId, PageControl pageControl);

    List<ResourceFlyweight> findResourcesByCompatibleGroup(Subject user, int compatibleGroupId, PageControl pageControl);

    /**
     * Update the ancestry for the specified resource and its child lineage.
     * <pre>
     * The ancestry is recursively defined as:
     *
     *     resourceAncestry=parentResourceResourceTypeId_:_parentResourceId_:_parentResourceName_::_parentResourceAncestry
     *
     *     * note that platform resources have no parent and therefore have a null ancestry
     *
     * </pre>
     * @param subject
     * @param resourceId
     */
    public void updateAncestry(Subject subject, int resourceId);

    /**
     * This method exists to support the GUI resource tree, by not returning an unlimited number of resources
     * but instead bounding the returned size.  Note, this routine does not offer paging and any PageControl set in
     * the Criteria is ignored. This is for use when paging is not required or possible, such as in a tree.
     *
     * @param subject
     * @param criteria
     * @param maxResources Will not return more than this number of resources.  If the original fetch exceeds maxResources
     * then maxResourcesByType will be enforced.  If, after trimming by type, maxResources is still exceeded, then the least
     * significant resources (the tail, assuming a sorted set) will be removed to enforce the limit. If <=0 the default
     * will be used.
     * @param maxResourcesByType If maxResources is exceeded by the initial result set then members of each type will be
     * trimmed down to meet this limit.  If <=0 the default will be used.
     * @return The resulting resources, trimmed if necessary to meet the specify sizing bounds.
     */
    List<Resource> findResourcesByCriteriaBounded(Subject subject, ResourceCriteria criteria, int maxResources,
        int maxResourcesByType);

    Map<Agent, AvailabilityReport> getDisableResourcesReportInNewTransaction(Subject subject, int[] resourceIds,
        List<Integer> disableResourceIds);

    Map<Agent, AvailabilityReport> getEnableResourcesReportInNewTransaction(Subject subject, int[] resourceIds,
        List<Integer> enableResourceIds);

    List<ResourceInstallCount> findResourceComplianceCounts(Subject subject);

    List<ResourceInstallCount> findResourceInstallCounts(Subject subject, boolean groupByVersions);

    PageList<ResourceComposite> findResourceCompositesByCriteria(Subject subject, ResourceCriteria criteria);

    Resource getPlaformOfResource(Subject subject, int resourceId);

    /**
     * @return the disambiguation result or null on error
     */
    <T> List<DisambiguationReport<T>> disambiguate(List<T> results, IntExtractor<? super T> resourceIdExtractor,
        DisambiguationUpdateStrategy updateStrategy);

    List<Integer> findIdsByTypeIds(List<Integer> resourceTypeIds);

    Integer getResourceCount(List<Integer> resourceTypeIds);

    PageList<Resource> findGroupMemberCandidateResources(Subject subject, ResourceCriteria criteria,
        int[] alreadySelectedResourceIds);
}
