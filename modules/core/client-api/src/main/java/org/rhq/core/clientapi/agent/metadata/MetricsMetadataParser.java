/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.agent.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rhq.core.clientapi.descriptor.configuration.MeasurementUnitsDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.StringUtils;

/**
 * Parses metrics metadata and builds the definition domain object
 *
 * @author Greg Hinkle
 */
public class MetricsMetadataParser {
    public static final long MIN_1 = 1L * 60 * 1000L;
    public static final long MIN_5 = 5L * MIN_1;
    public static final long MIN_10 = 10L * MIN_1;
    public static final long MIN_30 = 30L * MIN_1;

    public static final long MINIMUM_INTERVAL = MeasurementSchedule.MINIMUM_INTERVAL;

    public static List<MeasurementDefinition> parseMetricsMetadata(MetricDescriptor metricDescriptor,
        ResourceType resourceType) {
        MeasurementDefinition definition;

        DataType dataType = DataType.valueOf(metricDescriptor.getDataType().toUpperCase());
        DisplayType displayType = DisplayType.valueOf(metricDescriptor.getDisplayType().toUpperCase());

        long collectionInterval = MIN_10;
        MeasurementUnits units = getMeasurementUnits(metricDescriptor.getUnits(), dataType);

        switch (dataType) {
        case MEASUREMENT: {
            switch (resourceType.getCategory()) {
            case PLATFORM: {
                collectionInterval = MIN_1;
                break;
            }

            case SERVER: {
                collectionInterval = MIN_5;
                break;
            }

            case SERVICE: {
                collectionInterval = MIN_10;
                break;
            }
            }

            collectionInterval = (displayType == DisplayType.SUMMARY) ? collectionInterval : (collectionInterval * 2);
            break;
        }

        case TRAIT: {
            collectionInterval = (displayType == DisplayType.SUMMARY) ? MIN_10 : MIN_30;
            break;
        }

        case CALLTIME: {
            collectionInterval = MIN_1;
            if (units != MeasurementUnits.MILLISECONDS) {
                throw new IllegalStateException("Units must always be set to 'milliseconds' for call-time metrics.");
            }

            break;
        }

        default: {
            throw new IllegalStateException("Unsupported metric data type: " + dataType);
        }
        }

        collectionInterval = Math.max(MINIMUM_INTERVAL,
            (metricDescriptor.getDefaultInterval() == null) ? collectionInterval : metricDescriptor
                .getDefaultInterval().longValue());

        definition = new MeasurementDefinition(metricDescriptor.getProperty(), MeasurementCategory
            .valueOf(metricDescriptor.getCategory().toUpperCase()), getMeasurementUnits(metricDescriptor.getUnits(),
            dataType), dataType, NumericType.valueOf(metricDescriptor.getMeasurementType().toUpperCase()),
            metricDescriptor.isDefaultOn(), collectionInterval, displayType);

        if (metricDescriptor.getDisplayName() != null) {
            definition.setDisplayName(metricDescriptor.getDisplayName());
        } else {
            definition.setDisplayName(StringUtils.deCamelCase(definition.getName()));
        }
        if (metricDescriptor.getDescription() != null) {
            definition.setDescription(metricDescriptor.getDescription());
        } else {
            definition.setDescription(definition.getDisplayName());
        }

        definition.setDestinationType(metricDescriptor.getDestinationType());

        // Make sure that all summary properties are on by default.
        if (definition.getDisplayType() == DisplayType.SUMMARY) {
            definition.setDefaultOn(true);
        }

        if ((definition.getNumericType() == NumericType.TRENDSUP)
            || (definition.getNumericType() == NumericType.TRENDSDOWN)) {
            ArrayList<MeasurementDefinition> definitions = new ArrayList<MeasurementDefinition>();
            MeasurementDefinition perMinuteMetric = new MeasurementDefinition(definition);

            // Default to applying defaultOn, SUMMARY type, etc. to the per-minute version of the metric,
            // so only the per-minute, and not the raw, is enabled by default.
            definition.setDisplayType(DisplayType.DETAIL);
            definition.setDefaultOn(false);

            perMinuteMetric.setDisplayName(perMinuteMetric.getDisplayName() + " per Minute");

            // This keeps track of whether the associated raw metric is TRENDUP or TRENDSDOWN.
            perMinuteMetric.setRawNumericType(definition.getNumericType());

            // The per-minute metric is no longer monotonically increasing/decreasing, but swinging within some band.
            perMinuteMetric.setNumericType(NumericType.DYNAMIC);

            definitions.add(definition);
            definitions.add(perMinuteMetric);
            return definitions;
        } else {
            return Collections.singletonList(definition);
        }
    }

    static MeasurementUnits getMeasurementUnits(MeasurementUnitsDescriptor descriptor, DataType dataType) {
        if (descriptor == null) {
            return (dataType == DataType.CALLTIME) ? MeasurementUnits.MILLISECONDS : MeasurementUnits.NONE;
        } else {
            return MeasurementUnits.valueOf(descriptor.name().toUpperCase());
        }
    }
}