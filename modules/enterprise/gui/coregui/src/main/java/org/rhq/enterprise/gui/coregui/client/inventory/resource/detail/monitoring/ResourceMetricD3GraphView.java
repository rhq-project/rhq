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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;


public class ResourceMetricD3GraphView extends AbstractMetricD3GraphView {


    public ResourceMetricD3GraphView(String locatorId){
        super(locatorId);
    }


    public ResourceMetricD3GraphView(String locatorId, int resourceId, MeasurementDefinition def,
                                     List<MeasurementDataNumericHighLowComposite> data) {

        super(locatorId, resourceId, def, data);
    }


    @Override
    protected void renderGraph() {
        drawGraph();
    }

    @Override
    protected boolean supportsLiveGraphViewDialog() {
        return true;
    }

    @Override
    protected void displayLiveGraphViewDialog() {
        LiveGraphView.displayAsDialog(getLocatorId(), getEntityId(), getDefinition());
    }

    @Override
    public AbstractMetricD3GraphView getInstance(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {

        return new ResourceMetricD3GraphView(locatorId, entityId, def, data);
    }
}
