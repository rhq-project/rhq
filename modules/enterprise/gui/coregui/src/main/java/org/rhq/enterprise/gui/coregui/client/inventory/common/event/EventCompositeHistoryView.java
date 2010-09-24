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
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Joseph Marques
 */
public class EventCompositeHistoryView extends TableSection {

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier("timestamp", SortDirection.DESCENDING);
    private EntityContext context;
    private boolean hasWriteAccess;

    public static EventCompositeHistoryView get(ResourceGroupComposite composite) {
        String tableTitle = "Group Event History";
        EntityContext context = EntityContext.forGroup(composite.getResourceGroup().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isEvent();
        return new EventCompositeHistoryView(tableTitle, context, hasWriteAccess);
    }

    public static EventCompositeHistoryView get(ResourceComposite composite) {
        String tableTitle = "Resource Event History";
        EntityContext context = EntityContext.forResource(composite.getResource().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isEvent();
        return new EventCompositeHistoryView(tableTitle, context, hasWriteAccess);
    }

    private EventCompositeHistoryView(String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super("EventCompositeHistoryTable", tableTitle, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });
        this.context = context;
        this.hasWriteAccess = hasWriteAccess;

        setDataSource(new EventCompositeDatasource(this.context));
    }

    @Override
    protected void configureTableFilters() {
        final TextItem sourceFilter = new TextItem("source", "Source Filter");
        final TextItem detailsFilter = new TextItem("details", "Details Filter");
        final EnumSelectItem severityFilter = new EnumSelectItem("severities", "Severity Filter", EventSeverity.class);

        setFilterFormItems(sourceFilter, detailsFilter, severityFilter);
    }

    @Override
    protected void configureTable() {
        ListGrid grid = getListGrid();

        grid.setWrapCells(true);
        grid.setFixedRecordHeights(true);

        // getListGrid().getField("id").setWidth(60);

        grid.getField("timestamp").setWidth(125);

        grid.getField("severity").setWidth(75);
        grid.getField("severity").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return Canvas.imgHTML("subsystems/event/" + o + "_16.png", 16, 16) + o;
            }
        });

        grid.getField("source").setWidth(275);
        grid.getField("source").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String sourceLocation = (String) o;
                int length = sourceLocation.length();
                if (length > 40) {
                    return "..." + sourceLocation.substring(length - 40); // the last 40 chars
                }
                return sourceLocation;
            }
        });

        setupTableInteractions();
    }

    private void setupTableInteractions() {
        addTableAction("deleteButton", "Delete", hasWriteAccess ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    deleteButtonPressed(selection);
                }
            });

        addTableAction("purgeAllButton", "Purge All", hasWriteAccess ? SelectionEnablement.ALWAYS
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                purgeButtonPressed();
            }
        });
    }

    private void deleteButtonPressed(ListGridRecord[] selection) {
        List<Integer> eventIds = new ArrayList<Integer>();
        for (ListGridRecord nextRecord : selection) {
            eventIds.add(nextRecord.getAttributeAsInt("id"));
        }
        GWTServiceLookup.getEventService().deleteEventsForContext(context, eventIds, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Successfully deleted " + result + " events for " + context.toShortString(),
                        Severity.Info));
                refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to delete selected events for " + context.toShortString(), caught);
            }
        });
    }

    private void purgeButtonPressed() {
        GWTServiceLookup.getEventService().purgeEventsForContext(context, new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Successfully purged " + result + " events for " + context.toShortString(),
                        Severity.Info));
                refresh();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to purge events for " + context.toShortString(), caught);
            }
        });
    }

    @Override
    public Canvas getDetailsView(int eventId) {
        return EventCompositeDetailsView.getInstance();
    }
}
