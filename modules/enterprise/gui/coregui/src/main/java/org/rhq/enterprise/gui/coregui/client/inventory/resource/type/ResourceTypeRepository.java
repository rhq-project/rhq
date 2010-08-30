/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.type;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;

/**
 * @author Greg Hinkle
 */
public class ResourceTypeRepository {

    private HashMap<Integer, ResourceType> typeCache = new HashMap<Integer, ResourceType>();
    private HashMap<Integer, EnumSet<MetadataType>> typeCacheLevel = new HashMap<Integer, EnumSet<MetadataType>>();

    private static ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

    public enum MetadataType {
        children, operations, measurements, content, events, pluginConfigurationDefinition, resourceConfigurationDefinition,
        subCategory, parentTypes, processScans, productVersions
    }


    public static class Cache {
        private static final ResourceTypeRepository ourInstance = GWT.create(ResourceTypeRepository.class);

        public static ResourceTypeRepository getInstance() {
            return ourInstance;
        }
    }


    public static interface TypeLoadedCallback {
        void onTypesLoaded( ResourceType type);
    }


    public static interface TypesLoadedCallback {
        void onTypesLoaded(HashMap<Integer, ResourceType> types);
    }

    public static interface ResourceTypeLoadedCallback {
        void onResourceTypeLoaded(List<Resource> resources);
    }

    public void loadResourceTypes(final PageList<Resource> resources, final ResourceTypeLoadedCallback callback) {
        loadResourceTypes(resources, null, callback);
    }

    public void loadResourceTypes(final List<Resource> resources, final EnumSet<MetadataType> metadataTypes, final ResourceTypeLoadedCallback callback) {
        if (resources.size() == 0) {
            callback.onResourceTypeLoaded(resources);
            return;
        }

        long start = System.currentTimeMillis();

        HashSet<Integer> types = new HashSet<Integer>();
        for (Resource res : resources) {
            types.add(res.getResourceType().getId());
        }
        getResourceTypes(types.toArray(new Integer[types.size()]), metadataTypes, new TypesLoadedCallback() {
            public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                for (Resource res : resources) {
                    res.setResourceType(types.get(res.getResourceType().getId()));
                }
                callback.onResourceTypeLoaded(resources);
            }
        });

//        System.out.println("Loaded types from cache in " + (System.currentTimeMillis() - start));

    }


    public void getResourceTypes(Integer[] resourceTypeIds, final TypesLoadedCallback callback) {
        getResourceTypes(resourceTypeIds, null, callback);
    }


    public void getResourceTypes(final Integer resourceTypeId, final EnumSet<MetadataType> metadataTypes, final TypeLoadedCallback callback) {
        getResourceTypes(new Integer[]{resourceTypeId}, metadataTypes, new TypesLoadedCallback() {
            public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                callback.onTypesLoaded(types.get(resourceTypeId));
            }
        });
    }

    public void getResourceTypes(Integer[] resourceTypeIds, final EnumSet<MetadataType> metadataTypes, final TypesLoadedCallback callback) {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();

        final HashMap<Integer, ResourceType> cachedTypes = new HashMap<Integer, ResourceType>();


        ArrayList<Integer> typesNeeded = new ArrayList<Integer>();
        if (resourceTypeIds == null) {
            //preload all
        } else {

            for (Integer typeId : resourceTypeIds) {
                if (!typeCache.containsKey(typeId) || (metadataTypes != null && (typeCacheLevel.containsKey(typeId)) && !typeCacheLevel.get(typeId).containsAll(metadataTypes))) {
                    typesNeeded.add(typeId);
                } else {
                    cachedTypes.put(typeId, typeCache.get(typeId));
                }
            }

            if (typesNeeded.isEmpty()) {
                callback.onTypesLoaded(cachedTypes);
                return;
            }
            criteria.addFilterIds(typesNeeded.toArray(new Integer[typesNeeded.size()]));
        }

        if (metadataTypes != null) {
            for (MetadataType metadataType : metadataTypes) {
                switch (metadataType) {
                    case children:
                        criteria.fetchChildResourceTypes(true);
                        break;
                    case content:
                        criteria.fetchPackageTypes(true);
                        break;
                    case measurements:
                        criteria.fetchMetricDefinitions(true);
                        break;
                    case operations:
                        criteria.fetchOperationDefinitions(true);
                        break;
                    case events:
                        criteria.fetchEventDefinitions(true);
                        break;
                    case pluginConfigurationDefinition:
                        criteria.fetchPluginConfigurationDefinition(true);
                        break;
                    case resourceConfigurationDefinition:
                        criteria.fetchResourceConfigurationDefinition(true);
                        break;
                    case subCategory:
                        criteria.fetchSubCategory(true);
                        break;
                    case parentTypes:
                        criteria.fetchParentResourceTypes(true);
                        break;
                    case processScans:
                        criteria.fetchProcessScans(true);
                        break;
                    case productVersions:
                        criteria.fetchProductVersions(true);
                        break;
                }
            }
        }

        criteria.setPageControl(PageControl.getUnlimitedInstance());

        System.out.println("Loading " + typesNeeded.size() +  ((metadataTypes != null) ? (" types: " + metadataTypes.toString()) : ""));

        resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load resource type metadata",caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                for (ResourceType type : result) {
                    if (typeCache.containsKey(type.getId())) {
                        ResourceType cachedType = typeCache.get(type.getId());
                        if (metadataTypes != null) {
                            for (MetadataType metadataType : metadataTypes) {
                                switch (metadataType) {
                                    case children:
                                        cachedType.setChildResourceTypes(type.getChildResourceTypes());
                                        break;
                                    case operations:
                                        cachedType.setOperationDefinitions(type.getOperationDefinitions());
                                        break;
                                    case measurements:
                                        cachedType.setMetricDefinitions(type.getMetricDefinitions());
                                        break;
                                    case content:
                                        cachedType.setPackageTypes(type.getPackageTypes());
                                        break;
                                    case events:
                                        cachedType.setPackageTypes(type.getPackageTypes());
                                        break;
                                    case pluginConfigurationDefinition:
                                        cachedType.setPluginConfigurationDefinition(type.getPluginConfigurationDefinition());
                                        break;
                                    case resourceConfigurationDefinition:
                                        cachedType.setResourceConfigurationDefinition(type.getResourceConfigurationDefinition());
                                        break;
                                }
                            }
                        }
                        cachedTypes.put(type.getId(), cachedType);
                    } else {
                        typeCache.put(type.getId(), type);
                        cachedTypes.put(type.getId(), type);
                    }

                    if (metadataTypes != null) {
                        if (typeCacheLevel.containsKey(type.getId())) {
                            typeCacheLevel.get(type.getId()).addAll(metadataTypes);
                        } else {
                            typeCacheLevel.put(type.getId(), EnumSet.copyOf(metadataTypes));
                        }
                    }
                }
                callback.onTypesLoaded(cachedTypes);
            }
        });
    }


    public void preloadAll() {
        getResourceTypes((Integer[]) null, EnumSet.allOf(MetadataType.class), new TypesLoadedCallback() {
            public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                System.out.println("Preloaded ["+ types.size() + "] resource types");
            }
        });
    }
}
