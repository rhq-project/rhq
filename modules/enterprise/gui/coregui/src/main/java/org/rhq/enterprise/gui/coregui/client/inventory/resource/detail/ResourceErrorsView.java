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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceErrorsDataSource.Field;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class ResourceErrorsView extends Table<ResourceErrorsDataSource> {

    private static final String ERROR_ICON = "[SKIN]/Dialog/warn.png";

    private ResourceTitleBar titleBar;

    public ResourceErrorsView(Criteria criteria, ResourceTitleBar titleBar) {
        super(MSG.common_title_component_errors(), criteria);

        this.titleBar = titleBar;

        setWidth100();
        setHeight100();
        setShowHeader(false);
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.MULTIPLE;
    }

    @Override
    protected void configureTable() {
        ListGridField errorTypeField = new ListGridField(Field.ERROR_TYPE,
            MSG.dataSource_resourceErrors_field_errorType());
        errorTypeField.setAlign(Alignment.CENTER);
        errorTypeField.setAutoFitWidth(true);
        errorTypeField.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        ListGridField timeField = new ListGridField(Field.TIME_OCCURED,
            MSG.dataSource_resourceErrors_field_timeOccured());
        timeField.setType(ListGridFieldType.DATE);
        timeField.setAlign(Alignment.CENTER);
        timeField.setWidth("20%");
        TimestampCellFormatter.prepareDateField(timeField);

        ListGridField summaryField = new ListGridField(Field.SUMMARY, MSG.dataSource_resourceErrors_field_summary());
        summaryField.setAlign(Alignment.CENTER);
        summaryField.setCellAlign(Alignment.LEFT);
        summaryField.setWidth("*");

        ListGridField iconField = new ListGridField("icon");
        iconField.setType(ListGridFieldType.ICON);
        iconField.setAlign(Alignment.CENTER);
        iconField.setIcon(ERROR_ICON);
        iconField.setCellIcon(ERROR_ICON);
        iconField.setWidth("50");
        iconField.setCanSort(false);
        iconField.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                String details = event.getRecord().getAttribute(Field.DETAIL);
                popupDetails(details);
            }
        });
        iconField.setShowHover(true);
        iconField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = record.getAttribute(Field.DETAIL);
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.dataSource_resourceErrors_clickStatusIcon() + "</p>";
                }
                return html;
            }
        });

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length > 0) {
                    String details = selectedRows[0].getAttribute(Field.DETAIL);
                    popupDetails(details);
                }
            }
        });

        setListGridFields(errorTypeField, timeField, summaryField, iconField);

        ResourceComposite resourceComposite = titleBar.getResource();
        Set<Permission> resourcePermissions = resourceComposite.getResourcePermission().getPermissions();
        final boolean canModifyResource = resourcePermissions.contains(Permission.MODIFY_RESOURCE);
        addTableAction(MSG.common_button_delete(), MSG.common_msg_areYouSure(), new AbstractTableAction(
            canModifyResource ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
            public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                if (selection == null || selection.length == 0) {
                    return;
                }
                int[] resourceErrorIds = new int[selection.length];
                int i = 0;
                for (ListGridRecord record : selection) {
                    resourceErrorIds[i++] = record.getAttributeAsInt(Field.ID);
                }

                GWTServiceLookup.getResourceService().deleteResourceErrors(resourceErrorIds, new AsyncCallback<Void>() {
                    public void onSuccess(Void result) {
                        Message msg = new Message(MSG.dataSource_resourceErrors_deleteSuccess(String
                            .valueOf(selection.length)), Severity.Info);
                        CoreGUI.getMessageCenter().notify(msg);
                        refresh();
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.dataSource_resourceErrors_deleteFailure(), caught);
                    }
                });
            }
        });
    }

    @Override
    public void refresh() {
        super.refresh();
        this.titleBar.refreshResourceErrors();
    }

    private void popupDetails(String details) {
        final Window winModal = new Window();
        winModal.setTitle(MSG.common_title_component_errors());
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
            public void onCloseClick(CloseClickEvent event) {
                winModal.markForDestroy();
                titleBar.refreshResourceErrors();
            }
        });

        HTMLPane htmlPane = new HTMLPane();
        htmlPane.setMargin(10);
        htmlPane.setDefaultWidth(700);
        htmlPane.setDefaultHeight(500);
        htmlPane.setContents("<pre>" + details + "</pre>");
        winModal.addItem(htmlPane);
        winModal.show();
    }

}
