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

import java.util.Collection;
import java.util.List;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

/**
 * @author Denis Krusko
 */
public interface DataStorage
{

    /**
     * @param measurements data points for aggregation
     */
    void putValues(List<MeasurementDataNumericHighLowComposite> measurements);

    /**
     * @param time time for requested data
     * @return near value for specified time
     */
    Double getValue(long time);

    /**
     * @param start time
     * @param end   time
     * @return values ??between the start (inclusive) and end (exclusive)
     */
    Collection<Double> getValues(long start, long end);

    Collection<Double> getAllValues();
}
