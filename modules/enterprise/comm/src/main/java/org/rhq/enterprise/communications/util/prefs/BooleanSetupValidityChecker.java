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

/**
 * A validity checker that validates that the new value is a valid boolean (true or false).
 *
 * @author John Mazzitelli
 */
public class BooleanSetupValidityChecker implements SetupValidityChecker {
    /**
     * Makes sure the new value is either "true" or "false".
     *
     * @see SetupValidityChecker#checkValidity(String, String, Preferences, PrintWriter)
     */
    public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
        return (value_to_check.equals("true") || value_to_check.equals("false"));
    }
}