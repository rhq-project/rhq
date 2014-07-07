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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.rpc.DataSourceResponseStatistics;

/**
 * A view that gives a display of statistics for datasource responses.
 *  
 * @see RPCDataSource
 *
 * @author John Mazzitelli
 */
public class TestDataSourceResponseStatisticsView extends Table {

    public static void showInWindow() {
        new StatisticsWindow().show();
    }

    private static final String TABLE_TITLE_PREFIX = "DataSource Response Statistics";
    private static String timerTitleString = null;

    private static String getTableTitle() {
        String t = TABLE_TITLE_PREFIX + (DataSourceResponseStatistics.isEnableCollection() ? " (COLLECTING DATA)" : "");
        if (timerTitleString != null) {
            t += timerTitleString;
        }
        return t;
    }

    // these are used both as the name of the fields, titles of the headers and the columns of the CSV output
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_REQUEST_ID = "requestId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TOTAL_ROWS = "totalRows";
    private static final String FIELD_START_ROW = "startRow";
    private static final String FIELD_END_ROW = "endRow";

    private static final SortSpecifier[] defaultSorts = new SortSpecifier[] { new SortSpecifier(FIELD_TIMESTAMP,
        SortDirection.ASCENDING) };

    // if this is not null, this view is hosted by this standalone Window
    private StatisticsWindow window = null;
    private Timer refreshTimer = null;
    private boolean refreshOnPageChange = false;

    public TestDataSourceResponseStatisticsView() {
        super(getTableTitle(), null, defaultSorts, null, false);

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
        ListGridField timestamp = new ListGridField(FIELD_TIMESTAMP, "Timestamp");
        ListGridField reqId = new ListGridField(FIELD_REQUEST_ID, "Request ID");
        ListGridField status = new ListGridField(FIELD_STATUS, "Status");
        ListGridField totalRows = new ListGridField(FIELD_TOTAL_ROWS, "Total Rows");
        ListGridField startRow = new ListGridField(FIELD_START_ROW, "Start Row");
        ListGridField endRow = new ListGridField(FIELD_END_ROW, "End Row");

        timestamp.setType(ListGridFieldType.DATE);
        TimestampCellFormatter.prepareDateField(timestamp);

        status.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return "?";
                }
                int statusNum = ((Integer) value).intValue();
                if (statusNum == DSResponse.STATUS_SUCCESS) {
                    return "SUCCESS";
                } else if (statusNum == DSResponse.STATUS_FAILURE) {
                    return "FAILURE";
                } else if (statusNum == DSResponse.STATUS_SERVER_TIMEOUT) {
                    return "SERVER TIMEOUT";
                } else if (statusNum == DSResponse.STATUS_TRANSPORT_ERROR) {
                    return "TRANSPORT ERROR";
                }
                return value.toString(); // just return the actual number for all other, rare, error codes
            }
        });

        timestamp.setAlign(Alignment.LEFT);
        reqId.setAlign(Alignment.RIGHT);
        status.setAlign(Alignment.CENTER);
        totalRows.setAlign(Alignment.CENTER);
        startRow.setAlign(Alignment.CENTER);
        endRow.setAlign(Alignment.CENTER);

        timestamp.setWidth("20%");
        reqId.setWidth("*");
        status.setWidth("10%");
        totalRows.setWidth("10%");
        startRow.setWidth("10%");
        endRow.setWidth("10%");

        getListGrid().setFields(timestamp, reqId, status, totalRows, startRow, endRow);
        refresh();

        addTableAction("Toggle On/Off", MSG.common_msg_areYouSure(), ButtonColor.BLUE, new AbstractTableAction(
            TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                boolean toBeEnabled = !DataSourceResponseStatistics.isEnableCollection();
                DataSourceResponseStatistics.setEnableCollection(toBeEnabled);

                if (!toBeEnabled) {
                    // disabling collection - no need to periodically refresh anymore so cancel timers
                    refreshTimer.cancel();
                    refreshOnPageChange = false;
                    timerTitleString = null;
                }

                updateTitleCanvas(getTableTitle());
                if (window != null) {
                    window.setTitle(getTableTitle());
                }
                refresh();
            }
        });

        addTableAction(MSG.common_button_delete_all(), MSG.common_msg_areYouSure(), ButtonColor.RED, new AbstractTableAction(
            TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                DataSourceResponseStatistics.clearAll();
                refresh();
            }
        });

        addTableAction("Export To CSV", new AbstractTableAction(TableActionEnablement.ALWAYS) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                StringBuilder csv = new StringBuilder();
                csv.append(FIELD_TIMESTAMP).append(',') //
                    .append(FIELD_REQUEST_ID).append(',') //
                    .append(FIELD_STATUS).append(',') //
                    .append(FIELD_TOTAL_ROWS).append(',') //
                    .append(FIELD_START_ROW).append(',') //
                    .append(FIELD_END_ROW).append('\n');

                RecordList records = getListGrid().getDataAsRecordList();
                int recordsSize = records.getLength();
                for (int i = 0; i < recordsSize; i++) {
                    Record record = records.get(i);
                    csv.append(record.getAttributeAsDate(FIELD_TIMESTAMP)).append(',') //
                        .append(record.getAttribute(FIELD_REQUEST_ID)).append(',') //
                        .append(record.getAttribute(FIELD_STATUS)).append(',') //
                        .append(record.getAttribute(FIELD_TOTAL_ROWS)).append(',') //
                        .append(record.getAttribute(FIELD_START_ROW)).append(',') //
                        .append(record.getAttribute(FIELD_END_ROW)).append('\n');
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

            addTableAction("Refresh Timer", null, timerValues, ButtonColor.GRAY, new AbstractTableAction(
                TableActionEnablement.ALWAYS) {
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
                        timerTitleString = null;
                    } else if (timeout.intValue() == 0) {
                        refreshOnPageChange = true;
                        timerTitleString = " (refresh on page change)";
                    } else {
                        refreshTimer.scheduleRepeating(timeout.intValue() * 1000);
                        timerTitleString = " (refresh every " + timeout + "s)";
                    }

                    updateTitleCanvas(getTableTitle());
                    if (window != null) {
                        window.setTitle(getTableTitle());
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
        getListGrid().setRecords(transform(DataSourceResponseStatistics.getAll()));
        if (window != null) {
            window.blink();
        }
        refreshTableInfo();
    }

    private ListGridRecord[] transform(ArrayList<DataSourceResponseStatistics.Record> arrayList) {
        ListGridRecord[] results = new ListGridRecord[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            results[i] = transform(arrayList.get(i));
        }
        return results;
    }

    private ListGridRecord transform(DataSourceResponseStatistics.Record stat) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_TIMESTAMP, stat.timestamp);
        record.setAttribute(FIELD_REQUEST_ID, stat.requestId);
        record.setAttribute(FIELD_STATUS, stat.status);
        record.setAttribute(FIELD_TOTAL_ROWS, stat.totalRows);
        record.setAttribute(FIELD_START_ROW, stat.startRow);
        record.setAttribute(FIELD_END_ROW, stat.endRow);
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

            final TestDataSourceResponseStatisticsView view;
            view = new TestDataSourceResponseStatisticsView();
            view.window = this;

            setTitle(getTableTitle());
            setShowMinimizeButton(true);
            setShowMaximizeButton(true);
            setShowCloseButton(true);
            setIsModal(false);
            setShowModalMask(false);
            setWidth(800);
            setHeight(400);
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
                    setTitle(getTableTitle());
                }
            };
        }

        public void blink() {
            // window.flash() isn't working so do it ourselves
            if (getMinimized()) {
                setTitle(getTableTitle() + " *");
            } else {
                setBodyColor(getHiliteBodyColor());
            }
            redraw();
            blinkTimer.schedule(250);
        }
    }
}
