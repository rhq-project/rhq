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
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
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
 */
public class AlertHistoryView extends TableSection {
    // TODO: Create a subclass for the subsystem view.
    public static final String SUBSYSTEM_VIEW_ID = "RecentAlerts";
    private static final String SUBSYSTEM_VIEW_TITLE = "Recent Alerts";

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME,
        SortDirection.DESCENDING);
    EntityContext context;
    boolean hasWriteAccess;

    // for subsystem views
    public AlertHistoryView(String locatorId) {
        this(locatorId, SUBSYSTEM_VIEW_TITLE, EntityContext.forSubsystemView(), false);
    }

    protected AlertHistoryView(String locatorId, String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(locatorId, tableTitle, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(new AlertDataSource(context));
    }

    @Override
    protected void configureTableFilters() {
        final EnumSelectItem priorityFilter = new EnumSelectItem("severities", "Priority Filter", AlertPriority.class);

        setFilterFormItems(priorityFilter);
    }

    @Override
    protected void configureTable() {
        ListGridField ctimeField = new ListGridField("ctime", "Creation Time", 100);
        ctimeField.setCellFormatter(new TimestampCellFormatter());

        ListGridField nameField = new ListGridField("name", "Name", 100);
        ListGridField conditionTextField = new ListGridField("conditionText", "Details");
        ListGridField priorityField = new ListGridField("priority", "Priority", 50);

        ListGridField statusField = new ListGridField("status", "Status", 175);
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String ackTime = listGridRecord.getAttribute("acknowledgeTime");
                String ackSubject = listGridRecord.getAttribute("acknowledgingSubject");
                if (ackSubject == null) {
                    return "Not Yet Acknowledged";
                } else {
                    String formattedTime = TimestampCellFormatter.DATE_TIME_FORMAT.format(new Date(Long
                        .parseLong((String) ackTime)));
                    return " Acknowledged on " + formattedTime + "<br/>by " + ackSubject;
                }
            }
        });

        ListGridField resourceNameField = new ListGridField("resourceName", "Resource", 125);
        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer resourceId = listGridRecord.getAttributeAsInt("resourceId");
                return "<a href=\"" + LinkManager.getResourceLink(resourceId) + "\">" + o + "</a>";
            }
        });

        if (context.type == EntityContext.Type.Resource) {
            getListGrid().setFields(ctimeField, nameField, conditionTextField, priorityField, statusField);
        } else {
            getListGrid().setFields(ctimeField, nameField, conditionTextField, priorityField, statusField,
                resourceNameField);
        }

        setupTableInteractions();
    }

    private void setupTableInteractions() {
        addTableAction("DeleteAlert", "Delete", hasWriteAccess ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Delete the selected alert(s)?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    delete(selection);
                }
            });
        addTableAction("DeleteAll", "Delete All", hasWriteAccess ? SelectionEnablement.ALWAYS
            : SelectionEnablement.NEVER, "Delete all alerts from this source?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                deleteAll();
            }
        });
        addTableAction("AcknowledgeAlert", "Ack", hasWriteAccess ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Ack the selected alert(s)?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    acknowledge(selection);
                }
            });
        addTableAction("AcknowledgeAll", "Ack All", hasWriteAccess ? SelectionEnablement.ALWAYS
            : SelectionEnablement.NEVER, "Ack all alerts from this source?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
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
                    new Message("Deleted [" + resultCount + "] alerts", Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to delete alerts with id's: " + Arrays.toString(alertIds), caught);
            }
        });
    }

    void deleteAll() {
        GWTServiceLookup.getAlertService().deleteAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Deleted [" + resultCount + "] alerts", Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to delete all alerts from this source", caught);
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
                    new Message("Acknowledged [" + resultCount + "] alerts", Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to acknowledge Alerts with id's: " + Arrays.toString(alertIds), caught);
            }
        });
    }

    void acknowledgeAll() {
        GWTServiceLookup.getAlertService().acknowledgeAlertsByContext(context, new AsyncCallback<Integer>() {
            public void onSuccess(Integer resultCount) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Acknowledged [" + resultCount + "] alerts", Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to acknowledged all alerts from this source", caught);
            }
        });
    }

    @Override
    public Canvas getDetailsView(int alertId) {
        return AlertDetailsView.getInstance();
    }

}
