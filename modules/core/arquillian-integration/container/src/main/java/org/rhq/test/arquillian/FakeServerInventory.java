/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;

/**
 * This class represents a server side database store of the inventory for the purposes
 * of the unit tests that need to mock out the server functionality.
 * <p>
 * The methods are not exhaustive and were added on as-needed basis. If this class doesn't
 * cover what you need, either add the functionality directly to it, subclass or roll your
 * own. It is only meant as a helper.
 * <p>
 * This impl uses mockito for defining the answers to various calls.
 * 
 * @author Lukas Krejci
 */
public class FakeServerInventory {

    private static final Log LOG = LogFactory.getLog(FakeServerInventory.class);

    /**
     * You can {@link #waitForDiscoveryComplete()} on an instance of this class
     * for the complete discovery to finish in case your
     * fake server commits some resources (which starts off
     * asynchronous discovery of children).
     * 
     *
     * @author Lukas Krejci
     */
    public static class CompleteDiscoveryChecker {
        private volatile boolean depthReached;
        private final int expectedDepth;
        private final Object sync = new Object();
        private volatile boolean finished;

        public CompleteDiscoveryChecker(int expectedDepth) {
            this.expectedDepth = expectedDepth;
            this.depthReached = expectedDepth == 0;
        }

        public void waitForDiscoveryComplete() throws InterruptedException {
            waitForDiscoveryComplete(0);
        }

        public void waitForDiscoveryComplete(long timeoutMillis) throws InterruptedException {
            synchronized (sync) {
                if (!depthReached) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Waiting for the discovery to complete on " + this);
                    }
                    sync.wait(timeoutMillis);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Discovery already complete... no need to wait on " + this);
                    }
                }

                finished = true;
            }
        }

        private void setDepth(int resourceTreeDepth) {
            synchronized (sync) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Current tree depth is " + resourceTreeDepth + ", this checker is waiting for "
                        + expectedDepth + " on " + this);
                }
                if (!depthReached && resourceTreeDepth >= expectedDepth) {
                    depthReached = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //just way some more to give the PC time to really finish
                                //the handling of the inventory report.
                                //I know this sucks and is prone to races but we cannot
                                //properly implement this without modifying the plugin container.
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Ad-hoc wait for discovery to really complete...");
                                }
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                //well, we are going to finish in a few anyway
                            } finally {
                                synchronized (sync) {
                                    if (!finished) {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("Notifying about discovery complete on "
                                                + CompleteDiscoveryChecker.this);
                                        }
                                        sync.notifyAll();
                                    }
                                }
                            }
                        }
                    }).start();
                }
            }
        }
    }

    private Resource platform;
    private Map<String, Resource> resourceStore = new HashMap<String, Resource>();
    private int counter;
    private AtomicInteger metricScheduleCounter = new AtomicInteger(1); // used when creating measurement schedules
    private boolean failing;
    private boolean failUpgrade;

    private CompleteDiscoveryChecker discoveryChecker;

    private static final Comparator<Resource> ID_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            return o1.getId() - o2.getId();
        }
    };

    private static final Comparator<Resource> RESOURCE_TYPE_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            return o1.getResourceType().equals(o2.getResourceType()) ? 0 : o1.getId() - o2.getId();
        }
    };

    public FakeServerInventory() {
        this(false);
    }

    public FakeServerInventory(boolean failing) {
        this.failing = failing;
    }

    public synchronized void prepopulateInventory(Resource platform, Collection<Resource> topLevelServers) {
        this.platform = fakePersist(platform, InventoryStatus.COMMITTED, new HashSet<String>());
        for (Resource res : topLevelServers) {
            res.setParentResource(this.platform);
            fakePersist(res, InventoryStatus.COMMITTED, new HashSet<String>());
        }
    }

    public CompleteDiscoveryChecker createAsyncDiscoveryCompletionChecker(int expectedResourceTreeDepth) {
        discoveryChecker = new CompleteDiscoveryChecker(expectedResourceTreeDepth);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Created a new discovery complete checker with tree depth " + expectedResourceTreeDepth + ": "
                + discoveryChecker);
        }
        return discoveryChecker;
    }

    public synchronized Answer<MergeResourceResponse> addResource() {
        return new Answer<MergeResourceResponse>() {
            @Override
            public MergeResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                Resource r = (Resource) invocation.getArguments()[0];
                int subjectId = (Integer) invocation.getArguments()[1];

                LOG.debug("A request to add a resource [" + r + "] made by subject with id " + subjectId);

                boolean exists = getResourceStore().containsKey(r.getUuid());

                r = fakePersist(r, InventoryStatus.COMMITTED, new HashSet<String>());

                return new MergeResourceResponse(r.getId(), exists);
            }
        };
    }

    public synchronized Answer<MergeInventoryReportResults> mergeInventoryReport(
        final InventoryStatus requiredInventoryStatus) {
        return new Answer<MergeInventoryReportResults>() {
            @Override
            public MergeInventoryReportResults answer(InvocationOnMock invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    InventoryReport inventoryReport = (InventoryReport) invocation.getArguments()[0];

                    try {
                        throwIfFailing();

                        for (Resource res : inventoryReport.getAddedRoots()) {
                            Resource persisted = fakePersist(res, requiredInventoryStatus, new HashSet<String>());

                            if (res.getParentResource() == Resource.ROOT) {
                                platform = persisted;
                            }
                        }
                        return new MergeInventoryReportResults(getSyncInfo(), null);
                    } finally {
                        if (discoveryChecker != null && !inventoryReport.getAddedRoots().isEmpty()) {
                            discoveryChecker.setDepth(getResourceTreeDepth());
                        }
                    }
                }
            }
        };
    }

    public synchronized int getResourceTreeDepth() {
        if (platform == null) {
            return 0;
        }

        return getTreeDepth(platform);
    }

    private static int getTreeDepth(Resource root) {
        int maxDepth = 0;
        for (Resource c : root.getChildResources()) {
            int childDepth = getTreeDepth(c);
            if (maxDepth < childDepth) {
                maxDepth = childDepth;
            }
        }

        return maxDepth + 1;
    }

    public synchronized Answer<ResourceSyncInfo> clearPlatform() {
        return new Answer<ResourceSyncInfo>() {
            @Override
            public ResourceSyncInfo answer(InvocationOnMock invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();

                    platform = null;

                    return getSyncInfo();
                }
            }
        };
    }

    public synchronized Answer<Void> setResourceError() {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();

                    ResourceError error = (ResourceError) invocation.getArguments()[0];

                    Resource serverSideResource = resourceStore.get(error.getResource().getUuid());

                    if (serverSideResource != null) {
                        List<ResourceError> currentErrors = serverSideResource.getResourceErrors();
                        currentErrors.add(error);
                    }

                    return null;
                }
            }
        };
    }

    public synchronized Answer<Set<ResourceUpgradeResponse>> upgradeResources() {
        return new Answer<Set<ResourceUpgradeResponse>>() {
            @Override
            @SuppressWarnings({ "serial", "unchecked" })
            public Set<ResourceUpgradeResponse> answer(InvocationOnMock invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();

                    if (failUpgrade) {
                        throw new RuntimeException("Failing the upgrade purposefully.");
                    }

                    Object[] args = invocation.getArguments();
                    Set<ResourceUpgradeRequest> requests = (Set<ResourceUpgradeRequest>) args[0];
                    Set<ResourceUpgradeResponse> responses = new HashSet<ResourceUpgradeResponse>();

                    for (final ResourceUpgradeRequest request : requests) {
                        Resource resource = findResource(platform, new Resource() {
                            @Override
                            public int getId() {
                                return request.getResourceId();
                            }
                        }, ID_COMPARATOR);
                        if (resource != null) {

                            ResourceUpgradeResponse resp = new ResourceUpgradeResponse();
                            resp.setResourceId(resource.getId());

                            if (request.getNewDescription() != null) {
                                resource.setDescription(request.getNewDescription());
                                resp.setUpgradedResourceDescription(resource.getDescription());
                            }

                            if (request.getNewName() != null) {
                                resource.setName(request.getNewName());
                                resp.setUpgradedResourceName(resource.getName());
                            }

                            if (request.getNewResourceKey() != null) {
                                resource.setResourceKey(request.getNewResourceKey());
                                resp.setUpgradedResourceKey(resource.getResourceKey());
                            }

                            if (request.getNewPluginConfiguration() != null) {
                                resource.setPluginConfiguration(request.getNewPluginConfiguration());
                                resp.setUpgradedResourcePluginConfiguration(resource.getPluginConfiguration());
                            }

                            if (request.getUpgradeErrorMessage() != null) {
                                ResourceError error = new ResourceError(resource, ResourceErrorType.UPGRADE,
                                    request.getUpgradeErrorMessage(), request.getUpgradeErrorStackTrace(),
                                    request.getTimestamp());
                                resource.getResourceErrors().add(error);
                            }

                            responses.add(resp);
                        }
                    }
                    return responses;
                }
            }
        };
    }

    public synchronized Answer<Set<Resource>> getResources() {
        return new Answer<Set<Resource>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Set<Resource> answer(InvocationOnMock invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();

                    Object[] args = invocation.getArguments();
                    Set<Integer> resourceIds = (Set<Integer>) args[0];
                    boolean includeDescendants = (Boolean) args[1];

                    return getResources(resourceIds, includeDescendants);
                }
            }
        };
    }

    // this expects the mock invocation to have two parameters - Set<Integer> resourceIds, Boolean getChildSchedules
    public synchronized Answer<Set<ResourceMeasurementScheduleRequest>> getLatestSchedulesForResourceIds() {
        return new Answer<Set<ResourceMeasurementScheduleRequest>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Set<ResourceMeasurementScheduleRequest> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Set<Integer> resourceIds = (Set<Integer>) args[0];
                Boolean getChildSchedules = (Boolean) args[1];

                Set<Resource> resources = getResources(resourceIds, getChildSchedules);
                Set<ResourceMeasurementScheduleRequest> allSchedules = new HashSet<ResourceMeasurementScheduleRequest>();
                for (Resource resource : resources) {
                    ResourceMeasurementScheduleRequest resourceSchedules = getDefaultMeasurementSchedules(resource);
                    if (resourceSchedules != null) {
                        allSchedules.add(resourceSchedules);
                    }
                }
                return allSchedules;
            }
        };
    }

    // this expects the mock invocation to have one parameter - Set<Integer> resourceIds
    public synchronized Answer<Set<ResourceMeasurementScheduleRequest>> postProcessNewlyCommittedResources() {
        return new Answer<Set<ResourceMeasurementScheduleRequest>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Set<ResourceMeasurementScheduleRequest> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Set<Integer> resourceIds = (Set<Integer>) args[0];

                Set<Resource> resources = getResources(resourceIds, false);
                Set<ResourceMeasurementScheduleRequest> allSchedules = new HashSet<ResourceMeasurementScheduleRequest>();
                for (Resource resource : resources) {
                    ResourceMeasurementScheduleRequest resourceSchedules = getDefaultMeasurementSchedules(resource);
                    if (resourceSchedules != null) {
                        allSchedules.add(resourceSchedules);
                    }
                }
                return allSchedules;
            }
        };
    }

    private ResourceMeasurementScheduleRequest getDefaultMeasurementSchedules(Resource resource) {
        ResourceType rt = resource.getResourceType();
        Set<MeasurementDefinition> metrics = rt.getMetricDefinitions();
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        ResourceMeasurementScheduleRequest resourceSchedules = new ResourceMeasurementScheduleRequest(resource.getId());
        for (MeasurementDefinition metric : metrics) {
            int id = this.metricScheduleCounter.getAndIncrement();
            String name = metric.getName();
            long interval = metric.getDefaultInterval();
            boolean enabled = metric.isDefaultOn() || metric.getDisplayType() == DisplayType.SUMMARY;
            DataType dataType = metric.getDataType();
            NumericType nDataType = metric.getNumericType();
            MeasurementScheduleRequest schedule = new MeasurementScheduleRequest(id, name, interval, enabled, dataType,
                nDataType);
            resourceSchedules.addMeasurementScheduleRequest(schedule);
        }
        return resourceSchedules;
    }

    public synchronized boolean isFailing() {
        return failing;
    }

    public synchronized void setFailing(boolean failing) {
        this.failing = failing;
    }

    public synchronized boolean isFailUpgrade() {
        return failUpgrade;
    }

    public synchronized void setFailUpgrade(boolean failUpgrade) {
        this.failUpgrade = failUpgrade;
    }

    @SuppressWarnings("serial")
    public synchronized Set<Resource> findResourcesByType(final ResourceType type) {
        Set<Resource> result = new HashSet<Resource>();
        if (platform != null) {
            findResources(platform, new Resource() {
                @Override
                public ResourceType getResourceType() {
                    return type;
                }
            }, result, RESOURCE_TYPE_COMPARATOR);
        }
        return result;
    }

    @SuppressWarnings("serial")
    private Set<Resource> getResources(Set<Integer> resourceIds, boolean includeDescendants) {
        //it is important to keep the hierarchical order of the resource in the returned set
        //so that plugin container can merge the resources from top to bottom.
        Set<Resource> result = new LinkedHashSet<Resource>();

        for (final Integer id : resourceIds) {
            Resource r = findResource(platform, new Resource() {
                @Override
                public int getId() {
                    return id;
                }
            }, ID_COMPARATOR);
            if (r != null) {
                result.add(r);

                if (includeDescendants) {
                    for (Resource child : r.getChildResources()) {
                        result.addAll(getResources(Collections.singleton(child.getId()), true));
                    }
                }
            }
        }

        return result;
    }

    public void removeResource(Resource r) {
        resourceStore.remove(r.getUuid());
        Resource parent = r.getParentResource();
        if (parent != null) {
            parent.getChildResources().remove(r);
        }
        for (Resource child : r.getChildResources()) {
            removeResource(child);
        }
    }

    public Map<String, Resource> getResourceStore() {
        return resourceStore;
    }

    private Resource fakePersist(Resource agentSideResource, InventoryStatus requiredInventoryStatus,
        Set<String> inProgressUUIds) {
        Resource persisted = resourceStore.get(agentSideResource.getUuid());
        if (!inProgressUUIds.add(agentSideResource.getUuid())) {
            return persisted;
        }
        if (persisted == null) {
            persisted = new Resource();
            if (agentSideResource.getId() != 0 && counter < agentSideResource.getId()) {
                counter = agentSideResource.getId() - 1;
            }
            persisted.setId(++counter);
            persisted.setUuid(agentSideResource.getUuid());
            persisted.setAgent(agentSideResource.getAgent());
            persisted.setCurrentAvailability(agentSideResource.getCurrentAvailability());
            persisted.setDescription(agentSideResource.getDescription());
            persisted.setName(agentSideResource.getName());
            persisted.setPluginConfiguration(agentSideResource.getPluginConfiguration().clone());
            persisted.setResourceConfiguration(InventoryManager.getResourceConfiguration(agentSideResource).clone());
            persisted.setVersion(agentSideResource.getVersion());
            persisted.setInventoryStatus(requiredInventoryStatus);
            persisted.setResourceKey(agentSideResource.getResourceKey());
            persisted.setResourceType(agentSideResource.getResourceType());
            resourceStore.put(persisted.getUuid(), persisted);
        }

        Resource parent = agentSideResource.getParentResource();
        if (parent != null && parent != Resource.ROOT) {
            parent = fakePersist(agentSideResource.getParentResource(), requiredInventoryStatus, inProgressUUIds);
            persisted.setParentResource(parent);
            parent.getChildResources().add(persisted);
        } else {
            persisted.setParentResource(parent);
        }

        //persist the children
        Set<Resource> childResources = new LinkedHashSet<Resource>();
        for (Resource child : agentSideResource.getChildResources()) {
            childResources.add(fakePersist(child, requiredInventoryStatus, inProgressUUIds));
        }
        //now update the list with whatever the persisted resource contained in the past
        //i.e. we prefer the current results from the agent but keep the children we used to
        //have in the past. This is the same behavior as the actual RHQ server has.
        childResources.addAll(persisted.getChildResources());

        persisted.setChildResources(childResources);

        inProgressUUIds.remove(agentSideResource.getUuid());

        return persisted;
    }

    private ResourceSyncInfo getSyncInfo() {
        return platform != null ? convert(platform) : null;
    }

    private void throwIfFailing() {
        if (failing) {
            throw new RuntimeException("Fake server inventory is in the failing mode.");
        }
    }

    private static ResourceSyncInfo convert(Resource root) {
        return convertInternal(root, new HashMap<String, ResourceSyncInfo>());
    }

    private static ResourceSyncInfo convertInternal(Resource root, Map<String, ResourceSyncInfo> intermediateResults) {
        ResourceSyncInfo ret = intermediateResults.get(root.getUuid());

        if (ret != null) {
            return ret;
        }

        try {
            ret = new ResourceSyncInfo();

            intermediateResults.put(root.getUuid(), ret);

            Class<ResourceSyncInfo> clazz = ResourceSyncInfo.class;

            getPrivateField(clazz, "id").set(ret, root.getId());
            getPrivateField(clazz, "uuid").set(ret, root.getUuid());
            getPrivateField(clazz, "mtime").set(ret, root.getMtime());
            getPrivateField(clazz, "inventoryStatus").set(ret, root.getInventoryStatus());

            ResourceSyncInfo parent = root.getParentResource() == null ? null : convertInternal(
                root.getParentResource(), intermediateResults);

            getPrivateField(clazz, "parent").set(ret, parent);

            Set<ResourceSyncInfo> children = new LinkedHashSet<ResourceSyncInfo>();
            for (Resource child : root.getChildResources()) {
                ResourceSyncInfo syncChild = convertInternal(child, intermediateResults);

                children.add(syncChild);
            }
            getPrivateField(clazz, "childSyncInfos").set(ret, children);

            return ret;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert resource " + root
                + " to a ResourceSyncInfo. This should not happen.", e);
        }
    }

    private static Field getPrivateField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }

        return field;
    }

    private static Resource findResource(Resource root, Resource template, Comparator<Resource> comparator) {
        if (root == null)
            return null;
        if (comparator.compare(root, template) == 0) {
            return root;
        } else {
            for (Resource child : root.getChildResources()) {
                Resource found = findResource(child, template, comparator);
                if (found != null)
                    return found;
            }
        }

        return null;
    }

    private static void findResources(Resource root, Resource template, Set<Resource> result,
        Comparator<Resource> comparator) {
        if (root == null)
            return;
        if (comparator.compare(root, template) == 0) {
            result.add(root);
        } else {
            for (Resource child : root.getChildResources()) {
                findResources(child, template, result, comparator);
            }
        }
    }
}
