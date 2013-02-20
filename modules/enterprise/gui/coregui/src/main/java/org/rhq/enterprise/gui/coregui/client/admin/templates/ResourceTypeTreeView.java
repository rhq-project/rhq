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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
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
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * A tree view of all known ResourceTypes, which includes summaries of metric schedule and alert definition templates
 * and allows the user to edit those templates.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class ResourceTypeTreeView extends LocatableVLayout implements BookmarkableView {

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/";

    private Layout gridCanvas;

    public ResourceTypeTreeView() {
        super();

        setWidth100();
        setHeight100();
    }

    abstract protected String getEditLink(String type);

    abstract protected TitleBar getTitleBar();

    abstract protected Collection<ListGridField> getAdditionalListGridFields(boolean isTreeGrid);

    abstract protected ResourceTypeTreeNodeBuilder getNodeBuilderInstance(ListGrid platformsList,
        ListGrid platformServicesList, TreeGrid serversTreeGrid);

    abstract protected void editTemplates(ResourceType type, ViewPath viewPath);

    @Override
    public void renderView(final ViewPath viewPath) {
        if (viewPath.isEnd()) {
            switchToCanvas(this, getGridCanvas());

        } else {
            final int resourceTypeId;

            try {
                resourceTypeId = viewPath.getCurrentAsInt();
            } catch (Exception e) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_typeTree_badTypeId(viewPath.getCurrent().getPath()));
                return;
            }

            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceTypeId, getTypeMetadataTypes(),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        editTemplates(type, viewPath);
                    }
                });
        }
    }

    /**
     * @return The Set of metadata types required by the implementing class for the selected type.
     */
    protected EnumSet<ResourceTypeRepository.MetadataType> getTypeMetadataTypes() {
        return EnumSet.noneOf(ResourceTypeRepository.MetadataType.class);
    }

    private Canvas getGridCanvas() {
        if (this.gridCanvas == null) {
            LocatableVLayout layout = new LocatableVLayout(extendLocatorId("gridLayout"));

            TitleBar titleBar = getTitleBar();
            titleBar.setExtraSpace(10);
            layout.addMember(titleBar);

            SectionStack sectionStack = new SectionStack();
            sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);

            ListGrid platformsList = new CustomResourceTypeListGrid();
            SectionStackSection platforms = new SectionStackSection(MSG.view_adminTemplates_platforms());
            platforms.setExpanded(true);
            platforms.addItem(platformsList);

            ListGrid platformServicesList = new CustomResourceTypeListGrid();
            SectionStackSection platformServices = new SectionStackSection(MSG.view_adminTemplates_platformServices());
            platformServices.setExpanded(true);
            platformServices.addItem(platformServicesList);

            TreeGrid serversTreeGrid = new CustomResourceTypeTreeGrid();
            SectionStackSection servers = new SectionStackSection(MSG.view_adminTemplates_servers());
            servers.setExpanded(true);
            servers.addItem(serversTreeGrid);

            sectionStack.addSection(platforms);
            sectionStack.addSection(platformServices);
            sectionStack.addSection(servers);

            layout.addMember(sectionStack);
            this.gridCanvas = layout;

            // this will asynchronously populate the grids with the appropriate data
            getNodeBuilderInstance(platformsList, platformServicesList, serversTreeGrid);
        }

        return this.gridCanvas;
    }

    /**
     * This will remove all members from the given parent canvas and then add
     * the canvasToShow as the only member to the parent.
     * 
     * @param parentCanvas parent to show the given canvas
     * @param canvasToShow the canvas to show in the parent
     */
    protected void switchToCanvas(Layout parentCanvas, Canvas canvasToShow) {
        SeleniumUtility.destroyMembers(parentCanvas);

        parentCanvas.addMember(canvasToShow);
        parentCanvas.markForRedraw();
    }

    protected void prepareSubCanvas(Layout parentCanvas, Canvas canvasToShow, boolean showBackButton) {
        SeleniumUtility.destroyMembers(parentCanvas);

        if (showBackButton) {
            String backLink = getEditLink(null).substring(1); // strip the #
            BackButton backButton = new BackButton(MSG.view_tableSection_backButton(), backLink);
            parentCanvas.addMember(backButton);
        }

        parentCanvas.addMember(canvasToShow);
        parentCanvas.markForRedraw();
    }

    public class CustomResourceTypeListGrid extends ListGrid {

        public CustomResourceTypeListGrid() {
            super();

            setWrapCells(true);
            setFixedRecordHeights(false);
            setEmptyMessage(MSG.common_msg_loading());
            setSelectionType(SelectionStyle.NONE);

            ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

            final ListGridField nameField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME,
                MSG.common_title_name());
            nameField.setShowValueIconOnly(false);
            fields.add(nameField);

            final ListGridField pluginField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN,
                MSG.common_title_plugin());
            fields.add(pluginField);
            pluginField.setHidden(true);

            fields.addAll(getAdditionalListGridFields(false));

            ListGridField editField = new ListGridField(ResourceTypeTreeNodeBuilder.ATTRIB_EDIT,
                MSG.common_title_edit());
            editField.setType(ListGridFieldType.IMAGE);
            editField.setAlign(Alignment.CENTER);
            editField.setCanGroupBy(false);
            editField.addRecordClickHandler(new RecordClickHandler() {

                public void onRecordClick(RecordClickEvent event) {
                    Record record = event.getRecord();
                    String editAttr = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_EDIT);
                    if (ImageManager.getEditIcon().equals(editAttr)) {
                        String type = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID);
                        CoreGUI.goToView(getEditLink(type));
                    }
                }
            });
            fields.add(editField);

            nameField.setWidth("*");
            pluginField.setWidth("20%");
            // note that the additional fields should set their own width
            editField.setWidth("70");

            setFields(fields.toArray(new ListGridField[fields.size()]));
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
    }

    public class CustomResourceTypeTreeGrid extends TreeGrid {

        public CustomResourceTypeTreeGrid() {
            super();

            setWrapCells(true);
            setFixedRecordHeights(false);
            setEmptyMessage(MSG.common_msg_loading());
            setSelectionType(SelectionStyle.NONE);
            setAnimateFolders(false);

            ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

            final TreeGridField nameField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_NAME,
                MSG.common_title_name());
            fields.add(nameField);

            final TreeGridField pluginField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_PLUGIN,
                MSG.common_title_plugin());
            fields.add(pluginField);

            fields.addAll(getAdditionalListGridFields(true));

            ListGridField editField = new TreeGridField(ResourceTypeTreeNodeBuilder.ATTRIB_EDIT,
                MSG.common_title_edit());
            editField.setType(ListGridFieldType.IMAGE);
            editField.setAlign(Alignment.CENTER);
            editField.addRecordClickHandler(new RecordClickHandler() {

                public void onRecordClick(RecordClickEvent event) {
                    Record record = event.getRecord();
                    String editAttr = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_EDIT);
                    if (ImageManager.getEditIcon().equals(editAttr)) {
                        String type = record.getAttribute(ResourceTypeTreeNodeBuilder.ATTRIB_ID);
                        CoreGUI.goToView(getEditLink(type));
                    }
                }
            });
            fields.add(editField);

            nameField.setWidth("*");
            pluginField.setWidth("20%");
            // note that the additional fields should set their own width
            editField.setWidth("70");

            setFields(fields.toArray(new ListGridField[fields.size()]));
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
