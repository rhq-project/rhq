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
package org.rhq.enterprise.gui.coregui.client.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;

/**
 * The Inventory top-level view.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author Ian Springer
 */
public class InventoryView extends AbstractSectionedLeftNavigationView {
    public static final String VIEW_ID = "Inventory";

    // view IDs for Resources section
    private static final String RESOURCES_SECTION_VIEW_ID = "Resources";

    private static final String PAGE_AUTODISCOVERY_QUEUE = "AutodiscoveryQueue";
    private static final String PAGE_ALL_RESOURCES = "AllResources";
    private static final String PAGE_PLATFORMS = "Platforms";
    private static final String PAGE_SERVERS = "Servers";
    private static final String PAGE_SERVICES = "Services";
    private static final String PAGE_DOWN_SERVERS = "DownServers";

    // view IDs for Groups section
    private static final String GROUPS_SECTION_VIEW_ID = "Groups";

    private static final String PAGE_DYNAGROUP_DEFINITIONS = "DynagroupDefinitions";
    private static final String PAGE_ALL_GROUPS = "AllGroups";
    private static final String PAGE_COMPATIBLE_GROUPS = "CompatibleGroups";
    private static final String PAGE_MIXED_GROUPS = "MixedGroups";
    private static final String PAGE_PROBLEM_GROUPS = "ProblemGroups";

    private Set<Permission> globalPermissions;

    public InventoryView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID);
    }

    @Override
    protected void onInit() {
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_cannotGetGlobalPerms(), caught);
                globalPermissions = EnumSet.noneOf(Permission.class);
                InventoryView.super.onInit();
            }

            @Override
            public void onSuccess(Set<Permission> result) {
                globalPermissions = result;
                InventoryView.super.onInit();
            }
        });
    }

    protected Canvas defaultView() {
        String contents = "<h1>" + MSG.common_title_inventory() + "</h1>\n" + MSG.view_sectionHelp();
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection resourcesSection = buildResourcesSection();
        sections.add(resourcesSection);

        NavigationSection groupsSection = buildGroupsSection();
        sections.add(groupsSection);

        return sections;
    }

    private NavigationSection buildResourcesSection() {
        NavigationItem autodiscoveryQueueItem = new NavigationItem(PAGE_AUTODISCOVERY_QUEUE, "global/Recent_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new ResourceAutodiscoveryView(extendLocatorId(PAGE_AUTODISCOVERY_QUEUE));
                }
            }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));

        // TODO: Specify an icon for this item.
        NavigationItem allResourcesItem = new NavigationItem(PAGE_ALL_RESOURCES, null, new ViewFactory() {
            public Canvas createView() {
                return new ResourceSearchView(extendLocatorId(PAGE_ALL_RESOURCES), null, PAGE_ALL_RESOURCES,
                    "types/Platform_up_24.png", "types/Server_up_24.png", "types/Service_up_24.png");
            }
        });

        NavigationItem platformsItem = new NavigationItem(PAGE_PLATFORMS, "types/Platform_up_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new ResourceSearchView(extendLocatorId(PAGE_PLATFORMS), new Criteria(
                        ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.PLATFORM.name()),
                        PAGE_PLATFORMS, "types/Platform_up_24.png");
                }
            });

        NavigationItem serversItem = new NavigationItem(PAGE_SERVERS, "types/Server_up_16.png", new ViewFactory() {
            public Canvas createView() {
                return new ResourceSearchView(extendLocatorId(PAGE_SERVERS), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER.name()), PAGE_SERVERS,
                    "types/Server_up_24.png");
            }
        });

        NavigationItem servicesItem = new NavigationItem(PAGE_SERVICES, "types/Service_up_16.png", new ViewFactory() {
            public Canvas createView() {
                return new ResourceSearchView(extendLocatorId(PAGE_SERVICES), new Criteria(
                    ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVICE.name()), PAGE_SERVICES,
                    "types/Service_up_24.png");
            }
        });

        NavigationItem downServersItem = new NavigationItem(PAGE_DOWN_SERVERS, "types/Server_down_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    Criteria criteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                        AvailabilityType.DOWN.name());
                    criteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(), ResourceCategory.SERVER
                        .name());
                    // TODO (ips, 10/28/10): Should we include down platforms too?
                    return new ResourceSearchView(extendLocatorId(PAGE_DOWN_SERVERS), criteria, "Down Servers");
                }
            });

        return new NavigationSection(RESOURCES_SECTION_VIEW_ID, autodiscoveryQueueItem, allResourcesItem,
            platformsItem, serversItem, servicesItem, downServersItem);
    }

    private NavigationSection buildGroupsSection() {
        NavigationItem dynagroupDefinitionsItem = new NavigationItem(PAGE_DYNAGROUP_DEFINITIONS,
            "types/GroupDefinition_16.png", new ViewFactory() {
                public Canvas createView() {
                    // TODO: Do we have a 24x24 groupdef icon?
                    return new GroupDefinitionListView(extendLocatorId(PAGE_DYNAGROUP_DEFINITIONS),
                        "types/GroupDefinition_16.png");
                }
            }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));

        NavigationItem allGroupsItem = new NavigationItem(PAGE_ALL_GROUPS, "types/Group_up_16.png", new ViewFactory() {
            public Canvas createView() {
                return new ResourceGroupListView(extendLocatorId(PAGE_ALL_GROUPS), null, "All Groups",
                    "types/Cluster_up_24.png", "types/Group_up_24.png");
            }
        });

        NavigationItem compatibleGroupsItem = new NavigationItem(PAGE_COMPATIBLE_GROUPS, "types/Cluster_up_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new ResourceGroupListView(extendLocatorId(PAGE_COMPATIBLE_GROUPS), new Criteria(
                        ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.COMPATIBLE.name()), MSG
                        .common_title_compatibleGroups(), "types/Cluster_up_24.png");
                }
            });

        NavigationItem mixedGroupsItem = new NavigationItem(PAGE_MIXED_GROUPS, "types/Group_up_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    return new ResourceGroupListView(extendLocatorId(PAGE_MIXED_GROUPS), new Criteria(
                        ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.MIXED.name()), MSG
                        .common_title_mixedGroups(), "types/Group_up_24.png");
                }
            });

        NavigationItem problemGroupsItem = new NavigationItem(PAGE_PROBLEM_GROUPS, "types/Cluster_down_16.png",
            new ViewFactory() {
                public Canvas createView() {
                    // TODO: There is no underlying support for this criteria. Also, there should not be an active New
                    //       button on this page.
                    return new ResourceGroupListView(extendLocatorId(PAGE_PROBLEM_GROUPS), new Criteria("availability",
                        "down"), MSG.view_inventory_problemGroups(), "types/Cluster_down_16.png");
                }
            });

        return new NavigationSection(GROUPS_SECTION_VIEW_ID, dynagroupDefinitionsItem, allGroupsItem,
            compatibleGroupsItem, mixedGroupsItem, problemGroupsItem);
    }
}
