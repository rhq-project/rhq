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

import java.util.Map;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.TemplateAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A tree view of all known ResourceTypes, which includes summaries of metric schedule and alert definition templates
 * and allows the user to edit those templates.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceTypeTreeView extends LocatableVLayout implements BookmarkableView {

    private Layout gridCanvas;
    private Layout alertTemplateCanvas;
    private Layout metricTemplateCanvas;

    public ResourceTypeTreeView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.isEnd()) {
            switchToCanvas(this, getGridCanvas());
        } else {
            // we must be asked to go to a specific resource type
            // the path must be one of "Alert/#####" or "Metric/#####"
            // where ##### is a resource type ID
            ViewId typeOfTemplatePath = viewPath.getCurrent();
            final boolean isAlertTemplate; // true=alert template; false=metric template
            if ("Alert".equals(typeOfTemplatePath.getPath())) {
                isAlertTemplate = true;
            } else if ("Metric".equals(typeOfTemplatePath.getPath())) {
                isAlertTemplate = false;
            } else {
                CoreGUI.getErrorHandler().handleError(
                    "Invalid URL. Unknown template type: " + typeOfTemplatePath.getPath());
                return;
            }

            viewPath.next();
            final int resourceTypeId;
            try {
                resourceTypeId = viewPath.getCurrentAsInt();
            } catch (Exception e) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_typeTree_badTypeId(viewPath.getCurrent().getPath()));
                return;
            }

            if (isAlertTemplate) {
                editAlertTemplate(resourceTypeId, viewPath);
            } else {
                editMetricTemplate(resourceTypeId);
            }
        }
    }

    private Canvas getGridCanvas() {
        if (this.gridCanvas == null) {
            LocatableVLayout layout = new LocatableVLayout(extendLocatorId("gridLayout"));

            TitleBar titleBar = new TitleBar(this, MSG.view_adminConfig_templates(), ImageManager.getMetricEditIcon());
            titleBar.setExtraSpace(10);
            layout.addMember(titleBar);

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

            layout.addMember(sectionStack);
            this.gridCanvas = layout;

            // this will asynchronously populate the grids with the appropriate data
            new ResourceTypeTreeNodeBuilder(platformsList, platformServicesList, serversTreeGrid);
        }

        return this.gridCanvas;
    }

    private Layout getAlertTemplateCanvas() {
        if (this.alertTemplateCanvas == null) {
            LocatableVLayout layout = new LocatableVLayout(extendLocatorId("alertTemplateLayout"));
            layout.setHeight100();
            layout.setWidth100();
            layout.setMargin(5);
            this.alertTemplateCanvas = layout;
        }

        return this.alertTemplateCanvas;
    }

    private Layout getMetricTemplateCanvas() {
        if (this.metricTemplateCanvas == null) {
            LocatableVLayout layout = new LocatableVLayout(extendLocatorId("metricTemplateLayout"));
            layout.setHeight100();
            layout.setWidth100();
            layout.setMargin(5);
            this.metricTemplateCanvas = layout;
        }

        return this.metricTemplateCanvas;
    }

    /**
     * This will remove all members from the given parent canvas and then add
     * the canvasToShow as the only member to the parent.
     * 
     * @param parentCanvas parent to show the given canvas
     * @param canvasToShow the canvas to show in the parent
     */
    private void switchToCanvas(Layout parentCanvas, Canvas canvasToShow) {
        Canvas[] members = getMembers();
        if (members != null) {
            for (Canvas c : members) {
                parentCanvas.removeMember(c);
            }
        }
        parentCanvas.addMember(canvasToShow);
        parentCanvas.markForRedraw();
    }

    private void prepareSubCanvas(Layout parentCanvas, Canvas canvasToShow, boolean showBackButton) {
        Canvas[] members = getMembers();
        if (members != null) {
            for (Canvas c : members) {
                parentCanvas.removeMember(c);
                c.destroy();
            }
        }

        if (showBackButton) {
            String backLink = LinkManager.getAdminTemplatesLink().substring(1); // strip the #
            BackButton backButton = new BackButton(extendLocatorId("BackButton"), "Back to List", backLink);
            parentCanvas.addMember(backButton);
        }
        parentCanvas.addMember(canvasToShow);
        parentCanvas.markForRedraw();
    }

    private void editAlertTemplate(int resourceTypeId, final ViewPath viewPath) {
        final Integer[] idArray = new Integer[] { resourceTypeId };
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(idArray, new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                ResourceType rt = types.get(idArray[0]);
                Layout alertCanvas = getAlertTemplateCanvas();
                String locatorId = extendLocatorId("alertTemplateDef");
                TemplateAlertDefinitionsView def = new TemplateAlertDefinitionsView(locatorId, rt);
                def.renderView(viewPath.next());
                prepareSubCanvas(alertCanvas, def, viewPath.isEnd()); // don't show our back button if we are going to a template details pane which has its own back button
                switchToCanvas(ResourceTypeTreeView.this, alertCanvas);
            }
        });
    }

    private void editMetricTemplate(int resourceTypeId) {
        Layout metricCanvas = getMetricTemplateCanvas();
        TemplateSchedulesView templateSchedulesView = new TemplateSchedulesView(extendLocatorId("MetricTemplate"),
            resourceTypeId);
        prepareSubCanvas(metricCanvas, templateSchedulesView, true);
        switchToCanvas(ResourceTypeTreeView.this, metricCanvas);
    }

    public class CustomResourceTypeListGrid extends LocatableListGrid {
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
                metricTemplateImg.setSrc(ImageManager.getMetricEditIcon());
                metricTemplateImg.setPrompt("Edit Metric Template");
                metricTemplateImg.setHeight(16);
                metricTemplateImg.setWidth(16);
                metricTemplateImg.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        CoreGUI.goToView(LinkManager.getAdminTemplatesLink() + "/Metric/" + getRollOverId());
                    }
                });

                ImgButton alertTemplateImg = new ImgButton();
                alertTemplateImg.setShowDown(false);
                alertTemplateImg.setShowRollOver(false);
                alertTemplateImg.setLayoutAlign(Alignment.CENTER);
                alertTemplateImg.setSrc(ImageManager.getAlertEditIcon());
                alertTemplateImg.setPrompt("Edit Alert Template");
                alertTemplateImg.setHeight(16);
                alertTemplateImg.setWidth(16);
                alertTemplateImg.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        CoreGUI.goToView(LinkManager.getAdminTemplatesLink() + "/Alert/" + getRollOverId());
                    }
                });

                rollOverCanvas.addMember(metricTemplateImg);
                rollOverCanvas.addMember(alertTemplateImg);
            }
            return rollOverCanvas;
        }

        private int getRollOverId() {
            return Integer.parseInt(rollOverRecord.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID));
        }
    }

    public class CustomResourceTypeTreeGrid extends LocatableTreeGrid {
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
                metricTemplateImg.setSrc(ImageManager.getMetricEditIcon());
                metricTemplateImg.setPrompt("Edit Metric Template");
                metricTemplateImg.setHeight(16);
                metricTemplateImg.setWidth(16);
                metricTemplateImg.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        CoreGUI.goToView(LinkManager.getAdminTemplatesLink() + "/Metric/" + getRollOverId());
                    }
                });

                ImgButton alertTemplateImg = new ImgButton();
                alertTemplateImg.setShowDown(false);
                alertTemplateImg.setShowRollOver(false);
                alertTemplateImg.setLayoutAlign(Alignment.CENTER);
                alertTemplateImg.setSrc(ImageManager.getAlertEditIcon());
                alertTemplateImg.setPrompt("Edit Alert Template");
                alertTemplateImg.setHeight(16);
                alertTemplateImg.setWidth(16);
                alertTemplateImg.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        CoreGUI.goToView(LinkManager.getAdminTemplatesLink() + "/Alert/" + getRollOverId());
                    }
                });

                rollOverCanvas.addMember(metricTemplateImg);
                rollOverCanvas.addMember(alertTemplateImg);
            }
            return rollOverCanvas;
        }

        private int getRollOverId() {
            return Integer.parseInt(rollOverRecord.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID));
        }

        @Override
        protected String getIcon(Record record, boolean defaultState) {

            if (record instanceof TreeNode) {
                if (record instanceof ResourceTypeTreeNodeBuilder.ResourceTypeTreeNode) {
                    String categoryName = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY);
                    return ImageManager.getResourceIcon(ResourceCategory.valueOf(categoryName));
                }
            }
            return null;
        }
    }
}
