 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.clientapi.util.units;

import java.text.ParseException;

public class BytesFormatter extends BinaryFormatter {
    protected String getTagName() {
        return "B";
    }

    protected UnitNumber parseTag(double number, String tag, int tagIdx, ParseSpecifics specifics)
        throws ParseException {
        ScaleConstants scale;

        if (tag.equalsIgnoreCase("b") || tag.equalsIgnoreCase("bytes")) {
            scale = ScaleConstants.SCALE_NONE;
        } else if (tag.equalsIgnoreCase("k") || tag.equalsIgnoreCase("kb")) {
            scale = ScaleConstants.SCALE_KILO;
        } else if (tag.equalsIgnoreCase("m") || tag.equalsIgnoreCase("mb")) {
            scale = ScaleConstants.SCALE_MEGA;
        } else if (tag.equalsIgnoreCase("g") || tag.equalsIgnoreCase("gb")) {
            scale = ScaleConstants.SCALE_GIGA;
        } else if (tag.equalsIgnoreCase("t") || tag.equalsIgnoreCase("tb")) {
            scale = ScaleConstants.SCALE_TERA;
        } else if (tag.equalsIgnoreCase("p") || tag.equalsIgnoreCase("pb")) {
            scale = ScaleConstants.SCALE_PETA;
        } else {
            throw new ParseException("Unknown byte type '" + tag + "'", tagIdx);
        }

        return new UnitNumber(number, UnitsConstants.UNIT_BYTES, scale);
    }
}