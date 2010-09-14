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
package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricViewData;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricViewsPreferences;

/**
 * @author Joseph Marques
 */

@Stateless
public class MeasurementViewManagerBean implements MeasurementViewManagerLocal {

    public List<String> getViewNames(Subject user, EntityContext context) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewsPreferences prefs = measurementPreferences.getMetricViews(key);
        return prefs.views;
    }

    public void createView(Subject user, EntityContext context, String viewName) throws MeasurementViewException {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewsPreferences prefs = measurementPreferences.getMetricViews(key);
        if (prefs.views.contains(viewName)) {
            throw new MeasurementViewException("View name already in use: '" + viewName + "'");
        }
        prefs.views.add(viewName);
        measurementPreferences.setMetricViews(prefs, key);
    }

    public void deleteView(Subject user, EntityContext context, String viewName) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewsPreferences prefs = measurementPreferences.getMetricViews(key);
        prefs.views.remove(viewName); // graceful no-op if viewName did not exist in context
        measurementPreferences.setMetricViews(prefs, key);
        measurementPreferences.deleteMetricViewData(key, viewName);
    }

    public String getSelectedView(Subject user, EntityContext context) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        String selectedView = measurementPreferences.getSelectedView(key);
        return selectedView;
    }

    public void setSelectedView(Subject user, EntityContext context, String viewName) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        measurementPreferences.setSelectedView(key, viewName);
    }

    public List<String> getCharts(Subject user, EntityContext context, String viewName) throws MeasurementViewException {
        try {
            String key = context.getLegacyKey();
            MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
            MetricViewData viewData = measurementPreferences.getMetricViewData(key, viewName);
            return viewData.charts;
        } catch (IllegalArgumentException iae) {
            // view with 'viewName' was not found, this is OK as the caller will handle creating it by default
            throw new MeasurementViewException("Could not find view " + viewName + " in context " + context);
        }
    }

    public void saveCharts(Subject user, EntityContext context, String viewName, List<String> charts) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewData viewData = new MetricViewData();
        // don't even bother reading the current value out of the preferences, it's being overridden
        viewData.charts = charts;
        measurementPreferences.setMetricViewData(key, viewName, viewData);
    }

    public void moveChartUp(Subject user, EntityContext context, String viewName, String viewKey) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewData viewData = measurementPreferences.getMetricViewData(key, viewName);
        List<String> charts = viewData.charts;
        int index = charts.indexOf(viewKey);
        if (index < 1) {
            return; // either non-existent or viewKey is already at beginning of the list
        }

        charts.set(index, charts.get(index - 1)); // there is at least one guy in front of us, move him back
        charts.set(index - 1, viewKey);
        measurementPreferences.setMetricViewData(key, viewName, viewData);
    }

    public void moveChartDown(Subject user, EntityContext context, String viewName, String viewKey) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewData viewData = measurementPreferences.getMetricViewData(key, viewName);
        List<String> charts = viewData.charts;
        int index = charts.indexOf(viewKey);
        if (index == -1 && index >= charts.size() - 1) {
            return; // either non-existent or viewKey is already at the end of the list
        }

        charts.set(index, charts.get(index + 1)); // there is at least one guy in front of us, move him back
        charts.set(index + 1, viewKey);
        measurementPreferences.setMetricViewData(key, viewName, viewData);
    }

    public void addChart(Subject user, EntityContext context, String viewName, String viewKey) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewData viewData = measurementPreferences.getMetricViewData(key, viewName);
        // only add chart if it's not already in the list
        if (!viewData.charts.contains(viewKey)) {
            viewData.charts.add(viewKey); // new charts always go at the end
            measurementPreferences.setMetricViewData(key, viewName, viewData);
        }
    }

    public void removeChart(Subject user, EntityContext context, String viewName, String viewKey) {
        String key = context.getLegacyKey();
        MeasurementPreferences measurementPreferences = new MeasurementPreferences(user);
        MetricViewData viewData = measurementPreferences.getMetricViewData(key, viewName);
        viewData.charts.remove(viewKey); // graceful no-op if viewKey did not exist in context
        measurementPreferences.setMetricViewData(key, viewName, viewData);
    }

}
