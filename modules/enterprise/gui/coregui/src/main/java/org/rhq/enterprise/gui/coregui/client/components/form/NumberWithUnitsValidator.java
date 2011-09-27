/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.components.form;

import java.util.Set;

import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.coregui.client.util.measurement.MeasurementParser;

/**
 * Validates user entered numbers with units. This validator can be given
 * the units directly or it can derive the units based on a selection component
 * whose keys are measurement definition IDs - the measurement definitions must be
 * given so the ID can be looked up.
 *  
 * @author John Mazzitelli
 */
public class NumberWithUnitsValidator extends CustomValidator {

    private final MeasurementUnits units;
    private final Set<MeasurementDefinition> metricDefs;
    private final SelectItem metricDropDownMenu;

    public NumberWithUnitsValidator(Set<MeasurementDefinition> metricDefs, SelectItem metricDropDownMenu) {
        if (metricDefs == null) {
            throw new NullPointerException("metricDefs == null");
        }
        if (metricDropDownMenu == null) {
            throw new NullPointerException("metricDropDownMenu == null");
        }
        this.metricDefs = metricDefs;
        this.metricDropDownMenu = metricDropDownMenu;
        this.units = null;
    }

    public NumberWithUnitsValidator(MeasurementUnits units) {
        if (units == null) {
            throw new NullPointerException("units == null");
        }
        this.metricDefs = null;
        this.metricDropDownMenu = null;
        this.units = units;
    }

    @Override
    protected boolean condition(Object value) {
        // don't know if this is the right thing to do, but assume the value was optional.
        // if there is no value or its empty, assume that is OK and will be considered a 0
        if (value == null || value.toString().trim().length() == 0) {
            return true;
        }

        MeasurementUnits unitsToUse = this.units;

        if (unitsToUse == null) {
            // units weren't directly given to us, find it from the selected metric definition
            try {
                String idString = metricDropDownMenu.getValueAsString();
                Integer id = Integer.parseInt(idString);
                for (MeasurementDefinition metricDef : this.metricDefs) {
                    if (metricDef.getId() == id.intValue()) {
                        unitsToUse = metricDef.getUnits();
                        break;
                    }
                }
            } catch (Exception e) {
                unitsToUse = null;
            }
        }

        if (unitsToUse != null) {
            try {
                if (MeasurementParser.parse(value.toString(), unitsToUse) != null) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        } else {
            // no units? just see if its a valid floating point number
            try {
                Double.parseDouble(value.toString());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
