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
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;

/**
 * @author Denis Krusko
 * @author Mike Thompson
 */
public class GraphDataProvider implements Locatable, MetricProvider
{
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
        begin = end - (1000L * 60 * 60 * 8);

        graphTimer = new Timer()
        {
            public void run()
            {
                //int numPoints = Math.round((end - begin) / 60000f);
                end = System.currentTimeMillis();
                loadData(begin, end, 60);
                begin = end - (1000 * 60 * 5);
            }
        };
        start();
    }

    private void start()
    {
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
                    pointsStorage.add(new GraphDataStorage(400));
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


    private void loadData(long begin, long end, int numPoints)
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
                        List<List<MeasurementDataNumericHighLowComposite>> result)
                {
                    for (int i = 0; i < result.size(); i++)
                    {
                        pointsStorage.get(i).putValues(result.get(i));
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

    public List<MetricDisplaySummary> getMetricsList()
    {
        return metricsList;
    }

    public String getMetricsAsJson()
    {
        return jsonMetrics;
    }
}

