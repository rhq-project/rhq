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
 * This setup instruction will perform preprocessing to determine if the user should be prompted for the preference
 * value or not. If a given boolean preference (called the "enablement preference") is <code>true</code>, then this
 * instruction will leave the prompt message as-is thus prompting the user for a new value. If that enablement
 * preference is <code>false</code>, then the prompt message will be set to <code>null</code> forcing this instruction's
 * new preference value to be the default value.
 *
 * <p>Use this when there are a set of preferences that are configuration settings for a particular feature that can be
 * enabled or disabled. If that feature is disabled (via the enablement preference), then the user should not be asked
 * to configure that feature any further. If that feature is enabled, then it makes sense to ask the user to configure
 * the rest of that feature's settings.</p>
 *
 * <p>This class derives from {@link DefaultSetupInstruction} so it will default to the existing preference value if one
 * exists; otherwise, the original {@link #getDefaultValue()} is used.</p>
 *
 * @author John Mazzitelli
 */
public class PromptIfEnabledSetupInstruction extends DefaultSetupInstruction {
    private final String m_enablementPreference;
    private final boolean m_enablementPreferenceDefault;

    /**
     * The <code>enablement_preference</code> is the name of the boolean preference that will determine if this
     * instruction will or will not prompt the user. The <code>enablement_preference_default</code> is the default value
     * of the enablement preference if it is not defined (that is, if it is not set in the set of preferences given to
     * this instruction via {@link #setPreferences(java.util.prefs.Preferences)}).
     *
     * @see SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String)
     */
    public PromptIfEnabledSetupInstruction(String preference_name, String default_value,
        SetupValidityChecker validity_checker, String prompt_message, String help_message,
        String enablement_preference, boolean enablement_preference_default) throws IllegalArgumentException {
        super(preference_name, default_value, validity_checker, prompt_message, help_message);
        m_enablementPreference = enablement_preference;
        m_enablementPreferenceDefault = enablement_preference_default;
    }

    /**
     * If the enablement preference's value is <code>true</code>, then this method does nothing, but if it is <code>
     * false</code>, this method will set the {@link #setPromptMessage(String) prompt} to <code>null</code> so the user
     * isn't bothered to ask to configure this setting.
     *
     * @see SetupInstruction#preProcess()
     */
    public void preProcess() {
        super.preProcess();

        if (!getPreferences().getBoolean(m_enablementPreference, m_enablementPreferenceDefault)) {
            // the "feature" is not enabled, so we do not have to bother to prompt the user
            setPromptMessage(null);
        }

        return;
    }
}