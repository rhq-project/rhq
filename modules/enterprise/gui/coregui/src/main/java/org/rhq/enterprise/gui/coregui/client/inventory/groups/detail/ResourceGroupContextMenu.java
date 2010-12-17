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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceTypeGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupContextMenu extends LocatableMenu {

    private boolean isAutoGroup = false;

    public ResourceGroupContextMenu(String locatorId) {
        super(locatorId);
    }

    public ResourceGroupContextMenu(String locatorId, boolean isAutoGroup) {
        super(locatorId);
        this.isAutoGroup = isAutoGroup;
    }

    private ResourceType currentType;
    //private ResourceGroup group;
    private ResourceGroup currentGroup;

    public void showContextMenu(ResourceGroup compatibleGroup) {
        this.currentType = compatibleGroup.getResourceType();
        this.currentGroup = compatibleGroup;

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            currentType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.subCategory,
                ResourceTypeRepository.MetadataType.pluginConfigurationDefinition,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                    currentType = type;

                    buildResourceGroupContextMenu(currentGroup, type);
                    showContextMenu();
                }
            });
    }

    private void buildResourceGroupContextMenu(final ResourceGroup group, final ResourceType resourceType) {
        // name
        setItems(new MenuItem(group.getName()));

        // type name
        addItem(new MenuItem("Type: " + resourceType.getName()));

        // separator
        addItem(new MenuItemSeparator());

        // plugin config
        MenuItem editPluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        editPluginConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                if (isAutoGroup) {
                    CoreGUI.goToView(LinkManager.getAutoGroupTabLink(group.getId(), "Inventory", "ConnectionSettings"));
                } else {
                    CoreGUI.goToView(LinkManager.getResourceGroupTabLink(group.getId(), "Inventory",
                        "ConnectionSettings"));
                }
            }
        });
        editPluginConfiguration.setEnabled(resourceType.getPluginConfigurationDefinition() != null);
        addItem(editPluginConfiguration);

        MenuItem editResourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        editResourceConfiguration.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                int groupId = group.getId();
                int resourceTypeId = resourceType.getId();

                final Window configEditor = new Window();
                configEditor.setTitle(MSG.view_tree_common_contextMenu_editResourceConfiguration(group.getName()));
                configEditor.setWidth(800);
                configEditor.setHeight(800);
                configEditor.setIsModal(true);
                configEditor.setShowModalMask(true);
                configEditor.setCanDragResize(true);
                configEditor.setShowResizer(true);
                configEditor.centerInPage();
                configEditor.addCloseClickHandler(new CloseClickHandler() {
                    public void onCloseClick(CloseClientEvent closeClientEvent) {
                        configEditor.destroy();
                    }
                });
                // TODO group config editor
                //                configEditor.addItem(new ConfigurationEditor(resourceId, resourceTypeId,
                //                    ConfigurationEditor.ConfigType.resource));
                configEditor.show();

            }
        });
        editResourceConfiguration.setEnabled(resourceType.getResourceConfigurationDefinition() != null);
        addItem(editResourceConfiguration);

        addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.view_tree_common_contextMenu_operations());
        Menu opSubMenu = new Menu();
        if (resourceType.getOperationDefinitions() != null) {
            for (final OperationDefinition operationDefinition : resourceType.getOperationDefinitions()) {
                MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
                operationItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {

                        // TODO Group version
                        //                    ResourceCriteria criteria = new ResourceCriteria();
                        //                    criteria.addFilterId(selectedResourceId);
                        //
                        //                    GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                        //                        new AsyncCallback<PageList<Resource>>() {
                        //                            public void onFailure(Throwable caught) {
                        //                                CoreGUI.getErrorHandler()
                        //                                    .handleError("Failed to get resource to run operation", caught);
                        //                            }
                        //
                        //                            public void onSuccess(PageList<Resource> result) {
                        //                                new OperationCreateWizard(result.get(0), operationDefinition).startOperationWizard();
                        //                            }
                        //                        });

                    }
                });
                opSubMenu.addItem(operationItem);
            }
        }
        operations.setEnabled(resourceType.getOperationDefinitions() != null
            && !resourceType.getOperationDefinitions().isEmpty());
        operations.setSubmenu(opSubMenu);
        addItem(operations);

        addItem(buildMetricsMenu(resourceType));

        /* TODO: We don't support group factory create
        // Create Menu
        MenuItem createChildMenu = new MenuItem("Create Child");
        Menu createChildSubMenu = new Menu();
        for (final ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isCreatable()) {
                MenuItem createItem = new MenuItem(childType.getName());
                createChildSubMenu.addItem(createItem);
                createItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        ResourceFactoryCreateWizard.showCreateWizard(resource, childType);
                    }
                });

            }
        }
        createChildMenu.setSubmenu(createChildSubMenu);
        createChildMenu.setEnabled(createChildSubMenu.getItems().length > 0);
        contextMenu.addItem(createChildMenu);*/

        /*
        // TODO We don't group manual import
        // Manually Add Menu
        MenuItem importChildMenu = new MenuItem("Import");
        Menu importChildSubMenu = new Menu();
        for (ResourceType childType : resourceType.getChildResourceTypes()) {
            if (childType.isSupportsManualAdd()) {
                importChildSubMenu.addItem(new MenuItem(childType.getName()));
            }
        }
        if (resourceType.getCategory() == ResourceCategory.PLATFORM) {
            loadManuallyAddServersToPlatforms(importChildSubMenu);
        }
        importChildMenu.setSubmenu(importChildSubMenu);
        importChildMenu.setEnabled(importChildSubMenu.getItems().length > 0);
        addItem(importChildMenu);
        */
    }

    private void loadManuallyAddServersToPlatforms(final Menu manuallyAddMenu) {
        ResourceTypeGWTServiceAsync rts = GWTServiceLookup.getResourceTypeGWTService();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterSupportsManualAdd(true);
        criteria.fetchParentResourceTypes(true);
        rts.findResourceTypesByCriteria(criteria, new AsyncCallback<PageList<ResourceType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFail_children(), caught);
            }

            public void onSuccess(PageList<ResourceType> result) {
                for (ResourceType type : result) {
                    if (type.getParentResourceTypes() == null || type.getParentResourceTypes().isEmpty()) {
                        MenuItem item = new MenuItem(type.getName());
                        manuallyAddMenu.addItem(item);
                    }
                }
            }
        });
    }

    private MenuItem buildMetricsMenu(final ResourceType type) {
        MenuItem measurements = new MenuItem(MSG.view_tree_common_contextMenu_measurements());
        final Menu measurementsSubMenu = new Menu();

        GWTServiceLookup.getDashboardService().findDashboardsForSubject(new AsyncCallback<List<Dashboard>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFail_dashboards(), caught);
            }

            public void onSuccess(List<Dashboard> result) {

                if (type.getMetricDefinitions() != null) {
                    for (final MeasurementDefinition def : type.getMetricDefinitions()) {

                        MenuItem defItem = new MenuItem(def.getDisplayName());
                        measurementsSubMenu.addItem(defItem);
                        Menu defSubItem = new Menu();
                        defItem.setSubmenu(defSubItem);

                        for (final Dashboard d : result) {
                            MenuItem addToDBItem = new MenuItem(MSG.view_tree_common_contextMenu_addChartToDashboard(d
                                .getName()));
                            defSubItem.addItem(addToDBItem);

                            addToDBItem.addClickHandler(new ClickHandler() {
                                public void onClick(MenuItemClickEvent menuItemClickEvent) {

                                    DashboardPortlet p = new DashboardPortlet(def.getDisplayName() + " "
                                        + MSG.view_tree_common_contextMenu_chart(), GraphPortlet.KEY, 250);
                                    p.getConfiguration().put(
                                        new PropertySimple(GraphPortlet.CFG_RESOURCE_GROUP_ID, currentGroup.getId()));
                                    p.getConfiguration().put(
                                        new PropertySimple(GraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                    d.addPortlet(p, 0, 0);

                                    GWTServiceLookup.getDashboardService().storeDashboard(d,
                                        new AsyncCallback<Dashboard>() {
                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                    MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                                                    caught);
                                            }

                                            public void onSuccess(Dashboard result) {
                                                String msg = MSG
                                                    .view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                        .getName());
                                                CoreGUI.getMessageCenter().notify(
                                                    new Message(msg, Message.Severity.Info));
                                            }
                                        });

                                }
                            });

                        }

                    }
                }

            }
        });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }
}
