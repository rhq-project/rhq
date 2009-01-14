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

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;

public class AdvancedMetricSettingsUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(this.getClass());

    public static final String MANAGED_BEAN_NAME = "AdvancedMetricSettingsUIBean";
    private static final String DURATION_TYPE = "duration";
    private static final String INTERVAL_TYPE = "interval";

    private Integer duration;
    private Integer unit;
    private String metricType = AdvancedMetricSettingsUIBean.DURATION_TYPE;
    private Date fromTime;
    private Date toTime;

    public AdvancedMetricSettingsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        return null;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getUnit() {
        return unit;
    }

    public void setUnit(Integer unit) {
        this.unit = unit;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getMetricType() {
        return this.metricType;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public void execute() {
        FacesContext facesContext = FacesContextUtility.getFacesContext();
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

        if (this.getMetricType().equalsIgnoreCase(AdvancedMetricSettingsUIBean.DURATION_TYPE)) {
            rangePreferences.end -= this.getDuration() * this.getUnit();
            preferences.persistPreferences();
        }

        else if (this.getMetricType().equalsIgnoreCase(AdvancedMetricSettingsUIBean.INTERVAL_TYPE)) {
            Long fromTime = this.getFromTime().getTime();
            Long toTime = this.getToTime().getTime();
            if (toTime < fromTime) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Timing error",
                    "To time cannot be earlier than before time"));
            } else {
                rangePreferences.end -= toTime;
                rangePreferences.end -= fromTime;
            }
            preferences.persistPreferences();
        }
    }
}
