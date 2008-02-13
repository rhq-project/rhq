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

import java.util.prefs.Preferences;

/**
 * Instances of this class represent a single setup instruction that defines a preference that needs to be setup. It
 * defines things like what the preference name is, what its default value should be, a human-readable help text to
 * explain what the preference is for, and other things.
 *
 * <p>This class can be used as-is or can be subclassed if additional processing needs to be performed during the setup
 * (such as pre-processing to obtain a default value at runtime or post-processing the new user-entered value).</p>
 *
 * @author John Mazzitelli
 */
public class SetupInstruction {
    private final String m_preferenceName;
    private String m_defaultValue;
    private SetupValidityChecker m_validityChecker;
    private String m_promptMessage;
    private String m_helpMessage;
    private Preferences m_preferences;
    private boolean m_useNoEchoPrompt;

    /**
     * Constructor for {@link SetupInstruction}.
     *
     * @param  preference_name  the name of the preference that this instruction will setup (must not be <code>
     *                          null</code>)
     * @param  default_value    the default value of the preference should the user input be an empty return
     * @param  validity_checker an optional object that can be used to validate a new preference value (may be <code>
     *                          null</code>)
     * @param  prompt_message   the human-readable prompt string that will be shown to the user that asks for the
     *                          customized preference value the user wants to use (may be <code>null</code>, in which
     *                          case, the {@link Setup} should not stop and ask the user for a value - the default value
     *                          will be used automatically)
     * @param  help_message     the human-readable help string that provides more detail on the preference (for the user
     *                          to see if they don't know what the preference is for)
     * @param  no_echo_prompt   if <code>true</code>, the prompt is assumed to be for some secure setting that should no
     *                          be echoed to the screen to avoid spying eyes from seeing it. When <code>true</code>, the
     *                          prompt will be asked a second time to confirm the answer, since the user won't be able
     *                          to see what is being typed
     *
     * @throws IllegalArgumentException if <code>preference_name</code> is <code>null</code>
     */
    public SetupInstruction(String preference_name, String default_value, SetupValidityChecker validity_checker,
        String prompt_message, String help_message, boolean no_echo_prompt) throws IllegalArgumentException {
        if (preference_name == null) {
            throw new IllegalArgumentException("preference_name=null");
        }

        m_preferenceName = preference_name;
        m_defaultValue = default_value;
        m_validityChecker = validity_checker;
        m_promptMessage = prompt_message;
        m_helpMessage = help_message;
        m_preferences = null;
        m_useNoEchoPrompt = no_echo_prompt;
    }

    /**
     * This method is called by {@link Setup} to inform this object that the preferences are about to be setup with the
     * information in this instruction. Subclasses are free to override this method to perform some pre-processing in
     * case it wants to do things like define a {@link #getDefaultValue() default value} based on information it gathers
     * at runtime, define a different prompt message, etc.
     *
     * <p>This method implementation is a no-op and simply does nothing but return.</p>
     */
    public void preProcess() {
        return; // no-op
    }

    /**
     * This method is called by {@link Setup} to inform this object that the preferences have been setup with the
     * information in this instruction - {@link #getPreferences()} will return the preferences that have been changed.
     * Subclasses are free to override this method to perform some post-processing in case it wants to do things like
     * modify the preferences based on the new values in the {@link #getPreferences() preferences}.
     *
     * <p>This method implementation is a no-op and simply does nothing but return.</p>
     */
    public void postProcess() {
        return; // no-op
    }

    /**
     * Returns the preferences that are being setup according to this instruction.
     *
     * @return the preferences (will be <code>null</code> if this instruction hasn't
     *         {@link #setPreferences(Preferences) been told} what preferences are being setup)
     */
    public Preferences getPreferences() {
        return m_preferences;
    }

    /**
     * Sets the preferences that are being setup according to this instruction object.
     *
     * @param preferences the preferences being setup
     */
    public void setPreferences(Preferences preferences) {
        m_preferences = preferences;
    }

    /**
     * Returns the name of the preference that is setup by this instruction.
     *
     * @return preference name
     */
    public String getPreferenceName() {
        return m_preferenceName;
    }

    /**
     * Returns the default value that is to be assigned to the preference if the answer to the prompt was an empty
     * return.
     *
     * @return preference default value
     */
    public String getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * Sets the default value that is to be assigned to the preference if the answer to the prompt was an empty return.
     *
     * @param default_value the new value of defaultValue
     */
    protected void setDefaultValue(String default_value) {
        m_defaultValue = default_value;
    }

    /**
     * Returns the optionally defined validity checker object that will be responsible for making sure any new
     * preference value to be set is valid. If <code>null</code>, all values are considered valid.
     *
     * @return the object that performs validation checks on new values of the preference handled by this instruction
     *         (may be <code>null</code>)
     */
    public SetupValidityChecker getValidityChecker() {
        return m_validityChecker;
    }

    /**
     * Sets the validity checker object that will be responsible for making sure any new preference value to be set is
     * valid. If <code>null</code>, all values will be considered valid.
     *
     * @param validity_checker the object that performs validation checks on new values of the preference handled by
     *                         this instruction (may be <code>null</code>)
     */
    protected void setValidityChecker(SetupValidityChecker validity_checker) {
        m_validityChecker = validity_checker;
    }

    /**
     * Returns the human-readable prompt message that asks the question of the user for the new preference value. If the
     * returned value is <code>null</code>, the user will not be prompted; instead, the
     * {@link #getDefaultValue() default value} will be the value to be used as the new preference value.
     *
     * @return prompt message string (may be <code>null</code>)
     */
    public String getPromptMessage() {
        return m_promptMessage;
    }

    /**
     * Sets the human-readable prompt message that asks the question of the user for the new preference value. If the
     * <code>prompt_message</code> is <code>null</code>, the user will not be prompted; instead, the
     * {@link #getDefaultValue() default value} will be the value to be used as the new preference value.
     *
     * @param prompt_message the new prompt message string (may be <code>null</code>)
     */
    protected void setPromptMessage(String prompt_message) {
        m_promptMessage = prompt_message;
    }

    /**
     * Returns the human-readable help message that describes the preference being setup in more detail.
     *
     * @return help message string
     */
    public String getHelpMessage() {
        return m_helpMessage;
    }

    /**
     * Sets the human-readable help message that describes the preference being setup in more detail.
     *
     * @param help_message the new help message string
     */
    protected void setHelpMessage(String help_message) {
        m_helpMessage = help_message;
    }

    /**
     * If <code>true</code>, the prompt will not echo what the user typed (as you would want if the prompt is asking for
     * a password, for example).
     *
     * @return flag to indicate if the prompt will not echo back to the user what is being typed
     */
    public boolean isUsingNoEchoPrompt() {
        return m_useNoEchoPrompt;
    }

    /**
     * If <code>true</code>, the prompt will not echo what the user typed (as you would want if the prompt is asking for
     * a password, for example).
     *
     * @param no_echo flag to indicate if the prompt will not echo back to the user what is being typed
     */
    protected void setUsingNoEchoPrompt(boolean no_echo) {
        m_useNoEchoPrompt = no_echo;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return SetupInstruction.class.getName() + "[" + getPreferenceName() + "]";
    }
}