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
package org.rhq.enterprise.server.alert.engine.model;

/**
 * @author Joseph Marques
 */

public class MeasurementBaselineCacheElement extends NumericDoubleCacheElement {
    private String option;

    public MeasurementBaselineCacheElement(AlertConditionOperator operator, Double value, int conditionTriggerId,
        String option) {
        super(operator, value, conditionTriggerId);
        this.option = option;
    }

    public String getOption() {
        return option;
    }

    @Override
    public String toString() {
        return super.toString() + "; option=" + option;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((option == null) ? 0 : option.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final MeasurementBaselineCacheElement other = (MeasurementBaselineCacheElement) obj;
        if (option == null) {
            if (other.option != null) {
                return false;
            }
        } else if (!option.equals(other.option)) {
            return false;
        }

        return true;
    }
}