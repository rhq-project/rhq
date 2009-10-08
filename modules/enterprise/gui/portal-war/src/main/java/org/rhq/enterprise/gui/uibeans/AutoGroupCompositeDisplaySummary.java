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
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;

/**
 * Simple UI bean for the ListChildResourcesPanel, includes the AutoGroupComposite and the MetricDisplaySummary
 *
 * @author Jessica Sant
 */
public class AutoGroupCompositeDisplaySummary {
    private AutoGroupComposite composite;
    private List<MetricDisplaySummary> metricSummaries;

    public AutoGroupCompositeDisplaySummary(AutoGroupComposite composite, List<MetricDisplaySummary> metricSummaries) {
        this.composite = composite;
        this.metricSummaries = metricSummaries;
    }

    public AutoGroupComposite getComposite() {
        return composite;
    }

    public void setComposite(AutoGroupComposite composite) {
        this.composite = composite;
    }

    public List<MetricDisplaySummary> getMetricSummaries() {
        return metricSummaries;
    }

    public void setMetricSummaries(List<MetricDisplaySummary> metricSummaries) {
        this.metricSummaries = metricSummaries;
    }
}