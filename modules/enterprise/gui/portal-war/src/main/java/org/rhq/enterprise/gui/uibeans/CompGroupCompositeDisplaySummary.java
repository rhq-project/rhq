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
package org.rhq.enterprise.gui.uibeans;

import java.util.List;
import java.util.Map;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;

/**
 * Simple UI bean for the resource/group/../ListChildResourcesPanel, includes the CompGroupComposite and the
 * MetricDisplaySummary
 *
 * @author Jessica Sant
 * @author Heiko W. Rupp
 */
public class CompGroupCompositeDisplaySummary {
    private List<ResourceWithAvailability> resources;

    /**
     * Map keyed by resource id
     */
    private Map<Integer, List<MetricDisplaySummary>> metricSummaries;

    public CompGroupCompositeDisplaySummary(List<ResourceWithAvailability> resources,
        Map<Integer, List<MetricDisplaySummary>> meDis) {
        this.resources = resources;
        this.metricSummaries = meDis;
    }

    public List<ResourceWithAvailability> getResources() {
        return resources;
    }

    public void setResources(List<ResourceWithAvailability> composite) {
        this.resources = composite;
    }

    public Map<Integer, List<MetricDisplaySummary>> getMetricSummaries() {
        return metricSummaries;
    }

    public void setMetricSummaries(Map<Integer, List<MetricDisplaySummary>> metricSummaries) {
        this.metricSummaries = metricSummaries;
    }
}