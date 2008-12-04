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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.WebUserPreferences;

/**
 * The form object which captures the view name used for the indicator charts
 */
public class IndicatorViewsForm extends MetricDisplayRangeForm {
    private String action;
    private String view;
    private String[] views;
    private String[] metric;
    private String addMetric;
    private long timeToken;
    private String update;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getView() {
        if (view == null || "".equals(view))
            view = WebUserPreferences.PREF_MEASUREMENT_INDICATOR_VIEW_DEFAULT;
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String[] getViews() {
        return views;
    }

    public void setViews(String[] views) {
        this.views = views;
    }

    public String[] getMetric() {
        return metric;
    }

    public void setMetric(String[] metric) {
        this.metric = metric;
    }

    public void setAddMetric(String addMetric) {
        this.addMetric = addMetric;
    }

    public String getAddMetric() {
        return addMetric;
    }

    public long getTimeToken() {
        return timeToken;
    }

    public void setTimeToken(long timeToken) {
        this.timeToken = timeToken;
    }

    public String getUpdate() {
        return update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    @Override
    protected void setDefaults() {
        this.action = null;
        this.view = null;
        this.metric = new String[0];
        this.addMetric = null;
        this.timeToken = System.currentTimeMillis();
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        setDefaults();
        super.reset(mapping, request);
    }
}