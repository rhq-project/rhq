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
package org.rhq.coregui.client.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.admin.roles.RolesView;
import org.rhq.coregui.client.admin.storage.StorageNodeAdminView;
import org.rhq.coregui.client.admin.templates.AlertDefinitionTemplateTypeView;
import org.rhq.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.coregui.client.admin.templates.IgnoreResourceTypesView;
import org.rhq.coregui.client.admin.templates.MetricTemplateTypeView;
import org.rhq.coregui.client.admin.topology.AffinityGroupTableView;
import org.rhq.coregui.client.admin.topology.AgentTableView;
import org.rhq.coregui.client.admin.topology.PartitionEventTableView;
import org.rhq.coregui.client.admin.topology.ServerTableView;
import org.rhq.coregui.client.admin.users.UsersView;
import org.rhq.coregui.client.components.FullHTMLPane;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.coregui.client.components.view.NavigationItem;
import org.rhq.coregui.client.components.view.NavigationSection;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The Administration top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class AdministrationView extends AbstractSectionedLeftNavigationView {

    public static final ViewName VIEW_ID = new ViewName("Administration", MSG.view_admin_administration());

    public static final ViewName SECTION_SECURITY_VIEW_ID = new ViewName("Security", MSG.view_admin_security());
    public static final ViewName SECTION_TOPOLOGY_VIEW_ID = new ViewName("Topology", MSG.view_admin_topology());
    public static final ViewName SECTION_CONFIGURATION_VIEW_ID = new ViewName("Configuration",
        MSG.common_title_configuration());
    public static final ViewName SECTION_CONTENT_VIEW_ID = new ViewName("Content", MSG.view_admin_content());

    // TODO these iframe page view ids should go away in favor of the gwt view page view_id, when available
    private static final ViewName PAGE_CONTENT_SOURCES_VIEW_ID = new ViewName("ContentSources",
        MSG.view_adminContent_contentSources(), IconEnum.CONTENT);
    private static final ViewName PAGE_REPOS_VIEW_ID = new ViewName("Repositories", MSG.common_title_repositories(),
        IconEnum.CONTENT);

    public AdministrationView() {
        super(VIEW_ID.getName());
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection securitySection = buildSecuritySection();
        sections.add(securitySection);

        NavigationSection topologySection = buildTopologySection();
        sections.add(topologySection);

        NavigationSection configurationSection = buildConfigurationSection();
        sections.add(configurationSection);

        NavigationSection contentSection = buildContentSection();
        sections.add(contentSection);

        return sections;
    }

    protected VLayout defaultView() {
        EnhancedVLayout vLayout = new EnhancedVLayout();
        vLayout.setWidth100();

        // TODO: Admin icon.
        TitleBar titleBar = new TitleBar(MSG.view_admin_administration(), IconEnum.ADMIN.getIcon24x24Path());
        vLayout.addMember(titleBar);

        ProductInfo productInfo = CoreGUI.get().getProductInfo();

        Label label = new Label(MSG.view_admin_landing(productInfo.getShortName()));
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    private NavigationSection buildSecuritySection() {
        NavigationItem usersItem = new NavigationItem(UsersView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new UsersView(getGlobalPermissions().contains(Permission.MANAGE_SECURITY));
            }
        });

        NavigationItem rolesItem = new NavigationItem(RolesView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new RolesView(getGlobalPermissions().contains(Permission.MANAGE_SECURITY));
            }
        });

        return new NavigationSection(SECTION_SECURITY_VIEW_ID, usersItem, rolesItem);
    }

    private NavigationSection buildTopologySection() {
        ProductInfo productInfo = CoreGUI.get().getProductInfo();
        boolean isRHQ = (productInfo != null) && "RHQ".equals(productInfo.getShortName()); // use this to hide experimental features from product

        NavigationItem serversItem = new NavigationItem(ServerTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new ServerTableView(null, false);
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        NavigationItem storageNodesItem = new NavigationItem(StorageNodeAdminView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new StorageNodeAdminView();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        NavigationItem agentsItem = new NavigationItem(AgentTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new AgentTableView(null, false);
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        NavigationItem affinityGroupsItem = new NavigationItem(AffinityGroupTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new AffinityGroupTableView();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        NavigationItem partitionEventsItem = new NavigationItem(PartitionEventTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new PartitionEventTableView(PartitionEventTableView.VIEW_ID.getTitle());
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        // Arrays.asList returns a list with a fixed size, therefore there is the wrapping ArrayList
        List<NavigationItem> navigationItems = new ArrayList<NavigationItem>(Arrays.asList(serversItem,
            storageNodesItem, agentsItem, affinityGroupsItem, partitionEventsItem));

        NavigationSection topologyRegion = new NavigationSection(SECTION_TOPOLOGY_VIEW_ID,
            navigationItems.toArray(new NavigationItem[] {}));
        return topologyRegion;
    }

    private NavigationSection buildConfigurationSection() {
        NavigationItem systemSettingsItem = new NavigationItem(SystemSettingsView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new SystemSettingsView();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));
        systemSettingsItem.setRefreshRequired(true); // refresh so it always reloads the latest settings

        NavigationItem alertTemplatesItem = new NavigationItem(AlertDefinitionTemplateTypeView.VIEW_ID,
            new ViewFactory() {
                public Canvas createView() {
                    return new AlertDefinitionTemplateTypeView();
                }
            });
        alertTemplatesItem.setRefreshRequired(true); // we always need a new page

        NavigationItem driftTemplatesItem = new NavigationItem(DriftDefinitionTemplateTypeView.VIEW_ID,
            new ViewFactory() {
                public Canvas createView() {
                    return new DriftDefinitionTemplateTypeView();
                }
            });
        driftTemplatesItem.setRefreshRequired(true); // we always need a new page

        NavigationItem metricTemplatesItem = new NavigationItem(MetricTemplateTypeView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new MetricTemplateTypeView();
            }
        });
        metricTemplatesItem.setRefreshRequired(true); // we always need a new page

        NavigationItem ignoreResourceTypesItem = new NavigationItem(IgnoreResourceTypesView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new IgnoreResourceTypesView();
            }
        });
        ignoreResourceTypesItem.setRefreshRequired(true); // we always need a new page

        NavigationItem downloadsItem = new NavigationItem(DownloadsView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new DownloadsView();
            }
        });

        NavigationItem agentPluginsItem = new NavigationItem(AgentPluginTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new AgentPluginTableView();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        NavigationItem serverPluginsItem = new NavigationItem(ServerPluginTableView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new ServerPluginTableView();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_SETTINGS));

        return new NavigationSection(SECTION_CONFIGURATION_VIEW_ID, systemSettingsItem, alertTemplatesItem,
            driftTemplatesItem, metricTemplatesItem, ignoreResourceTypesItem, downloadsItem, agentPluginsItem,
            serverPluginsItem);
    }

    private NavigationSection buildContentSection() {
        NavigationItem contentSourcesItem = new NavigationItem(PAGE_CONTENT_SOURCES_VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane("/portal/rhq/content/listContentProviders-plain.xhtml");
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_REPOSITORIES));

        NavigationItem reposItem = new NavigationItem(PAGE_REPOS_VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane("/portal/rhq/content/listRepos-plain.xhtml");
            }
        });

        return new NavigationSection(SECTION_CONTENT_VIEW_ID, contentSourcesItem, reposItem);
    }
}
