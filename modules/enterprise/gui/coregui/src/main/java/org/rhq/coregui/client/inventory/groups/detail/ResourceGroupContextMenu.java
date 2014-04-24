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
package org.rhq.coregui.client.inventory.groups.detail;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTreeDatasource.AutoGroupTreeNode;
import org.rhq.coregui.client.inventory.resource.detail.ResourceTreeView;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceGroupContextMenu extends Menu {

    protected Messages MSG = CoreGUI.getMessages();

    private ResourceGroupComposite groupComposite;
    private ResourceGroup group;
    private ResourceType groupMemberType;

    private boolean isAutoCluster = false;
    private boolean isAutoGroup = false;

    public void showContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        final ResourceGroup group) {
        // we need the group composite to access permissions for context menu authz, so get it now
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(group.getId());

        // for autoclusters and private groups (autogroups) we need to add more criteria
        isAutoCluster = (null != group.getClusterResourceGroup());
        isAutoGroup = (null != group.getSubject());

        if (isAutoCluster) {
            criteria.addFilterVisible(false);

        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_group_detail_failLoadComp(String.valueOf(group.getId())), caught);
                }

                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result.isEmpty()) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_group_detail_failLoadComp(String.valueOf(group.getId())));
                    } else {
                        showContextMenu(treeView, treeGrid, node, result.get(0));
                    }
                }
            });
    }

    public void showContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        ResourceGroupComposite groupComposite) {
        this.groupComposite = groupComposite;
        group = groupComposite.getResourceGroup();

        // [BZ 817604] If the group type has changed to mixed we can't show a context menu.
        if (GroupCategory.MIXED == group.getGroupCategory()) {
            CoreGUI.goToView(LinkManager.getResourceGroupLink(group.getId()));
            return;
        }

        groupMemberType = group.getResourceType();
        isAutoCluster = (null != group.getClusterResourceGroup());
        isAutoGroup = (null != group.getSubject());

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            groupMemberType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.operations, ResourceTypeRepository.MetadataType.children,
                ResourceTypeRepository.MetadataType.pluginConfigurationDefinition,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                    groupMemberType = type;
                    buildResourceGroupContextMenu(treeView, treeGrid, node, group, type);
                    showContextMenu();
                }
            });
    }

    private void buildResourceGroupContextMenu(final VLayout treeView, final TreeGrid treeGrid, final TreeNode node,
        final ResourceGroup group, final ResourceType resourceType) {
        // name
        setItems(new MenuItem(group.getName()));

        // type name
        addItem(new MenuItem("Type: " + resourceType.getName()));

        // Mixed group refresh is not needed as there is only a single top node. Compat group
        // refresh makes sense after a group membership change but we already perform a CoreGUI refresh to
        // reset the whole detail view after that user action. So, only support refresh for autogroup nodes
        // in the resource tree.
        if (node instanceof AutoGroupTreeNode) {
            // separator
            addItem(new MenuItemSeparator());

            // refresh node
            MenuItem refresh = new MenuItem(MSG.common_button_refresh());
            refresh.addClickHandler(new ClickHandler() {

                public void onClick(MenuItemClickEvent event) {
                    // refresh the tree and detail
                    ((ResourceTreeView) treeView).contextMenuRefresh(treeGrid, node, false);
                }
            });
            addItem(refresh);
        }

        // separator
        addItem(new MenuItemSeparator());

        // plugin config
        MenuItem pluginConfiguration = new MenuItem(MSG.view_tabs_common_connectionSettings());
        boolean pluginConfigEnabled = resourceType.getPluginConfigurationDefinition() != null;
        pluginConfiguration.setEnabled(pluginConfigEnabled);
        if (pluginConfigEnabled) {
            pluginConfiguration.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getEntityTabLink(EntityContext.forGroup(group), "Inventory",
                        "ConnectionSettings"));
                }
            });
        }
        addItem(pluginConfiguration);

        // resource config
        MenuItem resourceConfiguration = new MenuItem(MSG.view_tree_common_contextMenu_resourceConfiguration());
        boolean resourceConfigEnabled = groupComposite.getResourcePermission().isConfigureRead()
            && resourceType.getResourceConfigurationDefinition() != null;
        resourceConfiguration.setEnabled(resourceConfigEnabled);
        if (resourceConfigEnabled) {
            resourceConfiguration.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getEntityTabLink(EntityContext.forGroup(group), "Configuration",
                        "Current"));
                }
            });
        }
        addItem(resourceConfiguration);

        // separator
        addItem(new MenuItemSeparator());

        // Operations Menu
        MenuItem operations = new MenuItem(MSG.common_title_operations());
        boolean operationsEnabled = (groupComposite.getResourcePermission().isControl()
            && null != resourceType.getOperationDefinitions() && !resourceType.getOperationDefinitions().isEmpty());
        operations.setEnabled(operationsEnabled);
        if (operationsEnabled) {
            Menu opSubMenu = new Menu();
            //sort the display items alphabetically
            TreeSet<String> ordered = new TreeSet<String>();
            Map<String, OperationDefinition> definitionMap = new HashMap<String, OperationDefinition>();
            for (OperationDefinition o : resourceType.getOperationDefinitions()) {
                ordered.add(o.getDisplayName());
                definitionMap.put(o.getDisplayName(), o);
            }

            for (String displayName : ordered) {
                final OperationDefinition operationDefinition = definitionMap.get(displayName);

                MenuItem operationItem = new MenuItem(operationDefinition.getDisplayName());
                operationItem.addClickHandler(new ClickHandler() {
                    public void onClick(MenuItemClickEvent event) {
                        String viewPath = LinkManager.getEntityTabLink(EntityContext.forGroup(group),
                            ResourceDetailView.Tab.Operations.NAME, "Schedules")
                            + "/0/"
                            + operationDefinition.getId();
                        CoreGUI.goToView(viewPath);
                    }
                });
                opSubMenu.addItem(operationItem);
            }
            operations.setSubmenu(opSubMenu);
        }
        addItem(operations);
    }

}
