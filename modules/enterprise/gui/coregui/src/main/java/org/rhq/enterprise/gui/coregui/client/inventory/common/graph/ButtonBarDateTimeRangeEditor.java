/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.TimeItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.measurement.RefreshIntervalMenu;
import org.rhq.enterprise.gui.coregui.client.inventory.AutoRefresh;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs using a radio button group.
 * The DateTimeButton enum defines the button labels and time ranges change this if you
 * wish to add/delete custom time ranges.
 *
 * @author Mike Thompson
 */
public class ButtonBarDateTimeRangeEditor extends EnhancedVLayout {

    private static final String TIMERANGE = "graphtimerange";
    private static final int BUTTON_WIDTH = 28;

    private MeasurementUserPreferences measurementUserPreferences;
    private Refreshable d3GraphListView;
    private Label dateRangeLabel;
    private static final DateTimeFormat fmt = DateTimeFormat.getFormat(MSG.common_buttonbar_datetime_format());
    private DateTimeButtonBarClickHandler dateTimeButtonBarClickHandler;
    private AbstractMeasurementRangeEditor.MetricRangePreferences prefs;
    final private ButtonBarDateTimeRangeEditor self;
    private RefreshIntervalMenu refreshIntervalMenu;
    private boolean allowPreferenceUpdateRefresh;

    public ButtonBarDateTimeRangeEditor(MeasurementUserPreferences measurementUserPrefs, Refreshable d3GraphListView) {
        this.self = this;
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;
        // if the encompassing view already handles its own refresh (e.g. AbstractD3GrpahListView) then don't
        // let a preference update cause a whole gui refresh (which it does by default to apply the new preference).
        // the two refreshes are redundant at best, step on each other at worst..
        this.allowPreferenceUpdateRefresh = !(this.d3GraphListView instanceof AutoRefresh);

        dateTimeButtonBarClickHandler = new DateTimeButtonBarClickHandler();
        prefs = measurementUserPreferences.getMetricRangePreferences();
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start Date: " + new Date(prefs.begin) + " end Date: "
            + new Date(prefs.end));

    }

    private void createButtons() {

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setHeight(34);

        toolStrip.addSpacer(10);

        for (DateTimeButton dateTimeButton : DateTimeButton.values()) {
            IButton oneHourButton = new IButton(dateTimeButton.label);
            oneHourButton.setWidth(BUTTON_WIDTH);
            oneHourButton.setActionType(SelectionType.RADIO);
            oneHourButton.setRadioGroup(TIMERANGE);
            oneHourButton.addClickHandler(dateTimeButtonBarClickHandler);

            toolStrip.addMember(oneHourButton);
        }

        IButton customButton = new IButton(MSG.common_buttonbar_custom());
        customButton.setWidth(60);
        customButton.setActionType(SelectionType.RADIO);
        customButton.setRadioGroup(TIMERANGE);
        customButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                CustomDateRangeWindow customDateRangeWindow = new CustomDateRangeWindow(MSG
                    .common_buttonbar_custom_window_title(), MSG.common_buttonbar_custom_window_subtitle(), self,
                    new Date(prefs.begin), new Date(prefs.end));
                customDateRangeWindow.show();
            }
        });
        toolStrip.addMember(customButton);

        toolStrip.addSpacer(30);

        dateRangeLabel = new Label();
        dateRangeLabel.setWidth(400);
        dateRangeLabel.addStyleName("graphDateTimeRangeLabel");
        updateDateTimeRangeDisplay(new Date(prefs.begin), new Date(prefs.end));
        toolStrip.addMember(dateRangeLabel);

        toolStrip.addSpacer(20);
        refreshIntervalMenu = new RefreshIntervalMenu();
        toolStrip.addMember(refreshIntervalMenu);

        addMember(toolStrip);
    }

    public void redrawGraphs() {
        d3GraphListView.refreshData();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createButtons();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createButtons();
    }

    public Label getDateRangeLabel() {
        return dateRangeLabel;
    }

    public Long getStartTime() {
        return measurementUserPreferences.getMetricRangePreferences().begin;
    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

    public Date calculateStartDate(Date endDate, String dateTimeSelection) {
        long dateTimeOffset = 0;
        for (DateTimeButton dateTimeButton : DateTimeButton.values()) {
            if (dateTimeButton.label.equals(dateTimeSelection)) {
                dateTimeOffset = dateTimeButton.timeSpanInSeconds * 1000;
                break;
            }
        }

        Log.debug("DateTimeSelection: " + dateTimeSelection + " = " + dateTimeOffset);
        return new Date(endDate.getTime() - dateTimeOffset);
    }

    public void updateDateTimeRangeDisplay(Date startDate, Date endDate) {
        String rangeString = fmt.format(startDate) + " - " + fmt.format(endDate);
        if (null != dateRangeLabel) {
            dateRangeLabel.setContents(rangeString);
        }
    }

    /**
     * Whenever we make a change to the date range save it here so it gets propagated to
     * the correct places.
     *
     * @param startTime double because JSNI doesn't support long
     * @param endTime   double because JSNI doesn't support long
     */
    public void saveDateRange(double startTime, double endTime) {
        final boolean advanced = true;
        prefs.explicitBeginEnd = advanced;
        prefs.begin = (long) startTime;
        prefs.end = (long) endTime;
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs, allowPreferenceUpdateRefresh);
        }

    }

    private class DateTimeButtonBarClickHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent clickEvent) {
            IButton button = (IButton) clickEvent.getSource();
            String selectedDateTimeRange = button.getTitle();
            Date calculatedStartDateTime = calculateStartDate(new Date(), selectedDateTimeRange);
            saveDateRange(calculatedStartDateTime.getTime(), new Date().getTime());
            redrawGraphs();
            updateDateTimeRangeDisplay(calculatedStartDateTime, new Date());
        }
    }

    @SuppressWarnings("GwtInconsistentSerializableClass")
    /**
     * This enum defines the button labels and time ranges used in the toolbar.
     */
    private enum DateTimeButton {
        oneHour("1h", 60 * 60), fourHour("4h", 4 * 60 * 60), eightHour("8h", 8 * 60 * 60), twelveHour("12h",
            12 * 60 * 60), oneDay("1d", 24 * 60 * 60), fiveDay("5d", 5 * 24 * 60 * 60), oneMonth("1m",
            30 * 24 * 60 * 60), threeMonth("3m", 3 * 30 * 24 * 60 * 60), sixMonth("6m", 6 * 30 * 24 * 60 * 60);

        private final String label;
        private final long timeSpanInSeconds;
        private final ClickHandler clickHandler;

        DateTimeButton(String label, long timeSpanInSeconds) {
            this.label = label;
            this.timeSpanInSeconds = timeSpanInSeconds;
            this.clickHandler = new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    IButton button = (IButton) clickEvent.getSource();
                    Log.debug("Button pressed for: " + button.getTitle());

                }
            };

        }

    }

    public class CustomDateRangeWindow extends Window {

        public CustomDateRangeWindow(String title, String windowTitle,
            final ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor, Date startTime, Date endTime) {
            super();
            setTitle(windowTitle + ": " + title);
            setShowMinimizeButton(false);
            setShowMaximizeButton(false);
            setShowCloseButton(true);
            setIsModal(true);
            setShowModalMask(true);
            setWidth(450);
            setHeight(340);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();
            DynamicForm form = new DynamicForm();
            form.setMargin(25);
            form.setAutoFocus(true);
            form.setShowErrorText(true);
            form.setErrorOrientation(FormErrorOrientation.BOTTOM);
            form.setHeight100();
            form.setWidth100();
            form.setPadding(5);
            form.setLayoutAlign(VerticalAlignment.BOTTOM);
            final DateItem startDateItem = new DateItem("startDate", MSG.common_buttonbar_start_date());
            startDateItem.setValue(startTime);
            final TimeItem startTimeItem = new TimeItem("startTime", MSG.common_buttonbar_start_time());
            startTimeItem.setValue(startTime);
            final DateItem endDateItem = new DateItem("endDate", MSG.common_buttonbar_end_date());
            endDateItem.setValue(endTime);
            final TimeItem endTimeItem = new TimeItem("endTime", MSG.common_buttonbar_end_time());
            endTimeItem.setValue(endTime);
            form.setFields(startDateItem, startTimeItem, new RowSpacerItem(), endDateItem, endTimeItem,
                new RowSpacerItem());
            this.addItem(form);

            HLayout buttonHLayout = new HLayout();
            buttonHLayout.setMargin(75);
            buttonHLayout.setMembersMargin(20);
            IButton cancelButton = new IButton(MSG.common_buttonbar_custom_cancel());
            cancelButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    CustomDateRangeWindow.this.destroy();
                }
            });
            buttonHLayout.addMember(cancelButton);

            IButton saveButton = new IButton(MSG.common_buttonbar_custom_save());
            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    Log.debug("Saving Custom Date Range Window.");
                    Date startTimeDate = (Date) startTimeItem.getValue();
                    Date endTimeDate = (Date) endTimeItem.getValue();

                    Date newStartDate = new Date(startDateItem.getValueAsDate().getYear(), startDateItem
                        .getValueAsDate().getMonth(), startDateItem.getValueAsDate().getDate(), startTimeDate
                        .getHours(), startTimeDate.getMinutes());
                    Date newEndDate = new Date(endDateItem.getValueAsDate().getYear(), endDateItem.getValueAsDate()
                        .getMonth(), endDateItem.getValueAsDate().getDate(), endTimeDate.getHours(), endTimeDate
                        .getMinutes());
                    buttonBarDateTimeRangeEditor.saveDateRange(newStartDate.getTime(), newEndDate.getTime());
                    redrawGraphs();
                    updateDateTimeRangeDisplay(startDateItem.getValueAsDate(), endDateItem.getValueAsDate());
                    CustomDateRangeWindow.this.destroy();
                }
            });

            buttonHLayout.addMember(saveButton);
            addItem(buttonHLayout);

            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    try {
                        CustomDateRangeWindow.this.destroy();
                    } catch (Throwable e) {
                        Log.warn("Cannot destroy custom date range window.", e);
                    }
                }
            });

        }
    }

}
