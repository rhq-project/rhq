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
package org.rhq.enterprise.gui.coregui.client.report;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform.PlatformPortletView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.OperationHistoryView;
import org.rhq.enterprise.gui.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TaggedView;

/**
 * The Reports top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ReportTopView extends AbstractSectionedLeftNavigationView {
    public static final String VIEW_ID = "Reports";

    private static final String SUBSYSTEMS_SECTION_VIEW_ID = "Subsystems";
    private static final String INVENTORY_SECTION_VIEW_ID = "Inventory";

    public ReportTopView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID);
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection subsystemsSection = buildSubsystemsSection();
        sections.add(subsystemsSection);

        NavigationSection inventorySection = buildInventorySection();
        sections.add(inventorySection);

        return sections;
    }

    @Override
    protected HTMLFlow defaultView() {
        String contents = "<h1>" + MSG.view_reportsTop_title() + "</h1>\n" + MSG.view_reportsTop_description();
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private NavigationSection buildSubsystemsSection() {
        NavigationItem tagItem = new NavigationItem(TaggedView.VIEW_ID, "global/Cloud_16.png", new ViewFactory() {
            public Canvas createView() {
                return new TaggedView(extendLocatorId("Tag"));
            }
        });

        NavigationItem suspectMetricsItem = new NavigationItem(MeasurementOOBView.VIEW_ID,
            "subsystems/monitor/Monitor_failed_16.png", new ViewFactory() {
            public Canvas createView() {
                return new MeasurementOOBView(extendLocatorId("SuspectMetrics"));
            }
        });

        NavigationItem recentConfigurationChangesItem = new NavigationItem(ConfigurationHistoryView.VIEW_ID,
            "subsystems/configure/Configure_16.png", new ViewFactory() {
            public Canvas createView() {
                return new ConfigurationHistoryView(extendLocatorId("ConfigHistory"));
            }
        });

        NavigationItem recentOperationsItem = new NavigationItem(OperationHistoryView.VIEW_ID,
            "subsystems/control/Operation_16.png", new ViewFactory() {
            public Canvas createView() {
                return new OperationHistoryView(extendLocatorId("RecentOps"));
            }
        });

        NavigationItem recentAlertsItem = new NavigationItem(AlertHistoryView.SUBSYSTEM_VIEW_ID,
            "subsystems/alert/Alert_LOW_16.png", new ViewFactory() {
            public Canvas createView() {
                return new AlertHistoryView(extendLocatorId("RecentAlerts"));
            }
        });

        NavigationItem alertDefinitionsItem = new NavigationItem("AlertDefinitions",
            "subsystems/alert/Alerts_16.png", new ViewFactory() {
            public Canvas createView() {
                return null; // TODO: mazz
            }
        });

        return new NavigationSection(SUBSYSTEMS_SECTION_VIEW_ID, tagItem, suspectMetricsItem,
            recentConfigurationChangesItem, recentOperationsItem, recentAlertsItem, alertDefinitionsItem);
    }

    private NavigationSection buildInventorySection() {
        NavigationItem
            inventorySummaryItem = new NavigationItem("InventorySummary", "subsystems/inventory/Inventory_16.png",
            new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId("InventorySummary"),
                    "/rhq/admin/report/resourceInstallReport-body.xhtml");
            }
        });

        NavigationItem platformSystemInfoItem = new NavigationItem(PlatformPortletView.VIEW_ID, "types/Platform_up_16.png",
            new ViewFactory() {
            public Canvas createView() {
                return new PlatformPortletView(extendLocatorId("Platforms"));
            }
        });

        return new NavigationSection(INVENTORY_SECTION_VIEW_ID, inventorySummaryItem, platformSystemInfoItem);
    }
}