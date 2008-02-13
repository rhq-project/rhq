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
package org.rhq.enterprise.gui.legacy.beans;

import java.util.List;
import org.rhq.enterprise.gui.image.data.IDataPoint;
import org.rhq.enterprise.gui.image.data.IEventPoint;

/**
 * Helper bean for passing chart data between action class and chart servlet.
 */
public final class ChartDataBean {
    private List<IDataPoint> dataPoints;
    private List<IEventPoint> eventPoints;

    public ChartDataBean(List dataPoints, List eventPoints) {
        this.dataPoints = dataPoints;
        this.eventPoints = eventPoints;
    }

    /**
     * Returns a <code>List</code> of <code>List</code> of IDataPoint objects.
     */
    public List<IDataPoint> getDataPoints() {
        return dataPoints;
    }

    /**
     * Returns a <code>List</code> of <code>List</code> of IEventPoint objects.
     */
    public List<IEventPoint> getEventPoints() {
        return eventPoints;
    }
}