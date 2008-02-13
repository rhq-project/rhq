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
import java.net.InetAddress;
import java.util.prefs.Preferences;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * A validity checker that validates that the new value is a valid IP address or hostname.
 *
 * @author John Mazzitelli
 */
public class InetAddressSetupValidityChecker implements SetupValidityChecker {
    /**
     * Checks to make sure the <code>new_value</code> is a valid IP address or hostname.
     *
     * @see SetupValidityChecker#checkValidity(String, String, Preferences, PrintWriter)
     */
    public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
        try {
            InetAddress.getByName(value_to_check);
            return true;
        } catch (Exception e) {
            out.println(CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.SETUP_NOT_AN_IP_ADDRESS_OR_HOSTNAME,
                value_to_check, e));
            return false;
        }
    }
}