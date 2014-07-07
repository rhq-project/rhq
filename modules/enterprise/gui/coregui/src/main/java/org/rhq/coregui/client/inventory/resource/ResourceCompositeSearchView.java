/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.coregui.client.inventory.resource.factory.ResourceFactoryImportWizard;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.TableUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Jay Shaughnessy
 */
public class ResourceCompositeSearchView extends ResourceSearchView {

    private final ResourceComposite parentResourceComposite;
    private boolean initialized;
    private List<Resource> singletonChildren;
    private Set<ResourceType> creatableChildTypes;
    private Set<ResourceType> importableChildTypes;
    private boolean hasCreatableTypes;
    private boolean hasImportableTypes;
    private boolean canCreate;

    public ResourceCompositeSearchView(ResourceComposite parentResourceComposite, Criteria criteria, String title,
        SortSpecifier[] sortSpecifier, String[] excludeFields, String headerIcon) {
        super(criteria, title, sortSpecifier, excludeFields, headerIcon);
        this.parentResourceComposite = parentResourceComposite;
        this.canCreate = this.parentResourceComposite.getResourcePermission().isCreateChildResources();
        setInitialCriteriaFixed(true);
    }

    public ResourceCompositeSearchView(ResourceComposite parentResourceComposite, Criteria criteria, String title,
        String headerIcon) {
        this(parentResourceComposite, criteria, title, null, null, headerIcon);
    }

    @Override
    protected void onInit() {

        // To properly filter Create Child and Import menus we need existing singleton child resources. If the
        // user has create permission and the parent type has singleton child types and creatable or importable child
        // types, perform an async call to fetch the singleton children. If we make the async call don't declare this
        // instance initialized until after it completes as we must have the children before the menu buttons can be drawn.

        final Resource parentResource = parentResourceComposite.getResource();
        ResourceType parentType = parentResource.getResourceType();
        creatableChildTypes = getCreatableChildTypes(parentType);
        importableChildTypes = getImportableChildTypes(parentType);
        hasCreatableTypes = !creatableChildTypes.isEmpty();
        hasImportableTypes = !importableChildTypes.isEmpty();
        refreshSingletons(parentResource, new AsyncCallback<PageList<Resource>>() {

            public void onFailure(Throwable caught) {
                ResourceCompositeSearchView.super.onInit();
                initialized = true;
            }

            public void onSuccess(PageList<Resource> result) {
                ResourceCompositeSearchView.super.onInit();
                initialized = true;
            }

        });

    }

    private void refreshSingletons(final Resource parentResource, final AsyncCallback<PageList<Resource>> callback) {
        singletonChildren = new ArrayList<Resource>(); // initialize to non-null

        Integer[] singletonChildTypes = getSingletonChildTypes(parentResource.getResourceType());

        if (canCreate && singletonChildTypes.length > 0 && (hasCreatableTypes || hasImportableTypes)) {
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterParentResourceId(parentResource.getId());
            criteria.addFilterResourceTypeIds(singletonChildTypes);
            GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                new AsyncCallback<PageList<Resource>>() {

                    @Override
                    public void onSuccess(PageList<Resource> result) {
                        singletonChildren = result;
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error("Failed to load child resources for [" + parentResource + "]", caught);
                        if (callback != null) {
                            callback.onFailure(caught);
                        }
                    }
                });
        } else {
            if (callback != null) {
                callback.onSuccess(new PageList<Resource>());
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && this.initialized;
    }

    // suppress unchecked warnings because the superclass has different generic types for the datasource
    @SuppressWarnings("rawtypes")
    @Override
    protected RPCDataSource getDataSourceInstance() {
        return ResourceCompositeDataSource.getInstance();
    }

    @Override
    protected void configureTable() {
        addTableAction(MSG.common_button_delete(), MSG.view_inventory_resources_deleteConfirm(), ButtonColor.RED,
            new AbstractTableAction(TableActionEnablement.ANY) {

                // only enabled if all selected are a deletable type and if the user has delete permission
                // on the resources.
                public boolean isEnabled(ListGridRecord[] selection) {
                    boolean isEnabled = super.isEnabled(selection);

                    if (isEnabled) {
                        for (ListGridRecord record : selection) {
                            ResourceComposite resComposite = (ResourceComposite) record
                                .getAttributeAsObject("resourceComposite");
                            Resource res = resComposite.getResource();
                            if (!(isEnabled = res.getResourceType().isDeletable())) {
                                break;
                            }
                            ResourcePermission resPermission = resComposite.getResourcePermission();
                            if (!(isEnabled = resPermission.isDeleteResource())) {
                                break;
                            }
                        }
                    }
                    return isEnabled;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    int[] resourceIds = TableUtility.getIds(selection);
                    ResourceGWTServiceAsync resourceManager = GWTServiceLookup.getResourceService();

                    resourceManager.deleteResources(resourceIds, new AsyncCallback<List<DeleteResourceHistory>>() {
                        public void onFailure(Throwable caught) {
                            if (caught instanceof CannotConnectToAgentException) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_inventory_resources_deleteFailed2(), Severity.Warning));
                            } else {
                                CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_deleteFailed(),
                                    caught);
                            }
                        }

                        public void onSuccess(List<DeleteResourceHistory> result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_inventory_resources_deleteSuccessful(), Severity.Info));

                            refresh(true);
                            // refresh the entire gui so it encompasses any relevant tree view. Don't just call this.refresh(),
                            // because CoreGUI.refresh is more comprehensive.
                            CoreGUI.refresh();
                        }
                    });
                }
            });

        addImportAndCreateButtons(false);

        super.configureTable();
    }

    @SuppressWarnings("unchecked")
    private void addImportAndCreateButtons(boolean override) {

        final Resource parentResource = parentResourceComposite.getResource();

        // Create Child Menu and Manual Import Menu
        if (canCreate && (hasCreatableTypes || hasImportableTypes)) {
            if (hasCreatableTypes) {
                Map<String, ResourceType> displayNameMap = getDisplayNames(creatableChildTypes);
                LinkedHashMap<String, ResourceType> createTypeValueMap = new LinkedHashMap<String, ResourceType>(
                    displayNameMap);
                removeExistingSingletons(singletonChildren, createTypeValueMap);
                AbstractTableAction createAction = new AbstractTableAction(TableActionEnablement.ALWAYS) {
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        ResourceFactoryCreateWizard.showCreateWizard(parentResource, (ResourceType) actionValue);
                        // we can refresh the table buttons immediately since the wizard is a dialog, the
                        // user can't access enabled buttons anyway.
                        ResourceCompositeSearchView.this.refreshTableInfo();
                    }
                };
                if (override) {
                    updateTableAction(MSG.common_button_create_child(), createTypeValueMap, createAction);
                } else {
                    addTableAction(MSG.common_button_create_child(), null, createTypeValueMap, ButtonColor.BLUE, createAction);
                }
            }

            if (hasImportableTypes) {
                Map<String, ResourceType> displayNameMap = getDisplayNames(importableChildTypes);
                LinkedHashMap<String, ResourceType> importTypeValueMap = new LinkedHashMap<String, ResourceType>(
                    displayNameMap);
                removeExistingSingletons(singletonChildren, importTypeValueMap);
                AbstractTableAction importAction = new AbstractTableAction(TableActionEnablement.ALWAYS) {
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        ResourceFactoryImportWizard.showImportWizard(parentResource, (ResourceType) actionValue);
                        // we can refresh the table buttons immediately since the wizard is a dialog, the
                        // user can't access enabled buttons anyway.
                        ResourceCompositeSearchView.this.refreshTableInfo();
                    }
                };
                if (override) {
                    updateTableAction(MSG.common_button_import(), importTypeValueMap, importAction);
                } else {
                    addTableAction(MSG.common_button_import(), null, importTypeValueMap, ButtonColor.BLUE, importAction);
                }
            }

        } else if (!override) {
            if (!canCreate && hasCreatableTypes) {
                addTableAction(MSG.common_button_create_child(), ButtonColor.BLUE, new AbstractTableAction(
                    TableActionEnablement.NEVER) {
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        // never called
                    }
                });
            }
            if (!canCreate && hasImportableTypes) {
                addTableAction(MSG.common_button_import(), ButtonColor.BLUE, new AbstractTableAction(
                    TableActionEnablement.NEVER) {
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        // never called
                    }
                });
            }
        }
    }

    private void removeExistingSingletons(List<Resource> singletonChildren, Map<String, ResourceType> displayNameMap) {

        List<String> existingSingletons = new ArrayList<String>();

        Set<String> displayNames = displayNameMap.keySet();
        for (final String displayName : displayNames) {
            final ResourceType type = displayNameMap.get(displayName);
            boolean exists = false;

            if (type.isSingleton()) {
                for (Resource child : singletonChildren) {
                    exists = child.getResourceType().equals(displayNameMap.get(displayName));
                    if (exists) {
                        existingSingletons.add(displayName);
                        break;
                    }
                }
            }
        }
        for (String existing : existingSingletons) {
            displayNameMap.remove(existing);
        }
    }

    private static Integer[] getSingletonChildTypes(ResourceType type) {
        Set<Integer> results = new TreeSet<Integer>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isSingleton()) {
                results.add(childType.getId());
            }
        }

        return results.toArray(new Integer[results.size()]);
    }

    private static Set<ResourceType> getImportableChildTypes(ResourceType type) {
        Set<ResourceType> results = new TreeSet<ResourceType>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isSupportsManualAdd()) {
                results.add(childType);
            }
        }
        return results;
    }

    private static Set<ResourceType> getCreatableChildTypes(ResourceType type) {
        Set<ResourceType> results = new TreeSet<ResourceType>();
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        for (ResourceType childType : childTypes) {
            if (childType.isCreatable()) {
                results.add(childType);
            }
        }
        return results;
    }

    private static Map<String, ResourceType> getDisplayNames(Set<ResourceType> types) {
        Set<String> allNames = new HashSet<String>();
        Set<String> repeatedNames = new HashSet<String>();
        for (ResourceType type : types) {
            String typeName = type.getName();
            if (allNames.contains(typeName)) {
                repeatedNames.add(typeName);
            } else {
                allNames.add(typeName);
            }
        }
        Map<String, ResourceType> results = new TreeMap<String, ResourceType>();
        for (ResourceType type : types) {
            String displayName = type.getName();
            if (repeatedNames.contains(type.getName())) {
                displayName += " (" + type.getPlugin() + " plugin)";
            }
            results.put(displayName, type);
        }
        return results;
    }

    protected void onUninventorySuccess() {
        refresh(true);
        // refresh the entire gui so it encompasses any relevant tree view. Don't just call this.refresh(),
        // because CoreGUI.refresh is more comprehensive.
        CoreGUI.refresh();
    }

    public ResourceComposite getParentResourceComposite() {
        return parentResourceComposite;
    }

    // -------- Static Utility loaders ------------

    public static ResourceCompositeSearchView getChildrenOf(ResourceComposite parentResourceComposite) {
        return new ResourceCompositeSearchView(parentResourceComposite, new Criteria("parentId",
            String.valueOf(parentResourceComposite.getResource().getId())), MSG.view_tabs_common_child_resources(), null);
    }

    @Override
    public void refresh() {
        refreshSingletons(parentResourceComposite.getResource(), new AsyncCallback<PageList<Resource>>() {

            @Override
            public void onSuccess(PageList<Resource> result) {
                addImportAndCreateButtons(true);
                ResourceCompositeSearchView.super.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                ResourceCompositeSearchView.super.refresh();
            }
        });

    }
}
