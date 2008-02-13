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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UnitsFormat {
    private static final HashMap<UnitsConstants, Object> formatters;

    static {
        formatters = new HashMap<UnitsConstants, Object>();

        formatters.put(UnitsConstants.UNIT_NONE, new NoFormatter());
        formatters.put(UnitsConstants.UNIT_BYTES, new BytesFormatter());
        formatters.put(UnitsConstants.UNIT_BITS, new BitRateFormatter());
        formatters.put(UnitsConstants.UNIT_DURATION, new DurationFormatter());
        formatters.put(UnitsConstants.UNIT_DATE, new DateFormatter());
        formatters.put(UnitsConstants.UNIT_PERCENTAGE, new PercentageFormatter());
    }

    private static Log log = LogFactory.getLog(UnitsFormat.class);

    public static FormattedNumber format(UnitNumber val) {
        return format(val, Locale.getDefault());
    }

    public static FormattedNumber format(UnitNumber val, Locale locale) {
        return format(val, locale, null);
    }

    private static Formatter getFormatter(UnitsConstants unitType) {
        Formatter res;

        res = (Formatter) formatters.get(unitType);
        if (res == null) {
            throw new IllegalStateException("Unhandled unit type: " + unitType);
        }

        return res;
    }

    public static FormattedNumber format(UnitNumber val, Locale locale, FormatSpecifics specifics) {
        FormattedNumber res;
        Formatter formatter;

        formatter = getFormatter(val.getUnits());

        res = formatter.format(val, locale, specifics);
        if (log.isDebugEnabled()) {
            log.debug("format(" + val.getValue() + ") -> " + res);
        }

        return res;
    }

    static BigDecimal getBaseValue(double value, UnitsConstants unitType, ScaleConstants scale) {
        return getFormatter(unitType).getBaseValue(value, scale);
    }

    static BigDecimal getScaledValue(BigDecimal baseValue, UnitsConstants unitType, ScaleConstants scale) {
        return getFormatter(unitType).getScaledValue(baseValue, scale);
    }
}