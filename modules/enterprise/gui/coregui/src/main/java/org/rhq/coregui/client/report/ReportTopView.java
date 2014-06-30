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
package org.rhq.coregui.client.report;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.alert.AlertHistoryView;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.coregui.client.components.view.NavigationItem;
import org.rhq.coregui.client.components.view.NavigationSection;
import org.rhq.coregui.client.components.view.ViewFactory;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.coregui.client.drift.DriftHistoryView;
import org.rhq.coregui.client.drift.SubsystemResourceDriftView;
import org.rhq.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;
import org.rhq.coregui.client.operation.OperationHistoryView;
import org.rhq.coregui.client.report.alert.SubsystemRecentAlertsView;
import org.rhq.coregui.client.report.configuration.SubsystemConfigurationHistoryListView;
import org.rhq.coregui.client.report.inventory.DriftComplianceReport;
import org.rhq.coregui.client.report.inventory.ResourceInstallReport;
import org.rhq.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.coregui.client.report.operation.SubsystemOperationHistoryListView;
import org.rhq.coregui.client.report.tag.TaggedView;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The Reports top-level view.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ReportTopView extends AbstractSectionedLeftNavigationView {

    public static final ViewName VIEW_ID = new ViewName("Reports", MSG.view_reportsTop_title());

    public static final ViewName SECTION_SUBSYSTEMS_VIEW_ID = new ViewName("Subsystems", MSG.view_reports_subsystems());
    public static final ViewName SECTION_INVENTORY_VIEW_ID = new ViewName("Inventory", MSG.common_title_inventory());

    public ReportTopView() {
        super(VIEW_ID.getName());
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
    protected VLayout defaultView() {
        EnhancedVLayout vLayout = new EnhancedVLayout();
        vLayout.setWidth100();

        TitleBar titleBar = new TitleBar(MSG.view_reportsTop_title(), IconEnum.REPORT.getIcon24x24Path());
        vLayout.addMember(titleBar);

        Label label = new Label(MSG.view_reportsTop_description());
        label.setPadding(10);
        vLayout.addMember(label);

        return vLayout;
    }

    private NavigationSection buildSubsystemsSection() {
        NavigationItem tagItem = new NavigationItem(TaggedView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new TaggedView();
            }
        });

        NavigationItem suspectMetricsItem = new NavigationItem(MeasurementOOBView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new MeasurementOOBView();
            }
        });

        NavigationItem recentConfigurationChangesItem = new NavigationItem(
            ResourceConfigurationHistoryListView.VIEW_ID, new ViewFactory() {
                public Canvas createView() {
                    return new SubsystemConfigurationHistoryListView(getGlobalPermissions().contains(
                        Permission.MANAGE_INVENTORY));
                }
            });

        NavigationItem recentOperationsItem = new NavigationItem(OperationHistoryView.SUBSYSTEM_VIEW_ID,
            new ViewFactory() {
                public Canvas createView() {
                    SubsystemOperationHistoryListView v = new SubsystemOperationHistoryListView(getGlobalPermissions().contains(
                        Permission.MANAGE_INVENTORY));
                    v.setShowHeader(false);
                    return v;
                }
            });

        NavigationItem recentAlertsItem = new NavigationItem(AlertHistoryView.SUBSYSTEM_VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                SubsystemRecentAlertsView v = new SubsystemRecentAlertsView(getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));
                v.setShowHeader(false);
                return v;
            }
        });

        NavigationItem alertDefinitionsItem = new NavigationItem(AlertDefinitionReportView.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new AlertDefinitionReportView();
            }
        });

        NavigationItem recentDriftsItem = new NavigationItem(DriftHistoryView.SUBSYSTEM_VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new SubsystemResourceDriftView(getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));
            }
        });

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            return new NavigationSection(SECTION_SUBSYSTEMS_VIEW_ID, tagItem, suspectMetricsItem,
                recentConfigurationChangesItem, recentOperationsItem, recentAlertsItem, alertDefinitionsItem,
                recentDriftsItem);
        } else {
            return new NavigationSection(SECTION_SUBSYSTEMS_VIEW_ID, suspectMetricsItem,
                recentConfigurationChangesItem, recentOperationsItem, recentAlertsItem, alertDefinitionsItem,
                recentDriftsItem);
        }
    }

    private NavigationSection buildInventorySection() {
        NavigationItem inventorySummaryItem = new NavigationItem(ResourceInstallReport.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new ResourceInstallReport();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        NavigationItem platformSystemInfoItem = new NavigationItem(PlatformSummaryPortlet.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new PlatformSummaryPortlet(true);
            }
        });

        NavigationItem driftComplianceItem = new NavigationItem(DriftComplianceReport.VIEW_ID, new ViewFactory() {
            public Canvas createView() {
                return new DriftComplianceReport();
            }
        }, getGlobalPermissions().contains(Permission.MANAGE_INVENTORY));

        return new NavigationSection(SECTION_INVENTORY_VIEW_ID, inventorySummaryItem, platformSystemInfoItem,
            driftComplianceItem);
    }
}
