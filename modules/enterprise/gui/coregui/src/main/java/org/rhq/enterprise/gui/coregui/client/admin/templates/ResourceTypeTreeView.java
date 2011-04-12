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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import java.util.Map;
import java.util.Set;

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

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.TemplateAlertDefinitionsView;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceCategoryCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A tree view of all known ResourceTypes, which includes summaries of metric schedule and alert definition templates
 * and allows the user to edit those templates.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceTypeTreeView extends LocatableVLayout implements BookmarkableView {

    public static final ViewName VIEW_ID = new ViewName("Templates", MSG.view_adminConfig_templates());
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

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
                CoreGUI.getErrorHandler()
                    .handleError(MSG.widget_typeTree_badTemplateType(typeOfTemplatePath.getPath()));
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
            SectionStackSection platforms = new SectionStackSection(MSG.view_adminTemplates_platforms());
            platforms.setExpanded(true);
            platforms.addItem(platformsList);

            ListGrid platformServicesList = new CustomResourceTypeListGrid(extendLocatorId("platformServicesList"));
            SectionStackSection platformServices = new SectionStackSection(MSG.view_adminTemplates_platformServices());
            platformServices.setExpanded(true);
            platformServices.addItem(platformServicesList);

            TreeGrid serversTreeGrid = new CustomResourceTypeTreeGrid(extendLocatorId("serversTree"));
            SectionStackSection servers = new SectionStackSection(MSG.view_adminTemplates_servers());
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
        SeleniumUtility.destroyMembers(parentCanvas);

        parentCanvas.addMember(canvasToShow);
        parentCanvas.markForRedraw();
    }

    private void prepareSubCanvas(Layout parentCanvas, Canvas canvasToShow, boolean showBackButton) {
        SeleniumUtility.destroyMembers(parentCanvas);

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

            public void onTypesLoaded(final Map<Integer, ResourceType> types) {
                new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

                    public void onPermissionsLoaded(Set<Permission> permissions) {
                        ResourceType rt = types.get(idArray[0]);
                        Layout alertCanvas = getAlertTemplateCanvas();
                        String locatorId = extendLocatorId("alertTemplateDef");
                        TemplateAlertDefinitionsView def = new TemplateAlertDefinitionsView(locatorId, rt, permissions);
                        def.renderView(viewPath.next());
                        prepareSubCanvas(alertCanvas, def, viewPath.isEnd()); // don't show our back button if we are going to a template details pane which has its own back button
                        switchToCanvas(ResourceTypeTreeView.this, alertCanvas);
                    }
                });
            }
        });
    }

    private void editMetricTemplate(final int resourceTypeId) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

            public void onPermissionsLoaded(Set<Permission> permissions) {
                Layout metricCanvas = getMetricTemplateCanvas();
                TemplateSchedulesView templateSchedulesView = new TemplateSchedulesView(
                    extendLocatorId("MetricTemplate"), resourceTypeId, permissions);
                prepareSubCanvas(metricCanvas, templateSchedulesView, true);
                switchToCanvas(ResourceTypeTreeView.this, metricCanvas);
            }
        });
    }

    public class CustomResourceTypeListGrid extends LocatableListGrid {
        private HLayout rollOverCanvas;
        private ListGridRecord rollOverRecord;

        public CustomResourceTypeListGrid(String locatorId) {
            super(locatorId);

            setWrapCells(true);
            setFixedRecordHeights(false);
            setShowRollOverCanvas(true);
            setEmptyMessage(MSG.common_msg_loading());
            setSelectionType(SelectionStyle.NONE);

            final ListGridField nameField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME, MSG
                .common_title_name());
            nameField.setShowValueIconOnly(false);

            final ListGridField pluginField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN, MSG
                .common_title_plugin());
            final ListGridField categoryField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY, MSG
                .common_title_category());
            categoryField.setCellFormatter(new ResourceCategoryCellFormatter());
            final ListGridField enabledAlertTemplatesField = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_ALERT_TEMPLATES, MSG
                    .view_adminTemplates_enabledAlertTemplates());
            final ListGridField disabledAlertTemplatesField = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_ALERT_TEMPLATES, MSG
                    .view_adminTemplates_disabledAlertTemplates());
            final ListGridField enabledMetricTemplatesField = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_METRIC_TEMPLATES, MSG
                    .view_adminTemplates_enabledMetricTemplates());
            final ListGridField disabledMetricTemplatesField = new ListGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_METRIC_TEMPLATES, MSG
                    .view_adminTemplates_disabledMetricTemplates());

            pluginField.setHidden(true);
            categoryField.setHidden(true);

            nameField.setWidth("*");
            pluginField.setWidth("10%");
            categoryField.setWidth("5%");
            enabledAlertTemplatesField.setWidth("10%");
            disabledAlertTemplatesField.setWidth("10%");
            enabledMetricTemplatesField.setWidth("10%");
            disabledMetricTemplatesField.setWidth("10%");

            enabledAlertTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_enabledAlertTemplates());
            disabledAlertTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_disabledAlertTemplates());
            enabledMetricTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_enabledMetricTemplates());
            disabledMetricTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_disabledMetricTemplates());

            setFields(nameField, pluginField, categoryField, enabledAlertTemplatesField, disabledAlertTemplatesField,
                enabledMetricTemplatesField, disabledMetricTemplatesField);
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
                metricTemplateImg.setPrompt(MSG.view_adminTemplates_editMetricTemplate());
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
                alertTemplateImg.setPrompt(MSG.view_adminTemplates_editAlertTemplate());
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

        @Override
        public String getValueIcon(ListGridField field, Object value, ListGridRecord record) {
            if (field.getName().equals(ResourceTypeTreeNodeBuilder.ATTRIB_NAME)) {
                String categoryName = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY);
                return ImageManager.getResourceIcon(ResourceCategory.valueOf(categoryName));
            } else {
                return super.getValueIcon(field, value, record);
            }
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
            setEmptyMessage(MSG.common_msg_loading());
            setSelectionType(SelectionStyle.NONE);
            setAnimateFolders(false);

            final TreeGridField nameField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME, MSG
                .common_title_name());
            final TreeGridField pluginField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN, MSG
                .common_title_plugin());
            final TreeGridField categoryField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_CATEGORY, MSG
                .common_title_category());
            categoryField.setCellFormatter(new ResourceCategoryCellFormatter());
            final TreeGridField enabledAlertTemplatesField = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_ALERT_TEMPLATES, MSG
                    .view_adminTemplates_enabledAlertTemplates());
            final TreeGridField disabledAlertTemplatesField = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_ALERT_TEMPLATES, MSG
                    .view_adminTemplates_disabledAlertTemplates());
            final TreeGridField enabledMetricTemplatesField = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_ENABLED_METRIC_TEMPLATES, MSG
                    .view_adminTemplates_enabledMetricTemplates());
            final TreeGridField disabledMetricTemplatesField = new TreeGridField(
                ResourceTypeTreeNodeBuilder.ATTRIB_DISABLED_METRIC_TEMPLATES, MSG
                    .view_adminTemplates_disabledMetricTemplates());

            categoryField.setHidden(true);

            nameField.setWidth("*");
            pluginField.setWidth("10%");
            categoryField.setWidth("5%");
            enabledAlertTemplatesField.setWidth("10%");
            disabledAlertTemplatesField.setWidth("10%");
            enabledMetricTemplatesField.setWidth("10%");
            disabledMetricTemplatesField.setWidth("10%");

            enabledAlertTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_enabledAlertTemplates());
            disabledAlertTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_disabledAlertTemplates());
            enabledMetricTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_enabledMetricTemplates());
            disabledMetricTemplatesField.setPrompt(MSG.view_adminTemplates_prompt_disabledMetricTemplates());

            setFields(nameField, pluginField, categoryField, enabledAlertTemplatesField, disabledAlertTemplatesField,
                enabledMetricTemplatesField, disabledMetricTemplatesField);
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
                metricTemplateImg.setPrompt(MSG.view_adminTemplates_editMetricTemplate());
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
                alertTemplateImg.setPrompt(MSG.view_adminTemplates_editAlertTemplate());
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
