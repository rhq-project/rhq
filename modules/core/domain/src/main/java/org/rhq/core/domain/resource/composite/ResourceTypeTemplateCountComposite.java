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

import org.rhq.core.domain.resource.ResourceType;

// implement Comparable because these are to be put in a sorted set
public class ResourceTypeTemplateCountComposite implements Serializable, Comparable<ResourceTypeTemplateCountComposite> {

    private static final long serialVersionUID = 1L;

    private final ResourceType type;
    private final long enabledMetricCount;
    private final long disabledMetricCount;
    private final long enabledAlertCount;
    private final long disabledAlertCount;

    public ResourceTypeTemplateCountComposite(ResourceType type, long enabledMetricCount, long disabledMetricCount,
        long enabledAlertCount, long disabledAlertCount) {
        super();
        this.type = type;
        this.enabledMetricCount = enabledMetricCount;
        this.disabledMetricCount = disabledMetricCount;
        this.enabledAlertCount = enabledAlertCount;
        this.disabledAlertCount = disabledAlertCount;
    }

    public ResourceType getType() {
        return type;
    }

    public long getEnabledMetricCount() {
        return enabledMetricCount;
    }

    public long getDisabledMetricCount() {
        return disabledMetricCount;
    }

    public long getEnabledAlertCount() {
        return enabledAlertCount;
    }

    public long getDisabledAlertCount() {
        return disabledAlertCount;
    }

    public int compareTo(ResourceTypeTemplateCountComposite other) {
        return this.type.compareTo(other.type);
    }

    @Override
    public String toString() {
        return "ResourceTypeTemplateCountComposite[ " + "type[" + "id=" + getType().getId() + ", " + "name="
            + getType().getName() + "], " + "metricTemplateCounts(" + enabledMetricCount + " / " + disabledMetricCount
            + "), " + "metricAlertCounts(" + enabledAlertCount + " / " + disabledAlertCount + ") ]";
    }
}
