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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.Set;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The set of facets a Resource supports - used to determine which quicknav icons and tabs to display in the UI.
 *
 * @author Ian Springer
 */
public class ResourceFacets implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean measurement;
    private boolean event;
    private boolean configuration;
    private boolean operation;
    private boolean content;
    private boolean callTime;

    public ResourceFacets(ResourceType type) {
        this(!type.getMetricDefinitions().isEmpty(), !type.getEventDefinitions().isEmpty(), type
            .getResourceConfigurationDefinition() != null, !type.getOperationDefinitions().isEmpty(), !type
            .getPackageTypes().isEmpty(), exposesCallTimeMetrics(type));
    }

    private static boolean exposesCallTimeMetrics(ResourceType resourceType) {
        Set<MeasurementDefinition> measurementDefs = resourceType.getMetricDefinitions();
        for (MeasurementDefinition measurementDef : measurementDefs) {
            if (measurementDef.getDataType() == DataType.CALLTIME) {
                return true;
            }
        }

        return false;
    }

    public ResourceFacets(boolean measurement, boolean event, boolean configuration, boolean operation,
        boolean content, boolean callTime) {
        this.measurement = measurement;
        this.event = event;
        this.configuration = configuration;
        this.operation = operation;
        this.content = content;
        this.callTime = callTime;
    }

    /**
     * Does this resource expose any metrics? (currently not used for anything in the GUI, since the Monitor and Alert
     * tabs are always displayed).
     *
     * @return true if the resource exposes any metrics, false otherwise
     */
    public boolean isMeasurement() {
        return measurement;
    }

    /**
     * Does this resource have any event definitions? 
     *
     * @return true if the resource has any event definitions
     */
    public boolean isEvent() {
        return event;
    }

    /**
     * Does this resource expose its configuration? If so, the Configure tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its configuration, false otherwise
     */
    public boolean isConfiguration() {
        return configuration;
    }

    /**
     * Does this resource expose any operations? If so, the Operations tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its operations, false otherwise
     */
    public boolean isOperation() {
        return operation;
    }

    /**
     * Does this resource expose any content? If so, the Content tab will be displayed in the GUI.
     *
     * @return true if the resource exposes its content, false otherwise
     */
    public boolean isContent() {
        return content;
    }

    /**
     * Does this resource expose any call-time metrics? If so, the Call Time sub-tab will be displayed in the GUI.
     *
     * @return true if the resource exposes any call-time metrics, false otherwise
     */
    public boolean isCallTime() {
        return callTime;
    }
}