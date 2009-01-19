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
    private String intervalType;
    private String durationType;
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

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }

    public String getIntervalType() {
        return this.intervalType;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public String getDurationType() {
        return this.durationType;
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

        String metricType = "";
        if (this.getIntervalType() == null) {
            metricType = getDurationType();
        } else {
            metricType = getIntervalType();
        }
        if ((metricType == null) || (metricType.equals(""))) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Range select error",
                "Please select one option to either setup the duration or the time interval"));
        } else if (metricType.equalsIgnoreCase(AdvancedMetricSettingsUIBean.DURATION_TYPE)) {
            long duration = this.getDuration() * this.getUnit();
            rangePreferences.begin = rangePreferences.end - (duration);
            preferences.persistPreferences();
        } else if (metricType.equalsIgnoreCase(AdvancedMetricSettingsUIBean.INTERVAL_TYPE)) {
            Long fromTime = this.getFromTime().getTime();
            Long toTime = this.getToTime().getTime();
            if ((toTime == null) || (fromTime == null)) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Timing error",
                    "Please fill in the required fields"));
            } else if (toTime < fromTime) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Timing error",
                    "To time cannot be earlier than before time"));
            } else {
                rangePreferences.begin = fromTime;
                rangePreferences.end = toTime;
            }
            preferences.persistPreferences();
        }
    }
}
