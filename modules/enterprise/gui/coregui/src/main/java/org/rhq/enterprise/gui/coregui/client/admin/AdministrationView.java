/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can retribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is tributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.admin.agent.install.RemoteAgentInstallView;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.admin.templates.ResourceTypeTreeView;
import org.rhq.enterprise.gui.coregui.client.admin.users.UsersView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;

/**
 * The Administration top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class AdministrationView extends AbstractSectionedLeftNavigationView {
    public static final String VIEW_ID = "Administration";

    private static final String SECTION_SECURITY_VIEW_ID = "Security";
    private static final String SECTION_TOPOLOGY_VIEW_ID = "Topology";
    private static final String SECTION_CONFIGURATION_VIEW_ID = "Configuration";

    // TODO these iframe page view ids should go away in favor of the gwt view page view_id, when available
    private static final String PAGE_SERVERS_VIEW_ID = "Servers";
    private static final String PAGE_AGENTS_VIEW_ID = "Agents";
    private static final String PAGE_AFFINITY_GROUPS_VIEW_ID = "AffinityGroups";
    private static final String PAGE_PARTITION_EVENTS_VIEW_ID = "PartitionEvents";

    private static final String PAGE_SYSTEM_SETTINGS_VIEW_ID = "SystemSettings";
    private static final String PAGE_TEMPLATES_VIEW_ID = "Templates";
    private static final String PAGE_DOWNLOADS_VIEW_ID = "Downloads";
    private static final String PAGE_LICENSE_VIEW_ID = "License";
    private static final String PAGE_PLUGINS_VIEW_ID = "Plugins";

    public AdministrationView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID);
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

        return sections;
    }

    protected HTMLFlow defaultView() {
        String contents = "<h1>Administration</h1>\n"
            + "From this section, the RHQ global settings can be administered. This includes configuring \n"
            + "<a href=\"\">Security</a>, setting up <a href=\"\">Plugins</a> and various other stuff.";
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private NavigationSection buildSecuritySection() {
        NavigationItem usersItem = new NavigationItem(UsersView.VIEW_ID, "global/User_16.png",
            new ViewFactory() {
            public Canvas createView() {
                return new UsersView(extendLocatorId("Users"));
            }
        });

        NavigationItem rolesItem = new NavigationItem(RolesView.VIEW_ID, "global/Role_16.png",
            new ViewFactory() {
            public Canvas createView() {
                return new RolesView(extendLocatorId("Roles"));
            }
        });

        return new NavigationSection(SECTION_SECURITY_VIEW_ID, usersItem, rolesItem);
    }

    private NavigationSection buildTopologySection() {
        NavigationItem serversItem = new NavigationItem(PAGE_SERVERS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_SERVERS_VIEW_ID), "/rhq/ha/listServers-plain.xhtml?nomenu=true");
            }
        });

        NavigationItem agentsItem = new NavigationItem(PAGE_AGENTS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_AGENTS_VIEW_ID), "/rhq/ha/listAgents-plain.xhtml?nomenu=true");
            }
        });

        NavigationItem affinityGroupsItem = new NavigationItem(PAGE_AFFINITY_GROUPS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_AFFINITY_GROUPS_VIEW_ID),
                    "/rhq/ha/listAffinityGroups-plain.xhtml?nomenu=true");
            }
        });

        NavigationItem partitionEventsItem = new NavigationItem(PAGE_PARTITION_EVENTS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_PARTITION_EVENTS_VIEW_ID),
                    "/rhq/ha/listPartitionEvents-plain.xhtml?nomenu=true");
            }
        });

        NavigationItem remoteAgentInstallItem = new NavigationItem(RemoteAgentInstallView.VIEW_ID, "global/Agent_16.png",
            new ViewFactory() {
            public Canvas createView() {
                return new RemoteAgentInstallView(extendLocatorId("RemoteAgentInstall"));
            }
        });

        return new NavigationSection(SECTION_TOPOLOGY_VIEW_ID, serversItem, agentsItem, affinityGroupsItem,
            partitionEventsItem, remoteAgentInstallItem);
    }

    private NavigationSection buildConfigurationSection() {
        NavigationItem systemSettingsItem = new NavigationItem(PAGE_SYSTEM_SETTINGS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_SYSTEM_SETTINGS_VIEW_ID),
                    "/admin/config/Config.do?mode=edit&nomenu=true");
            }
        });

        NavigationItem templatesItem = new NavigationItem(PAGE_TEMPLATES_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new ResourceTypeTreeView(extendLocatorId(PAGE_TEMPLATES_VIEW_ID));
            }
        });

        NavigationItem downloadsItem = new NavigationItem(PAGE_DOWNLOADS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_DOWNLOADS_VIEW_ID),
                    "/rhq/admin/downloads-body.xhtml?nomenu=true");
            }
        });

        NavigationItem licenseItem = new NavigationItem(PAGE_LICENSE_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_LICENSE_VIEW_ID),
                    "/admin/license/LicenseAdmin.do?mode=view&nomenu=true");
            }
        });

        NavigationItem pluginsItem = new NavigationItem(PAGE_PLUGINS_VIEW_ID, null,
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId(PAGE_PLUGINS_VIEW_ID),
                    "/rhq/admin/plugin/plugin-list-plain.xhtml?nomenu=true");
            }
        });

        return new NavigationSection(SECTION_CONFIGURATION_VIEW_ID, systemSettingsItem, templatesItem, downloadsItem,
            licenseItem, pluginsItem);
    }
}
