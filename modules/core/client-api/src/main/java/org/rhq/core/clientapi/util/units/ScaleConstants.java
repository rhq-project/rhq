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

import java.util.EnumSet;

/**
 * Constants that deal with scaling of units.
 *
 * @author Heiko W. Rupp
 */
public enum ScaleConstants {
    SCALE_NONE,

    // Binary based scaling factors
    SCALE_KILO, SCALE_MEGA, SCALE_GIGA, SCALE_TERA, SCALE_PETA,

    // Time based scaling factors
    SCALE_YEAR, SCALE_WEEK, SCALE_DAY, SCALE_HOUR, SCALE_MIN, SCALE_SEC, SCALE_JIFFY, SCALE_MILLI, SCALE_MICRO, SCALE_NANO;

    /**
     * Return an EnumSet that contain all SCALE_ enum constants dealing with binary scaling
     */
    public static EnumSet<ScaleConstants> getBinaryScaleSet() {
        return EnumSet.range(SCALE_KILO, SCALE_PETA);
    }

    /**
     * Return an EnumSet that contain all SCALE_ enum constants dealing with time
     */
    public static EnumSet<ScaleConstants> getTimeSet() {
        return EnumSet.range(SCALE_YEAR, SCALE_NANO);
    }

    /**
     * Return an EnumSet that conatins all SCALE_ enum constants
     */
    public static EnumSet<ScaleConstants> getAllScales() {
        return EnumSet.range(SCALE_NONE, SCALE_NANO);
    }

    /**
     * Return the UnitsConstants constant that has ord as its ordinal value
     *
     * @param  ord
     *
     * @return
     */
    public static ScaleConstants getConstantWithOrdinal(int ord) {
        ScaleConstants[] values = ScaleConstants.values();
        if ((ord < 0) || (ord > values.length)) {
            throw new IllegalArgumentException("Ordinal " + ord + " is not valid for ScaleConstants");
        }

        return values[ord]; // TODO - is this always true? Or do we need to loop through them?
    }
}