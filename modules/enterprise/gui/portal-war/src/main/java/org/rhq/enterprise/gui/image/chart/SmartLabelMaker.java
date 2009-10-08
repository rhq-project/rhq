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
package org.rhq.enterprise.gui.image.chart;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.rhq.core.clientapi.util.TimeUtil;

class SmartLabelMaker {
    private static final SimpleDateFormat DF_SHORT_FULL = new SimpleDateFormat("H:mm");
    private static final SimpleDateFormat DF_SHORT_ABBREV = new SimpleDateFormat(":mm");
    private static final SimpleDateFormat DF_SHORT_CHECKDIFF = new SimpleDateFormat("H");

    private static final SimpleDateFormat DF_MEDIUM_FULL_TOP = new SimpleDateFormat("H:mm");
    private static final SimpleDateFormat DF_MEDIUM_FULL_BOTTOM = new SimpleDateFormat("M/d");
    private static final SimpleDateFormat DF_MEDIUM_ABBREV = new SimpleDateFormat("H:mm");
    private static final SimpleDateFormat DF_MEDIUM_CHECKDIFF = new SimpleDateFormat("d");

    private static final SimpleDateFormat DF_LONG_FULL_TOP = new SimpleDateFormat("M/d");
    private static final SimpleDateFormat DF_LONG_FULL_BOTTOM = new SimpleDateFormat("H:mm");
    private static final SimpleDateFormat DF_LONG_ABBREV_TOP = new SimpleDateFormat("M/d");
    private static final SimpleDateFormat DF_LONG_ABBREV_BOTTOM = new SimpleDateFormat("H:mm");
    private static final SimpleDateFormat DF_LONG_CHECKDIFF = new SimpleDateFormat("d");

    private SimpleDateFormat _fullFormatTop = null;
    private SimpleDateFormat _fullFormatBottom = null;
    private SimpleDateFormat _abbrevFormatTop = null;
    private SimpleDateFormat _abbrevFormatBottom = null;
    private SimpleDateFormat _checkDiff = null;
    private int _labelSpacing = 4;
    private String _lastCheck = "";

    public SmartLabelMaker(long interval) {
        // Decide which formatter we use
        long absInterval = Math.abs(interval);
        if (absInterval < (TimeUtil.MILLIS_IN_MINUTE * 10)) {
            _fullFormatTop = DF_SHORT_FULL;
            _abbrevFormatTop = DF_SHORT_ABBREV;
            _checkDiff = DF_SHORT_CHECKDIFF;
            _labelSpacing = 4; // label every other major tick mark
        } else if (absInterval < (TimeUtil.MILLIS_IN_HOUR * 2)) {
            _fullFormatTop = DF_MEDIUM_FULL_TOP;
            _fullFormatBottom = DF_MEDIUM_FULL_BOTTOM;
            _abbrevFormatTop = DF_MEDIUM_ABBREV;
            _checkDiff = DF_MEDIUM_CHECKDIFF;
            _labelSpacing = 4; // label every 4th major tick mark
        } else {
            _fullFormatTop = DF_LONG_FULL_TOP;
            _fullFormatBottom = DF_LONG_FULL_BOTTOM;
            _abbrevFormatTop = DF_LONG_ABBREV_TOP;
            _abbrevFormatBottom = DF_LONG_ABBREV_BOTTOM;
            _checkDiff = DF_LONG_CHECKDIFF;
            _labelSpacing = 4; // label every 4th major tick mark
        }
    }

    public int getLabelSpacing() {
        return _labelSpacing;
    }

    public SmartLabel getLabels(boolean forceFullLabel, long absoluteMillis) {
        SmartLabel label = new SmartLabel();
        Date d = new Date(absoluteMillis);
        String check = _checkDiff.format(d);

        if (forceFullLabel || !check.equals(_lastCheck)) {
            _lastCheck = check;
            label.top = _fullFormatTop.format(d);
            label.bottom = (_fullFormatBottom == null) ? "" : _fullFormatBottom.format(d);
        } else {
            label.top = _abbrevFormatTop.format(d);
            label.bottom = (_abbrevFormatBottom == null) ? "" : _abbrevFormatBottom.format(d);
        }

        return label;
    }
}