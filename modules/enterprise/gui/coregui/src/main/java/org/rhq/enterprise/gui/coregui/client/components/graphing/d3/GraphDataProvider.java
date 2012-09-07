/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.graphing.d3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

/**
 * @author Denis Krusko
 * @author Mike Thompson
 */
public class GraphDataProvider implements Locatable, MetricProvider
{
    private static final int MAX_OLD_VALUES = 400;
    private static final int VALUES_TO_LOAD = 60;
    private static final long INITIAL_8_HOURS = 1000L * 60 * 60 * 8;
    private static final int FIVE_SECONDS = 1000 * 60 * 5;
    private List<MetricDisplaySummary> metricsList;
    private String jsonMetrics;
    private int[] definitions;
    private int resourceId;
    private String locatorId;
    private long begin;
    private long end;
    private Timer graphTimer;
    private int step;
    private AbstractGraphCanvas graphCanvas;
    private List<GraphBackingStore> pointsStorage = new ArrayList<GraphBackingStore>();

    public GraphDataProvider(String locatorId, int resourceId)
    {
        this.locatorId = locatorId;
        this.resourceId = resourceId;
    }

    @Override
    public void initDataProvider(AbstractGraphCanvas graphCanvas, final int step)
    {
        this.graphCanvas = graphCanvas;
        this.step = step;
        fetchAndGraphMetrics();
        end = System.currentTimeMillis();
        begin = end - INITIAL_8_HOURS;

        graphTimer = new Timer()
        {
            public void run()
            {
                end = System.currentTimeMillis();
                //@todo: make more flexible
                loadMeasurementData(begin, end, VALUES_TO_LOAD);
                begin = end - (FIVE_SECONDS);
            }
        };
        graphTimer.scheduleRepeating(step);
    }


    @Override
    public void stop()
    {
        graphTimer.cancel();
    }

    private void fetchAndGraphMetrics()
    {
        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForResource(resourceId, locatorId, new AsyncCallback<ArrayList<MetricDisplaySummary>>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                CoreGUI.getErrorHandler().handleError(MSG.view_measureTable_getLive_failure(), caught);
            }

            @Override
            public void onSuccess(ArrayList<MetricDisplaySummary> metricSummaryList)
            {
                metricsList = metricSummaryList;
                definitions = new int[metricSummaryList.size()];
                for (int i = 0; i < metricSummaryList.size(); i++)
                {
                    definitions[i] = metricSummaryList.get(i).getDefinitionId();
                    pointsStorage.add(new GraphDataStorage(MAX_OLD_VALUES));
                }
                jsonMetrics = getMetricsAsJson(metricSummaryList);
                graphCanvas.drawCharts();
            }
        });
    }

    @Override
    //@todo: use GWT JSON to produce json
    //@todo: use generics
    public String getMetricsAsJson(List<MetricDisplaySummary> metrics)
    {
        String s = "[";
        MetricDisplaySummary metric;
        for (int i = 0; i < metrics.size(); i++)
        {
            metric = metrics.get(i);
            //@todo:optimize
            s += " {label:'" + metric.getLabel() + "',metricIndex:" + i + ",metricName:'" + metric.getMetricName() + "',metricUnit:'" + metric.getUnits() + "'},";
        }
        s += "]";
        return s;
    }


    private void loadMeasurementData(long begin, long end, int numPoints)
    {
        if (definitions != null && definitions.length > 0)
        {
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitions,
                    begin, end, numPoints, new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                }

                @Override
                public void onSuccess(
                        List<List<MeasurementDataNumericHighLowComposite>> measurementDataList)
                {
                    Log.info("Loaded MeasurementData: " + measurementDataList.size());
                    for (int i = 0; i < measurementDataList.size(); i++)
                    {
                        pointsStorage.get(i).putValues(measurementDataList.get(i));
                    }
                }
            });
        }
    }

    @Override
    public String getAllJSONPoints()
    {
        String s = "{";
        for (int i = 0; i < definitions.length; i++)
        {
            s += "" + definitions[i] + ":" + getStoredPointsAsJson(i) + ",";
        }
        s += "}";
        return s;
    }

    @Override
    /**
     * Grab all the metrics data for a particular metric, already stored in the client as json.
     */
    public String getStoredPointsAsJson(int metricIndex)
    {
        Collection<Double> points = pointsStorage.get(metricIndex).getAllValues();
        String s = "[";
        for (Double point : points)
        {
            s += point + ",";
        }
        s += "]";
        return s;
    }

    @Override
    public String getPointsAsJson(int metricIndex, long start, long stop)
    {
        Collection<Double> points = pointsStorage.get(metricIndex).getValuesForRange(start, stop);
        String s = "[";
        for (Double point : points)
        {
            s += point + ",";
        }
        s += "]";
        return s;
    }


    @Override
    public String getLocatorId()
    {
        return locatorId;
    }

    @Override
    public String extendLocatorId(String extension)
    {
        return this.locatorId + "_" + extension;
    }

    /**
     *
     * @return
     */
    public List<MetricDisplaySummary> getMetricsList()
    {
        return metricsList;
    }

    /**
     * Get the list of metrics as a Json structure.
     * @return String Json String
     */
    @Override
    public String getMetricsAsJson()
    {
        return jsonMetrics;
    }
}

