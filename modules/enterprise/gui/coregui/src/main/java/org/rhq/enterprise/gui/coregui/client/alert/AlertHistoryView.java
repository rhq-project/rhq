/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.alert;

import java.util.Arrays;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter those alerts, sort those alerts, double-click a row to view full details an alert, and perform
 * various operations on the the alerts: delete selected, delete all from source, ack selected, ack all from source.
 * This view full respects the user's authorization, and will not allow operations on the alerts unless the user is
 * either the inventory manager or has MANAGE_ALERTS permission on every resource corresponding to the alerts being
 * operated on.
 *
 * @author Joseph Marques
 * @author Ian Springer
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class AlertHistoryView extends TableSection {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("RecentAlerts", MSG.common_title_recent_alerts());

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME,
        SortDirection.DESCENDING);
    EntityContext context;
    boolean hasWriteAccess;

    // for subsystem views
    public AlertHistoryView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), false);
    }

    protected AlertHistoryView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(new AlertDataSource(context));
    }

    @Override
    protected void configureTableFilters() {
        SelectItem priorityFilter = new SelectItem("severities", MSG.view_alerts_table_filter_priority());
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
        priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
        priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
        priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        priorityFilter.setValueMap(priorities);
        priorityFilter.setValueIcons(priorityIcons);
        priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());

        if (isShowFilterForm()) {
            setFilterFormItems(priorityFilter);
        }
    }

    @Override
    protected void configureTable() {
        getListGrid().setFields(((AlertDataSource) getDataSource()).getListGridFields().toArray(new ListGridField[0]));
        setupTableInteractions();
    }

    private void setupTableInteractions() {
        TableActionEnablement singleTargetEnablement = hasWriteAccess ? TableActionEnablement.ANY
            : TableActionEnablement.NEVER;
        TableActionEnablement multipleTargetEnablement = hasWriteAccess ? TableActionEnablement.ALWAYS
            : TableActionEnablement.NEVER;

        addTableAction("DeleteAlert", MSG.common_button_delete(), MSG.view_alerts_delete_confirm(),
            new AbstractTableAction(singleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    delete(selection);
                }
            });
        addTableAction("DeleteAll", MSG.common_button_delete_all(), MSG.view_alerts_delete_confirm_all(),
            new AbstractTableAction(multipleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteAll();
                }
            });
        addTableAction("AcknowledgeAlert", MSG.common_button_ack(), MSG.view_alerts_ack_confirm(),
            new AbstractTableAction(singleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    acknowledge(selection);
                }
            });
        addTableAction("AcknowledgeAll", MSG.common_button_ack_all(), MSG.view_alerts_ack_confirm_all(),
            new AbstractTableAction(multipleTargetEnablement) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    acknowledgeAll();
                }
            });
    }

    void delete(ListGridRecord[] records) {
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
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler()
                    .handleError(MSG.view_alerts_delete_failure(Arrays.toString(alertIds)), caught);
            }
        });
    }

    void deleteAll() {
        GWTServiceLookup.getAlertService().deleteAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_delete_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_delete_failure_all(), caught);
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
            }
        });
    }

    void acknowledgeAll() {
        GWTServiceLookup.getAlertService().acknowledgeAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_alerts_ack_success(String.valueOf(resultCount)), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_ack_failure_all(), caught);
            }
        });
    }

    @Override
    public Canvas getDetailsView(int alertId) {
        return AlertDetailsView.getInstance();
    }

}
