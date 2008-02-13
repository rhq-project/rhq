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
 * This interface defines an object that will check the validity of a preference value. These are used during the
 * construction of {@link SetupInstruction} objects.
 *
 * @author John Mazzitelli
 */
public interface SetupValidityChecker {
    /**
     * This will check the validity of the given value. The name of the preference to be checked is also provided,
     * though implementations probably will not need that in most cases. In addition, the full set of preferences
     * currently set are also passed in, in case the validity checker needs to examine other preference values to
     * determine the validity of this value to check.
     *
     * @param  pref_name      the name of the preference being checked
     * @param  value_to_check the value to check
     * @param  preferences    the full set of preferences
     * @param  out            an output stream this instruction can use to print out any error messages that it deems
     *                        appropriate
     *
     * @return <code>true</code> if <code>value_to_check</code> is valid; <code>false</code> otherwise
     */
    boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out);
}