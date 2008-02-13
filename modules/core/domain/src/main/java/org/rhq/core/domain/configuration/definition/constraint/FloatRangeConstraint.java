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
package org.rhq.core.domain.configuration.definition.constraint;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

/**
 * @author Jason Dobies
 */
@DiscriminatorValue("FLOAT_RANGE")
@Entity
public class FloatRangeConstraint extends Constraint {
    /**
     * Symbol used in the merged details to indicate one of the boundaries (minimum or maximum) was not specified.
     */
    private static final String UNBOUNDED_SYMBOL = "*";

    private static final long serialVersionUID = 1L;

    /* equals() and hashCode() are overridden in the ConfigurationProperty base class. */

    @Transient
    private Double minimum;

    @Transient
    private Double maximum;

    public FloatRangeConstraint(Double minimum, Double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
        rebuildDetails();
    }

    protected FloatRangeConstraint() {
        // JPA use
    }

    @Override
    public void setDetails(String details) {
        super.setDetails(details);

        if (details != null) {
            splitAndPopulate();
        }
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
        rebuildDetails();
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
        rebuildDetails();
    }

    public Double getMaximum() {
        return maximum;
    }

    @PostLoad
    private void splitAndPopulate() {
        String[] split = details.split(DELIMITER);

        assert ((split != null) && (split.length == 2)) : "DoubleRangeConstraint.setDetails - Details could not be split. Details: "
            + details;

        minimum = (UNBOUNDED_SYMBOL.equals(split[0])) ? null : new Double(split[0]);
        maximum = (UNBOUNDED_SYMBOL.equals(split[1])) ? null : new Double(split[1]);
    }

    private void rebuildDetails() {
        String min = (minimum == null) ? UNBOUNDED_SYMBOL : minimum.toString();
        String max = (maximum == null) ? UNBOUNDED_SYMBOL : maximum.toString();

        super.setDetails(min + DELIMITER + max);
    }
}