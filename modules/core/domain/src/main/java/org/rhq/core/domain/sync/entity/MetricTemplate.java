/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.sync.entity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;

/**
 * This is a exportable/importable representation of a metric template.
 *
 * @author Lukas Krejci
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MetricTemplate extends AbstractExportedEntity {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String resourceTypeName;
    
    @XmlAttribute
    private String resourceTypePlugin;
    
    @XmlAttribute
    private String metricName;

    @XmlAttribute
    private boolean perMinute;
    
    @XmlAttribute
    private long defaultInterval;
    
    @XmlAttribute
    private boolean enabled;

    public MetricTemplate() {
    }
    
    public MetricTemplate(MeasurementDefinition definition) {
        resourceTypeName = definition.getResourceType().getName();
        resourceTypePlugin = definition.getResourceType().getPlugin();
        metricName = definition.getName();
        defaultInterval = definition.getDefaultInterval();
        enabled = definition.isDefaultOn();
        perMinute = definition.getRawNumericType() != null;
        setReferencedEntityId(definition.getId());
    }
    
    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

    public String getResourceTypePlugin() {
        return resourceTypePlugin;
    }

    public void setResourceTypePlugin(String resourceTypePlugin) {
        this.resourceTypePlugin = resourceTypePlugin;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public long getDefaultInterval() {
        return defaultInterval;
    }

    public void setDefaultInterval(long defaultInterval) {
        this.defaultInterval = defaultInterval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the perMinute
     */
    public boolean isPerMinute() {
        return perMinute;
    }
    
    /**
     * @param perMinute the perMinute to set
     */
    public void setPerMinute(boolean perMinute) {
        this.perMinute = perMinute;
    }
}
