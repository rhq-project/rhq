/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package org.rhq.server.control;

import java.io.Console;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.obfuscation.Obfuscator;
import org.rhq.enterprise.server.installer.ServerProperties;
import org.rhq.storage.installer.StorageProperty;

/**
 * @author John Sanda
 */
public class RHQControl {

    private final Log log = LogFactory.getLog(RHQControl.class);

    private Commands commands = new Commands();

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhqctl <cmd> [options]";
        String header = "\nwhere <cmd> is one of:";
        String footer = "\n* For help on a specific command: rhqctl <cmd> --help\n" //
            + "\n* Limit commands to a single component with one of: --storage, --server, --agent";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, commands.getOptions(), footer);
    }

    public void exec(String[] args) {
        ControlCommand command = null;
        boolean undo = false;
        AbortHook abortHook = new AbortHook();

        try {
            if (args.length == 0) {
                printUsage();
            } else {
                String commandName = findCommand(commands, args);
                command = commands.get(commandName);

                logWarningIfAgentRPMIsInstalled(command);

                validateInstallCommand(command, args);

                // in case the installer gets killed, prepare the shutdown hook to try the undo
                abortHook.setCommand(command);
                Runtime.getRuntime().addShutdownHook(abortHook);

                // run the command
                command.exec(getCommandLine(commandName, args));
            }
        } catch (UsageException e) {
            printUsage();
        } catch (RHQControlException e) {
            undo = true;

            Throwable rootCause = ThrowableUtil.getRootCause(e);
            // Only show the messy stack trace if we're in debug mode. Otherwise keep it cleaner for the user...
            if (log.isDebugEnabled()) {
                log.error(rootCause.getMessage(), rootCause);
            } else {
                log.error(rootCause.getMessage());
            }
        } catch (Throwable t) {
            undo = true;
            log.error(t);
        } finally {
            abortHook.setCommand(null);
            Runtime.getRuntime().removeShutdownHook(abortHook);
        }

        if (undo && command != null) {
            try {
                if (!Boolean.getBoolean("rhqctl.skip.undo")) {
                    command.undo();
                } else {
                    throw new Exception("Was told by user to skip clean up attempt.");
                }
            } catch (Throwable t) {
                log.warn("Failed to clean up after the failed installation attempt. "
                    + "You may have to clean up some things before attempting to install again", t);
            }
        }

        return;
    }

    private void logWarningIfAgentRPMIsInstalled(ControlCommand command) {
        // we only care about warning if the user is installing or upgrading; otherwise, just return silently
        if (!"install".equalsIgnoreCase(command.getName()) && (!"upgrade".equalsIgnoreCase(command.getName()))) {
            return;
        }

        // see if we can find an RPM installation somewhere.
        boolean rpmInstalled;
        File rpmParentLocation = new File("/usr/share");
        File[] rpms = rpmParentLocation.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith("jboss-on-")) {
                    File jonDir = new File(dir, name);
                    // technically, we should look for the agent in new File(jonDir, "agent") because that's the rpm install dir.
                    // but there are no other rpms other than the agent, so if we see this jboss-on-* location, we know the agent is here.
                    log.warn("An agent RPM installation was found in ["
                        + jonDir
                        + "]!!! You will not be able to successfully run this older agent anymore. You should manually remove it.");
                    return true;
                } else {
                    return false;
                }
            }
        });

        // if there is an RPM install on this box, we need to pause for some time to give the user a chance to read the message
        rpmInstalled = (rpms != null && rpms.length > 0);
        if (rpmInstalled) {
            try {
                log.warn("Please read the above warnings about the existence of agent RPM installations. This "
                    + command.getName() + " will resume in a few seconds.");
                Thread.sleep(30000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        return;
    }

    private void validateInstallCommand(ControlCommand command, String[] args) {
        if (!"install".equalsIgnoreCase(command.getName())) {
            return;
        }

        // just return if we're asking for help
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help")) {
            return;
        }

        // don't perform validation for components not involved in the command
        boolean validateServer = argsList.contains("--server")
            || (!argsList.contains("--storage") && !argsList.contains("--agent"));
        boolean validateStorage = argsList.contains("--storage")
            || (!argsList.contains("--server") && !argsList.contains("--agent"));

        // perform any up front validation we can at this point.  Note that after this point we
        // lose stdin due to the use of ProcessExecutions.

        if (validateServer) {
            File serverPropertiesFile = new File("bin/rhq-server.properties");

            if (validateServer && !serverPropertiesFile.isFile()) {
                throw new RHQControlException(
                    "The required rhq-server.properties file can not be found in the expected location ["
                        + serverPropertiesFile.getAbsolutePath() + "]. Installation is canceled.");
            }

            // Prompt for critical required values, if not yet set.
            try {
                PropertiesFileUpdate pfu = new PropertiesFileUpdate(serverPropertiesFile);
                Properties props = pfu.loadExistingProperties();

                promptForProperty(pfu, props, serverPropertiesFile.getName(), ServerProperties.PROP_JBOSS_BIND_ADDRESS,
                    false);
                promptForProperty(pfu, props, serverPropertiesFile.getName(), ServerProperties.PROP_DATABASE_PASSWORD,
                    true);

            } catch (Throwable t) {
                throw new RHQControlException("The rhq-server.properties file is not valid. Installation is canceled: "
                    + t.getMessage());
            }

            // Now, validate the property settings
            try {
                ServerProperties.validate(serverPropertiesFile);

            } catch (Throwable t) {
                throw new RHQControlException("The rhq-server.properties file is not valid. Installation is canceled: "
                    + t.getMessage());
            }
        }

        if (validateStorage) {
            try {
                File storagePropertiesFile = new File("bin/rhq-storage.properties");

                if (validateStorage && !storagePropertiesFile.isFile()) {
                    throw new RHQControlException(
                        "The required rhq-storage.properties file can not be found in the expected location ["
                            + storagePropertiesFile.getAbsolutePath() + "]. Installation is canceled.");
                }

                StorageProperty.validate(storagePropertiesFile);

            } catch (Throwable t) {
                throw new RHQControlException(
                    "The rhq-storage.properties file is not valid. Installation is canceled: " + t.getMessage());
            }
        }
    }

    private void promptForProperty(PropertiesFileUpdate pfu, Properties props, String propertiesFileName,
        String propertyName, boolean encode) throws Exception {

        String propertyValue = props.getProperty(propertyName);
        if (StringUtil.isBlank(propertyValue)) {

            // prompt for the property value
            Console console = System.console();
            console.format("\nThe [%s] property is required but not set in [%s].\n", propertyName, propertiesFileName);
            console.format("Do you want to set [%s] value now?\n", propertyName);
            String response = "";
            while (!(response.startsWith("n") || response.startsWith("y"))) {
                response = String.valueOf(console.readLine("%s", "yes|no: ")).toLowerCase();
            }
            if (response.startsWith("n")) {
                throw new RHQControlException("Please update the [" + propertiesFileName + "] file as required.");
            }

            do {
                propertyValue = "";
                while (StringUtil.isBlank(propertyValue)) {
                    propertyValue = String.valueOf(console.readLine("%s", propertyName
                        + ((encode ? " (enter as plain text): " : ": "))));
                }

                console.format("Is [" + propertyValue + "] correct?\n");
                response = "";
                while (!(response.startsWith("n") || response.startsWith("y"))) {
                    response = String.valueOf(console.readLine("%s", "yes|no: ")).toLowerCase();
                }
            } while (response.startsWith("n"));

            props.setProperty(propertyName, encode ? Obfuscator.encode(propertyValue) : propertyValue);
            pfu.update(props);
        }
    }

    private String findCommand(Commands commands, String[] args) throws RHQControlException {
        List<String> commandNames = new LinkedList<String>();
        for (String arg : args) {
            if (commands.contains(arg)) {
                commandNames.add(arg);
            }
        }

        if (commandNames.size() != 1) {
            throw new UsageException();
        }

        return commandNames.get(0);
    }

    private String[] getCommandLine(String cmd, String[] args) {
        String[] cmdLine = new String[args.length - 1];
        int i = 0;
        for (String arg : args) {
            if (arg.equals(cmd)) {
                continue;
            }
            cmdLine[i++] = arg;
        }
        return cmdLine;
    }

    public static void main(String[] args) throws Exception {
        RHQControl control = new RHQControl();
        try {
            control.exec(args);
            System.exit(0);
        } catch (RHQControlException e) {
            Throwable rootCause = ThrowableUtil.getRootCause(e);
            // Only show the messy stack trace if we're in debug mode. Otherwise keep it cleaner for the user...
            if (control.log.isDebugEnabled()) {
                control.log.error("There was an unexpected error: " + rootCause.getMessage(), rootCause);
            } else {
                control.log.error("There was an unexpected error: " + rootCause.getMessage());
            }
            System.exit(1);
        }
    }

    private class AbortHook extends Thread {
        private ControlCommand command = null;

        public AbortHook() {
            super("Controller Abort Hook");
        }

        public void setCommand(ControlCommand cmd) {
            this.command = cmd;
        }

        @Override
        public void run() {
            try {
                if (this.command != null) {
                    this.command.undo();
                }
            } catch (Throwable t) {
                log.warn("An attempt to clean up after an aborted installation was unsuccessful. "
                    + "You may have to clean up some things before attempting to install again", t);
            }
        }
    }
}
