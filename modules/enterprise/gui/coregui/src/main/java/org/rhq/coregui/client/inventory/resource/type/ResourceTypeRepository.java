/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.type;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.coregui.client.util.Log;

/**
 * A cache for ResourceTypes and their various fields. Fields are only fetched as needed.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTypeRepository {

    static private final Messages MSG = CoreGUI.getMessages();

    private Map<Integer, ResourceType> typeCache = new HashMap<Integer, ResourceType>();
    private Map<Integer, EnumSet<MetadataType>> typeCacheLevel = new HashMap<Integer, EnumSet<MetadataType>>();
    private Set<ResourceType> topLevelServerAndServiceTypes;

    private static ResourceTypeGWTServiceAsync resourceTypeService = GWTServiceLookup.getResourceTypeGWTService();

    /**
     * The following MetadadaTypes are subject to change and are always fetched from the database:<br/>
     * driftDefinitionTemplates
     *
     * @author Jay Shaughnessy
     */
    public enum MetadataType {
        children, operations, measurements, content, events, pluginConfigurationDefinition, resourceConfigurationDefinition, parentTypes, processScans, productVersions, driftDefinitionTemplates(
            true), bundleConfiguration;

        private boolean isFetchAlways;

        private MetadataType() {
            this(false);
        }

        /**
         * @param isFetchAlways if true then the cache for this metadata will be refreshed each time it is requested.
         * Meaning, the db will always be called because this metadata is subject to change.
         */
        private MetadataType(boolean isFetchAlways) {
            this.isFetchAlways = isFetchAlways;
        }

        public boolean isFetchAlways() {
            return isFetchAlways;
        }
    }

    public static class Cache {
        private static final ResourceTypeRepository ourInstance = GWT.create(ResourceTypeRepository.class);

        public static ResourceTypeRepository getInstance() {
            return ourInstance;
        }
    }

    public static interface TypeLoadedCallback {
        void onTypesLoaded(ResourceType type);
    }

    public static interface TypesLoadedCallback {
        void onTypesLoaded(Map<Integer, ResourceType> types);
    }

    public static interface ResourceTypeLoadedCallback {
        void onResourceTypeLoaded(List<Resource> resources);
    }

    public static interface ResourceTypeLoadedInGroupCallback {
        void onResourceTypeLoaded(List<ResourceGroup> resources);
    }

    public void loadResourceTypes(final PageList<Resource> resources, final ResourceTypeLoadedCallback callback) {
        loadResourceTypes(resources, null, callback);
    }

    public void loadResourceTypes(final List<Resource> resources, final EnumSet<MetadataType> metadataTypes,
        final ResourceTypeLoadedCallback callback) {
        if (resources.size() == 0) {
            if (callback != null) {
                callback.onResourceTypeLoaded(resources);
            }
            return;
        }

        //long start = System.currentTimeMillis();

        Set<Integer> types = new HashSet<Integer>();
        for (Resource res : resources) {
            types.add(res.getResourceType().getId());
        }
        getResourceTypes(types.toArray(new Integer[types.size()]), metadataTypes, new TypesLoadedCallback() {
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                for (Resource res : resources) {
                    res.setResourceType(types.get(res.getResourceType().getId()));
                }
                if (callback != null) {
                    callback.onResourceTypeLoaded(resources);
                }
            }
        });

        //Log.info("Loaded types from cache in " + (System.currentTimeMillis() - start));
    }

    public void loadResourceTypes(final PageList<ResourceGroup> groups, final ResourceTypeLoadedInGroupCallback callback) {
        loadResourceTypes(groups, null, callback);
    }

    public void loadResourceTypes(final List<ResourceGroup> groups, final EnumSet<MetadataType> metadataTypes,
        final ResourceTypeLoadedInGroupCallback callback) {
        if (groups.size() == 0) {
            if (callback != null) {
                callback.onResourceTypeLoaded(groups);
            }
            return;
        }

        // long start = System.currentTimeMillis();

        Set<Integer> types = new HashSet<Integer>();
        for (ResourceGroup group : groups) {
            ResourceType type = group.getResourceType();
            if (type != null) {
                types.add(type.getId());
            }
        }
        getResourceTypes(types.toArray(new Integer[types.size()]), metadataTypes, new TypesLoadedCallback() {
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                for (ResourceGroup group : groups) {
                    ResourceType type = group.getResourceType();
                    if (type != null) {
                        group.setResourceType(types.get(type.getId()));
                    }
                }
                if (callback != null) {
                    callback.onResourceTypeLoaded(groups);
                }
            }
        });

        //Log.info("Loaded types from cache in " + (System.currentTimeMillis() - start));
    }

    public void getResourceTypes(Integer[] resourceTypeIds, final TypesLoadedCallback callback) {
        getResourceTypes(resourceTypeIds, null, callback);
    }

    public void getResourceTypes(final Integer resourceTypeId, final EnumSet<MetadataType> metadataTypes,
        final TypeLoadedCallback callback) {
        getResourceTypes(new Integer[] { resourceTypeId }, metadataTypes, new TypesLoadedCallback() {
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                if (callback != null) {
                    callback.onTypesLoaded(types.get(resourceTypeId));
                }
            }
        });
    }

    public void getResourceTypes(Integer[] resourceTypeIds, EnumSet<MetadataType> metadataTypes,
        final TypesLoadedCallback callback) {

        // note, metadataTypes == null implies EnumSet.noneOf(MetadataType.class)
        metadataTypes = (null == metadataTypes) ? EnumSet.noneOf(MetadataType.class) : metadataTypes;

        final Map<Integer, ResourceType> cachedTypes = new HashMap<Integer, ResourceType>();
        List<Integer> typesNeeded = new ArrayList<Integer>();
        EnumSet<MetadataType> metadataTypesNeeded = EnumSet.noneOf(MetadataType.class);
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterIgnored(null); // we will cache both unignored and ignored types

        if (resourceTypeIds == null) {
            //preload all
        } else {
            for (Integer typeId : resourceTypeIds) {
                // we need to query for data if:
                // 1. we don't have the resource type in our cache at all, or...
                // 2. we have the basic resource type but no additional metadata, but the caller is asking for additional metadata
                // 3. we have the resource type and some additional metadata, but the caller is asking for metadata that we don't have
                if (!typeCache.containsKey(typeId) // 1.
                    || (!metadataTypes.isEmpty() && (!typeCacheLevel.containsKey(typeId) // 2.
                    || !typeCacheLevel.get(typeId).containsAll(metadataTypes)))) // 3.
                {
                    // add this type to the types we need to fetch
                    typesNeeded.add(typeId);

                    // make sure we fetch the metadata needed for this type
                    if (metadataTypesNeeded.size() < metadataTypes.size()) {
                        EnumSet<MetadataType> metadataTypesCached = typeCacheLevel.get(typeId);

                        if (metadataTypesCached == null) {
                            metadataTypesNeeded = metadataTypes;

                        } else {
                            for (MetadataType metadataType : metadataTypes) {
                                if (!metadataTypesCached.contains(metadataType)) {
                                    metadataTypesNeeded.add(metadataType);
                                }
                            }
                        }
                    }
                } else {
                    cachedTypes.put(typeId, typeCache.get(typeId));
                }
            }

            if (typesNeeded.isEmpty()) {
                if (callback != null) {
                    callback.onTypesLoaded(cachedTypes);
                }
                return;
            }
            criteria.addFilterIds(typesNeeded.toArray(new Integer[typesNeeded.size()]));
        }

        for (MetadataType metadataType : metadataTypesNeeded) {
            switch (metadataType) {
            case children:
                criteria.fetchChildResourceTypes(true);
                break;
            case content:
                criteria.fetchPackageTypes(true);
                break;
            case events:
                criteria.fetchEventDefinitions(true);
                break;
            case measurements:
                criteria.fetchMetricDefinitions(true);
                break;
            case operations:
                criteria.fetchOperationDefinitions(true);
                break;
            case parentTypes:
                criteria.fetchParentResourceTypes(true);
                break;
            case pluginConfigurationDefinition:
                criteria.fetchPluginConfigurationDefinition(true);
                break;
            case processScans:
                criteria.fetchProcessScans(true);
                break;
            case productVersions:
                criteria.fetchProductVersions(true);
                break;
            case resourceConfigurationDefinition:
                criteria.fetchResourceConfigurationDefinition(true);
                break;
            case driftDefinitionTemplates:
                criteria.fetchDriftDefinitionTemplates(true);
                break;
            case bundleConfiguration:
                criteria.fetchBundleConfiguration(true);
                break;

            default:
                Log.error("Metadata type [" + metadataType.name() + "] not incorporated into ResourceType criteria.");
            }
        }

        criteria.setPageControl(PageControl.getUnlimitedInstance());

        Log.info("Loading [" + typesNeeded.size() + "] types with facets=[" + metadataTypesNeeded + "]...");

        if ((topLevelServerAndServiceTypes == null) && metadataTypesNeeded.contains(MetadataType.children)) {
            // Perform a one-time load of server and service types with no parent types. These types are implicitly
            // children of all platform types, even though they are not included in the platform types'
            // childResourceTypes field in the DB. After the top-level types are loaded, loadRequestedTypes() will be
            // called to load the requested types. For any requested types that are platforms, the top-level
            // server/service types will be added to the platform types' childResourceTypes fields.
            loadTopLevelServerAndServiceTypes(callback, metadataTypesNeeded, criteria, cachedTypes);
        } else {
            loadRequestedTypes(callback, metadataTypesNeeded, criteria, cachedTypes);
        }
    }

    private void loadTopLevelServerAndServiceTypes(final TypesLoadedCallback callback,
        final EnumSet<MetadataType> metadataTypes, final ResourceTypeCriteria criteria,
        final Map<Integer, ResourceType> cachedTypes) {
        ResourceTypeCriteria topLevelCriteria = new ResourceTypeCriteria();
        topLevelCriteria.addFilterIgnored(null); // we will cache both unignored and ignored types
        topLevelCriteria.addFilterCategories(ResourceCategory.SERVER, ResourceCategory.SERVICE);
        topLevelCriteria.addFilterParentResourceTypesEmpty(true);
        topLevelCriteria.addSortCategory(PageOrdering.DESC);
        topLevelCriteria.addSortName(PageOrdering.ASC);
        resourceTypeService.findResourceTypesByCriteria(topLevelCriteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onSuccess(PageList<ResourceType> types) {
                topLevelServerAndServiceTypes = new LinkedHashSet<ResourceType>(types.size());
                for (ResourceType type : types) {
                    if (cachedTypes.containsKey(type.getId())) {
                        ResourceType cachedType = cachedTypes.get(type.getId());
                        topLevelServerAndServiceTypes.add(cachedType);
                    } else {
                        cachedTypes.put(type.getId(), type);
                        topLevelServerAndServiceTypes.add(type);
                    }
                }
                if (Log.isDebugEnabled()) {
                    Set<String> typeNames = new LinkedHashSet<String>(topLevelServerAndServiceTypes.size());
                    for (ResourceType type : topLevelServerAndServiceTypes) {
                        typeNames.add(type.getPlugin() + ":" + ResourceTypeUtility.displayName(type));
                    }
                    Log.debug("Loaded " + typeNames.size() + " top-level server and service types: " + typeNames);
                }
                loadRequestedTypes(callback, metadataTypes, criteria, cachedTypes);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_typeCache_loadFail(), caught);
                loadRequestedTypes(callback, metadataTypes, criteria, cachedTypes);
            }
        });
    }

    private void loadRequestedTypes(final TypesLoadedCallback callback, final EnumSet<MetadataType> metadataTypes,
        ResourceTypeCriteria criteria, final Map<Integer, ResourceType> cachedTypes) {
        resourceTypeService.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_typeCache_loadFail(), caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                for (ResourceType type : result) {
                    if (typeCache.containsKey(type.getId())) {
                        ResourceType cachedType = typeCache.get(type.getId());
                        if (metadataTypes != null) {
                            for (MetadataType metadataType : metadataTypes) {
                                switch (metadataType) {
                                case children:
                                    Set<ResourceType> childTypes = type.getChildResourceTypes();
                                    if (type.getCategory() == ResourceCategory.PLATFORM
                                        && topLevelServerAndServiceTypes != null) {
                                        // Add server and service types with no parent types to the list of child types.
                                        // These types are implicitly children of all platform types, even though they
                                        // are not included in the platform types' childResourceTypes field.
                                        childTypes.addAll(topLevelServerAndServiceTypes);
                                    }
                                    cachedType.setChildResourceTypes(childTypes);
                                    break;
                                case content:
                                    cachedType.setPackageTypes(type.getPackageTypes());
                                    break;
                                case events:
                                    cachedType.setEventDefinitions(type.getEventDefinitions());
                                    break;
                                case measurements:
                                    cachedType.setMetricDefinitions(type.getMetricDefinitions());
                                    break;
                                case operations:
                                    cachedType.setOperationDefinitions(type.getOperationDefinitions());
                                    break;
                                case parentTypes:
                                    cachedType.setParentResourceTypes(type.getParentResourceTypes());
                                    break;
                                case pluginConfigurationDefinition:
                                    cachedType.setPluginConfigurationDefinition(type.getPluginConfigurationDefinition());
                                    break;
                                case processScans:
                                    cachedType.setProcessScans(type.getProcessScans());
                                    break;
                                case productVersions:
                                    cachedType.setProductVersions(type.getProductVersions());
                                    break;
                                case resourceConfigurationDefinition:
                                    cachedType.setResourceConfigurationDefinition(type
                                        .getResourceConfigurationDefinition());
                                    break;
                                case driftDefinitionTemplates:
                                    cachedType.setDriftDefinitionTemplates(type.getDriftDefinitionTemplates());
                                    break;
                                case bundleConfiguration:
                                    cachedType.setResourceTypeBundleConfiguration(type
                                        .getResourceTypeBundleConfiguration());
                                    break;
                                default:
                                    Log.error("ERROR: metadataType " + metadataType.name()
                                        + " not merged into cached ResourceType.");
                                }
                            }
                        }
                        cachedTypes.put(type.getId(), cachedType);

                    } else {
                        if (type.getCategory() == ResourceCategory.PLATFORM && topLevelServerAndServiceTypes != null
                            && metadataTypes.contains(MetadataType.children)) {
                            // Add server and service types with no parent types to the list of child types.
                            // These types are implicitly children of all platform types, even though they
                            // are not included in the platform types' childResourceTypes field.
                            if (null == type.getChildResourceTypes()) {
                                type.setChildResourceTypes(topLevelServerAndServiceTypes);
                            } else {
                                type.getChildResourceTypes().addAll(topLevelServerAndServiceTypes);
                            }
                        }

                        typeCache.put(type.getId(), type);
                        cachedTypes.put(type.getId(), type);
                    }

                    if (metadataTypes != null && !metadataTypes.isEmpty()) {
                        EnumSet<MetadataType> cachedMetadata = typeCacheLevel.get(type.getId());
                        cachedMetadata = (null == cachedMetadata) ? EnumSet.noneOf(MetadataType.class) : cachedMetadata;
                        for (MetadataType metadataType : metadataTypes) {
                            if (!metadataType.isFetchAlways()) {
                                cachedMetadata.add(metadataType);
                            }
                        }
                        typeCacheLevel.put(type.getId(), cachedMetadata);
                    }
                }

                if (callback != null) {
                    callback.onTypesLoaded(cachedTypes);
                }
            }
        });
    }

    public void preloadAll() {
        getResourceTypes((Integer[]) null, EnumSet.allOf(MetadataType.class), new TypesLoadedCallback() {
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                Log.info("Preloaded [" + types.size() + "] Resource types.");
            }
        });
    }

    public void clear() {
        Log.info("Clearing cache...");
        typeCache.clear();
        typeCacheLevel.clear();
        topLevelServerAndServiceTypes = null;
    }

}
