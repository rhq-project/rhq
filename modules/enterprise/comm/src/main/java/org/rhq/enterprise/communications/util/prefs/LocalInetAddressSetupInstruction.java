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
import java.net.UnknownHostException;

/**
 * This setup instruction is used to define a preference whose value is an IP address that is to be bound to the local
 * host, but by default not the loopback device. This is used to allow a user to default the IP address to bind to
 * something other than the loopback device (localhost or 127.0.0.1) in order for clients external to this local box to
 * connect to it.
 *
 * @author John Mazzitelli
 */
public class LocalInetAddressSetupInstruction extends SetupInstruction {
    /**
     * Creates a new {@link LocalInetAddressSetupInstruction} object.
     *
     * @param  preference_name
     * @param  prompt_message
     * @param  help_message
     *
     * @throws IllegalArgumentException
     *
     * @see    SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public LocalInetAddressSetupInstruction(String preference_name, String prompt_message, String help_message)
        throws IllegalArgumentException {
        super(preference_name, null, new InetAddressSetupValidityChecker(), prompt_message, help_message, false);
    }

    /**
     * Sets the default as an IP address for this local host. We try not to set it to a loopback device.
     *
     * @see SetupInstruction#preProcess()
     */
    public void preProcess() {
        super.preProcess();

        try {
            setDefaultValue(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            setDefaultValue("127.0.0.1");
        }

        return;
    }
}