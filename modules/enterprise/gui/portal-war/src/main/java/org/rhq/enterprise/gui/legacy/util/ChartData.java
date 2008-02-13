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
package org.rhq.enterprise.gui.legacy.util;

import java.util.List;
import java.util.Map;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;

/**
 * A bean that stores data for rendering a chart containing call-time data for a set of URLs.
 *
 * @author Ian Springer
 */
public class ChartData {
    //-------------------------------------instance variables

    private Boolean showAvg;
    private Boolean showLow;
    private Boolean showPeak;
    private Boolean showReq = Boolean.TRUE;
    private List<CallTimeDataComposite> summaries;
    private Map segments; // TODO (ips): Do we need this field for anything?
    private MeasurementDefinition measurementDefinition;

    //-------------------------------------constructors

    public ChartData() {
        super();
    }

    //-------------------------------------public methods

    public Map getSegments() {
        return segments;
    }

    public void setSegments(Map m) {
        segments = m;
    }

    public Boolean getShowAvg() {
        return showAvg;
    }

    public void setShowAvg(Boolean b) {
        showAvg = b;
    }

    public Boolean getShowLow() {
        return showLow;
    }

    public void setShowLow(Boolean b) {
        showLow = b;
    }

    public Boolean getShowPeak() {
        return showPeak;
    }

    public void setShowPeak(Boolean b) {
        showPeak = b;
    }

    public Boolean getShowReq() {
        return showReq;
    }

    public void setShowReq(Boolean b) {
        showReq = b;
    }

    public List<CallTimeDataComposite> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<CallTimeDataComposite> summaries) {
        this.summaries = summaries;
    }

    public MeasurementDefinition getMeasurementDefinition() {
        return measurementDefinition;
    }

    public void setMeasurementDefinition(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }
}