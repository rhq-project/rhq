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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.graph;

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
 */
public class GraphDataProvider implements Locatable, PointsDataProvider
{
    private List<MetricDisplaySummary> metrics;
    private String JSONmetrics;
    private int[] definitions;
    private int resourceId;
    private String locatorId;
    private long begin;
    private long end;
    private Timer pointTimer;
    private int step;
    private GraphCanvas graphCanvas;
    private List<DataStorage> pointsStorage = new ArrayList<DataStorage>();

    public GraphDataProvider(String locatorId, int resourceId)
    {
        this.locatorId = locatorId;
        this.resourceId = resourceId;
    }

    public void initDataProvider(GraphCanvas graphCanvas, final int step)
    {
        this.graphCanvas = graphCanvas;
        this.step = step;
        initMetrics();
        end = System.currentTimeMillis();
        begin = end - (1000L * 60 * 60 * 8);

        pointTimer = new Timer()
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
        pointTimer.scheduleRepeating(step);
    }

    public void stop()
    {
        pointTimer.cancel();
    }

    public void initMetrics()
    {
        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForResource(resourceId, locatorId, new AsyncCallback<ArrayList<MetricDisplaySummary>>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                CoreGUI.getErrorHandler().handleError(MSG.view_measureTable_getLive_failure(), caught);
            }

            @Override
            public void onSuccess(ArrayList<MetricDisplaySummary> result)
            {
                metrics = result;
                definitions = new int[result.size()];
                for (int i = 0; i < result.size(); i++)
                {
                    definitions[i] = result.get(i).getDefinitionId();
                    pointsStorage.add(new GraphDataStorage(400));
                }
                JSONmetrics = getJSONMetrics(result);
                graphCanvas.drawCharts();
            }
        });
    }

    @Override
    public String getJSONMetrics(List<MetricDisplaySummary> metrics)
    {
        String s = "[";
        MetricDisplaySummary metric;
        for (int i = 0; i < metrics.size(); i++)
        {
            metric = metrics.get(i);
            s += " {label:'" + metric.getLabel() + "',metricIndex:" + i + ",metricName:'" + metric.getMetricName() + "',metricUnit:'" + metric.getUnits() + "'},";
        }
        s += "]";
        return s;
    }


    public void loadData(long begin, long end, int numPoints)
    {
        if (definitions != null && definitions.length > 0)
        {
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitions,
                    begin, end, numPoints, new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>()
            {
                public void onFailure(Throwable caught)
                {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                }

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
            s += "" + definitions[i] + ":" + getJSONPoints(i) + ",";
        }
        s += "}";
        return s;
    }

    @Override
    public String getJSONPoints(int metricIndex)
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
    public String getJSONPoints(int metricIndex, long start, long stop)
    {
        Collection<Double> points = pointsStorage.get(metricIndex).getValues(start, stop);
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

    public List<MetricDisplaySummary> getMetrics()
    {
        return metrics;
    }

    public String getJSONmetrics()
    {
        return JSONmetrics;
    }
}

