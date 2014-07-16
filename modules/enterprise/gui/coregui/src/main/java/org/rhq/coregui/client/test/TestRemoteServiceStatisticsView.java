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
package org.rhq.coregui.client.test;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.rpc.RemoteServiceStatistics;
import org.rhq.coregui.client.util.rpc.RemoteServiceStatistics.Record.Summary;

/**
 * A view that gives a display of statistics for all remote services executed since the application was loaded.
 *  
 * @author Joseph Marques
 */
public class TestRemoteServiceStatisticsView extends Table {

    public static void showInWindow() {
        new StatisticsWindow().show();
    }

    public static final String TABLE_TITLE = "Remote Service Statistics";

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

    // if this is not null, this view is hosted by this standalone Window
    private StatisticsWindow window = null;
    private Timer refreshTimer = null;
    private boolean refreshOnPageChange = false;

    public TestRemoteServiceStatisticsView() {
        super(TABLE_TITLE, null, defaultSorts, null, false);

        refreshTimer = new Timer() {
            @Override
            public void run() {
                refresh();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureTable() {
        ListGridField serviceName = new ListGridField(FIELD_SERVICENAME, "Service Name");
        ListGridField methodName = new ListGridField(FIELD_METHODNAME, "Method Name");
        ListGridField count = new ListGridField(FIELD_COUNT, "Count");
        ListGridField slowest = new ListGridField(FIELD_SLOWEST, "Slowest (ms)");
        ListGridField average = new ListGridField(FIELD_AVERAGE, "Average (ms)");
        ListGridField fastest = new ListGridField(FIELD_FASTEST, "Fastest (ms)");
        ListGridField stddev = new ListGridField(FIELD_STDDEV, "Std Dev");

        count.setAlign(Alignment.CENTER);
        slowest.setAlign(Alignment.RIGHT);
        average.setAlign(Alignment.RIGHT);
        fastest.setAlign(Alignment.RIGHT);
        stddev.setAlign(Alignment.RIGHT);

        serviceName.setWidth("20%");
        methodName.setWidth("*");
        count.setWidth("8%");
        slowest.setWidth("13%");
        average.setWidth("13%");
        fastest.setWidth("13%");
        stddev.setWidth("8%");

        getListGrid().setFields(serviceName, methodName, count, slowest, average, fastest, stddev);
        refresh();

        addTableAction(MSG.common_button_delete_all(), MSG.common_msg_areYouSure(), ButtonColor.RED, new AbstractTableAction(
            TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                RemoteServiceStatistics.clearAll();
                refresh();
            }
        });

        addTableAction("Export To CSV", new AbstractTableAction(TableActionEnablement.ALWAYS) {
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

                new MessageWindow("Export To CSV", "<pre>" + csv.toString() + "</pre>").show();

                refresh();
            }
        });

        addTableAction(MSG.common_button_refresh(), new AbstractTableAction(TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                refresh();
            }
        });

        if (window != null) {
            LinkedHashMap<String, Integer> timerValues = new LinkedHashMap<String, Integer>();
            timerValues.put("Now", Integer.valueOf("-2"));
            timerValues.put(MSG.common_val_never(), Integer.valueOf("-1"));
            timerValues.put("On Page Change", Integer.valueOf("0"));
            timerValues.put("1", Integer.valueOf("1"));
            timerValues.put("5", Integer.valueOf("5"));
            timerValues.put("10", Integer.valueOf("10"));
            timerValues.put("30", Integer.valueOf("30"));
            timerValues.put("60", Integer.valueOf("60"));
            History.addValueChangeHandler(new ValueChangeHandler<String>() {
                @Override
                public void onValueChange(ValueChangeEvent<String> event) {
                    if (refreshOnPageChange) {
                        refresh();
                    }
                }
            });

            addTableAction("Refresh Timer", null, timerValues, ButtonColor.RED, new AbstractTableAction(TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {

                    Integer timeout = (Integer) actionValue;

                    // if being asked to refresh now, just refresh but don't touch our schedules
                    if (timeout == null || timeout.intValue() == -2) {
                        refresh();
                        return;
                    }

                    // cancel everything - will reinstate if user elected to do one of these
                    refreshTimer.cancel();
                    refreshOnPageChange = false;

                    if (timeout.intValue() == -1) {
                        updateTitleCanvas(TABLE_TITLE);
                    } else if (timeout.intValue() == 0) {
                        refreshOnPageChange = true;
                        updateTitleCanvas(TABLE_TITLE + " (refresh on page change)");
                    } else {
                        refreshTimer.scheduleRepeating(timeout.intValue() * 1000);
                        updateTitleCanvas(TABLE_TITLE + " (refresh every " + timeout + "s)");
                    }
                    refreshTableInfo();
                }
            });
        } else { // not in the standalone window
            addTableAction("Show In Window", new AbstractTableAction(TableActionEnablement.ALWAYS) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    new StatisticsWindow().show();
                    refresh();
                }
            });
        }

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
        if (window != null) {
            window.blink();
        }
        refreshTableInfo();
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

    class MessageWindow extends Window {
        public MessageWindow(String title, String message) {
            super();

            HTMLPane htmlPane = new HTMLPane();
            htmlPane.setMargin(10);
            htmlPane.setDefaultWidth(600);
            htmlPane.setDefaultHeight(400);
            htmlPane.setContents(message);

            setTitle(title);
            setShowMinimizeButton(false);
            setShowMaximizeButton(true);
            setShowCloseButton(true);
            setDismissOnEscape(false); // force close button to be pressed to ensure our close handler is called
            setIsModal(true);
            setShowModalMask(true);
            setAutoSize(true);
            setAutoCenter(true);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    markForDestroy();
                }
            });
            addItem(htmlPane);
        }
    }

    static class StatisticsWindow extends Window {
        private Timer blinkTimer;

        public StatisticsWindow() {
            super();

            final TestRemoteServiceStatisticsView view;
            view = new TestRemoteServiceStatisticsView();
            view.window = this;

            setTitle(TABLE_TITLE);
            setShowMinimizeButton(true);
            setShowMaximizeButton(true);
            setShowCloseButton(true);
            setIsModal(false);
            setShowModalMask(false);
            setWidth(700);
            setHeight(300);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    view.refreshTimer.cancel();
                    view.refreshOnPageChange = false;
                    view.markForDestroy();
                    markForDestroy();
                }
            });

            addItem(view);

            final String origColor = getBodyColor();
            blinkTimer = new Timer() {
                @Override
                public void run() {
                    setBodyColor(origColor);
                    setTitle(TABLE_TITLE);
                }
            };
        }

        public void blink() {
            // window.flash() isn't working so do it ourselves
            if (getMinimized()) {
                setTitle(TABLE_TITLE + " *");
            } else {
                setBodyColor(getHiliteBodyColor());
            }
            redraw();
            blinkTimer.schedule(250);
        }
    }
}
