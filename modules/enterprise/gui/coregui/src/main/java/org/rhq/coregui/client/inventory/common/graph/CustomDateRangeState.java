/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.common.graph;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * This object provides determination of the 'Custom' date range
 * of the ButtonBarDateTimeRangeEditor without having
 * There are places where I want to set or query the state of
 * the date range to see if it is custom or not, without having to
 * have access to the ButtonBarDateTimeRangeEditor class.
 * This provides a single access point to the 'Custom Date Range'
 * as there are many instances of ButtonBarDateTimeRangeEditor and
 * various paths to access it.
 *
 * @author  Mike Thompson
 */
public class CustomDateRangeState {

    final private static Messages MSG = CoreGUI.getMessages();
    private static CustomDateRangeState INSTANCE = null;
    private boolean isCustomDateRangeActive;
    private MeasurementUserPreferences measurementUserPreferences;
    private AbstractMeasurementRangeEditor.MetricRangePreferences prefs;

    private CustomDateRangeState() {
        measurementUserPreferences = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        prefs = measurementUserPreferences.getMetricRangePreferences();
    }

    public static CustomDateRangeState getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new CustomDateRangeState();
        }
        return INSTANCE;
    }
    
    public static void invalidateInstance() {
        INSTANCE = null;
    }

    public boolean isCustomDateRangeActive() {
        return isCustomDateRangeActive;
    }

    public void setCustomDateRangeActive(boolean customDateRange) {
        isCustomDateRangeActive = customDateRange;
    }

    public Long getStartTime() {
        return measurementUserPreferences.getMetricRangePreferences().begin;
    }

    public Long getEndTime() {
        return measurementUserPreferences.getMetricRangePreferences().end;
    }

    public Long getTimeRange() {
        return getEndTime() - getStartTime();
    }

    /**
     * SaveDateRange with implicit auto-refresh.
     * @param startTime - long representation
     * @param endTime - long representation
     */
    public void saveDateRange(double startTime, double endTime) {
        saveDateRange(startTime, endTime, true);
    }

    /**
     * Whenever we make a change to the date range save it here so it gets propagated to
     * the correct places.
     *
     * @param startTime double because JSNI doesn't support long
     * @param endTime   double because JSNI doesn't support long
     */
    public void saveDateRange(double startTime, double endTime, boolean allowPreferenceUpdateRefresh) {
        prefs.explicitBeginEnd = true; // default to advanced
        prefs.begin = (long) startTime;
        prefs.end = (long) endTime;
        if (null != prefs.begin && null != prefs.end && prefs.begin > prefs.end) {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_measureTable_startBeforeEnd()));
        } else {
            measurementUserPreferences.setMetricRangePreferences(prefs, allowPreferenceUpdateRefresh);
        }

    }

}
