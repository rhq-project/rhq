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
package org.rhq.core.domain.measurement.composite;

import org.rhq.core.domain.measurement.MeasurementDefinition;

import java.io.Serializable;

/**
 * A composite object used to display metric collection schedules - represents one of the following:
 *
 * <ul>
 *   <li>the metric collection schedule for a resource</li>
 *   <li>the metric collection schedule for a compatible group or autogroup</li>
 *   <li>the default metric collection schedule for a resource type</li>
 * </ul>
 *
 * @author Ian Springer
 */
public class MeasurementScheduleComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private MeasurementDefinition measurementDefinition;
    private boolean collectionEnabled;
    private long collectionInterval;

    public MeasurementScheduleComposite(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
        this.collectionEnabled = measurementDefinition.isDefaultOn();
        this.collectionInterval = measurementDefinition.getDefaultInterval();
    }

    public MeasurementScheduleComposite(MeasurementDefinition measurementDefinition, boolean collectionEnabled,
        long collectionInterval) {
        this.measurementDefinition = measurementDefinition;
        this.collectionEnabled = collectionEnabled;
        this.collectionInterval = collectionInterval;
    }

    public MeasurementDefinition getMeasurementDefinition() {
        return this.measurementDefinition;
    }

    public boolean isCollectionEnabled() {
        return this.collectionEnabled;
    }

    public long getCollectionInterval() {
        return this.collectionInterval;
    }

    @Override
    public String toString() {
        return "MeasurementScheduleComposite[measurementDefinition=" + this.measurementDefinition
            + ", collectionEnabled=" + this.collectionEnabled + ", collectionInterval=" + this.collectionInterval + "]";
    }
}