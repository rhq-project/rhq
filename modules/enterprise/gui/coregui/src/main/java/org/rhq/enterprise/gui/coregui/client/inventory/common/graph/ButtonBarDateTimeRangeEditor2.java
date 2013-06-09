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
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Component to allow selection of Date/Time range for graphs using a button group from bootstrap.
 *
 * @author Mike Thompson
 */
public class ButtonBarDateTimeRangeEditor2 extends EnhancedVLayout {

    static final String TIMERANGE = "timerange";
    static final int BUTTON_WIDTH = 28;

    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractD3GraphListView d3GraphListView;
    private static final Messages MSG = CoreGUI.getMessages();
    private Label dateRangeLabel;
    //@todo: pull dateformat messages.properties
    private static final DateTimeFormat fmt = DateTimeFormat.getFormat("MM/dd/yyyy h:mm a");
    private DateTimeButtonBarClickHandler dateTimeButtonBarClickHandler;
    private AbstractMeasurementRangeEditor.MetricRangePreferences prefs;

    public ButtonBarDateTimeRangeEditor2(MeasurementUserPreferences measurementUserPrefs,
                                         AbstractD3GraphListView d3GraphListView) {
        this.measurementUserPreferences = measurementUserPrefs;
        this.d3GraphListView = d3GraphListView;

        dateTimeButtonBarClickHandler = new DateTimeButtonBarClickHandler();
        prefs = measurementUserPreferences.getMetricRangePreferences();
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start: " + prefs.begin + " end: " + prefs.end);
        Log.debug("ButtonBarDateTimeRangeEditor initialized with start Date: " + new Date(prefs.begin) + " end Date: "
                + new Date(prefs.end));
        createButtons();
    }

    public void createButtons() {

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setHeight(24);

        toolStrip.addSpacer(10);

        for(DateTimeButton dateTimeButton : DateTimeButton.values()){
            IButton oneHourButton = new IButton(dateTimeButton.label);
            oneHourButton.setWidth(BUTTON_WIDTH);
            oneHourButton.setActionType(SelectionType.RADIO);
            oneHourButton.setRadioGroup(TIMERANGE);
            oneHourButton.addClickHandler(dateTimeButtonBarClickHandler);

            toolStrip.addMember(oneHourButton);
        }


        IButton customButton = new IButton("Custom...");
        customButton.setWidth(60);
        customButton.disable();
        customButton.setActionType(SelectionType.RADIO);
        customButton.setRadioGroup(TIMERANGE);
        customButton.addClickHandler(dateTimeButtonBarClickHandler);
        toolStrip.addMember(customButton);

        toolStrip.addSpacer(20);

        dateRangeLabel = new Label();
        updateDateTimeRangeDisplay(new Date(prefs.begin), new Date(prefs.end));
        dateRangeLabel.setWidth(260);
        toolStrip.addMember(dateRangeLabel);

        toolStrip.addSpacer(20);

        IButton resetZoomButton = new IButton("Reset Manual Zoom");
        resetZoomButton.setWidth(150);
        resetZoomButton.disable();
        toolStrip.addMember(resetZoomButton);

        addMember(toolStrip);
    }


    public void redrawGraphs() {
        d3GraphListView.redrawGraphs();
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
        //drawJsniChart();
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
        for(DateTimeButton dateTimeButton : DateTimeButton.values()){
            if(dateTimeButton.label.equals(dateTimeSelection)){
                dateTimeOffset = dateTimeButton.timeSpanInSeconds * 1000;
                break;
            }
        }

        Log.debug("DateTimeSelection: "+ dateTimeSelection + " = "+ dateTimeOffset);
        return new Date(endDate.getTime() - dateTimeOffset);
    }

    public void updateDateTimeRangeDisplay(Date startDate, Date endDate) {
        String rangeString = fmt.format(startDate) + " - " + fmt.format(endDate);
        dateRangeLabel.setContents(rangeString);

    }

    /**
     * Whenever we make a change to the date range save it here so it gets propogated to
     * the correct places.
     *
     * @param startTime double because JSNI doesnt support long
     * @param endTime   double because JSNI doesnt support long
     */
    public void saveDateRange(double startTime, double endTime) {

        //@todo: uncomment when ready
        final boolean advanced = true;
        AbstractMeasurementRangeEditor.MetricRangePreferences prefs = measurementUserPreferences
                .getMetricRangePreferences();
        prefs.explicitBeginEnd = advanced;
        prefs.begin = (long) startTime;
        prefs.end = (long) endTime;
        ///prefs.setPreference(PREF_METRIC_RANGE, Arrays.asList(prefs.begin, prefs.end));
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs);
        }

    }

    private class DateTimeButtonBarClickHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent clickEvent) {
            IButton button = (IButton) clickEvent.getSource();
            String selectedDateTimeRange = button.getTitle();
            Log.info("Button pressed for: " + selectedDateTimeRange);
            Date calculatedStartDateTime = calculateStartDate(new Date(getEndTime()), selectedDateTimeRange);
            updateDateTimeRangeDisplay(calculatedStartDateTime, new Date());
            redrawGraphs();
        }
    }

    @SuppressWarnings("GwtInconsistentSerializableClass")
    private enum DateTimeButton {
        oneHour( "1h", 60 * 60 ),
        sixHour( "6h", 6 * 60 * 60 ),
        twelveHour( "12h", 12 * 60 * 60 ),
        oneDay("1d", 24 * 60 * 60 ),
        fiveDay("5d", 5 * 24 * 60 * 60 ),
        oneMonth("1m", 30 * 24 * 60 * 60 ),
        threeMonth("3m", 3 * 30 * 24 * 60 * 60 ),
        oneYear("1y",365  * 24 * 60 * 60 );

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

}
