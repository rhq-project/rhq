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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.rhq.core.domain.measurement.MeasurementSchedule;

/**
 * Represents the controls on pages that display call-time stats.
 */
public class PerformanceForm extends MetricsControlForm {
    public static final Boolean DEFAULT_AVG = Boolean.TRUE;
    public static final Boolean DEFAULT_LOW = Boolean.TRUE;
    public static final Boolean DEFAULT_PEAK = Boolean.TRUE;
    public static final Boolean DEFAULT_REQ = Boolean.TRUE;
    public static final Integer DEFAULT_SCHEDULE_ID = -1;

    //-------------------------------------instance variables

    Boolean avg;
    ImageButtonBean chart;
    Boolean low;
    ImageButtonBean next;
    Boolean peak;
    ImageButtonBean prev;
    ImageButtonBean redraw;
    String[] url;
    Integer pn;
    String sc;
    String so;
    MeasurementSchedule[] schedules;
    Integer scheduleId;
    Integer metricCount;

    //-------------------------------------constructors

    public PerformanceForm() {
        super();
        setDefaults();
    }

    //-------------------------------------public methods

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(super.toString());
        s.append(" low=").append(low);
        s.append(" avg=").append(avg);
        s.append(" peak=").append(peak);
        s.append(" chart=").append(chart);
        s.append(" redraw=").append(redraw);
        s.append(" next=").append(next);
        s.append(" prev=").append(prev);
        s.append(" url=").append(Arrays.asList(url));
        return s.toString();
    }

    public Boolean getAvg() {
        return avg;
    }

    public void setAvg(Boolean b) {
        avg = b;
    }

    public ImageButtonBean getChart() {
        return chart;
    }

    public void setChart(ImageButtonBean ibb) {
        chart = ibb;
    }

    public Boolean getLow() {
        return low;
    }

    public void setLow(Boolean b) {
        low = b;
    }

    public ImageButtonBean getNext() {
        return next;
    }

    public void setNext(ImageButtonBean ibb) {
        next = ibb;
    }

    public Boolean getPeak() {
        return peak;
    }

    public void setPeak(Boolean b) {
        peak = b;
    }

    public ImageButtonBean getPrev() {
        return prev;
    }

    public void setPrev(ImageButtonBean ibb) {
        prev = ibb;
    }

    public ImageButtonBean getRedraw() {
        return redraw;
    }

    public void setRedraw(ImageButtonBean ibb) {
        redraw = ibb;
    }

    public String[] getUrl() {
        return url;
    }

    public void addUrl(String s) {
        if (getUrl() == null) {
            setUrl(new String[0]);
        }

        int len = getUrl().length;
        String[] tmp = new String[len + 1];
        System.arraycopy(getUrl(), 0, tmp, 0, len);
        tmp[len] = s;
        setUrl(tmp);
    }

    public void setUrl(String[] l) {
        url = l;
    }

    public MeasurementSchedule[] getSchedules() {
        return schedules;
    }

    public void setSchedules(MeasurementSchedule[] schedules) {
        this.schedules = schedules;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getMetricCount() {
        return metricCount;
    }

    public void setMetricCount(Integer metricCount) {
        this.metricCount = metricCount;
    }

    public Integer getPn() {
        return pn;
    }

    public void setPn(Integer i) {
        pn = i;
    }

    public String getSo() {
        return so;
    }

    public void setSo(String s) {
        so = s;
    }

    public String getSc() {
        return sc;
    }

    public void setSc(String s) {
        sc = ((s == null) || !s.equals("")) ? s : null;
    }

    public boolean isChartClicked() {
        return getChart().isSelected();
    }

    public boolean isNextClicked() {
        return getNext().isSelected();
    }

    public boolean isPrevClicked() {
        return getPrev().isSelected();
    }

    public boolean isRedrawClicked() {
        return getRedraw().isSelected();
    }

    public boolean isAnythingClicked() {
        return super.isAnythingClicked() || isChartClicked() || isNextClicked() || isPrevClicked() || isRedrawClicked()
            || (getScheduleId() != DEFAULT_SCHEDULE_ID) || (getPs() != null) || (getPn() != null) || (getSc() != null)
            || (getSo() != null);
    }

    public boolean isRangeNow() {
        if (getRe() == null) {
            return false;
        }

        return ((System.currentTimeMillis() - getRe()) < MetricRange.SHIFT_RANGE);
    }

    //-------------------------------------public accessors

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    //-------------------------------------private methods

    protected void setDefaults() {
        super.setDefaults();
        avg = DEFAULT_AVG;
        chart = new ImageButtonBean();
        low = DEFAULT_LOW;
        next = new ImageButtonBean();
        peak = DEFAULT_PEAK;
        prev = new ImageButtonBean();
        redraw = new ImageButtonBean();
        url = new String[0];
        schedules = new MeasurementSchedule[0];
        scheduleId = DEFAULT_SCHEDULE_ID;
        pn = null;
        so = "DESC";
        sc = "SUM(value.total)/SUM(value.count)";
    }
}