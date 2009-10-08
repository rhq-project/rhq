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

import java.net.InetAddress;

/**
 * This setup instruction is used to define a preference whose value is a fully qualified hostname that is to be bound
 * to the local host, but by default not the loopback device. This is used to allow a user to default the hostname to
 * something other than the loopback device (localhost or 127.0.0.1) in order for clients external to this local box to
 * connect to it.
 *
 * @author John Mazzitelli
 */
public class LocalInetHostnameSetupInstruction extends SetupInstruction {
    /**
     * If this is the default value at the time preProcess is called, the preference's current value should be the
     * default.
     */
    private static final String GET_PREF_DEFAULT_MARKER = ":*default*:";

    /**
     * Creates a new {@link LocalInetHostnameSetupInstruction} object. This constructor is to allow an instruction to
     * define a custom hostname whose default is the current preference's value. The default will not be the platform's
     * actual FQDN.
     *
     * @param  preference_name
     * @param  prompt_message
     * @param  help_message
     * @param  validity_checker
     *
     * @throws IllegalArgumentException
     *
     * @see    SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public LocalInetHostnameSetupInstruction(String preference_name, SetupValidityChecker validity_checker,
        String prompt_message, String help_message) throws IllegalArgumentException {
        super(preference_name, GET_PREF_DEFAULT_MARKER, validity_checker, prompt_message, help_message, false);
    }

    /**
     * Creates a new {@link LocalInetHostnameSetupInstruction} object. This adds a validity checker such that if the
     * hostname string that was entered is not a true, DNS resolvable hostname, it fails.
     *
     * @param  preference_name
     * @param  prompt_message
     * @param  help_message
     *
     * @throws IllegalArgumentException
     *
     * @see    SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public LocalInetHostnameSetupInstruction(String preference_name, String prompt_message, String help_message)
        throws IllegalArgumentException {
        super(preference_name, null, new InetAddressSetupValidityChecker(), prompt_message, help_message, false);
    }

    /**
     * Sets the default as a hostname for this local host. We try not to set it to a loopback device, but if we have no
     * choice, we do. We'll fallback to the current preference value if the default is {@link #GET_PREF_DEFAULT_MARKER}.
     *
     * @see SetupInstruction#preProcess()
     */
    public void preProcess() {
        super.preProcess();

        String current_value = null;

        if (GET_PREF_DEFAULT_MARKER.equals(getDefaultValue())) {
            current_value = getPreferences().get(getPreferenceName(), null);
            setDefaultValue(current_value);
        }

        if (current_value == null) {
            try {
                setDefaultValue(InetAddress.getLocalHost().getCanonicalHostName());
            } catch (Exception e) {
                setDefaultValue("localhost");
            }
        }

        return;
    }
}