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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.LinkedHashMap;
import java.util.List;

import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Mike Thompson
 */
public class SliderRangeEditor extends LocatableVLayout {

    private static Messages MSG = CoreGUI.getMessages();

    //keyed map of translated date units Ex. minutes,hours,days
    protected static LinkedHashMap<String, String> lastUnits;
    //array of values available for displaying/selecting 'last N hours|minutes|days'.
    protected static String[] lastValues;

    static {
        lastUnits = new LinkedHashMap<String, String>(4);
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_MINUTES), MSG.common_unit_minutes());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_HOURS), MSG.common_unit_hours());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_DAYS), MSG.common_unit_days());
        lastUnits.put(String.valueOf(MeasurementUtility.UNIT_WEEKS), MSG.common_unit_weeks());

        lastValues = new String[] { "4", "8", "12", "24", "30", "36", "48", "60", "90", "120" };
    }


    private MeasurementUserPreferences measurementUserPrefs;

    public SliderRangeEditor(String locatorId) {
        super(locatorId);
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        HLayout hLayout = new HLayout();
        Label availabilityLabel = new Label();
        availabilityLabel.setTitle("Availability");
        hLayout.addMember(availabilityLabel);
        Label rangeLabel = new Label();
        rangeLabel.setTitle(getFormattedRange());
        hLayout.addMember(rangeLabel);
        addMember(hLayout);
        setHeight100();
        setWidth100();

    }

    public String getFormattedRange(){
        return "Feb 14 - Feb 17";
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list
     */
    public List<Long> getBeginEndTimes() {
        return measurementUserPrefs.getMetricRangePreferences().getBeginEndTimes();
    }

    public AbstractMeasurementRangeEditor.MetricRangePreferences getMetricRangePreferences() {
        return measurementUserPrefs.getMetricRangePreferences();
    }

    public void setMetricRangeProperties(AbstractMeasurementRangeEditor.MetricRangePreferences prefs) {
        measurementUserPrefs.setMetricRangePreferences(prefs);
    }


}
