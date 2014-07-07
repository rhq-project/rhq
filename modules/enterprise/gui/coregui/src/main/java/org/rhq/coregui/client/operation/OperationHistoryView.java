/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.operation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.form.EnumSelectItem;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.TableActionInfoBuilder;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryDetailsView;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Option;
import org.rhq.coregui.client.util.message.Message.Severity;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * A view that displays a paginated table of operation history. Support exists of subsystem and resource contexts.
 * Group operation history is (currently) handled separately as the view is comprised of group operation history
 * entities, not [resource] operation history entities.
 (
 * @author Jay Shaughnessy
 */
public class OperationHistoryView extends TableSection<OperationHistoryDataSource> implements HasViewName {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("RecentOperations",
        MSG.common_title_recent_operations(), IconEnum.RECENT_OPERATIONS);

    private static final Criteria INITIAL_CRITERIA = new Criteria();

    private static final SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(
        OperationHistoryDataSource.Field.CREATED_TIME, SortDirection.DESCENDING);

    protected SelectItem statusFilter;
    protected DateFilterItem startDateFilter;
    protected DateFilterItem endDateFilter;

    EntityContext context;
    boolean hasControlPermission;
    OperationHistoryDataSource dataSource;
    private boolean showNewScheduleButton = true;

    static {
        OperationRequestStatus[] statusValues = OperationRequestStatus.values();
        String[] statusNames = new String[statusValues.length];
        int i = 0;
        for (OperationRequestStatus s : statusValues) {
            statusNames[i++] = s.name();
        }

        INITIAL_CRITERIA.addCriteria(OperationHistoryDataSource.Field.STATUS, statusNames);
    }

    // for subsystem views
    public OperationHistoryView() {
        this(SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public OperationHistoryView(EntityContext entityContext) {
        this(SUBSYSTEM_VIEW_ID.getTitle(), entityContext, false);
    }

    public OperationHistoryView(String tableTitle, EntityContext entityContext) {
        this(tableTitle, entityContext, false);
    }

    protected OperationHistoryView(String tableTitle, EntityContext context, boolean hasControlPermission) {
        super(tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasControlPermission = hasControlPermission;

        setInitialCriteriaFixed(false);
        setDataSource(getDataSource());
    }

    @Override
    public OperationHistoryDataSource getDataSource() {
        if (null == this.dataSource) {
            this.dataSource = new OperationHistoryDataSource(context);
        }
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        LinkedHashMap<String, String> statusValues = new LinkedHashMap<String, String>(4);
        statusValues.put(OperationRequestStatus.SUCCESS.name(), MSG.common_status_success());
        statusValues.put(OperationRequestStatus.INPROGRESS.name(), MSG.common_status_inprogress());
        statusValues.put(OperationRequestStatus.CANCELED.name(), MSG.common_status_canceled());
        statusValues.put(OperationRequestStatus.FAILURE.name(), MSG.common_status_failed());
        LinkedHashMap<String, String> statusIcons = new LinkedHashMap<String, String>(3);
        statusIcons.put(OperationRequestStatus.SUCCESS.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.SUCCESS));
        statusIcons.put(OperationRequestStatus.INPROGRESS.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.INPROGRESS));
        statusIcons.put(OperationRequestStatus.CANCELED.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.CANCELED));
        statusIcons.put(OperationRequestStatus.FAILURE.name(),
            ImageManager.getOperationResultsIcon(OperationRequestStatus.FAILURE));

        statusFilter = new EnumSelectItem(OperationHistoryDataSource.Field.STATUS, MSG.common_title_operation_status(),
            OperationRequestStatus.class, statusValues, statusIcons);

        startDateFilter = new DateFilterItem(DateFilterItem.START_DATE_FILTER, MSG.filter_from_date());
        endDateFilter = new DateFilterItem(DateFilterItem.END_DATE_FILTER, MSG.filter_to_date());

        SpacerItem spacerItem = new SpacerItem();
        spacerItem.setColSpan(2);

        if (isShowFilterForm()) {
            setFilterFormItems(statusFilter, startDateFilter, spacerItem, endDateFilter);
        }
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        setupTableInteractions();

        super.configureTable();
    }

    protected boolean hasControlPermission() {
        return this.hasControlPermission;
    }

    protected void setupTableInteractions() {
        addTableAction(MSG.common_button_delete(), getDeleteConfirmMessage(), ButtonColor.RED, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                int count = selection.length;
                return (count >= 1 && hasControlPermission());
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                deleteSelectedRecords();
            }
        });

        addTableAction(MSG.view_operationHistoryList_button_forceDelete(), getDeleteConfirmMessage(), ButtonColor.RED,
            new TableAction() {
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
        
        addTableAction(MSG.common_button_cancel(), MSG.view_operationHistoryList_cancelConfirm(), new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                int count = selection.length;
                for (ListGridRecord item : selection) {
                    if (!OperationRequestStatus.INPROGRESS.name().equals(
                        item.getAttribute(OperationHistoryDataSource.Field.STATUS))) {
                        count--; // one selected item was not in-progress, it doesn't count
                    }
                }
                return (count >= 1 && hasControlPermission());
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                int numCancelRequestsSubmitted = 0;
                OperationGWTServiceAsync opService = GWTServiceLookup.getOperationService();
                for (ListGridRecord toBeCanceled : selection) {
                    // only cancel those selected operations that are currently in progress
                    if (OperationRequestStatus.INPROGRESS.name().equals(
                        toBeCanceled.getAttribute(OperationHistoryDataSource.Field.STATUS))) {
                        numCancelRequestsSubmitted++;
                        final int historyId = toBeCanceled.getAttributeAsInt(OperationHistoryDataSource.Field.ID);
                        opService.cancelOperationHistory(historyId, false, new AsyncCallback<Void>() {
                            public void onSuccess(Void result) {
                                Message msg = new Message(MSG.view_operationHistoryList_cancelSuccess(String
                                    .valueOf(historyId)), Severity.Info, EnumSet.of(Option.BackgroundJobResult));
                                CoreGUI.getMessageCenter().notify(msg);
                            };

                            public void onFailure(Throwable caught) {
                                Message msg = new Message(MSG.view_operationHistoryList_cancelFailure(String
                                    .valueOf(historyId)), caught, Severity.Error, EnumSet
                                    .of(Option.BackgroundJobResult));
                                CoreGUI.getMessageCenter().notify(msg);
                            };
                        });
                    }
                }
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_operationHistoryList_cancelSubmitted(String
                        .valueOf(numCancelRequestsSubmitted)), Severity.Info));
                refreshTableInfo();
            }
        });

        if (!context.isSubsystemView() && showNewScheduleButton) {
            TableActionInfo rescheduleAction = new TableActionInfoBuilder(MSG.common_button_reschedule(),
                new RescheduleTableAction()).setTooltip(MSG.common_button_reschedule_tooltip())
                .setButtonColor(ButtonColor.BLUE).createTableActionInfo();
            addTableAction(rescheduleAction);
            addTableAction(MSG.common_button_new() + " " + MSG.common_button_schedule(), ButtonColor.BLUE,
                new NewScheduleTableAction());
        }
    }

    @Override
    protected void deleteSelectedRecords(DSRequest requestProperties) {
        disableAllFooterControls(); // wait for this to complete before we allow more...

        final ListGridRecord[] recordsToBeDeleted = getListGrid().getSelectedRecords();
        final int numberOfRecordsToBeDeleted = recordsToBeDeleted.length;
        final Boolean forceValue = (requestProperties != null && requestProperties.getAttributeAsBoolean("force"));
        final boolean force = ((forceValue != null) && forceValue);
        final int[] idsToBeDeleted = new int[numberOfRecordsToBeDeleted];
        int i = 0;
        for (ListGridRecord record : recordsToBeDeleted) {
            idsToBeDeleted[i++] = record.getAttributeAsInt(OperationHistoryDataSource.Field.ID);
        }
        GWTServiceLookup.getOperationService().deleteOperationHistories(idsToBeDeleted, force,
            new AsyncCallback<Void>() {
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_operationHistoryList_deleteSuccess(String
                            .valueOf(numberOfRecordsToBeDeleted))));
                    refresh();
                    refreshTableInfo(); //enable proper buttons
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_operationHistoryList_deleteFailure(), caught);
                    refresh();
                    refreshTableInfo(); // enable proper buttons
                }
            });
    }

    public EntityContext getContext() {
        return context;
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ResourceOperationHistoryDetailsView();
    }

    @Override
    protected String getTitleFieldName() {
        return OperationHistoryDataSource.Field.OPERATION_NAME;
    }

    @Override
    public ViewName getViewName() {
        return SUBSYSTEM_VIEW_ID;
    }
    
    public void setShowNewScheduleButton(boolean showNewScheduleButton) {
        this.showNewScheduleButton = showNewScheduleButton;
    }

    private class NewScheduleTableAction implements TableAction {
        public boolean isEnabled(ListGridRecord[] selection) {
            return hasControlPermission();
        }

        public void executeAction(ListGridRecord[] selection, Object actionValue) {
            String url = getViewUrl(selection);
            if (url != null) {
                CoreGUI.goToView(url);
            }
        }

        protected String getViewUrl(ListGridRecord[] selection) {
            // CoreGUI.goToView(LinkManager.getEntityTabLink(context, "Operations", "Schedules/0"));
            // the above doesn't work because EntityContext doesn't know if it is autogroup or not
            // -> using the relative URL hack
            String url = History.getToken();
            String lastChunk = url.substring(url.lastIndexOf("/") + 1);
            if ("Activity".equals(lastChunk)) {
                url = url.substring(0, url.lastIndexOf("/"));
                url = url.substring(0, url.lastIndexOf("/")) + "/Operations";
            } else if ("History".equals(lastChunk)) {
                url = url.substring(0, url.lastIndexOf("/"));
            } else {
                try {
                    Integer.parseInt(lastChunk);
                    url += "/Operations";
                } catch (NumberFormatException nfe) {
                    // do nothing
                }
            }
            if ("Operations".equals(url.substring(url.lastIndexOf("/") + 1))) {
                url += "/Schedules/0";
                return url;
            }
            return null;
        }
    }

    private class RescheduleTableAction extends NewScheduleTableAction {
        @Override
        public boolean isEnabled(ListGridRecord[] selection) {
            return super.isEnabled(selection) && selection.length == 1;
        }

        @Override
        protected String getViewUrl(ListGridRecord[] selection) {
            String viewUrl = super.getViewUrl(selection);
            if (viewUrl != null) {
                ListGridRecord selectedRecord = selection[0];
                Integer operationHistoryId = selectedRecord.getAttributeAsInt(OperationHistoryDataSource.Field.ID);
                viewUrl += "/example=" + operationHistoryId;
            }
            return viewUrl;
        }
    }
}
