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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation;

import java.util.EnumSet;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail.OperationDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class OperationHistoryView extends TableSection<OperationHistoryDataSource> {
    public static final ViewName VIEW_ID = new ViewName("RecentOperations", MSG.common_title_recent_operations());

    private ResourceComposite composite;

    public OperationHistoryView(String locatorId) {
        super(locatorId, VIEW_ID.getTitle());
        setWidth100();
        setHeight100();

        setDataSource(new OperationHistoryDataSource());
    }

    public OperationHistoryView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, VIEW_ID.getTitle(), new Criteria(OperationHistoryDataSource.CriteriaField.RESOURCE_ID, String
            .valueOf(resourceComposite.getResource().getId())));
        this.composite = resourceComposite;

        setDataSource(new OperationHistoryDataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(OperationHistoryDataSource.Field.ID, MSG.common_title_id());

        ListGridField opNameField = new ListGridField(OperationHistoryDataSource.Field.OPERATION_NAME, MSG
            .dataSource_operationHistory_operationName());

        ListGridField subjectField = new ListGridField(OperationHistoryDataSource.Field.SUBJECT, MSG
            .common_title_user());

        ListGridField statusField = new ListGridField(OperationHistoryDataSource.Field.STATUS, MSG
            .common_title_status());
        statusField.setAlign(Alignment.CENTER);
        statusField.setCellAlign(Alignment.CENTER);
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String statusStr = record.getAttribute(OperationHistoryDataSource.Field.STATUS);
                OperationRequestStatus status = OperationRequestStatus.valueOf(statusStr);
                switch (status) {
                case SUCCESS: {
                    return MSG.common_status_success();
                }
                case FAILURE: {
                    return MSG.common_status_failed();
                }
                case INPROGRESS: {
                    return MSG.common_status_inprogress();
                }
                case CANCELED: {
                    return MSG.common_status_canceled();
                }
                }
                return "unknown"; // should never get here
            }
        });
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                OperationRequestStatus status = OperationRequestStatus.valueOf((String) o);
                String icon = ImageManager.getOperationResultsIcon(status);
                return Canvas.imgHTML(icon, 16, 16);
            }
        });
        statusField.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                Record record = event.getRecord();
                String statusStr = record.getAttribute(OperationHistoryDataSource.Field.STATUS);
                OperationRequestStatus status = OperationRequestStatus.valueOf(statusStr);
                if (status == OperationRequestStatus.FAILURE) {
                    final Window winModal = new LocatableWindow(OperationHistoryView.this
                        .extendLocatorId("statusDetailsWin"));
                    winModal.setTitle(MSG.common_title_details());
                    winModal.setOverflow(Overflow.VISIBLE);
                    winModal.setShowMinimizeButton(false);
                    winModal.setShowMaximizeButton(true);
                    winModal.setIsModal(true);
                    winModal.setShowModalMask(true);
                    winModal.setAutoSize(true);
                    winModal.setAutoCenter(true);
                    winModal.setShowResizer(true);
                    winModal.setCanDragResize(true);
                    winModal.centerInPage();
                    winModal.addCloseClickHandler(new CloseClickHandler() {
                        @Override
                        public void onCloseClick(CloseClientEvent event) {
                            winModal.markForDestroy();
                        }
                    });

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane(OperationHistoryView.this
                        .extendLocatorId("statusDetailsPane"));
                    htmlPane.setMargin(10);
                    htmlPane.setDefaultWidth(500);
                    htmlPane.setDefaultHeight(400);
                    String errorMsg = record.getAttribute(OperationHistoryDataSource.Field.ERROR_MESSAGE);
                    if (errorMsg == null) {
                        errorMsg = MSG.common_status_failed();
                    }
                    htmlPane.setContents("<pre>" + errorMsg + "</pre>");
                    winModal.addItem(htmlPane);
                    winModal.show();
                }
            }
        });

        ListGridField startedTimeField = new ListGridField(OperationHistoryDataSource.Field.STARTED_TIME, MSG
            .dataSource_operationHistory_startedTime());
        startedTimeField.setType(ListGridFieldType.DATE);
        startedTimeField.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);
        startedTimeField.setAlign(Alignment.LEFT);
        startedTimeField.setCellAlign(Alignment.LEFT);

        final Resource resource = this.composite.getResource();

        if (resource == null) { // if null, we aren't viewing op history for a single resource
            ListGridField resourceField = new ListGridField(OperationHistoryDataSource.Field.RESOURCE, MSG
                .common_title_resource());
            resourceField.setAlign(Alignment.LEFT);
            resourceField.setCellAlign(Alignment.LEFT);
            resourceField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Resource res = (Resource) o;
                    return "<a href=\"#Resource/" + res.getId() + "\">" + res.getName() + "</a>";
                }
            });

            idField.setWidth(10);
            opNameField.setWidth("25%");
            subjectField.setWidth("25%");
            statusField.setWidth(50);
            startedTimeField.setWidth("25%");
            resourceField.setWidth("25%");

            setListGridFields(idField, opNameField, startedTimeField, subjectField, statusField, resourceField);
        } else {
            idField.setWidth(10);
            opNameField.setWidth("34%");
            subjectField.setWidth("33%");
            statusField.setWidth(50);
            startedTimeField.setWidth("33%");

            setListGridFields(idField, opNameField, startedTimeField, subjectField, statusField);
        }

        if (resource != null && composite.getResourcePermission().isControl()) {
            final Menu operationMenu = new LocatableMenu(this.extendLocatorId("Operation"));
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.operations),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        for (final OperationDefinition od : type.getOperationDefinitions()) {
                            MenuItem menuItem = new MenuItem(od.getDisplayName());
                            operationMenu.addItem(menuItem);
                            menuItem.addClickHandler(new ClickHandler() {
                                public void onClick(MenuItemClickEvent event) {
                                    new OperationCreateWizard(resource, od).startOperationWizard();
                                }
                            });
                        }
                    }
                });

            IMenuButton operationsButton = new LocatableIMenuButton(this.extendLocatorId("Run"), MSG
                .view_operationHistoryList_button_runOperation(), operationMenu);
            operationsButton.setShowMenuBelow(false);
            operationsButton.setAutoFit(true);
            addExtraWidget(operationsButton);
        }

    }

    @Override
    protected ListGridField getNameField() {
        // TODO: What field if any should we return here?
        return null;
    }

    @Override
    public Canvas getDetailsView(int id) {
        OperationDetailsView detailsView = new OperationDetailsView(this.extendLocatorId("Details"));
        return detailsView;
    }

    public static OperationHistoryView getResourceHistoryView(String locatorId, ResourceComposite resource) {
        return new OperationHistoryView(locatorId, resource);
    }

}
