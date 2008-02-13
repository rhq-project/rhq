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
package org.rhq.core.clientapi.util.units;

import java.text.NumberFormat;

public class NoFormatter extends SimpleFormatter {
    protected UnitsConstants getUnitType() {
        return UnitsConstants.UNIT_NONE;
    }

    protected ScaleConstants getUnitScale() {
        return ScaleConstants.SCALE_NONE;
    }

    protected FormattedNumber formatNumber(double rawValue, NumberFormat fmt) {
        return new FormattedNumber(fmt.format(rawValue), "");
    }
}