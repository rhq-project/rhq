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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceTypeTreeView extends LocatableVLayout {

    public ResourceTypeTreeView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();

        SectionStack sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);

        ListGrid platformsList = new CustomResourceTypeListGrid(extendLocatorId("platformsList"));
        SectionStackSection platforms = new SectionStackSection("Platforms");
        platforms.setExpanded(true);
        platforms.addItem(platformsList);

        ListGrid platformServicesList = new CustomResourceTypeListGrid(extendLocatorId("platformServicesList"));
        SectionStackSection platformServices = new SectionStackSection("Platform Services");
        platformServices.setExpanded(true);
        platformServices.addItem(platformServicesList);

        TreeGrid serversTreeGrid = new CustomResourceTypeTreeGrid(extendLocatorId("serversTree"));
        SectionStackSection servers = new SectionStackSection("Servers");
        servers.setExpanded(true);
        servers.addItem(serversTreeGrid);

        sectionStack.addSection(platforms);
        sectionStack.addSection(platformServices);
        sectionStack.addSection(servers);

        addMember(sectionStack);

        new ResourceTypeTreeNodeBuilder(platformsList, platformServicesList, serversTreeGrid);
    }

    private static void editAlertTemplate(Record record) {
        SC.say("Alert Template : " //
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_NAME)
            + "==>"
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN)
            + "==>"
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID));
    }

    private static void editMetricTemplate(Record record) {
        // http://localhost:7080/admin/platform/monitor/Config.do?nomenu=true&mode=configure&id=#####&type=#####
        SC.say("Metric Template: " //
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_NAME)
            + "==>"
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN)
            + "==>"
            + record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID));
    }

    public static class CustomResourceTypeListGrid extends LocatableListGrid {
        private HLayout rollOverCanvas;
        private ListGridRecord rollOverRecord;

        public CustomResourceTypeListGrid(String locatorId) {
            super(locatorId);

            setWrapCells(true);
            setFixedRecordHeights(false);
            setShowRollOverCanvas(true);
            setEmptyMessage("Loading...");
            setSelectionType(SelectionStyle.NONE);

            final ListGridField name = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME, "Name");
            final ListGridField plugin = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN, "Plugin");
            final ListGridField category = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY, "Category");
            final ListGridField enabledAlertTemplates = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_ALERT_TEMPLATES, "Enabled Alert Templates");
            final ListGridField disabledAlertTemplates = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_ALERT_TEMPLATES, "Disabled Alert Templates");
            final ListGridField enabledMetricTemplates = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_METRIC_TEMPLATES, "Enabled Metric Templates");
            final ListGridField disabledMetricTemplates = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_METRIC_TEMPLATES, "Disabled Metric Templates");

            plugin.setHidden(true);
            category.setHidden(true);

            name.setWidth("40%");
            plugin.setWidth("10%");
            category.setWidth("10%");
            enabledAlertTemplates.setWidth("10%");
            disabledAlertTemplates.setWidth("10%");
            enabledMetricTemplates.setWidth("10%");
            disabledMetricTemplates.setWidth("10%");

            enabledAlertTemplates.setPrompt("Number of alert templates that are enabled on this resource type");
            disabledAlertTemplates
                .setPrompt("Number of alert templates that are created but disabled on this resource type");
            enabledMetricTemplates
                .setPrompt("Number of metric schedules that are enabled by default on this resource type");
            disabledMetricTemplates
                .setPrompt("Number of metric schedules that are disabled by default on this resource type");

            setFields(name, plugin, category, enabledAlertTemplates, disabledAlertTemplates, enabledMetricTemplates,
                disabledMetricTemplates);
        }

        @Override
        protected Canvas getRollOverCanvas(Integer rowNum, Integer colNum) {
            rollOverRecord = this.getRecord(rowNum);

            if (rollOverCanvas == null) {
                rollOverCanvas = new HLayout(3);
                rollOverCanvas.setSnapTo("TR");
                rollOverCanvas.setWidth(50);
                rollOverCanvas.setHeight(22);

                ImgButton metricTemplateImg = new ImgButton();
                metricTemplateImg.setShowDown(false);
                metricTemplateImg.setShowRollOver(false);
                metricTemplateImg.setLayoutAlign(Alignment.CENTER);
                metricTemplateImg.setSrc("subsystems/monitor/Edit_Metric.png");
                metricTemplateImg.setPrompt("Edit Metric Template");
                metricTemplateImg.setHeight(16);
                metricTemplateImg.setWidth(16);
                metricTemplateImg.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        editMetricTemplate(rollOverRecord);
                    }
                });

                ImgButton alertTemplateImg = new ImgButton();
                alertTemplateImg.setShowDown(false);
                alertTemplateImg.setShowRollOver(false);
                alertTemplateImg.setLayoutAlign(Alignment.CENTER);
                alertTemplateImg.setSrc("subsystems/alert/Edit_Alert.png");
                alertTemplateImg.setPrompt("Edit Alert Template");
                alertTemplateImg.setHeight(16);
                alertTemplateImg.setWidth(16);
                alertTemplateImg.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        editAlertTemplate(rollOverRecord);
                    }
                });

                rollOverCanvas.addMember(metricTemplateImg);
                rollOverCanvas.addMember(alertTemplateImg);
            }
            return rollOverCanvas;
        }
    }

    public static class CustomResourceTypeTreeGrid extends LocatableTreeGrid {
        private HLayout rollOverCanvas;
        private ListGridRecord rollOverRecord;

        public CustomResourceTypeTreeGrid(String locatorId) {
            super(locatorId);

            setWrapCells(true);
            setFixedRecordHeights(false);
            setShowRollOverCanvas(true);
            setEmptyMessage("Loading...");
            setSelectionType(SelectionStyle.NONE);
            setAnimateFolders(false);

            final TreeGridField name = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME, "Name");
            final TreeGridField plugin = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN, "Plugin");
            final TreeGridField category = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY, "Category");
            final TreeGridField enabledAlertTemplates = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_ALERT_TEMPLATES, "Enabled Alert Templates");
            final TreeGridField disabledAlertTemplates = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_ALERT_TEMPLATES, "Disabled Alert Templates");
            final TreeGridField enabledMetricTemplates = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_METRIC_TEMPLATES, "Enabled Metric Templates");
            final TreeGridField disabledMetricTemplates = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_METRIC_TEMPLATES, "Disabled Metric Templates");

            name.setWidth("40%");
            plugin.setWidth("10%");
            category.setWidth("10%");
            enabledAlertTemplates.setWidth("10%");
            disabledAlertTemplates.setWidth("10%");
            enabledMetricTemplates.setWidth("10%");
            disabledMetricTemplates.setWidth("10%");

            enabledAlertTemplates.setPrompt("Number of alert templates that are enabled on this resource type");
            disabledAlertTemplates
                .setPrompt("Number of alert templates that are created but disabled on this resource type");
            enabledMetricTemplates
                .setPrompt("Number of metric schedules that are enabled by default on this resource type");
            disabledMetricTemplates
                .setPrompt("Number of metric schedules that are disabled by default on this resource type");

            setFields(name, plugin, category, enabledAlertTemplates, disabledAlertTemplates, enabledMetricTemplates,
                disabledMetricTemplates);
        }

        @Override
        protected Canvas getRollOverCanvas(Integer rowNum, Integer colNum) {
            rollOverRecord = this.getRecord(rowNum);

            if (rollOverCanvas == null) {
                rollOverCanvas = new HLayout(3);
                rollOverCanvas.setSnapTo("TR");
                rollOverCanvas.setWidth(50);
                rollOverCanvas.setHeight(22);

                ImgButton metricTemplateImg = new ImgButton();
                metricTemplateImg.setShowDown(false);
                metricTemplateImg.setShowRollOver(false);
                metricTemplateImg.setLayoutAlign(Alignment.CENTER);
                metricTemplateImg.setSrc("subsystems/monitor/Edit_Metric.png");
                metricTemplateImg.setPrompt("Edit Metric Template");
                metricTemplateImg.setHeight(16);
                metricTemplateImg.setWidth(16);
                metricTemplateImg.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        editMetricTemplate(rollOverRecord);
                    }
                });

                ImgButton alertTemplateImg = new ImgButton();
                alertTemplateImg.setShowDown(false);
                alertTemplateImg.setShowRollOver(false);
                alertTemplateImg.setLayoutAlign(Alignment.CENTER);
                alertTemplateImg.setSrc("subsystems/alert/Edit_Alert.png");
                alertTemplateImg.setPrompt("Edit Alert Template");
                alertTemplateImg.setHeight(16);
                alertTemplateImg.setWidth(16);
                alertTemplateImg.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        editAlertTemplate(rollOverRecord);
                    }
                });

                rollOverCanvas.addMember(metricTemplateImg);
                rollOverCanvas.addMember(alertTemplateImg);
            }
            return rollOverCanvas;
        }

        @Override
        protected String getIcon(Record record, boolean defaultState) {

            if (record instanceof TreeNode) {
                if (record instanceof ResourceTypeTreeNodeBuilder.ResourceTypeTreeNode) {
                    String c = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY);
                    switch (ResourceCategory.valueOf(c)) {
                    case PLATFORM:
                        return "types/Platform_up_16.png";
                    case SERVER:
                        return "types/Server_up_16.png";
                    case SERVICE:
                        return "types/Service_up_16.png";
                    }
                }
            }
            return null;
        }
    }
}
