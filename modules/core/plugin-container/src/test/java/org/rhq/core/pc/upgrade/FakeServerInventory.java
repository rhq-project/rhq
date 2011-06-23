/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import static org.testng.Assert.fail;

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

import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This class represents a server side database store of the inventory for the purposes
 * of the ResourceUpgradeTest unit test.
 * 
 * @author Lukas Krejci
 */
public class FakeServerInventory {

    private Resource platform;
    private Map<String, Resource> resourceStore = new HashMap<String, Resource>();
    private int counter;
    private boolean failing;
    private boolean failUpgrade;
    
    private static final Comparator<Resource> ID_COMPARATOR = new Comparator<Resource>() {
        public int compare(Resource o1, Resource o2) {
            return o1.getId() - o2.getId();
        }
    };

    private static final Comparator<Resource> RESOURCE_TYPE_COMPARATOR = new Comparator<Resource>() {
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
    
    public synchronized CustomAction mergeInventoryReport(final InventoryStatus requiredInventoryStatus) {
        return new CustomAction("updateServerSideInventory") {
            public Object invoke(Invocation invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();
    
                    InventoryReport inventoryReport = (InventoryReport) invocation.getParameter(0);
    
                    for (Resource res : inventoryReport.getAddedRoots()) {
                        Resource persisted = fakePersist(res, requiredInventoryStatus, new HashSet<String>());
    
                        if (res.getParentResource() == Resource.ROOT) {
                            platform = persisted;
                        }
                    }
                    return getSyncInfo();
                }
            }
        };
    }

    public synchronized CustomAction clearPlatform() {
        return new CustomAction("updateServerSideInventory - report platform deleted on the server") {
            public Object invoke(Invocation invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();
    
                    platform = null;
    
                    return getSyncInfo();
                }
            }
        };
    }

    public synchronized CustomAction setResourceError() {
        return new CustomAction("setResourceError") {
            public Object invoke(Invocation invocation) throws Throwable {
                synchronized(FakeServerInventory.this) {
                    throwIfFailing();
                    
                    ResourceError error = (ResourceError) invocation.getParameter(0);
                    
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
    
    public synchronized CustomAction upgradeResources() {
        return new CustomAction("upgradeServerSideInventory") {
            @SuppressWarnings({ "serial", "unchecked" })
            public Object invoke(Invocation invocation) throws Throwable {
                synchronized(FakeServerInventory.this) {
                    throwIfFailing();
    
                    if (failUpgrade) {
                        throw new RuntimeException("Failing the upgrade purposefully.");                        
                    }
                    
                    Set<ResourceUpgradeRequest> requests = (Set<ResourceUpgradeRequest>) invocation.getParameter(0);
                    Set<ResourceUpgradeResponse> responses = new HashSet<ResourceUpgradeResponse>();
    
                    for (final ResourceUpgradeRequest request : requests) {
                        Resource resource = findResource(platform, new Resource() {
                            public int getId() {
                                return request.getResourceId();
                            }
                        }, ID_COMPARATOR);
                        if (resource != null) {
                            if (request.getNewDescription() != null) {
                                resource.setDescription(request.getNewDescription());
                            }
                            if (request.getNewName() != null) {
                                resource.setName(request.getNewName());
                            }
    
                            if (request.getNewResourceKey() != null) {
                                resource.setResourceKey(request.getNewResourceKey());
                            }
    
                            if (request.getUpgradeErrorMessage() != null) {
                                ResourceError error = new ResourceError(resource, ResourceErrorType.UPGRADE,
                                    request.getUpgradeErrorMessage(), request.getUpgradeErrorStackTrace(),
                                    request.getTimestamp());
                                resource.getResourceErrors().add(error);
                            }
    
                            ResourceUpgradeResponse resp = new ResourceUpgradeResponse();
                            resp.setResourceId(resource.getId());
                            resp.setUpgradedResourceName(resource.getName());
                            resp.setUpgradedResourceKey(resource.getResourceKey());
                            resp.setUpgradedResourceDescription(resource.getDescription());
                            responses.add(resp);
                        }
                    }
                    return responses;
                }
            }
        };
    }

    public synchronized CustomAction getResources() {
        return new CustomAction("getResources") {
            @SuppressWarnings("unchecked")
            public Object invoke(Invocation invocation) throws Throwable {
                synchronized (FakeServerInventory.this) {
                    throwIfFailing();
    
                    Set<Integer> resourceIds = (Set<Integer>) invocation.getParameter(0);
                    boolean includeDescendants = (Boolean) invocation.getParameter(1);
    
                    return getResources(resourceIds, includeDescendants);
                }
            }
        };
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
        for(Resource child : r.getChildResources()) {
            removeResource(child);
        }
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
            resourceStore.put(persisted.getUuid(), persisted);
        }
        persisted.setAgent(agentSideResource.getAgent());
        persisted.setCurrentAvailability(agentSideResource.getCurrentAvailability());
        persisted.setDescription(agentSideResource.getDescription());
        persisted.setName(agentSideResource.getName());
        persisted.setPluginConfiguration(agentSideResource.getPluginConfiguration().clone());
        persisted.setResourceConfiguration(agentSideResource.getResourceConfiguration().clone());
        persisted.setVersion(agentSideResource.getVersion());
        persisted.setInventoryStatus(requiredInventoryStatus);
        persisted.setResourceKey(agentSideResource.getResourceKey());
        persisted.setResourceType(agentSideResource.getResourceType());

        Resource parent = agentSideResource.getParentResource();
        if (parent != null && parent != Resource.ROOT) {
            parent = fakePersist(agentSideResource.getParentResource(), requiredInventoryStatus,
                inProgressUUIds);
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
            fail("Failed to convert resource " + root + " to a ResourceSyncInfo. This should not happen.", e);
            return null;
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
