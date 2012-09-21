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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

/**
 * Abstract class for
 * @author Mike Thompson
 */
public abstract class AbstractGraphCanvas extends Canvas implements GraphCanvasProperties {

    HTMLFlow flow;
    protected MetricProvider dataProvider;

    // Shadow Properties for JSNI.
    // parent properties here are shadowed in the child so JSNI can see them.
    // linkage occurs in  the copyShadowProperties method.
    String parentChartId;
    int parentServerDelay = 5000;
    int parentClientDelay = 5000;
    int parentStep = 30 * 1000;

    boolean initialized = false;

    public AbstractGraphCanvas(String chartId)
    {
        this.parentChartId = chartId;
        init();
    }
    public abstract void initOverride();

    /**
     * This is a hack for JSNI. JSNI requires properties to be in the class referred to
     * by JSNI (so in this case the child class). Properties in the parent Abstract
     * class cannot be accessed by JSNI.
     */
    public abstract void copyShadowProperties();

    private void init()
    {
        copyShadowProperties();
        flow = new HTMLFlow();
        flow.setContents("<div id = \"" + parentChartId + "\" />");
        this.setHeight100();
        this.setWidth100();
        this.addChild(flow);
        initOverride();
    }

    @Override
    public void setChartId(String chartId) {

    }

    @Override
    public void setWidth(int width)
    {
        //this.width = width;
        flow.setWidth(width);
    }

    @Override
    public void setHeight(int height)
    {
        //this.height = height;
        flow.setHeight(height);
    }

    public void setDataProvider(MetricProvider dataProvider)
    {
        this.dataProvider = dataProvider;
        dataProvider.initDataProvider(this, parentStep);
        initialized = true;
    }

    /**
     * The JSNI method to actually draw the charts.
     */
    public abstract void drawCharts();

    @Override
    public void redraw()
    {
        super.redraw();
        if (initialized)
        {
            drawCharts();
        }
    }


}
