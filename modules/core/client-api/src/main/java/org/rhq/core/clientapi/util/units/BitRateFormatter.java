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

import java.text.ParseException;

public class BitRateFormatter extends BinaryFormatter {
    protected String getTagName() {
        return "b";
    }

    protected UnitNumber parseTag(double number, String tag, int tagIdx, ParseSpecifics specifics)
        throws ParseException {
        ScaleConstants scale;

        if (tag.equalsIgnoreCase("b") || tag.equalsIgnoreCase("b/sec")) {
            scale = ScaleConstants.SCALE_NONE;
        } else if (tag.equalsIgnoreCase("k") || tag.equalsIgnoreCase("kb") || tag.equalsIgnoreCase("kb/sec")) {
            scale = ScaleConstants.SCALE_KILO;
        } else if (tag.equalsIgnoreCase("m") || tag.equalsIgnoreCase("mb") || tag.equalsIgnoreCase("mb/sec")) {
            scale = ScaleConstants.SCALE_MEGA;
        } else if (tag.equalsIgnoreCase("g") || tag.equalsIgnoreCase("gb") || tag.equalsIgnoreCase("gb/sec")) {
            scale = ScaleConstants.SCALE_GIGA;
        } else if (tag.equalsIgnoreCase("t") || tag.equalsIgnoreCase("tb") || tag.equalsIgnoreCase("tb/sec")) {
            scale = ScaleConstants.SCALE_TERA;
        } else if (tag.equalsIgnoreCase("p") || tag.equalsIgnoreCase("pb") || tag.equalsIgnoreCase("pb/sec")) {
            scale = ScaleConstants.SCALE_PETA;
        } else {
            throw new ParseException("Unknown bitrate type '" + tag + "'", tagIdx);
        }

        return new UnitNumber(number, UnitsConstants.UNIT_BITS, scale);
    }
}