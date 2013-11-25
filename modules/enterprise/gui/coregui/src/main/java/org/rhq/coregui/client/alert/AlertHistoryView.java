/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.alert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.form.EnumSelectItem;
import org.rhq.coregui.client.components.table.RecordExtractor;
import org.rhq.coregui.client.components.table.ResourceAuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter those alerts, sort those alerts, double-click a row to view full details an alert, and perform
 * various operations on the the alerts: delete selected, delete all from source, ack selected, ack all from source.
 * This view fully respects the user's authorization, and will not allow operations on the alerts unless the user is
 * either the inventory manager or has MANAGE_ALERTS permission on every resource corresponding to the alerts being
 * operated on.
 *
 * @author Joseph Marques
 * @author Ian Springer
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class AlertHistoryView extends TableSection<AlertDataSource> implements HasViewName {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("RecentAlerts", MSG.common_title_recent_alerts(),
        IconEnum.RECENT_ALERTS);

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME,
        SortDirection.DESCENDING);
    private static final Criteria INITIAL_CRITERIA = new Criteria();

    protected SelectItem priorityFilter;
    protected DateFilterItem startDateFilter;
    protected DateFilterItem endDateFilter;

    private EntityContext context;
    private boolean hasWriteAccess;

    static {
        AlertPriority[] priorityValues = AlertPriority.values();
        String[] priorityNames = new String[priorityValues.length];
        for (int i = 0, priorityValuesLength = priorityValues.length; i < priorityValuesLength; i++) {
            priorityNames[i] = priorityValues[i].name();
        }

        INITIAL_CRITERIA.addCriteria(AlertDataSource.FILTER_PRIORITIES, priorityNames);
    }

    // for subsystem views
    public AlertHistoryView() {
        this(SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    public AlertHistoryView(String tableTitle, EntityContext entityContext) {
        this(tableTitle, entityContext, false);
    }

    protected AlertHistoryView(String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(tableTitle, INITIAL_CRITERIA, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });

        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setInitialCriteriaFixed(false);
        setDataSource(new AlertDataSource(context));
    }
    
    public AlertHistoryView(String tableTitle, int[] resourceIds) {
        super(tableTitle, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        
        Criteria initialCriteria = new Criteria();
        AlertPriority[] priorityValues = AlertPriority.values();
        String[] priorityNames = new String[priorityValues.length];
        for (int i = 0, priorityValuesLength = priorityValues.length; i < priorityValuesLength; i++) {
            priorityNames[i] = priorityValues[i].name();
        }
        initialCriteria.addCriteria(AlertDataSource.FILTER_PRIORITIES, priorityNames);
        initialCriteria.setAttribute("resourceIds", resourceIds);
        setInitialCriteria(initialCriteria);

        this.context = new EntityContext();
        this.context.type = EntityContext.Type.SubsystemView;
        this.hasWriteAccess = true;

        setDataSource(new AlertDataSource(context));
    }

    @Override
    protected void configureTableFilters() {
        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
        priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
        priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
        priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        priorityFilter = new EnumSelectItem(AlertDataSource.FILTER_PRIORITIES, MSG.view_alerts_table_filter_priority(),
            AlertPriority.class, priorities, priorityIcons);

        startDateFilter = new DateFilterItem(DateFilterItem.START_DATE_FILTER, MSG.filter_from_date());
        endDateFilter = new DateFilterItem(DateFilterItem.END_DATE_FILTER, MSG.filter_to_date());

        SpacerItem spacerItem = new SpacerItem();
        spacerItem.setColSpan(2);

        if (isShowFilterForm()) {
            setFilterFormItems(priorityFilter, startDateFilter, spacerItem, endDateFilter);
        }
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
        setupTableInteractions(this.hasWriteAccess);

        super.configureTable();
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return AlertCriteria.SORT_FIELD_CTIME;
    }

    /**
     * Subclasses can override this to indicate they do not support
     * deleting all alerts or acknowledging all alerts. Portlet subclasses
     * that only trim their views to the top N alerts can override this to
     * return false so they don't delete or acknowledge alerts that aren't displayed
     * to the user.
     * 
     * @return this default implementation returns true
     */
    protected boolean canSupportDeleteAndAcknowledgeAll() {
        return true;
    }

    protected void setupTableInteractions(final boolean hasWriteAccess) {

        addTableAction(MSG.common_button_delete(), MSG.view_alerts_delete_confirm(), new ResourceAuthorizedTableAction(
            AlertHistoryView.this, TableActionEnablement.ANY, (hasWriteAccess ? null : Permission.MANAGE_ALERTS),
            new RecordExtractor<Integer>() {
                public Collection<Integer> extract(Record[] records) {
                    List<Integer> result = new ArrayList<Integer>(records.length);
                    for (Record record : records) {
                        result.add(record.getAttributeAsInt("resourceId"));
                    }
                    return result;
                }
            }) {

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                delete(selection);
            }
        });
        addTableAction(MSG.common_button_ack(), MSG.view_alerts_ack_confirm(), new ResourceAuthorizedTableAction(
            AlertHistoryView.this, TableActionEnablement.ANY, (hasWriteAccess ? null : Permission.MANAGE_ALERTS),
            new RecordExtractor<Integer>() {
                public Collection<Integer> extract(Record[] records) {
                    List<Integer> result = new ArrayList<Integer>(records.length);
                    for (Record record : records) {
                        result.add(record.getAttributeAsInt("resourceId"));
                    }
                    return result;
                }
            }) {

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                acknowledge(selection);
            }
        });

        if (canSupportDeleteAndAcknowledgeAll()) {
            addTableAction(MSG.common_button_delete_all(), MSG.view_alerts_delete_confirm_all(), new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    ListGrid grid = getListGrid();
                    ResultSet resultSet = (null != grid) ? grid.getResultSet() : null;
                    return (hasWriteAccess && grid != null && resultSet != null && !resultSet.isEmpty());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteAll();
                }
            });
            addTableAction(MSG.common_button_ack_all(), MSG.view_alerts_ack_confirm_all(), new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    ListGrid grid = getListGrid();
                    ResultSet resultSet = (null != grid) ? grid.getResultSet() : null;
                    return (hasWriteAccess && grid != null && resultSet != null && !resultSet.isEmpty());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    acknowledgeAll();
                }
            });
        }
    }

    protected void delete(ListGridRecord[] records) {
        final int[] alertIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer alertId = record.getAttributeAsInt("id");
            alertIds[i] = alertId;
        }

        GWTServiceLookup.getAlertService().deleteAlerts(alertIds, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_delete_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh(true);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler()
                    .handleError(MSG.view_alerts_delete_failure(Arrays.toString(alertIds)), caught);
                refreshTableInfo();
            }
        });
    }

    protected void deleteAll() {
        int rpcTimeout = 10000 + getListGrid().getTotalRows();
        GWTServiceLookup.getAlertService(rpcTimeout).deleteAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_delete_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh(true);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_delete_failure_all(), caught);
                refreshTableInfo();
            }
        });
    }

    public void acknowledge(ListGridRecord[] records) {
        final int[] alertIds = new int[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer alertId = record.getAttributeAsInt("id");
            alertIds[i] = alertId;
        }

        GWTServiceLookup.getAlertService().acknowledgeAlerts(alertIds, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_ack_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_ack_failure(Arrays.toString(alertIds)), caught);
                refreshTableInfo();
            }
        });
    }

    protected void acknowledgeAll() {
        int rpcTimeout = 10000 + getListGrid().getTotalRows();
        GWTServiceLookup.getAlertService(rpcTimeout).acknowledgeAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_ack_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_ack_failure_all(), caught);
                refreshTableInfo();
            }
        });
    }

    @Override
    public Canvas getDetailsView(Integer alertId) {
        return AlertDetailsView.getInstance();
    }

    public EntityContext getContext() {
        return context;
    }

    @Override
    public ViewName getViewName() {
        return SUBSYSTEM_VIEW_ID;
    }
}
