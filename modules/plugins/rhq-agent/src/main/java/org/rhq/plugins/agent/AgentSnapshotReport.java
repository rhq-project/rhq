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
package org.rhq.plugins.agent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.util.SnapshotReport;

/**
 * Performs some slight customizations of the snapshot report utility such as taking a snapshot of the live
 * configuration. It also takes the agent plugin configuration and builds the report configuration out of it.
 * 
 * @author John Mazzitelli
 *
 */
public class AgentSnapshotReport extends SnapshotReport {
    private static final Log log = LogFactory.getLog(AgentSnapshotReport.class);

    private final String agentInstallDir;
    private final Properties agentConfiguration;

    /**
     * Constructor used to build this agent snapshot report object.
     * 
     * @param name the name of the snapshot report
     * @param description the description of this report
     * @param agentPluginConfiguration the plugin configuration for the agent resource whose snapshot report is to be generated
     * @param agentInstallDir the agent's root installation directory
     * @param agentConfiguration the agent's live configuration 
     * @param tmpDirectory a tmp location where we can store the snapshot 
     */
    public AgentSnapshotReport(String name, String description, Configuration agentPluginConfiguration,
        String agentInstallDir, Properties agentConfiguration, String tmpDirectory) {

        super(name, description, buildConfiguration(agentPluginConfiguration, agentInstallDir, tmpDirectory));

        this.agentInstallDir = agentInstallDir;
        this.agentConfiguration = agentConfiguration;
    }

    /**
     * We want to snapshot the live configuration taken from the Java Preferences so this will write a temporary
     * conf file with that live data so it gets placed in the snapshot report.
     */
    @Override
    public File generate() throws Exception {
        File liveConfigFile = writeLiveConfigurationFile();
        try {
            return super.generate();
        } finally {
            if (liveConfigFile != null) {
                liveConfigFile.delete();
            }
        }
    }

    private File writeLiveConfigurationFile() {
        try {
            File configDir = new File(this.agentInstallDir, "conf");
            File liveConfigFile = new File(configDir, "live-agent-configuration.properties");
            FileOutputStream fos = new FileOutputStream(liveConfigFile);
            try {
                this.agentConfiguration.store(new BufferedOutputStream(fos), null);
            } finally {
                fos.close();
            }
            return liveConfigFile;
        } catch (Exception e) {
            log.warn("Cannot store live agent config - will not snapshot it. Cause: " + e);
            return null; // we can't snapshot it but just keep going so we can snapshot everything else
        }
    }

    private static Configuration buildConfiguration(Configuration pluginConfiguration, String installDir, String tmpDir) {

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_REPORT_OUTPUT_DIRECTORY, tmpDir));
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, installDir));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_LOG_FILES, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_DATA_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_DATA_FILES, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, "conf"));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, "logs"));
        config.put(new PropertySimple(SnapshotReport.PROP_DATA_DIRECTORY, "data"));
        config.put(new PropertySimple(SnapshotReport.PROP_DATA_REGEX, ".*\\.dat"));

        return config;
    }
}
