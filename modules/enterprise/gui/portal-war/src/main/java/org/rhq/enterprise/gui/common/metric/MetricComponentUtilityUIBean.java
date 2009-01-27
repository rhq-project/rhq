/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.metric;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.metric.MetricComponent.TimeUnit;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;

public class MetricComponentUtilityUIBean {

    private boolean readOnly;

    public MetricComponentUtilityUIBean() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        this.readOnly = rangePreferences.readOnly;
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public String update() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

        int value = FacesContextUtility.getOptionalRequestParameter(MetricComponent.VALUE, Integer.class, -1);
        String unit = FacesContextUtility.getOptionalRequestParameter(MetricComponent.UNIT, String.class, null);

        rangePreferences.lastN = value;
        rangePreferences.unit = TimeUnit.valueOf(unit).getMetricUntilOrdinal();
        preferences.setMetricRangePreferences(rangePreferences);
        preferences.persistPreferences();
        return "success";
    }

    public String switchToSimpleMode() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        rangePreferences.unit = 3;
        rangePreferences.lastN = 8;
        rangePreferences.readOnly = readOnly = false;
        preferences.setMetricRangePreferences(rangePreferences);
        preferences.persistPreferences();
        return "success";
    }
}
