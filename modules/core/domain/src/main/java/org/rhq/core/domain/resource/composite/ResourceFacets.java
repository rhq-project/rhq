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

/**
 * The set of facets a Resource supports - used to determine which quicknav icons and tabs to display in the UI.
 *
 * @author Ian Springer
 */
public class ResourceFacets implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean measurement;
    private boolean event;
    private boolean pluginConfiguration;
    private boolean configuration;
    private boolean operation;
    private boolean content;
    private boolean callTime;

    public ResourceFacets(boolean measurement, boolean event, boolean pluginConfiguration, boolean configuration,
        boolean operation, boolean content, boolean callTime) {
        this.measurement = measurement;
        this.event = event;
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
        this.operation = operation;
        this.content = content;
        this.callTime = callTime;
    }

    public ResourceFacets(Number measurement, Number event, Number pluginConfiguration, Number configuration,
        Number operation, Number content, Number callTime) {
        this.measurement = measurement.intValue() != 0;
        this.event = event.intValue() != 0;
        this.pluginConfiguration = pluginConfiguration.intValue() != 0;
        this.configuration = configuration.intValue() != 0;
        this.operation = operation.intValue() != 0;
        this.content = content.intValue() != 0;
        this.callTime = callTime.intValue() != 0;
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
     * Does this resource have a plugin configuration? If so, the Inventory>Connection subtab will be displayed in the 
     * GUI.
     *
     * @return true if the resource has a plugin configuration, false otherwise
     */
    public boolean isPluginConfiguration() {
        return pluginConfiguration;
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