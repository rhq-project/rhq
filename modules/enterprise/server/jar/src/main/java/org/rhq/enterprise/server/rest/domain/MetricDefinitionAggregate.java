/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * Metric data that could be used to draw a chart
 * @author Heiko W. Rupp
 */
@ApiClass("An aggregate of metrics data for a given metric definition (for a group)")
@XmlRootElement
public class MetricDefinitionAggregate {

    Integer definitionId;
    Double min;
    Double avg;
    Double max;
    long minTimeStamp;
    long maxTimeStamp;

    public MetricDefinitionAggregate() {
    }

    public MetricDefinitionAggregate(Integer definitionId, Double min, Double avg, Double max) {
        this();
        this.definitionId = definitionId;

        this.min = min;
        this.avg = avg;
        this.max = max;

    }

    @ApiProperty("Minimum value for the data")
    @XmlElement
    public Double getMin() {
        return min;
    }

    @ApiProperty("Average value for the data")
    @XmlElement
    public Double getAvg() {
        return avg;
    }

    @ApiProperty("Maximum value for the data")
    @XmlElement
    public Double getMax() {
        return max;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public void setMax(Double max) {
        this.max = max;
    }


    @ApiProperty("Id of the metric *definition*")
    @XmlElement
    public Integer getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Integer definitionId) {
        this.definitionId = definitionId;
    }

    @ApiProperty("Timestamp of the earliest data point in the list")
    public long getMinTimeStamp() {
        return minTimeStamp;
    }

    public void setMinTimeStamp(long minTimeStamp) {
        this.minTimeStamp = minTimeStamp;
    }

    @ApiProperty("Timestamp fo the latest data point in the list")
    public long getMaxTimeStamp() {
        return maxTimeStamp;
    }

    public void setMaxTimeStamp(long maxTimeStamp) {
        this.maxTimeStamp = maxTimeStamp;
    }

}
