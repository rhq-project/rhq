/*
 * RHQ Management Platform
 * Copyright (C) 2012-2013 Red Hat, Inc.
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

import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * One condition for an alert definition.
 * An alert definition can have any number of definitions
 * @author Heiko W. Rupp
 */
@XmlRootElement(name = "condition")
public class AlertConditionRest {

    private AlertConditionOperator name;
    private AlertConditionCategory category;
    private int id;
    private Double threshold;
    private String option;
    private Integer triggerId;
    private String comparator;
    private int measurementDefinition;

    public AlertConditionRest() {
    }

    public AlertConditionOperator getName() {
        return name;
    }

    public void setName(AlertConditionOperator name) {
        this.name = name;
    }

    public AlertConditionCategory getCategory() {
        return category;
    }

    public void setCategory(AlertConditionCategory category) {
        this.category = category;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Integer getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(Integer triggerId) {
        this.triggerId = triggerId;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public int getMeasurementDefinition() {
        return measurementDefinition;
    }

    public void setMeasurementDefinition(int measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }
}
