/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.coregui.client.components.measurement;

import java.util.List;

import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.util.Moment;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;

public class UserPreferencesMeasurementRangeEditor extends AbstractMeasurementRangeEditor {
    private static final int START_TIME_INDEX = 0;
    private static final int END_TIME_INDEX = 1;

    private MeasurementUserPreferences measurementUserPrefs;
    public UserPreferencesMeasurementRangeEditor() {
        super();
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list
     */
    @Override
    public List<Moment> getBeginEndTimes() {
        return measurementUserPrefs.getMetricRangePreferences().getBeginEndTimes();
    }

    @Override
    public MetricRangePreferences getMetricRangePreferences() {
        return measurementUserPrefs.getMetricRangePreferences();
    }

    @Override
    public void setMetricRangeProperties(MetricRangePreferences prefs) {
        measurementUserPrefs.setMetricRangePreferences(prefs);
    }

    public Moment getStartTime(){
        List<Moment> beginEndTimes = getBeginEndTimes();
        return beginEndTimes.get(START_TIME_INDEX);

    }
    public Moment getEndTime(){
        List<Moment> beginEndTimes = getBeginEndTimes();
        return beginEndTimes.get(END_TIME_INDEX);

    }
}
