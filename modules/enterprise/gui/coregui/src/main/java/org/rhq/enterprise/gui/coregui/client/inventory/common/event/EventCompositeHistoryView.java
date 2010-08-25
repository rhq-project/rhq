/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common.event;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.TableFilterForm;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table.SelectionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Joseph Marques
 */
public abstract class EventCompositeHistoryView extends VLayout {

    private EventCompositeHistoryTable eventCompositeHistoryTable;
    private EntityContext entityContext;
    private boolean permitted;

    public EventCompositeHistoryView() {
        setWidth100();
        setHeight100();
        setMembersMargin(10);
    }

    @Override
    protected void onInit() {
        super.onInit();

        entityContext = getEntityContext();
        eventCompositeHistoryTable = new EventCompositeHistoryTable(getTableTitle(), entityContext);
        permitted = checkModifyPermission();

        setupTableFilters();
        setupTableContents();
    }

    private void setupTableFilters() {
        TableFilterForm form = new TableFilterForm(eventCompositeHistoryTable);

        final TextItem sourceFilter = new TextItem("source", "Source Filter");
        final TextItem detailsFilter = new TextItem("details", "Details Filter");

        final SelectItem severityMultiFilter = new SelectItem("severities", "Severity Filter");
        severityMultiFilter.setMultiple(true);
        severityMultiFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);
        String[] severities = new String[EventSeverity.values().length];
        int i = 0;
        for (EventSeverity nextOption : EventSeverity.values()) {
            severities[i++] = nextOption.name();
        }
        severityMultiFilter.setValueMap(severities);
        severityMultiFilter.setValues(severities); // select them all by default

        form.setItems(sourceFilter, detailsFilter, severityMultiFilter);
        addMember(form);
    }

    private void setupTableContents() {
        eventCompositeHistoryTable.addTableAction("deleteButton", "Delete", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                deleteButtonPressed(selection);
            }
        });

        eventCompositeHistoryTable.addTableAction("purgeAllButton", "Purge All",
            (permitted) ? SelectionEnablement.ALWAYS : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    purgeButtonPressed();
                }
            });

        addMember(eventCompositeHistoryTable);
    }

    protected abstract String getTableTitle();

    protected abstract EntityContext getEntityContext();

    protected abstract boolean checkModifyPermission();

    private void deleteButtonPressed(ListGridRecord[] selection) {
        List<Integer> eventIds = new ArrayList<Integer>();
        for (ListGridRecord nextRecord : selection) {
            eventIds.add(nextRecord.getAttributeAsInt("id"));
        }
        GWTServiceLookup.getEventService().deleteEventsForContext(entityContext, eventIds,
            new AsyncCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Successfully deleted " + result + " events for " + entityContext.toShortString(),
                            Severity.Info));
                    eventCompositeHistoryTable.refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        "Failed to delete selected events for " + entityContext.toShortString(), caught);
                }
            });
    }

    private void purgeButtonPressed() {
        GWTServiceLookup.getEventService().purgeEventsForContext(entityContext, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Successfully purged " + result + " events for " + entityContext.toShortString(),
                        Severity.Info));
                eventCompositeHistoryTable.refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to purge events for " + entityContext.toShortString(),
                    caught);
            }
        });
    }

}
