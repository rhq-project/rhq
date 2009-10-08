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
package org.rhq.enterprise.communications.util.prefs;

import java.io.PrintWriter;
import java.util.prefs.Preferences;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * A setup validity checker that validates that the new value is a valid long within an optionally specified range.
 *
 * @author John Mazzitelli
 */
public class LongSetupValidityChecker implements SetupValidityChecker {
    private Long m_min;
    private Long m_max;

    /**
     * Defines the checker that validates the new value as a long that is between <code>min_value_allowed</code> and
     * <code>max_value_allowed</code> inclusive. If either the min or max is <code>null</code>, it will not be checked
     * (that is to say, you can have the value limited only on a floor or ceiling value, or you can limit it for both).
     *
     * @param min_value_allowed if not <code>null</code>, the minimum value the value is allowed to be
     * @param max_value_allowed if not <code>null</code>, the maximum value the value is allowed to be
     */
    public LongSetupValidityChecker(Long min_value_allowed, Long max_value_allowed) {
        m_min = min_value_allowed;
        m_max = max_value_allowed;
    }

    /**
     * Checks to make sure the <code>value_to_check</code> is a valid long within the defined range.
     *
     * @see SetupValidityChecker#checkValidity(String, String, java.util.prefs.Preferences, PrintWriter)
     */
    public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
        try {
            long new_long = Long.parseLong(value_to_check);

            if (m_min != null) {
                if (new_long < m_min.longValue()) {
                    out.println(CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.SETUP_NUMBER_TOO_LOW,
                        value_to_check, m_min));
                    return false;
                }
            }

            if (m_max != null) {
                if (new_long > m_max.longValue()) {
                    out.println(CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.SETUP_NUMBER_TOO_HIGH,
                        value_to_check, m_max));
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            out.println(CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.SETUP_NOT_A_LONG, value_to_check));
            return false;
        }

        return true;
    }
}