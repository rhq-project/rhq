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

package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.factory.ResourceFactoryImportWizard;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class ResourceCompositeSearchView extends ResourceSearchView {

    private final ResourceComposite parentResourceComposite;

    public ResourceCompositeSearchView(String locatorId, ResourceComposite parentResourceComposite, Criteria criteria,
        String title, SortSpecifier[] sortSpecifier, String[] excludeFields, String... headerIcons) {
        super(locatorId, criteria, title, sortSpecifier, excludeFields, headerIcons);
        this.parentResourceComposite = parentResourceComposite;
    }

    public ResourceCompositeSearchView(String locatorId, ResourceComposite parentResourceComposite, Criteria criteria,
        String title, String... headerIcons) {
        this(locatorId, parentResourceComposite, criteria, title, null, null, headerIcons);
    }

    public ResourceCompositeSearchView(String locatorId, Criteria criteria, String title, String... headerIcons) {
        this(locatorId, null, criteria, title, headerIcons);
    }

    // surpress unchecked warnings because the superclass has different generic types for the datasource
    @SuppressWarnings("unchecked")
    @Override
    protected RPCDataSource getDataSourceInstance() {
        return ResourceCompositeDataSource.getInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureTable() {
        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), MSG
            .view_inventory_resources_deleteConfirm(), new AbstractTableAction(TableActionEnablement.ANY) {

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
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_deleteFailed(), caught);
                    }

                    public void onSuccess(List<DeleteResourceHistory> result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_inventory_resources_deleteSuccessful(), Severity.Info));

                        ResourceCompositeSearchView.this.refresh();
                    }
                });
            }
        });

        if (this.parentResourceComposite.getResourcePermission().isCreateChildResources()) {
            ResourceType parentType = parentResourceComposite.getResource().getResourceType();

            // manual import type menu
            LinkedHashMap<String, ResourceType> importTypeValueMap = new LinkedHashMap<String, ResourceType>();

            for (ResourceType childType : parentType.getChildResourceTypes()) {
                if (childType.isSupportsManualAdd()) {
                    importTypeValueMap.put(childType.getName(), childType);
                }
            }
            addTableAction(extendLocatorId("Import"), MSG.common_button_import(), null, importTypeValueMap,
                new AbstractTableAction(TableActionEnablement.ALWAYS) {

                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        ResourceFactoryImportWizard.showImportWizard(parentResourceComposite.getResource(),
                            (ResourceType) actionValue);
                    }
                });

            // creatable child type menu
            LinkedHashMap<String, ResourceType> createTypeValueMap = new LinkedHashMap<String, ResourceType>();

            for (ResourceType childType : parentType.getChildResourceTypes()) {
                if (childType.isCreatable()) {
                    createTypeValueMap.put(childType.getName(), childType);
                }
            }
            if (!createTypeValueMap.isEmpty()) {
                addTableAction(extendLocatorId("CreateChild"), MSG.common_button_create_child(), null,
                    createTypeValueMap, new AbstractTableAction(TableActionEnablement.ALWAYS) {

                        public void executeAction(ListGridRecord[] selection, Object actionValue) {
                            ResourceFactoryCreateWizard.showCreateWizard(parentResourceComposite.getResource(),
                                (ResourceType) actionValue);
                        }
                    });
            }

        }

        super.configureTable();
    }

    public ResourceComposite getParentResourceComposite() {
        return parentResourceComposite;
    }

    // -------- Static Utility loaders ------------

    public static ResourceCompositeSearchView getChildrenOf(String locatorId, ResourceComposite parentResourceComposite) {
        return new ResourceCompositeSearchView(locatorId, parentResourceComposite, new Criteria("parentId", String
            .valueOf(parentResourceComposite.getResource().getId())), MSG.view_inventory_resources_title_children());
    }

}
