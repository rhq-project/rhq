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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
public abstract class AbstractOperationHistoryListView<T extends AbstractOperationHistoryDataSource> extends
    TableSection<T> {

    private static final String HEADER_ICON = "subsystems/control/Operation_24.png";

    public AbstractOperationHistoryListView(String locatorId, T dataSource, String title) {
        super(locatorId, title);
        setDataSource(dataSource);
        setHeaderIcon(HEADER_ICON);
    }

    public AbstractOperationHistoryListView(String locatorId, T dataSource, String title, Criteria criteria) {
        super(locatorId, title, criteria);
        setDataSource(dataSource);
    }

    protected abstract boolean hasControlPermission();

    @Override
    protected void configureTable() {
        List<ListGridField> fields = createFields();
        setListGridFields(fields.toArray(new ListGridField[fields.size()]));

        // explicitly sort on started time so the user can see the last operation at the top and is sorted descendingly
        SortSpecifier sortspec = new SortSpecifier(AbstractOperationHistoryDataSource.Field.STARTED_TIME,
            SortDirection.DESCENDING);
        getListGrid().setSort(new SortSpecifier[] { sortspec });

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    return (count >= 1 && hasControlPermission());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteSelectedRecords();
                }
            });

        // TODO: i18n
        addTableAction(extendLocatorId("ForceDelete"), MSG.view_operationHistoryList_button_forceDelete(),
            getDeleteConfirmMessage(), new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    return (count >= 1 && hasControlPermission());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    DSRequest requestProperties = new DSRequest();
                    requestProperties.setAttribute("force", true);
                    deleteSelectedRecords(requestProperties);
                }
            });

        super.configureTable();
    }

    protected List<ListGridField> createFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = new ListGridField(AbstractOperationHistoryDataSource.Field.ID);
        idField.setWidth(38);
        fields.add(idField);

        ListGridField opNameField = new ListGridField(AbstractOperationHistoryDataSource.Field.OPERATION_NAME);
        opNameField.setWidth("34%");
        fields.add(opNameField);

        ListGridField subjectField = new ListGridField(AbstractOperationHistoryDataSource.Field.SUBJECT);
        subjectField.setWidth("33%");
        fields.add(subjectField);

        ListGridField statusField = createStatusField();
        fields.add(statusField);

        ListGridField startedTimeField = createStartedTimeField();
        startedTimeField.setWidth("33%");
        fields.add(startedTimeField);

        return fields;
    }

    protected ListGridField createStartedTimeField() {
        ListGridField startedTimeField = new ListGridField(AbstractOperationHistoryDataSource.Field.STARTED_TIME);
        startedTimeField.setAlign(Alignment.LEFT);
        startedTimeField.setCellAlign(Alignment.LEFT);
        startedTimeField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value != null) {
                    Date date = (Date) value;
                    return DateTimeFormat.getMediumDateTimeFormat().format(date);
                } else {
                    return "<i>" + MSG.view_operationHistoryList_notYetStarted() + "</i>";
                }
            }
        });
        return startedTimeField;
    }

    protected ListGridField createStatusField() {
        ListGridField statusField = new ListGridField(AbstractOperationHistoryDataSource.Field.STATUS);
        statusField.setAlign(Alignment.CENTER);
        statusField.setCellAlign(Alignment.CENTER);
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String statusStr = record.getAttribute(AbstractOperationHistoryDataSource.Field.STATUS);
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
                // should never get here
                return MSG.common_status_unknown();
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
                String statusStr = record.getAttribute(AbstractOperationHistoryDataSource.Field.STATUS);
                OperationRequestStatus status = OperationRequestStatus.valueOf(statusStr);
                if (status == OperationRequestStatus.FAILURE) {
                    final Window winModal = new LocatableWindow(AbstractOperationHistoryListView.this
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

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane(AbstractOperationHistoryListView.this
                        .extendLocatorId("statusDetailsPane"));
                    htmlPane.setMargin(10);
                    htmlPane.setDefaultWidth(500);
                    htmlPane.setDefaultHeight(400);
                    String errorMsg = record.getAttribute(AbstractOperationHistoryDataSource.Field.ERROR_MESSAGE);
                    if (errorMsg == null) {
                        errorMsg = MSG.common_status_failed();
                    }
                    htmlPane.setContents("<pre>" + errorMsg + "</pre>");
                    winModal.addItem(htmlPane);
                    winModal.show();
                }
            }
        });
        statusField.setWidth(44);

        return statusField;
    }

    protected ListGridField createResourceField() {
        ListGridField resourceField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
        resourceField.setAlign(Alignment.LEFT);
        resourceField.setCellAlign(Alignment.LEFT);
        resourceField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return SeleniumUtility.getLocatableHref(url, o.toString(), null);
            }
        });
        resourceField.setShowHover(true);
        resourceField.setHoverCustomizer(new HoverCustomizer() {

            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });

        return resourceField;
    }

    protected ListGridField createAncestryField() {
        ListGridField ancestryField = new ListGridField(AncestryUtil.RESOURCE_ANCESTRY, MSG.common_title_ancestry());
        ancestryField.setAlign(Alignment.LEFT);
        ancestryField.setCellAlign(Alignment.LEFT);
        ancestryField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return listGridRecord.getAttributeAsString(AncestryUtil.RESOURCE_ANCESTRY_VALUE);
            }
        });
        ancestryField.setShowHover(true);
        ancestryField.setHoverCustomizer(new HoverCustomizer() {

            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
            }
        });

        return ancestryField;
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return AbstractOperationHistoryDataSource.Field.OPERATION_NAME;
    }

}
