/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas.util;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.util.SnapshotReport;

/**
 * Builds a SnapshotReport with the proper directory locations for things.
 * 
 * @author John Mazzitelli
 */
public class JBossASSnapshotReport extends SnapshotReport {
    public JBossASSnapshotReport(String name, String description, Configuration pluginConfig, String configPath, String tmpDir) {
        super(name, description, buildConfiguration(pluginConfig, configPath, tmpDir));
    }

    private static Configuration buildConfiguration(Configuration pluginConfiguration, String configPath, String tmpDir) {

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_REPORT_OUTPUT_DIRECTORY, tmpDir));
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, configPath));
        
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, "true")));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_LOG_FILES, "true")));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_DATA_FILES, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_SNAPSHOT_DATA_FILES, "false")));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_CONFIG_DIRECTORY, "conf")));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_LOG_DIRECTORY, "log")));
        config.put(new PropertySimple(SnapshotReport.PROP_DATA_DIRECTORY, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_DATA_DIRECTORY, "data")));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_REGEX, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_CONFIG_REGEX, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_REGEX, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_LOG_REGEX, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_DATA_REGEX, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_DATA_REGEX, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_RECURSIVE, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_CONFIG_RECURSIVE, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_RECURSIVE, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_LOG_RECURSIVE, null)));
        config.put(new PropertySimple(SnapshotReport.PROP_DATA_RECURSIVE, pluginConfiguration.getSimpleValue(
            SnapshotReport.PROP_DATA_RECURSIVE, null)));
        
        // our list-o-maps have the same names in the plugin descriptor as required
        // by SnapshotReport object so we can just make a shallow copy. 
        PropertyList additionaFilesList = pluginConfiguration.getList(PROP_ADDITIONAL_FILES_LIST);
        if (additionaFilesList != null) {
            // a list is defined, so as a whole, we enable the ability to snapshot additional files globally.
            // each map in the list has the ability to individual enable/disable themselves if they want
           config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_ADDITIONAL_FILES, "true"));
           config.put(additionaFilesList);
        }

        return config;
    }
}
