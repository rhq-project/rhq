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

import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvas;
import org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphDataProvider;
import org.rhq.enterprise.gui.coregui.client.components.graphing.d3.MetricProvider;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Denis Krusko
 * @author Mike Thompson
 */
public class D3GraphMonitoringView extends LocatableVLayout implements RefreshableView
{
    private ResourceComposite resourceComposite;
    private MetricProvider metricProvider;
    private D3GraphCanvas graphCanvas;

    public D3GraphMonitoringView(String locatorId, ResourceComposite resourceComposite)
    {
        super(locatorId);
        this.resourceComposite = resourceComposite;
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit()
    {
        super.onInit();
        addMember(createGraphForm());
    }

    private LocatableDynamicForm createGraphForm()
    {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Graphing"));
        final int PIXEL_OFFSET = 5;
        form.setWidth100();
        form.setAutoHeight();

        //@todo: get rid of all these hard-coded values
        graphCanvas = new D3GraphCanvas("d3_chart");
        //graphCanvas.setWidth(form.getInnerContentWidth() - PIXEL_OFFSET);
        //graphCanvas.setHeight(form.getInnerHeight() - PIXEL_OFFSET);
        graphCanvas.setWidth(900);
        graphCanvas.setHeight(700);
        form.addChild(graphCanvas);
        metricProvider = new GraphDataProvider(getLocatorId(), resourceComposite.getResource().getId());
        graphCanvas.setDataProvider(metricProvider);
        return form;
    }
    @Override
    public void destroy()
    {
        super.destroy();
        metricProvider.stop();
    }

    @Override
    public void refresh()
    {
        removeMembers(getMembers());
        addMember(createGraphForm());
    }
}

