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


/**
 * Same as {@link LocalInetAddressSetupInstruction}, but it does not
 * predefine a default.  Allows the user to more easily tell us that
 * he wants to keep the value undefined and rely on the system default.
 *
 * @author John Mazzitelli
 */
public class LocalInetAddressNoDefaultSetupInstruction extends SetupInstruction {
    /**
     * Creates a new {@link LocalInetAddressNoDefaultSetupInstruction} object.
     *
     * @param  preference_name
     * @param  prompt_message
     * @param  help_message
     *
     * @throws IllegalArgumentException
     *
     * @see    SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public LocalInetAddressNoDefaultSetupInstruction(String preference_name, String prompt_message, String help_message)
        throws IllegalArgumentException {
        super(preference_name, null, new InetAddressSetupValidityChecker(), prompt_message, help_message, false);
    }

    /**
     * Sets the default as an IP address for this local host. We try not to set it to a loopback device.
     *
     * @see SetupInstruction#preProcess()
     */
    @Override
    public void preProcess() {
        return; // no-op - leave the default as null/undefined
    }
}