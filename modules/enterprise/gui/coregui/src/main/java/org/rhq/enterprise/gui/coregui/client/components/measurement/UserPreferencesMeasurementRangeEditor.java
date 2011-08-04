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

package org.rhq.enterprise.gui.coregui.client.components.measurement;

import java.util.List;

import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

public class UserPreferencesMeasurementRangeEditor extends AbstractMeasurementRangeEditor {

    private MeasurementUserPreferences measurementUserPrefs;

    public UserPreferencesMeasurementRangeEditor(String locatorId) {
        super(locatorId);
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
    }

    /**
     * Returns the current range that is persisted. Note this may NOT be the begin and end times
     * as shown in the UI if the user changed the values but did not hit the set button.
     * @return begin/end epoch times in a list
     */
    @Override
    public List<Long> getBeginEndTimes() {
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

}
