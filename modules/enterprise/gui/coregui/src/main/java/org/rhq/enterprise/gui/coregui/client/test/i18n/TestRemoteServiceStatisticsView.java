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
package org.rhq.enterprise.gui.coregui.client.test.i18n;

import java.util.List;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.util.rpc.RemoteServiceStatistics;
import org.rhq.enterprise.gui.coregui.client.util.rpc.RemoteServiceStatistics.Record.Summary;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * A view that gives a display of statistics for all remote services executed since the application was loaded.
 *  
 * @author Joseph Marques
 */
public class TestRemoteServiceStatisticsView extends Table {

    // these are used both as the name of the fields, titles of the headers and the columns of the CSV output
    private static final String FIELD_SERVICENAME = "serviceName";
    private static final String FIELD_METHODNAME = "methodName";
    private static final String FIELD_COUNT = "count";
    private static final String FIELD_SLOWEST = "slowest";
    private static final String FIELD_AVERAGE = "average";
    private static final String FIELD_FASTEST = "fastest";
    private static final String FIELD_STDDEV = "stddev";

    private static final SortSpecifier[] defaultSorts = new SortSpecifier[] { new SortSpecifier("average",
        SortDirection.DESCENDING) };

    public TestRemoteServiceStatisticsView(String locatorId) {
        super(locatorId, "Remote Service Statistics", null, defaultSorts, null, false);
    }

    @Override
    protected void configureTable() {
        ListGridField serviceName = new ListGridField(FIELD_SERVICENAME, "Service Name");
        ListGridField methodName = new ListGridField(FIELD_METHODNAME, "Method Name");
        ListGridField count = new ListGridField(FIELD_COUNT, "Count");
        count.setAlign(Alignment.CENTER);
        ListGridField slowest = new ListGridField(FIELD_SLOWEST, "Slowest (ms)");
        slowest.setAlign(Alignment.RIGHT);
        ListGridField average = new ListGridField(FIELD_AVERAGE, "Average (ms)");
        average.setAlign(Alignment.RIGHT);
        ListGridField fastest = new ListGridField(FIELD_FASTEST, "Fastest (ms)");
        fastest.setAlign(Alignment.RIGHT);
        ListGridField stddev = new ListGridField(FIELD_STDDEV, "Std Dev");
        stddev.setAlign(Alignment.RIGHT);

        getListGrid().setFields(serviceName, methodName, count, slowest, average, fastest, stddev);
        refresh();

        addTableAction(extendLocatorId("deleteAll"), MSG.common_button_delete_all(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    RemoteServiceStatistics.clearAll();
                    refresh();
                }
            });

        addTableAction(extendLocatorId("refresh"), MSG.common_button_refresh(), new AbstractTableAction(
            TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                refresh();
            }
        });

        addTableAction(extendLocatorId("export"), "Export To CSV",
            new AbstractTableAction(TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    StringBuilder csv = new StringBuilder();
                    csv.append(FIELD_SERVICENAME).append(',') //
                        .append(FIELD_METHODNAME).append(',') //
                        .append(FIELD_COUNT).append(',') //
                        .append(FIELD_SLOWEST).append(',') //
                        .append(FIELD_AVERAGE).append(',') //
                        .append(FIELD_FASTEST).append(',') //
                        .append(FIELD_STDDEV).append('\n');

                    RecordList records = getListGrid().getDataAsRecordList();
                    int recordsSize = records.getLength();
                    for (int i = 0; i < recordsSize; i++) {
                        Record record = records.get(i);
                        csv.append(record.getAttribute(FIELD_SERVICENAME)).append(',') //
                            .append(record.getAttribute(FIELD_METHODNAME)).append(',') //
                            .append(record.getAttribute(FIELD_COUNT)).append(',') //
                            .append(record.getAttribute(FIELD_SLOWEST)).append(',') //
                            .append(record.getAttribute(FIELD_AVERAGE)).append(',') //
                            .append(record.getAttribute(FIELD_FASTEST)).append(',') //
                            .append(record.getAttribute(FIELD_STDDEV)).append('\n');
                    }

                    new MessageWindow(extendLocatorId("csv"), "Export To CSV", "<pre>" + csv.toString() + "</pre>")
                        .show();
                }
            });
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.NONE;
    }

    @Override
    public boolean isShowFooterRefresh() {
        return false;
    }

    @Override
    public void refresh() {
        super.refresh();
        getListGrid().setRecords(transform(RemoteServiceStatistics.getAll()));
    }

    private ListGridRecord[] transform(List<Summary> stats) {
        ListGridRecord[] results = new ListGridRecord[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            results[i] = transform(stats.get(i));
        }
        return results;
    }

    private ListGridRecord transform(Summary stat) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_SERVICENAME, stat.serviceName);
        record.setAttribute(FIELD_METHODNAME, stat.methodName);
        record.setAttribute(FIELD_COUNT, stat.count);
        record.setAttribute(FIELD_SLOWEST, stat.slowest);
        record.setAttribute(FIELD_AVERAGE, stat.average);
        record.setAttribute(FIELD_FASTEST, stat.fastest);
        record.setAttribute(FIELD_STDDEV, stat.stddev);
        return record;
    }

    class MessageWindow extends LocatableWindow {
        public MessageWindow(String locatorId, String title, String message) {
            super(locatorId);

            LocatableHTMLPane htmlPane = new LocatableHTMLPane(extendLocatorId("winDetailsPane"));
            htmlPane.setMargin(10);
            htmlPane.setDefaultWidth(500);
            htmlPane.setDefaultHeight(400);
            htmlPane.setContents(message);

            setupWindow(title, htmlPane);
        }

        private void setupWindow(String title, Canvas item) {
            setTitle(title);
            setShowMinimizeButton(false);
            setShowMaximizeButton(true);
            setIsModal(true);
            setShowModalMask(true);
            setAutoSize(true);
            setAutoCenter(true);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClientEvent event) {
                    markForDestroy();
                }
            });
            addItem(item);
        }
    }
}
