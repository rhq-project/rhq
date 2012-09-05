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

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;

/**
 * @author Denis Krusko
 */
public class GraphDataStorage implements GraphBackingStore
{
    private SortedMap<Long, Double> processedPoints = new TreeMap<Long, Double>();
    private int maxOldValues = 0;

    /**
     * @param maxOldValues maximum old time of stored values. Old values are remove
     */
    public GraphDataStorage(int maxOldValues)
    {
        this.maxOldValues = maxOldValues;
    }

    /**
     * @param measurements data points for aggregation
     */
    @Override
    public void putValues(List<MeasurementDataNumericHighLowComposite> measurements)
    {
        for (MeasurementDataNumericHighLowComposite item : measurements)
        {
            if (!Double.isNaN(item.getValue()))
            {
                processedPoints.put(item.getTimestamp(), item.getValue());
            }
        }
        //Clear old values
        long time = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        SortedMap<Long, Double> beforeMap = processedPoints.headMap(time);
        beforeMap.clear();
    }

    @Override
    public Collection<Double> getAllValues()
    {
        return processedPoints.values();
    }

    /**
     * @param start time
     * @param end   time
     * @return values ??between the start (inclusive) and end (exclusive)
     */
    public Collection<Double> getValuesForRange(long start, long end)
    {
        SortedMap<Long, Double> requestMap = processedPoints.subMap(start, end);
        return requestMap.values();
    }

    /**
     * @param time time for requested data
     * @return near value for specified time
     */
    @Override
    public Double getValueAtTime(long time)
    {
        SortedMap<Long, Double> afterMap = processedPoints.tailMap(time);
        if (!afterMap.isEmpty())
        {
            return afterMap.get(afterMap.firstKey());
        }
        SortedMap<Long, Double> beforeMap = processedPoints.headMap(time);
        if (!beforeMap.isEmpty())
        {
            return beforeMap.get(beforeMap.lastKey());
        }
        else
        {
            processedPoints.put(time, 0.0);
        }
        return 0.0;
    }
}
