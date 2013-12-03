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
package org.rhq.enterprise.agent.promptcmd;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import mazz.i18n.Msg;

import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.enterprise.agent.AgentConfiguration;
import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.AgentPromptInfo;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.agent.i18n.AgentSetupInstructions;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.communications.util.prefs.BooleanSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.DefaultSetupInstruction;
import org.rhq.enterprise.communications.util.prefs.FloatSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.InetAddressSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.IntegerSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.LocalInetAddressNoDefaultSetupInstruction;
import org.rhq.enterprise.communications.util.prefs.LocalInetHostnameSetupInstruction;
import org.rhq.enterprise.communications.util.prefs.LongSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.PromptIfEnabledSetupInstruction;
import org.rhq.enterprise.communications.util.prefs.PromptInput;
import org.rhq.enterprise.communications.util.prefs.RegexSetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.RemotingLocatorUriParamsValidityChecker;
import org.rhq.enterprise.communications.util.prefs.Setup;
import org.rhq.enterprise.communications.util.prefs.SetupInstruction;
import org.rhq.enterprise.communications.util.prefs.SetupValidityChecker;
import org.rhq.enterprise.communications.util.prefs.UrlSetupValidityChecker;

/**
 * Sets up the agent with configuration information that the user enters at the prompt.
 *
 * @author John Mazzitelli
 */
public class SetupPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();
    private static final Msg SETUPMSG = new Msg(AgentSetupInstructions.BASE_BUNDLE_NAME);

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.SETUP);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        if (agent.isStarted()) {
            agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.SETUP_MUST_BE_STOPPED));
        } else {
            if (args.length == 1) {
                performBasicSetup(agent.getConfiguration().getPreferences(), new AgentPromptInfo(agent), agent.getOut());
            } else if ((args.length == 2) && args[1].equals(MSG.getMsg(AgentI18NResourceKeys.SETUP_ADVANCED))) {
                performAdvancedSetup(agent.getConfiguration().getPreferences(), new AgentPromptInfo(agent), agent
                    .getOut());
            } else if ((args.length == 2) && args[1].equals(MSG.getMsg(AgentI18NResourceKeys.SETUP_ALL))) {
                performAllSetup(agent.getConfiguration().getPreferences(), new AgentPromptInfo(agent), agent.getOut());
            } else {
                agent.getOut().println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            }
        }

        return true;
    }

    /**
     * This performs the actual setup by executing the setup instructions - it only asks for a minimal, basic set of
     * setup values to get the agent up and running.
     *
     * @param prefs the agent's configuration preferences
     * @param in    the input that the agent uses to get user input
     * @param out   the output stream that the agent uses to show messages to the user
     */
    public void performBasicSetup(Preferences prefs, PromptInput in, PrintWriter out) {
        Setup setup = new Setup(prefs, MSG.getMsg(AgentI18NResourceKeys.SETUP_INTRO), createBasicSetupInstructions(),
            in, out);

        if (setup.setup()) {
            prefs.putBoolean(AgentConfigurationConstants.CONFIG_SETUP, true);
        }

        return;
    }

    /**
     * This performs the actual setup by executing the setup instructions - it asks for some additional configuration
     * settings above and beyond the {@link #performBasicSetup(Preferences, PromptInput, PrintWriter) basic} setup
     * allowing for a more advanced ability to configure the agent.
     *
     * @param prefs the agent's configuration preferences
     * @param in    the input stream that the agent uses to get user input
     * @param out   the output stream that the agent uses to show messages to the user
     */
    public void performAdvancedSetup(Preferences prefs, PromptInput in, PrintWriter out) {
        Setup setup = new Setup(prefs, MSG.getMsg(AgentI18NResourceKeys.SETUP_INTRO_ADVANCED),
            createAdvancedSetupInstructions(), in, out);

        if (setup.setup()) {
            prefs.putBoolean(AgentConfigurationConstants.CONFIG_SETUP, true);
        }

        return;
    }

    /**
     * This performs the actual setup by executing the setup instructions - it asks for practically all possible
     * configuration settings allowing for a complete ability to configure the agent.
     *
     * @param prefs the agent's configuration preferences
     * @param in    the input that the agent uses to get user input
     * @param out   the output stream that the agent uses to show messages to the user
     */
    public void performAllSetup(Preferences prefs, PromptInput in, PrintWriter out) {
        Setup setup = new Setup(prefs, MSG.getMsg(AgentI18NResourceKeys.SETUP_INTRO_ALL), createAllSetupInstructions(),
            in, out);

        if (setup.setup()) {
            prefs.putBoolean(AgentConfigurationConstants.CONFIG_SETUP, true);
        }

        return;
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.SETUP_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.SETUP_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return getHelp();
    }

    /**
     * Creates the list of basic setup instructions that are to be used to setup the agent configuration by asking a
     * minimal amount of questions.
     *
     * @return the list of instructions used to perform the basic setup
     */
    private List<SetupInstruction> createBasicSetupInstructions() {
        List<SetupInstruction> instr = new ArrayList<SetupInstruction>();

        instr.add(new LocalInetHostnameSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTNAME_PREF, null, // don't pass this in if we want to force fqdn to be verified as a real FQDN
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTNAME_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTNAME_HELP)));

        instr.add(new LocalInetAddressNoDefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORBINDADDR_PREF, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORBINDADDR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORBINDADDR_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORPORT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORPORT_DEFAULT, new IntegerSetupValidityChecker(1, 65535),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORPORT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORPORT_HELP)));

        instr.add(new ServerAddressSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERBINDADDR_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERBINDADDR_DEFAULT, new InetAddressSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERBINDADDR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERBINDADDR_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPORT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPORT_DEFAULT, new IntegerSetupValidityChecker(1, 65535),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPORT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPORT_HELP)));

        return instr;
    }

    /**
     * Creates the list of advanced setup instructions that are to be used to setup the agent configuration by asking
     * more advanced questions that require more knowledge from the user. This mostly allows the user to setup secure
     * communications by changing the transports and SSL settings.
     *
     * @return the list of instructions used to perform the advanced setup
     */
    private List<SetupInstruction> createAdvancedSetupInstructions() {
        List<SetupInstruction> instr = new ArrayList<SetupInstruction>();

        instr.addAll(createBasicSetupInstructions());

        final String MIN_KEYSTORE_PASSWORD_LENGTH = ".{6,}";
        final String VALID_SERVER_TRANSPORTS_REGEX = "servlet|sslservlet|socket|sslsocket";

        // insert after the other connector setting instructions (requires knowledge of the basic instruction ordering!)
        instr.add(3, new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORT_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORT_HELP)));

        instr.add(4, new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_DEFAULT,
            new RemotingLocatorUriParamsValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORTRANSPORTPARAMS_HELP)));

        // the rest of the instructions should go after the basic setup instructions
        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORT_DEFAULT, new RegexSetupValidityChecker(
                VALID_SERVER_TRANSPORTS_REGEX, CommI18NResourceKeys.NOT_SERVLET_TRANSPORT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORT_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_DEFAULT,
            new RemotingLocatorUriParamsValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRANSPORTPARAMS_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERALIAS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERALIAS_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERALIAS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERALIAS_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_DEFAULT,
            new ClientAuthModeSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERCLIENTAUTHMODE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_DEFAULT,
            new BooleanSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSERVERAUTHMODEENABLED_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERSECUREPROTOCOL_HELP)));

        instr.add(new SecurityDataFileSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREFILE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREFILE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREFILE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREFILE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALGORITHM_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTORETYPE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTORETYPE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTORETYPE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTORETYPE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREPASSWORD_HELP), true));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREKEYPASSWORD_HELP), true));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERKEYSTOREALIAS_HELP)));

        instr.add(new SecurityDataFileSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREFILE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREALGORITHM_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTORETYPE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERTRUSTSTOREPASSWORD_HELP), true));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSECUREPROTOCOL_HELP)));

        instr.add(new SecurityDataFileSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREFILE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALGORITHM_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTORETYPE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREPASSWORD_HELP), true));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREKEYPASSWORD_HELP), true));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTKEYSTOREALIAS_HELP)));

        instr.add(new SecurityDataFileSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREFILE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREALGORITHM_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTORETYPE_HELP)));

        instr.add(new SecurityEnabledSetupInstruction(AgentConfigurationConstants.SERVER_TRANSPORT,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_DEFAULT, new RegexSetupValidityChecker(
                MIN_KEYSTORE_PASSWORD_LENGTH, CommI18NResourceKeys.KEYSTORE_PASSWORD_MIN_LENGTH), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTTRUSTSTOREPASSWORD_HELP), true));

        return instr;
    }

    /**
     * Creates the list of all setup instructions that are to be used to setup the agent configuration by asking a large
     * amount of questions, essentially asking for all agent configuration preferences (well, 99% of them anyway).
     *
     * @return the list of instructions used to perform the 'all' setup
     */
    private List<SetupInstruction> createAllSetupInstructions() {
        List<SetupInstruction> instr = new ArrayList<SetupInstruction>();

        instr.addAll(createAdvancedSetupInstructions());

        // the rest of the instructions should go after the basic and advanced setup instructions
        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPOLLING_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPOLLING_DEFAULT, new LongSetupValidityChecker(0L, null),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPOLLING_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERPOLLING_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERAUTODETECT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_SERVERAUTODETECT_DEFAULT, new BooleanSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERAUTODETECT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_SERVERAUTODETECT_HELP)));

        instr.add(new MulticastDetectorEnabledSetupInstruction());

        instr.add(new PromptIfEnabledSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_DEFAULT,
            new InetAddressSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORMCADDR_HELP),
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED));

        instr.add(new PromptIfEnabledSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_DEFAULT,
            new InetAddressSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORBINDADDR_HELP),
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED));

        instr.add(new PromptIfEnabledSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORPORT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORPORT_DEFAULT, new IntegerSetupValidityChecker(1,
                65535), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORPORT_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORPORT_HELP),
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED));

        instr.add(new PromptIfEnabledSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_DEFAULT,
            new LongSetupValidityChecker(1000L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORDEFAULTTIMEDELAY_HELP),
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED));

        instr.add(new PromptIfEnabledSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_DEFAULT,
            new LongSetupValidityChecker(1000L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTORHEARTBEATTIMEDELAY_HELP),
            ServiceContainerConfigurationConstants.MULTICASTDETECTOR_ENABLED,
            ServiceContainerConfigurationConstants.DEFAULT_MULTICASTDETECTOR_ENABLED));

        // TODO [mazz]: Lease period currently affects nothing - need to add a connector listener as part of auto-discovery
        //      instr.add( new DefaultSetupInstruction( AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORLEASE_PREF,
        //                                              AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORLEASE_DEFAULT,
        //                                              new LongSetupValidityChecker( -1L, null ),
        //                                              SETUPMSG.getMsg( AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORLEASE_PROMPT ),
        //                                              SETUPMSG.getMsg( AgentSetupInstructions.SETUP_INSTRUCTION_CONNECTORLEASE_HELP ) ) );

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_DEFAULT, new BooleanSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_UPDATEPLUGINSATSTARTUP_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_DEFAULT,
            new BooleanSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_TESTFAILOVERLISTATSTARTUP_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEENABLED_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEENABLED_DEFAULT, new BooleanSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEENABLED_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEENABLED_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_DEFAULT, new UrlSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEVERSIONURL_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_DEFAULT, new UrlSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTUPDATEDOWNLOADURL_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_DEFAULT,
            new BooleanSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_REGISTERWITHSERVERATSTARTUP_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_DEFAULT, new LongSetupValidityChecker(
                0L, null),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_WAITFORSERVERATSTARTUPMSECS_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_DEFAULT,
            new LongSetupValidityChecker(0L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PRIMARYSERVERSWITCHOVERCHECKINTERVAL_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_DISABLENATIVESYSTEM_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_DISABLENATIVESYSTEM_DEFAULT, new BooleanSetupValidityChecker(),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_DISABLENATIVESYSTEM_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_DISABLENATIVESYSTEM_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVERDISCOVERYINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSSERVICEDISCOVERYINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_DEFAULT, new LongSetupValidityChecker(1L,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_DEFAULT,
            new IntegerSetupValidityChecker(1, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSAVAILSCANTHREADPOOLSIZE_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSMEASUREMENTCOLLINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_DEFAULT, new IntegerSetupValidityChecker(
                1, null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCMEASUREMENTTHREADCOUNT_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_DEFAULT, new IntegerSetupValidityChecker(1,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPERATIONTHREADCOUNT_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_DEFAULT, new LongSetupValidityChecker(1L,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PCOPINVOCATIONTIMEOUT_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_DEFAULT, new LongSetupValidityChecker(
                0L, null),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDRIFTDETECTIONINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_DEFAULT,
            new LongSetupValidityChecker(0L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_DEFAULT,
            new IntegerSetupValidityChecker(1, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONTENTDISCOVERYTHREADCOUNT_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_DEFAULT,
            new LongSetupValidityChecker(0L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(1L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSCONFIGURATIONDISCOVERYINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_DEFAULT, new LongSetupValidityChecker(
                30L, null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERPERIOD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_DEFAULT,
            new LongSetupValidityChecker(30L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTSENDERINITIALDELAY_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_DEFAULT,
            new IntegerSetupValidityChecker(0, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXPERSRC_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_DEFAULT,
            new IntegerSetupValidityChecker(0, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSEVENTREPORTMAXTOTAL_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_DEFAULT, new LongSetupValidityChecker(0L,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKINTERVAL_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_DEFAULT,
            new FloatSetupValidityChecker(0.00f, 1.00f), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWHEAPMEMTHRESHOLD_HELP)));

        instr.add(new DefaultSetupInstruction(
            AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_DEFAULT,
            new FloatSetupValidityChecker(0.00f, 1.00f), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_VMHEALTHCHECKLOWNONHEAPMEMTHRESHOLD_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_DEFAULT, new LongSetupValidityChecker(-1L,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERTIMEOUT_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_DEFAULT, new LongSetupValidityChecker(
                1000L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERRETRYINTERVAL_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_DEFAULT, new IntegerSetupValidityChecker(0,
                null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXRETRIES_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_DEFAULT, new LongSetupValidityChecker(
                1L, null), SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_PROMPT),
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERMAXCONCURRENT_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQSIZE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQSIZE_DEFAULT,
            new LongSetupValidityChecker(100L, null), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQSIZE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQSIZE_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_DEFAULT,
            new ParametersSetupValidityChecker(ParametersType.QUEUE_THROTTLING_PARAMS), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERQTHROTTLING_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_DEFAULT,
            new ParametersSetupValidityChecker(ParametersType.SEND_THROTTLING_PARAMS), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSENDTHROTTLING_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_STREAMIDLE_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_STREAMIDLE_DEFAULT, new LongSetupValidityChecker(5000L,
                1000L * 60 * 60), // no less than 5s, no more than an hour
            SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_STREAMIDLE_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_STREAMIDLE_HELP)));

        instr.add(new DataDirectorySetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTDATADIR_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_AGENTDATADIR_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTDATADIR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_AGENTDATADIR_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDIR_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDIR_DEFAULT, null, SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDIR_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_PLUGINSDIR_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_DEFAULT,
            new ParametersSetupValidityChecker(ParametersType.COMMAND_SPOOL_PARAMS), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERSPOOLPARAMS_HELP)));

        instr.add(new DefaultSetupInstruction(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_PREF,
            AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_DEFAULT,
            new BooleanSetupValidityChecker(), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_PROMPT), SETUPMSG
                .getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_CLIENTSENDERCOMPRESSSPOOL_HELP)));

        return instr;
    }

    /**
     * This setup instruction sets up multicast detector enabled preference - it will force a value without asking the
     * user based on whether the multicast detector is needed or not. If server auto-detection is not enabled, no
     * multicast detector configuration needs to be set up since the multicast detector should be disabled. The reverse
     * is true - if auto-detection is enabled, the multicast detector must be enabled as well.
     */
    private class MulticastDetectorEnabledSetupInstruction extends SetupInstruction {
        /**
         * @see SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
         */
        public MulticastDetectorEnabledSetupInstruction() {
            super(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTOR_PREF, null, // no default - our preProcess will define it based on the auto-detection pref
                null, // no validity checker needed - preProcess will guarantee a valid value
                null, // no prompt - the instruction will set the value, no need to ask user
                SETUPMSG.getMsg(AgentSetupInstructions.SETUP_INSTRUCTION_MULTICASTDETECTOR_HELP), false);
        }

        /**
         * Sets the default value to <code>true</code> if the server auto-detection preference is <code>true</code> and
         * vice versa. Prompt will be forced to <code>null</code> - the user should never need to be prompted for the
         * value since it can always be determined what this value should be based on the server auto-detection value.
         */
        @Override
        public void preProcess() {
            super.preProcess();

            if (getPreferences().getBoolean(AgentConfigurationConstants.SERVER_AUTO_DETECTION,
                AgentConfigurationConstants.DEFAULT_SERVER_AUTO_DETECTION)) {
                setDefaultValue(Boolean.TRUE.toString());
            } else {
                setDefaultValue(Boolean.FALSE.toString());
            }

            return;
        }
    }

    /**
     * The setup instruction that makes sure the agent and comm layers use the same data directory.
     */
    private class DataDirectorySetupInstruction extends SetupInstruction {
        /**
         * @see SetupInstruction#SetupInstruction(String, String, SetupValidityChecker, String, String, boolean)
         */
        public DataDirectorySetupInstruction(String preference_name, String default_value,
            SetupValidityChecker validity_checker, String prompt_message, String help_message)
            throws IllegalArgumentException {
            super(preference_name, default_value, validity_checker, prompt_message, help_message, false);
        }

        /**
         * The comm layer's data directory should always be the same as the agent's data directory. This post process
         * method ensures that.
         *
         * @see SetupInstruction#postProcess()
         */
        @Override
        public void postProcess() {
            super.postProcess();

            Preferences preferences = getPreferences();
            AgentConfiguration agent_config = new AgentConfiguration(preferences);
            File agent_dir = agent_config.getDataDirectory();

            // let's forcibly make sure that the agent and comm dir values are stored in preferences
            // we need to make sure they are the same
            preferences.put(AgentConfigurationConstants.DATA_DIRECTORY, agent_dir.getPath());
            preferences.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, agent_dir.getPath());

            return;
        }
    }

    /**
     * Enumeration for the different types of parameters values that the parameters validity checker can validate.
     */
    private enum ParametersType {
        QUEUE_THROTTLING_PARAMS, SEND_THROTTLING_PARAMS, COMMAND_SPOOL_PARAMS
    }

    /**
     * This checks the validity of some of the agent preferences that have complex types.
     */
    private class ParametersSetupValidityChecker implements SetupValidityChecker {
        private final ParametersType m_paramsType;

        ParametersSetupValidityChecker(ParametersType params_id) {
            m_paramsType = params_id;
        }

        /**
         * @see SetupValidityChecker#checkValidity(String, String, Preferences, PrintWriter)
         */
        public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
            boolean is_valid = true;
            String err_msg_key = null;

            if (value_to_check != null) {
                AgentConfiguration agent_config = new AgentConfiguration(preferences);

                if (m_paramsType.equals(ParametersType.QUEUE_THROTTLING_PARAMS)) {
                    err_msg_key = AgentI18NResourceKeys.SETUP_BAD_QUEUE_THROTTLING_PARAMS;
                    is_valid = null != agent_config.isClientSenderQueueThrottlingValueValid(value_to_check);
                } else if (m_paramsType.equals(ParametersType.SEND_THROTTLING_PARAMS)) {
                    err_msg_key = AgentI18NResourceKeys.SETUP_BAD_SEND_THROTTLING_PARAMS;
                    is_valid = null != agent_config.isClientSenderSendThrottlingValueValid(value_to_check);
                } else if (m_paramsType.equals(ParametersType.COMMAND_SPOOL_PARAMS)) {
                    err_msg_key = AgentI18NResourceKeys.SETUP_BAD_COMMAND_SPOOL_PARAMS;
                    is_valid = null != agent_config.isClientSenderCommandSpoolFileParamsValueValid(value_to_check);
                }
            }

            if (!is_valid) {
                out.println(MSG.getMsg(err_msg_key, value_to_check));
            }

            return is_valid;
        }
    }

    /**
     * This checks the validity of the client-auth mode value.
     */
    private class ClientAuthModeSetupValidityChecker implements SetupValidityChecker {
        /**
         * @see SetupValidityChecker#checkValidity(String, String, Preferences, PrintWriter)
         */
        public boolean checkValidity(String pref_name, String value_to_check, Preferences preferences, PrintWriter out) {
            String none = SSLSocketBuilder.CLIENT_AUTH_MODE_NONE;
            String want = SSLSocketBuilder.CLIENT_AUTH_MODE_WANT;
            String need = SSLSocketBuilder.CLIENT_AUTH_MODE_NEED;

            boolean is_valid = (none.equals(value_to_check) || want.equals(value_to_check) || need
                .equals(value_to_check));

            if (!is_valid) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.SETUP_BAD_CLIENT_AUTH_MODE, value_to_check, none, want,
                    need));
            }

            return is_valid;
        }
    }

    /**
     * This setup instruction sets up security enabled preferences - it will force a value without asking the user based
     * on whether security is enabled or not.
     */
    private class SecurityEnabledSetupInstruction extends DefaultSetupInstruction {
        /**
         * Name of the preference whose value will indicate if security is enabled or not.
         */
        private String m_securityPreferenceName;

        /**
         * Constructor for {@link SecurityEnabledSetupInstruction} that is the same as the superclass constructor except
         * you also tell it the security preference to check. If the given security preference value indicates that
         * security is disabled, this instruction will not prompt the user for an answer.
         *
         * @param  security_preference_name
         * @param  preference_name
         * @param  default_value
         * @param  validity_checker
         * @param  prompt_message
         * @param  help_message
         * @param  no_echo
         *
         * @throws IllegalArgumentException
         */
        public SecurityEnabledSetupInstruction(String security_preference_name, String preference_name,
            String default_value, SetupValidityChecker validity_checker, String prompt_message, String help_message,
            boolean no_echo) throws IllegalArgumentException {
            super(preference_name, default_value, validity_checker, prompt_message, help_message, no_echo);
            m_securityPreferenceName = security_preference_name;
        }

        /**
         * Same as above constructor except the prompt will echo the input.
         *
         * @param  security_preference_name
         * @param  preference_name
         * @param  default_value
         * @param  validity_checker
         * @param  prompt_message
         * @param  help_message
         *
         * @throws IllegalArgumentException
         */
        public SecurityEnabledSetupInstruction(String security_preference_name, String preference_name,
            String default_value, SetupValidityChecker validity_checker, String prompt_message, String help_message)
            throws IllegalArgumentException {
            this(security_preference_name, preference_name, default_value, validity_checker, prompt_message,
                help_message, false);
        }

        /**
         * If the security preference value does not indicate that security is enabled, this method will set the prompt
         * string to <code>null</code> so the user is never prompted for this instruction's value.
         */
        @Override
        public void preProcess() {
            super.preProcess();

            if (!SecurityUtil.isTransportSecure(getPreferences().get(m_securityPreferenceName, ""))) {
                setPromptMessage(null);
            }

            return;
        }
    }

    /**
     * This setup instruction prefixes the given default with the current data directory preference. Note that any
     * instructions using this should be done after the data directory has been entered by the user.
     */
    private class SecurityDataFileSetupInstruction extends SecurityEnabledSetupInstruction {
        /**
         * @see SecurityDataFileSetupInstruction#SecurityDataFileSetupInstruction(String, String, String,
         *      SetupValidityChecker, String, String)
         */
        public SecurityDataFileSetupInstruction(String security_preference_name, String preference_name,
            String default_value, SetupValidityChecker validity_checker, String prompt_message, String help_message)
            throws IllegalArgumentException {
            super(security_preference_name, preference_name, default_value, validity_checker, prompt_message,
                help_message);
        }

        /**
         * Prefixes the default value with the current data directory preference of the agent.
         *
         * @see SetupInstruction#preProcess()
         */
        @Override
        public void preProcess() {
            String data_dir = new AgentConfiguration(getPreferences()).getDataDirectoryIfDefined();
            String default_file = getDefaultValue();

            if ((data_dir != null) && (default_file != null)) {
                setDefaultValue(new File(data_dir, default_file).getPath());
            }

            // give our superclass the opportunity to override our new default
            super.preProcess();

            return;
        }
    }

    private class ServerAddressSetupInstruction extends DefaultSetupInstruction {
        public ServerAddressSetupInstruction(String pref_name, String default_value,
            InetAddressSetupValidityChecker validity_checker, String prompt_msg, String help) {
            super(pref_name, default_value, validity_checker, prompt_msg, help, false);
        }

        @Override
        public void preProcess() {
            String address = new AgentConfiguration(getPreferences()).getServerBindAddress();
            if (address != null) {
                setDefaultValue(address);
            } else {
                super.preProcess();
            }
        }
    }
}