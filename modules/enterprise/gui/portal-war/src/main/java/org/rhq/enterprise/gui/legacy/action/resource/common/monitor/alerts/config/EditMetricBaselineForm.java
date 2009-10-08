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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;

/**
 * An extension of <code>BaseValidatorForm</code> representing the <em>Edit Metric Baseline</em> form.
 */
public class EditMetricBaselineForm extends ResourceForm {
    //-------------------------------------instance variables

    private Integer m;
    private Integer ad;
    private long fromDate;
    private long toDate;
    private Double mean;
    private Long numOfPts;
    private String metricName;
    private String oldMode;

    //-------------------------------------constructors

    public EditMetricBaselineForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer getM() {
        return this.m;
    }

    public void setM(Integer m) {
        this.m = m;
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    public long getFromDate() {
        return this.fromDate;
    }

    public void setFromDate(long fromDate) {
        this.fromDate = fromDate;
    }

    public long getToDate() {
        return this.toDate;
    }

    public void setToDate(long toDate) {
        this.toDate = toDate;
    }

    public Double getMean() {
        return this.mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Long getNumOfPts() {
        return this.numOfPts;
    }

    public void setNumOfPts(Long numOfPts) {
        this.numOfPts = numOfPts;
    }

    public String getMetricName() {
        return this.metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getOldMode() {
        return this.oldMode;
    }

    public void setOldMode(String oldMode) {
        this.oldMode = oldMode;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("m=" + m + " ");
        s.append("ad=" + ad + " ");
        s.append("fromDate=" + fromDate + " ");
        s.append("toDate=" + toDate + " ");
        s.append("mean=" + mean.toString() + " ");
        s.append("numOfPts=" + numOfPts.toString() + " ");
        s.append("oldMode=" + oldMode + " ");
        s.append("metricName=" + metricName + " ");

        return s.toString();
    }

    public String getRecalc() {
        return "";
    }
}