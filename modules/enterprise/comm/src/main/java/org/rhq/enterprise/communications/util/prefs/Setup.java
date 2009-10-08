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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.prefs.Preferences;
import mazz.i18n.Logger;
import mazz.i18n.Msg;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This provides a way to initially setup a set of preferences. Normally, this is used when a new installation is laid
 * down and the user needs to be asked for some preference values to customize the installation with the user's own
 * settings. This could concievably also be used to customize any set of preferences, even those that already exist or
 * those that have recently been {@link PreferencesUpgrade upgraded}.
 *
 * @author John Mazzitelli
 */
public class Setup {
    /**
     * If the user enters this string at a prompt, it means the preference should not be set and its internal default
     * should be assumed.
     */
    private static final String INTERNAL_DEFAULT_PROMPT_VALUE = "!*";

    /**
     * If the user enters this string at a prompt, it means the preference help text needs to be shown to the user.
     */
    private static final String HELP_PROMPT_VALUE = "!?";

    /**
     * If the user enters this string at a prompt, it means the user wants to prematurely stop the setup, but leave the
     * settings already setup intact.
     */
    private static final String STOP_PROMPT_VALUE = "!+";

    /**
     * If the user enters this string at a prompt, it means the user wants to prematurely stop the setup, and abort any
     * changes made, thus reverting the preferences back to their original values.
     */
    private static final String CANCEL_PROMPT_VALUE = "!-";

    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(Setup.class);

    /**
     * I18N messages for output to console
     */
    private static final Msg MSG = CommI18NFactory.getMsg();

    private final Preferences m_preferences;
    private final String m_introMessage;
    private final List<SetupInstruction> m_instructions;
    private final PromptInput m_in;
    private final PrintWriter m_out;

    /**
     * Creates a new {@link Setup} object.
     *
     * @param preferences   the set of preferences that need to be customized
     * @param intro_message a human-readable string that will be {@link #getOut() output} prior to starting the
     *                      {@link #setup()} - this can be used to explain to the user what these set of preferences are
     *                      for (may be <code>null</code>)
     * @param instructions  the set of instructions this class will use to customize the preferences
     * @param in            the input where the answers to the setup prompts will come from
     * @param out           the output stream where the setup prompts will be output
     */
    public Setup(Preferences preferences, String intro_message, List<SetupInstruction> instructions, PromptInput in,
        PrintWriter out) {
        m_preferences = preferences;
        m_introMessage = intro_message;
        m_instructions = instructions;
        m_in = in;
        m_out = out;
    }

    /**
     * Returns the preferences that are to be customized via this setup instance.
     *
     * @return preferences
     */
    public Preferences getPreferences() {
        return m_preferences;
    }

    /**
     * Returns the intro message that will be printed to the {@link #getOut() output stream} before the {@link #setup()}
     * begins processing the instructions. If this is <code>null</code>, it will be ignored.
     *
     * @return human-readable introductory message (may be <code>null</code>)
     */
    public String getIntroMessage() {
        return m_introMessage;
    }

    /**
     * Returns the set of instructions that are used to ask the setup questions.
     *
     * @return instructions
     */
    public List<SetupInstruction> getInstructions() {
        return m_instructions;
    }

    /**
     * Returns the input object where the answers to the setup prompts will be read from.
     *
     * @return in
     */
    public PromptInput getIn() {
        return m_in;
    }

    /**
     * Returns the output stream where the prompts and any other output messages will be written to.
     *
     * @return out
     */
    public PrintWriter getOut() {
        return m_out;
    }

    /**
     * Performs the setup by asking questions and setting preferences according to this object's
     * {@link #getInstructions() setup instructions}.
     *
     * @return <code>true</code> if the setup finished; <code>false</code> if the user canceled the setup and reverted
     *         back to the original values
     *
     * @throws RuntimeException if failed to access the preferences backend store
     */
    public boolean setup() {
        PromptInput in = getIn();
        PrintWriter out = getOut();
        List<SetupInstruction> all_instructions = getInstructions();
        Preferences preferences = getPreferences();
        ByteArrayOutputStream backup = backupPreferences(preferences);

        // print an introductory message to the user explaining that we are going to ask some questions
        if (all_instructions.size() > 0) {
            if (getIntroMessage() != null) {
                out.println(getIntroMessage());
            }

            out.println(MSG.getMsg(CommI18NResourceKeys.SETUP_STANDARD_INTRO));
        }

        boolean user_stop = false; // will be true if the user asked to prematurely stop the setup but keep the current values
        boolean user_cancel = false; // will be true if the user asked to cancel the setup and restore the original values

        // for each setup instruction, ask the user to define the preference value as per the instruction
        for (SetupInstruction instruction : all_instructions) {
            // give the instruction the set of preferences that are being setup and
            // give the instruction a chance to pre-process its internal state before we begin
            LOG.debug(CommI18NResourceKeys.SETUP_PREPROCESS_INSTRUCTION, instruction);
            instruction.setPreferences(preferences);
            instruction.preProcess();

            // get the information needed to prompt the user for the new preference value
            String pref_name = instruction.getPreferenceName();
            String default_value = instruction.getDefaultValue();
            String prompt_message = instruction.getPromptMessage();
            String help_message = instruction.getHelpMessage();
            boolean no_echo = instruction.isUsingNoEchoPrompt();
            String new_value = null;

            if (prompt_message != null) {
                boolean is_valid;
                do {
                    new_value = prompt(prompt_message, pref_name, help_message, default_value, in, out, no_echo);

                    is_valid = true; // assume this new value is valid unless our validity checker (if one exists) tells us otherwise

                    if (new_value != null) {
                        if (new_value.length() > 0) {
                            user_stop = new_value.equals(STOP_PROMPT_VALUE);
                            user_cancel = new_value.equals(CANCEL_PROMPT_VALUE);

                            if ((instruction.getValidityChecker() != null) && !(user_stop || user_cancel)) {
                                is_valid = instruction.getValidityChecker().checkValidity(pref_name, new_value,
                                    preferences, out);
                            }
                        } else {
                            new_value = default_value;
                        }
                    }
                } while (!is_valid);

                if (!(user_stop || user_cancel)) {
                    LOG.debug(CommI18NResourceKeys.SETUP_NEW_VALUE, pref_name, new_value);
                }
            } else {
                // not required to prompt - use the instruction's default - no need to validate since that value came from the instruction directly
                new_value = default_value;
                LOG.debug(CommI18NResourceKeys.SETUP_NEW_VALUE_NO_PROMPT, pref_name, new_value);
            }

            if (user_stop || user_cancel) {
                break; // user asked to stop so do not process any more instructions
            }

            // now that we got the new value, put it in the preferences
            if (new_value != null) {
                preferences.put(pref_name, new_value);
            } else {
                // a null means the user wants to pick up our internal defaults, so we'll just remove the preference
                preferences.remove(pref_name);
            }

            // give the instruction a chance to post-process the preferences in case it needs to
            // do some more things to the preferences after the user entered the new preference value
            LOG.debug(CommI18NResourceKeys.SETUP_POSTPROCESS_INSTRUCTION, instruction);
            instruction.postProcess();
        }

        // print out a message so the user knows we are done asking questions and log the new set of preference values
        if (user_stop) {
            out.println(MSG.getMsg(CommI18NResourceKeys.SETUP_USER_STOP, preferences.absolutePath()));
        } else if (user_cancel) {
            out.println(MSG.getMsg(CommI18NResourceKeys.SETUP_USER_CANCEL, preferences.absolutePath()));
            restorePreferences(backup);
        } else {
            out.println(MSG.getMsg(CommI18NResourceKeys.SETUP_COMPLETE, preferences.absolutePath()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(CommI18NResourceKeys.SETUP_COMPLETE_WITH_DUMP, preferencesDump(preferences));
        }

        return !user_cancel;
    }

    /**
     * Prints the prompt message to the output stream and waits for the new value to be input. If the input is empty
     * (e.g. the user just hit the return key), an empty string is returned. A help message will be shown if the user
     * asks for it.
     *
     * <p>Note that the <code>default_value</code> is only used to show in the prompt. Specifically, it will not be
     * returned if the user simply hit the ENTER key without entering a value. In that case, an empty string is
     * returned; the caller should detect this and set the value to the default as appropriate.</p>
     *
     * <p>The returned value will be <code>null</code> if the user wants to rely on the system internal default and not
     * set the preference value at all.</p>
     *
     * @param  prompt_message the prompt message printed to the output stream that tells the user what is being asked
     *                        for
     * @param  pref_name      the actual preference name that is to be set to the answer of the prompt
     * @param  help_message   a more detailed help message that tells the user what is being asked for
     * @param  default_value  what the value should be if the user just hits the ENTER key
     * @param  in             where the user input is coming from
     * @param  out            the stream where the prompt messages will be written
     * @param  no_echo        if <code>true</code>, user should not see what is being typed at the prompt
     *
     * @return the new value entered by the user that came across in the input stream (may be <code>null</code>)
     *
     * @throws RuntimeException if failed to read from the input stream
     */
    protected String prompt(String prompt_message, String pref_name, String help_message, String default_value,
        PromptInput in, PrintWriter out, boolean no_echo) {
        String full_prompt = prompt_message + " ["
            + ((default_value != null) ? default_value : INTERNAL_DEFAULT_PROMPT_VALUE) + "] : ";
        String new_value = "";
        boolean keep_asking = true;

        while (keep_asking) {
            out.print(full_prompt);
            out.flush();

            try {
                if (no_echo) {
                    new_value = in.readLineNoEcho();
                } else {
                    new_value = in.readLine();
                }
            } catch (IOException e) {
                // this will abort the entire setup - but there is nothing we can do about this error, so that's what we have to do
                throw new RuntimeException(e);
            }

            if (new_value.equals(HELP_PROMPT_VALUE)) {
                out.println(help_message);
                out.println("(" + pref_name + ")");
            } else if (new_value.equals(INTERNAL_DEFAULT_PROMPT_VALUE)) {
                new_value = null;
                keep_asking = false;
            } else {
                keep_asking = false;
            }
        }

        return new_value;
    }

    /**
     * Given a set of preferences, this will dump each name/value in the returned string separated with a newline.
     *
     * @param  prefs the preferences whose values are to be dumped in the given string
     *
     * @return the preference name/value pairs separated by newlines
     */
    protected String preferencesDump(Preferences prefs) {
        try {
            StringBuffer dump = new StringBuffer(prefs.toString() + '\n');
            String[] pref_keys = prefs.keys();
            for (int i = 0; i < pref_keys.length; i++) {
                dump.append(pref_keys[i]);
                dump.append('=');
                dump.append(prefs.get(pref_keys[i], "<>"));
                dump.append('\n');
            }

            return dump.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Given a set of preferences, this will export them and return them in the given stream; in effect backing up their
     * values.
     *
     * @param  preferences the preferences to backup
     *
     * @return the backed up preferences.
     *
     * @throws RuntimeException if failed to access the backing store
     */
    private ByteArrayOutputStream backupPreferences(Preferences preferences) throws RuntimeException {
        try {
            ByteArrayOutputStream backup = new ByteArrayOutputStream();
            preferences.exportSubtree(backup);
            return backup;
        } catch (Exception e) {
            // should rarely occur, but if it does, something is probably wrong with the backing store so no need to continue
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a stream containing a {@link #backupPreferences(Preferences) backed up set of preferences}, this will
     * import them back in; in effect restoring their values.
     *
     * @param  backup the original, backed up preferences
     *
     * @throws RuntimeException if failed to access the backing store
     */
    private void restorePreferences(ByteArrayOutputStream backup) throws RuntimeException {
        try {
            getPreferences().clear();
            Preferences.importPreferences(new ByteArrayInputStream(backup.toByteArray()));
        } catch (Exception e) {
            // should rarely occur, but if it does, something is probably wrong with the backing store so no need to continue
            throw new RuntimeException(e);
        }
    }
}