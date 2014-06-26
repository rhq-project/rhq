/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.resource.ResourceTypeFacet;

/**
 * The set of facets a Resource or compatible Resource group supports - used to determine which quick-nav icons and tabs
 * to display in the GUI.
 *
 * @author Ian Springer
 */
public class ResourceFacets implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ResourceFacets NONE = new ResourceFacets(-1, false, false, false, false, false, false, false, false,
        false, false);
    public static ResourceFacets ALL = new ResourceFacets(-1, true, true, true, true, true, true, true, true, true,
        true);

    private int resourceTypeId;
    private boolean measurement;
    private boolean event;
    private boolean pluginConfiguration;
    private boolean configuration;
    private boolean operation;
    private boolean content;
    private boolean callTime;
    private boolean support;
    private boolean drift;
    private boolean bundle;
    private Set<ResourceTypeFacet> facets;

    // no-arg constructor required by GWT compiler
    public ResourceFacets() {
    }

    public ResourceFacets(int resourceTypeId, boolean measurement, boolean event, boolean pluginConfiguration,
        boolean configuration, boolean operation, boolean content, boolean callTime, boolean support, boolean drift,
        boolean bundle) {
        this.resourceTypeId = resourceTypeId;
        this.measurement = measurement;
        this.event = event;
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
        this.operation = operation;
        this.content = content;
        this.callTime = callTime;
        this.support = support;
        this.drift = drift;
        this.bundle = bundle;
    }

    public ResourceFacets(int resourceTypeId, Number measurement, Number event, Number pluginConfiguration,
        Number configuration, Number operation, Number content, Number callTime, Number support, Number drift,
        Number bundle) {
        this.resourceTypeId = resourceTypeId;
        this.measurement = measurement.intValue() != 0;
        this.event = event.intValue() != 0;
        this.pluginConfiguration = pluginConfiguration.intValue() != 0;
        this.configuration = configuration.intValue() != 0;
        this.operation = operation.intValue() != 0;
        this.content = content.intValue() != 0;
        this.callTime = callTime.intValue() != 0;
        this.support = support.intValue() != 0;
        this.drift = drift.intValue() != 0;
        this.bundle = bundle.intValue() != 0;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
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

    /**
     * Does this resource expose support snapshot capability? If so, the Support sub-tab will be displayed in the GUI.
     *
     * @return true if the resource allows support snapshots, false otherwise
     */
    public boolean isSupport() {
        return support;
    }

    /**
     * Does this resource expose drift capability? If so, the Drift sub-tab will be displayed in the GUI.
     *
     * @return true if the resource allows drift detection, false otherwise
     */
    public boolean isDrift() {
        return drift;
    }

    /**
     * Does this resource expose bundle deploy capability? If so, the Bundle sub-tab will be displayed in the GUI
     * (TODO: this is not currently implemented but we have other needs for knowning whether a type supports
     * bundle deployment.
     *
     * @return true if the resource allows bundle deployment, false otherwise
     */
    public boolean isBundle() {
        return bundle;
    }

    /**
     * Returns an enum representation of the facets.
     *
     * @return an enum representation of the facets
     */
    public Set<ResourceTypeFacet> getFacets() {
        if (facets == null) {
            initEnum();
        }
        return facets;
    }

    private void initEnum() {
        this.facets = new HashSet<ResourceTypeFacet>();
        if (measurement)
            this.facets.add(ResourceTypeFacet.MEASUREMENT);
        if (event)
            this.facets.add(ResourceTypeFacet.EVENT);
        if (pluginConfiguration)
            this.facets.add(ResourceTypeFacet.PLUGIN_CONFIGURATION);
        if (configuration)
            this.facets.add(ResourceTypeFacet.CONFIGURATION);
        if (operation)
            this.facets.add(ResourceTypeFacet.OPERATION);
        if (content)
            this.facets.add(ResourceTypeFacet.CONTENT);
        if (callTime)
            this.facets.add(ResourceTypeFacet.CALL_TIME);
        if (support)
            this.facets.add(ResourceTypeFacet.SUPPORT);
        if (drift)
            this.facets.add(ResourceTypeFacet.DRIFT);
        if (bundle)
            this.facets.add(ResourceTypeFacet.BUNDLE);
    }
}