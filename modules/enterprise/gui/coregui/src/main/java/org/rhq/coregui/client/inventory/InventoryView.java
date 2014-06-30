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
package org.rhq.coregui.client.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.NavigationItem;
import org.rhq.coregui.client.components.view.NavigationSection;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField;
import org.rhq.coregui.client.inventory.groups.ResourceGroupListView;
import org.rhq.coregui.client.inventory.groups.definitions.GroupDefinitionListView;
import org.rhq.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The Inventory top-level view.
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author Ian Springer
 */
public class InventoryView extends AbstractSectionedLeftNavigationView {
    public static final ViewName VIEW_ID = new ViewName("Inventory", MSG.common_title_inventory());

    // view IDs for Resources section
    public static final ViewName RESOURCES_SECTION_VIEW_ID = new ViewName("Resources", MSG.common_title_resources());

    public static final ViewName PAGE_ALL_RESOURCES = new ViewName("AllResources", MSG.view_inventory_allResources(),
        IconEnum.ALL_RESOURCES);
    public static final ViewName PAGE_PLATFORMS = new ViewName("Platforms", MSG.view_inventory_platforms(),
        IconEnum.PLATFORMS);
    public static final ViewName PAGE_SERVERS_TOP = new ViewName("ServersTop", MSG.view_inventory_serversTop(),
        IconEnum.SERVERS);
    public static final ViewName PAGE_SERVERS = new ViewName("Servers", MSG.view_inventory_servers(), IconEnum.SERVERS);
    public static final ViewName PAGE_SERVICES = new ViewName("Services", MSG.view_inventory_services(),
        IconEnum.SERVICES);
    public static final ViewName PAGE_IGNORED_RESOURCES = new ViewName("IgnoredResources",
        MSG.view_inventory_ignoredResources(), IconEnum.ALL_RESOURCES);
    public static final ViewName PAGE_UNAVAIL_SERVERS = new ViewName("UnavailableServers",
        MSG.view_inventory_unavailableServers(), IconEnum.UNAVAILABLE_SERVERS);

    // view IDs for Groups section
    public static final ViewName GROUPS_SECTION_VIEW_ID = new ViewName("Groups", MSG.view_inventory_groups());

    public static final ViewName PAGE_DYNAGROUP_DEFINITIONS = new ViewName("DynagroupDefinitions",
        MSG.view_inventory_dynagroupDefs(), IconEnum.DYNAGROUPS);
    public static final ViewName PAGE_ALL_GROUPS = new ViewName("AllGroups", MSG.view_inventory_allGroups(),
        IconEnum.ALL_GROUPS);
    public static final ViewName PAGE_COMPATIBLE_GROUPS = new ViewName("CompatibleGroups",
        MSG.common_title_compatibleGroups(), IconEnum.COMPATIBLE_GROUPS);
    public static final ViewName PAGE_MIXED_GROUPS = new ViewName("MixedGroups", MSG.common_title_mixedGroups(),
        IconEnum.MIXED_GROUPS);
    public static final ViewName PAGE_PROBLEM_GROUPS = new ViewName("ProblemGroups",
        MSG.view_inventory_problemGroups(), IconEnum.PROBLEM_GROUPS);

    private Set<Permission> globalPermissions;

    public InventoryView() {
        super(VIEW_ID.getName());
    }

    @Override
    protected void onInit() {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = (permissions != null) ? permissions : EnumSet.noneOf(Permission.class);
                InventoryView.super.onInit();
            }
        });
    }

    protected Canvas defaultView() {
        EnhancedVLayout vLayout = new EnhancedVLayout();
        vLayout.setWidth100();

        TitleBar titleBar = new TitleBar(MSG.common_title_inventory(), IconEnum.INVENTORY.getIcon24x24Path());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_inventory_sectionHelp());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
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

    private ResourceSearchView createResourceSearchView(ViewName viewName, Criteria initialCriteria) {
        return new ResourceSearchView(initialCriteria, viewName.getTitle(), viewName.getIcon().getIcon24x24Path());
    }

    private ResourceSearchView createIgnoredResourceSearchView(ViewName viewName, Criteria initialCriteria) {

        return new ResourceSearchView(initialCriteria, viewName.getTitle(),
          viewName.getIcon().getIcon24x24Path()) {

            @Override
            protected boolean shouldShowIgnoreButton() {
                return false;
            }

            @Override
            protected boolean shouldShowUnignoreButton() {
                return true;
            }
          };
    }

    private NavigationSection buildResourcesSection() {
        NavigationItem autodiscoveryQueueItem = new NavigationItem(ResourceAutodiscoveryView.VIEW_ID,
            new ViewFactory() {
                public Canvas createView() {
                    return new ResourceAutodiscoveryView();
                }
            }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));
        autodiscoveryQueueItem.setRefreshRequired(true);

        NavigationItem allResourcesItem = new NavigationItem(PAGE_ALL_RESOURCES, new ViewFactory() {
            public Canvas createView() {
                return createResourceSearchView(PAGE_ALL_RESOURCES, null);
            }
        });

        NavigationItem platformsItem = new NavigationItem(PAGE_PLATFORMS, new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.CATEGORY.propertyName(),
                    ResourceCategory.PLATFORM.name());
                return createResourceSearchView(PAGE_PLATFORMS, initialCriteria);
            }
        });

        NavigationItem topLevelServersItem = new NavigationItem(PAGE_SERVERS_TOP, new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.CATEGORY.propertyName(),
                    ResourceCategory.SERVER.name());
                initialCriteria.addCriteria(ResourceDatasource.FILTER_PARENT_CATEGORY, ResourceCategory.PLATFORM.name());
                return createResourceSearchView(PAGE_SERVERS_TOP, initialCriteria);
            }
        });

        NavigationItem serversItem = new NavigationItem(PAGE_SERVERS, new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.CATEGORY.propertyName(),
                    ResourceCategory.SERVER.name());
                return createResourceSearchView(PAGE_SERVERS, initialCriteria);
            }
        });

        NavigationItem servicesItem = new NavigationItem(PAGE_SERVICES, new ViewFactory() {
            public Canvas createView() {

                Criteria initialCriteria = new Criteria(ResourceDataSourceField.CATEGORY.propertyName(),
                    ResourceCategory.SERVICE.name());
                return createResourceSearchView(PAGE_SERVICES, initialCriteria);
            }
        });

        NavigationItem ignoredResourcesItem = new NavigationItem(PAGE_IGNORED_RESOURCES, new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.INVENTORY_STATUS.propertyName(),
                    InventoryStatus.IGNORED.name());
                initialCriteria.addCriteria(ResourceDataSourceField.PARENT_INVENTORY_STATUS.propertyName(),
                    InventoryStatus.COMMITTED.name());

                return createIgnoredResourceSearchView(PAGE_IGNORED_RESOURCES, initialCriteria);
            }
        });

        NavigationItem downServersItem = new NavigationItem(PAGE_UNAVAIL_SERVERS, new ViewFactory() {
            public Canvas createView() {
                Criteria initialCriteria = new Criteria(ResourceDataSourceField.AVAILABILITY.propertyName(),
                    AvailabilityType.DOWN.name());
                initialCriteria.addCriteria(ResourceDataSourceField.CATEGORY.propertyName(),
                    ResourceCategory.SERVER.name());

                return createResourceSearchView(PAGE_UNAVAIL_SERVERS, initialCriteria);
            }
        });

        return new NavigationSection(RESOURCES_SECTION_VIEW_ID, autodiscoveryQueueItem, allResourcesItem,
            platformsItem, topLevelServersItem, serversItem, servicesItem, ignoredResourcesItem, downServersItem);
    }

    private NavigationSection buildGroupsSection() {
        NavigationItem dynagroupDefinitionsItem = new NavigationItem(PAGE_DYNAGROUP_DEFINITIONS, new ViewFactory() {
            public Canvas createView() {
                GroupDefinitionListView table =  new GroupDefinitionListView();
                table.setTitleString(PAGE_DYNAGROUP_DEFINITIONS.getTitle());
                table.setTitleIcon(PAGE_DYNAGROUP_DEFINITIONS.getIcon().getIcon24x24Path() );
                return table;
            }
        }, this.globalPermissions.contains(Permission.MANAGE_INVENTORY));

        NavigationItem allGroupsItem = new NavigationItem(PAGE_ALL_GROUPS, new ViewFactory() {
            public Canvas createView() {
                return new ResourceGroupListView(null, PAGE_ALL_GROUPS.getTitle(), PAGE_ALL_GROUPS.getIcon().getIcon24x24Path());
            }
        });

        NavigationItem compatibleGroupsItem = new NavigationItem(PAGE_COMPATIBLE_GROUPS, new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view = new ResourceGroupListView(new Criteria(
                    ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.COMPATIBLE.name()),
                    PAGE_COMPATIBLE_GROUPS.getTitle(), PAGE_COMPATIBLE_GROUPS.getIcon().getIcon24x24Path());
                return view;
            }
        });

        NavigationItem mixedGroupsItem = new NavigationItem(PAGE_MIXED_GROUPS, new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view = new ResourceGroupListView(new Criteria(
                    ResourceGroupDataSourceField.CATEGORY.propertyName(), GroupCategory.MIXED.name()),
                    PAGE_MIXED_GROUPS.getTitle(), PAGE_MIXED_GROUPS.getIcon().getIcon24x24Path());
                return view;
            }
        });

        NavigationItem problemGroupsItem = new NavigationItem(PAGE_PROBLEM_GROUPS, new ViewFactory() {
            public Canvas createView() {
                ResourceGroupListView view =
                  new ResourceGroupListView(new Criteria("downMemberCount", "1"),
                    PAGE_PROBLEM_GROUPS.getTitle() , PAGE_PROBLEM_GROUPS.getIcon().getIcon24x24Path());
                view.setShowNewButton(false);
                return view;
            }
        });

        return new NavigationSection(GROUPS_SECTION_VIEW_ID, allGroupsItem, dynagroupDefinitionsItem,
            compatibleGroupsItem, mixedGroupsItem, problemGroupsItem);
    }
/*
    private class ResourceSearchViewWrapper extends ResourceSearchView implements HasViewName {

        private ViewName viewName;

        private final boolean showIgnoreButton;
        private final boolean showUnignoreButton;

        public ResourceSearchViewWrapper(ViewName viewName, Criteria criteria, String headerIcon) {
            this(true, false, viewName, criteria, headerIcon);
        }

        public ResourceSearchViewWrapper(boolean showIgnoredButton, boolean showUnignoreButton, ViewName viewName,
            Criteria criteria, String headerIcon) {
            super(criteria, viewName.getTitle(), headerIcon);
            this.viewName = viewName;
            this.showIgnoreButton = showIgnoredButton;
            this.showUnignoreButton = showUnignoreButton;
        }

        @Override
        public ViewName getViewName() {
            return viewName;
        }

        @Override
        protected boolean shouldShowIgnoreButton() {
            return this.showIgnoreButton;
        }

        @Override
        protected boolean shouldShowUnignoreButton() {
            return this.showUnignoreButton;
        }
    }*/
}
