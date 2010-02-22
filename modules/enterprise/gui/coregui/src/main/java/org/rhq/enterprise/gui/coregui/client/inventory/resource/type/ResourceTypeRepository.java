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

import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceTypeRepository {

    private HashMap<Integer, ResourceType> typeCache = new HashMap<Integer, ResourceType>();
    private HashMap<Integer, EnumSet<MetadataType>> typeCacheLevel = new HashMap<Integer, EnumSet<MetadataType>>();

    private static ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

    public enum MetadataType {
        children, operations, measurements, content, events, pluginConfigurationDefinition, resourceConfigurationDefinition
    }


    public static class Cache {
        private static final ResourceTypeRepository ourInstance = GWT.create(ResourceTypeRepository.class);

        public static ResourceTypeRepository getInstance() {
            return ourInstance;
        }
    }


    public static interface TypeLoadedCallback {
        void onTypesLoaded(HashMap<Integer, ResourceType> types);
    }

    public static interface ResourceTypeLoadedCallback {
        void onResourceTypeLoaded(List<Resource> resources);
    }

    public void loadResourceTypes(final PageList<Resource> resources, final ResourceTypeLoadedCallback callback) {
        loadResourceTypes(resources, null, callback);
    }

    public void loadResourceTypes(final List<Resource> resources, final EnumSet<MetadataType> metadataTypes, final ResourceTypeLoadedCallback callback) {


        resourceTypeService.dummy(new RawConfiguration(), new AsyncCallback<RawConfiguration>() {
            public void onFailure(Throwable caught) {
                System.out.println("dummy failed");
            }

            public void onSuccess(RawConfiguration result) {
                System.out.println("dummy worked");
            }
        });

        if (resources.size() == 0) {
            callback.onResourceTypeLoaded(resources);
            return;
        }

        System.out.println("Getting types from cache");
        long start = System.currentTimeMillis();

        HashSet<Integer> types = new HashSet<Integer>();
        for (Resource res : resources) {
            types.add(res.getResourceType().getId());
        }
        getResourceTypes(types.toArray(new Integer[types.size()]), metadataTypes, new TypeLoadedCallback() {
            public void onTypesLoaded(HashMap<Integer, ResourceType> types) {
                for (Resource res : resources) {
                    res.setResourceType(types.get(res.getResourceType().getId()));
                }
                callback.onResourceTypeLoaded(resources);
            }
        });

        System.out.println("Loaded types from cache in " + (System.currentTimeMillis() - start));

    }


    public void getResourceTypes(Integer[] resourceTypeIds, final TypeLoadedCallback callback) {
        getResourceTypes(resourceTypeIds, null, callback);
    }


    public void getResourceTypes(Integer[] resourceTypeIds, final EnumSet<MetadataType> metadataTypes, final TypeLoadedCallback callback) {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();

        ArrayList<Integer> typesNeeded = new ArrayList<Integer>();

        final HashMap<Integer, ResourceType> cachedTypes = new HashMap<Integer, ResourceType>();

        for (Integer typeId : resourceTypeIds) {
            if (!typeCache.containsKey(typeId) || (metadataTypes != null && !typeCacheLevel.get(typeId).containsAll(metadataTypes))) {
                typesNeeded.add(typeId);
            } else {
                cachedTypes.put(typeId, typeCache.get(typeId));
            }
        }



        if (typesNeeded.isEmpty()) {
            callback.onTypesLoaded(cachedTypes);
            return;
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
                }
            }
        }

        criteria.addFilterIds(typesNeeded.toArray(new Integer[typesNeeded.size()]));

        criteria.setPageControl(PageControl.getUnlimitedInstance());

        resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                // TODO: Implement this method.
                caught.printStackTrace();
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


}
