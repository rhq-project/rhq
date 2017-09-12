/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.alert.builder.condition;

import java.io.Serializable;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;

/**
 * @author Michael Burman
 */
public abstract class AbstractCondition implements Serializable {
    private String name;
    private String option;
    private String comparator;
    private Double threshold;
    private Integer measurementDefinitionId = Integer.valueOf(0);

    void setOption(AlertConditionOperator operator) {
        // How pretty..
        switch(operator) {
            case CHANGES:
                break;
            case CHANGES_TO:
                break;
            case CHANGES_FROM:
                break;
            case AVAIL_GOES_DOWN:
                break;
            case AVAIL_GOES_DISABLED:
                break;
            case AVAIL_GOES_UNKNOWN:
                break;
            case AVAIL_GOES_NOT_UP:
                break;
            case AVAIL_GOES_UP:
                break;
            case AVAIL_DURATION_DOWN:
                break;
            case AVAIL_DURATION_NOT_UP:
                break;
            default:
                throw new IllegalArgumentException("Given AlertConditionOperator"  + operator.toString() + " is not an operator");
        }
        this.option = operator.name();
    }

    void setComparator(AlertConditionOperator comparator) {
        switch(comparator) {
            case LESS_THAN:
                this.comparator = "<";
                break;
            case LESS_THAN_OR_EQUAL_TO:
                this.comparator = "<=";
                break;
            case EQUALS:
                if(getCategory().equals(AlertConditionCategory.RANGE)) {
                    throw new IllegalArgumentException("Range condition does not support " + comparator.toString() + " comparator");
                } else {
                    this.comparator = "=";
                }
                break;
            case REGEX:
                break;
            case GREATER_THAN:
                this.comparator = ">";
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                this.comparator = ">=";
                break;
            default:
                throw new IllegalArgumentException("Given AlertConditionOperator"  + comparator.toString() + " is not a comparator");
        }
    }

    /**
     * Allows to override what's written to the OPTION-field on the database. Used for example by Drift-detection
     * @param option
     */
    void overrideOption(String option) {
        this.option = option;
    }

    void setThreshold(Double value) {
        threshold = value;
    }

    void setMeasurementDefinitionId(Integer id) {
        this.measurementDefinitionId = id;
    }

    void setName(String name) {
        this.name = name;
    }

    abstract AlertConditionCategory getCategory();

    public Integer getMeasurementDefinitionId() {
        return measurementDefinitionId;
    }

    public AlertCondition getAlertCondition() {
        AlertCondition condition = new AlertCondition();
        condition.setThreshold(this.threshold);
        condition.setComparator(this.comparator);
        condition.setOption(this.option);
        condition.setName(name);
        condition.setCategory(getCategory());

        // Metrics can't be handled here, they're done in the AlertDefinitionManager
        return condition;
    }
}
