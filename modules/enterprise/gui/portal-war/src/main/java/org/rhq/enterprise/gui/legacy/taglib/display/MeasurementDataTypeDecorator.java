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
package org.rhq.enterprise.gui.legacy.taglib.display;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;

/**
 * This decorator is used to show the {@link DataType} of an entry and then depending on it the
 * {@link MeasurementCategory} of entries.
 *
 * @author Heiko W. Rupp
 */
public class MeasurementDataTypeDecorator extends BaseDecorator {
    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.legacy.taglib.display.ColumnDecorator#decorate(java.lang.Object)
     */
    @Override
    public String decorate(Object columnValue) {
        if (!(columnValue instanceof MeasurementDefinition)) {
            throw new IllegalArgumentException("Input needs to be of type 'MeasurementDefinition'");
        }

        MeasurementDefinition def = (MeasurementDefinition) columnValue;
        DataType dataType = def.getDataType();

        if (null != dataType) {
            switch (dataType) {
            case TRAIT:
                return "Trait";
            case COMPLEX:
                return "Complex";
            }
        }

        MeasurementCategory category = def.getCategory();
        String cat = category.toString();

        // display category with first char as uppercase
        cat = cat.substring(0, 1).toUpperCase().concat(cat.substring(1));

        return cat;
    }
}