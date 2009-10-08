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
 * This setup instruction sets its default to the current preference value already set. If there is no preference value
 * already set, the original default passed to the constructor takes effect.
 *
 * <p>Use this setup instruction directly or subclass it if you want to give the user the opportunity to keep the same
 * value that already exists by providing that value as the prompt default.</p>
 *
 * @author John Mazzitelli
 */
public class DefaultSetupInstruction extends SetupInstruction {
    /**
     * @see SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public DefaultSetupInstruction(String preference_name, String default_value, SetupValidityChecker validity_checker,
        String prompt_message, String help_message) throws IllegalArgumentException {
        this(preference_name, default_value, validity_checker, prompt_message, help_message, false);
    }

    /**
     * @see SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
     */
    public DefaultSetupInstruction(String preference_name, String default_value, SetupValidityChecker validity_checker,
        String prompt_message, String help_message, boolean no_echo_prompt) throws IllegalArgumentException {
        super(preference_name, default_value, validity_checker, prompt_message, help_message, no_echo_prompt);
    }

    /**
     * This sets the default value to the current value found in the preferences. If there is no preference value
     * currently set, the original {@link #getDefaultValue()} remains in effect.
     *
     * @see SetupInstruction#preProcess()
     */
    public void preProcess() {
        String current_value = getPreferences().get(getPreferenceName(), null);

        if (current_value != null) {
            setDefaultValue(current_value);
        }

        return;
    }
}