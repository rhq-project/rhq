/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.server.control.command;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli 
 */
public class Upgrade extends ControlCommand {

    static private final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    static private final String FROM_SERVER_DIR_OPTION = "from-server-dir";

    private Options options;

    public Upgrade() {
        options = new Options()
            .addOption(
                null,
                FROM_AGENT_DIR_OPTION,
                true,
                "Full path to install directory of the RHQ Agent to be upgraded. Required only if an existing agent exists and is not installed in the default location: <from-server-dir>/rhq-agent")
            .addOption(null, FROM_SERVER_DIR_OPTION, true,
                "Full path to install directory of the RHQ Server to be upgraded. Required.");
    }

    @Override
    public String getName() {
        return "upgrade";
    }

    @Override
    public String getDescription() {
        return "Upgrades RHQ services from an earlier installed version";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        try {
            List<String> errors = validateOptions(commandLine);
            if (!errors.isEmpty()) {
                for (String error : errors) {
                    log.error(error);
                }
                log.error("Exiting due to the previous errors");
                return;
            }

            updateServerPropertiesFile(commandLine);

        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the upgrade command", e);
        }
    }

    private void updateServerPropertiesFile(CommandLine commandLine) throws Exception {
        File oldServerDir = getFromServerDir(commandLine);
        File oldServerPropsFile = new File(oldServerDir, "bin/rhq-server.properties");
        Properties oldServerProps = new Properties();
        FileInputStream oldServerPropsFileInputStream = new FileInputStream(oldServerPropsFile);
        try {
            oldServerProps.load(oldServerPropsFileInputStream);
        } finally {
            oldServerPropsFileInputStream.close();
        }

        String newServerPropsFilePath = new File(getBinDir(), "rhq-server.properties").getAbsolutePath();
        PropertiesFileUpdate newServerPropsFile = new PropertiesFileUpdate(newServerPropsFilePath);
        newServerPropsFile.update(oldServerProps);
    }

    private List<String> validateOptions(CommandLine commandLine) {
        List<String> errors = new LinkedList<String>();

        if (!commandLine.hasOption(FROM_SERVER_DIR_OPTION)) {
            errors.add("Missing required option: " + FROM_SERVER_DIR_OPTION);
        } else {
            File fromServerDir = new File(commandLine.getOptionValue(FROM_SERVER_DIR_OPTION));
            if (!fromServerDir.isDirectory()) {
                errors.add("The " + FROM_SERVER_DIR_OPTION + " directory does not exist: [" + fromServerDir.getPath()
                    + "]");
            } else {
                File serverPropsFile = new File(fromServerDir, "bin/rhq-server.properties");
                if (!serverPropsFile.isFile()) {
                    errors.add("The " + FROM_SERVER_DIR_OPTION
                        + " directory does not appear to be an RHQ installation. Missing expected file: ["
                        + serverPropsFile.getPath() + "]");
                }
            }
        }

        if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
            File fromAgentDir = new File(commandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
            if (!fromAgentDir.isDirectory()) {
                errors.add("The " + FROM_AGENT_DIR_OPTION + " directory does not exist: [" + fromAgentDir.getPath()
                    + "]");
            } else {
                String agentEnvFileName = (File.separatorChar == '/') ? "bin/rhq-agent-env.sh"
                    : "bin/rhq-agent-env.bat";
                File agentEnvFile = new File(fromAgentDir, agentEnvFileName);
                if (!agentEnvFile.isFile()) {
                    errors.add("The " + FROM_AGENT_DIR_OPTION
                        + " directory does not appear to be an RHQ Agent installation. Missing expected file: ["
                        + agentEnvFile.getPath() + "]");
                }
            }
        }

        return errors;
    }

    static public File getFromAgentDir(CommandLine commandLine) {
        return (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) ? new File(
            commandLine.getOptionValue(FROM_AGENT_DIR_OPTION)) : null;
    }

    static public File getFromServerDir(CommandLine commandLine) {
        return (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) ? new File(
            commandLine.getOptionValue(FROM_AGENT_DIR_OPTION)) : null;
    }

}
