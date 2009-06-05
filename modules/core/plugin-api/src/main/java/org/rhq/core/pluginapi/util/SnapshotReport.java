/*
 * RHQ Management Platform
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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A common snapshot report that allows you to prepare a gzip'ed file containing
 * a snapshot of configuration and log files.
 * 
 * @author John Mazzitelli
 */
public class SnapshotReport {
    private static final Log log = LogFactory.getLog(SnapshotReport.class);

    /**
     * The relative directory under the report where the config files can be found. This
     * is the directory that will be found inside the zip file.
     */
    public static final String REPORT_CONFIG_DIRECTORY = "config";

    /**
     * The relative directory under the report where the log files can be found. This
     * is the directory that will be found inside the zip file.
     */
    public static final String REPORT_LOG_DIRECTORY = "log";

    /**
     * A boolean config property that dictates if a snapshot of config files should be in the report.
     */
    public static final String PROP_SNAPSHOT_CONFIG_FILES = "snapshotConfigEnabled";

    /**
     * A config property whose value is the directory relative to the base directory where the
     * configuration files are located.
     */
    public static final String PROP_CONFIG_DIRECTORY = "snapshotConfigDirectory";

    /**
     * A config property whose value is a regular expression that matches all config files
     * that are to be snapshotted. This regex must match files under the config directory.
     */
    public static final String PROP_CONFIG_REGEX = "snapshotConfigRegex";

    /**
     * A boolean config property that dictates if a snapshot of log files should be in the report.
     */
    public static final String PROP_SNAPSHOT_LOG_FILES = "snapshotLogEnabled";

    /**
     * A config property whose value is the directory relative to the base directory where the
     * log files are located.
     */
    public static final String PROP_LOG_DIRECTORY = "snapshotLogDirectory";

    /**
     * A config property whose value is a regular expression that matches all log files
     * that are to be snapshotted. This regex must match files under the log directory.
     */
    public static final String PROP_LOG_REGEX = "snapshotLogRegex";

    /**
     * A boolean config property that dictates if a snapshot of the additional files should be in the report.
     */
    public static final String PROP_SNAPSHOT_ADDITIONAL_FILES = "snapshotAdditionalFilesEnabled";

    /**
     * The property name for the list of of additional files.
     */
    public static final String PROP_ADDITIONAL_FILES_LIST = "snapshotAdditionalFilesList";

    /**
     * A config property whose value is a directory relative to the base directory where
     * additional files are located.
     * This should be a property inside a map where that map is a list item within a additional files list.
     */
    public static final String PROP_ADDITIONAL_FILES_DIRECTORY = "snapshotAdditionalFilesDirectory";

    /**
     * A config property whose value is a regular expression that matches all additional files
     * that are to be snapshotted. This regex must match files under its associated
     * additional files directory.
     * This should be a property inside a map where that map is a list item within a additional files list.
     */
    public static final String PROP_ADDITIONAL_FILES_REGEX = "snapshotAdditionalFilesRegex";

    /**
     * A config property whose value is the base directory where all config and logs files are under
     */
    public static final String PROP_BASE_DIRECTORY = "snapshotBaseDirectory";

    private final String name;
    private final String description;
    private final Configuration configuration;

    public SnapshotReport(String name, String description, Configuration config) {
        this.name = name;
        this.description = description;
        this.configuration = config;

    }

    public File generate() throws Exception {
        File outputFile = File.createTempFile(getName(), ".zip");
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zip = new ZipOutputStream(fos);
        try {
            // generate the file that will contain some general info about the snapshot report
            ZipEntry zipEntry = new ZipEntry("snapshot.properties");
            zip.putNextEntry(zipEntry);
            Properties properties = new Properties();
            properties.setProperty("name", getName());
            properties.setProperty("description", getDescription());
            properties.setProperty("epochmillis", Long.toString(System.currentTimeMillis()));
            properties.store(zip, null);

            // now store a snapshot of each file to the report
            Map<String, URL> allFiles = getAllFilesToSnapshot();
            for (Map.Entry<String, URL> snapshotFileEntry : allFiles.entrySet()) {
                zipEntry = new ZipEntry(snapshotFileEntry.getKey());
                zip.putNextEntry(zipEntry);
                InputStream input = snapshotFileEntry.getValue().openStream();
                try {
                    StreamUtil.copy(input, zip, false);
                } finally {
                    input.close();
                }
            }
        } finally {
            zip.close();
        }

        return outputFile;
    }

    protected String getName() {
        return this.name;
    }

    protected String getDescription() {
        return this.description;
    }

    protected Configuration getConfiguration() {
        return this.configuration;
    }

    protected Map<String, URL> getAllFilesToSnapshot() throws Exception {
        Map<String, URL> allFiles = new HashMap<String, URL>();

        Map<String, URL> configFiles = getConfigFilesToSnapshot();
        if (configFiles != null) {
            allFiles.putAll(configFiles);
        }

        Map<String, URL> logFiles = getLogFilesToSnapshot();
        if (logFiles != null) {
            allFiles.putAll(logFiles);
        }

        Map<String, URL> additionalFiles = getAdditionalFilesToSnapshot();
        if (additionalFiles != null) {
            allFiles.putAll(additionalFiles);
        }

        return allFiles;
    }

    protected Map<String, URL> getConfigFilesToSnapshot() throws Exception {
        Configuration config = getConfiguration();
        if (!"true".equals(config.getSimpleValue(PROP_SNAPSHOT_CONFIG_FILES, "false"))) {
            return null; // config files are not to be snapshotted into the report, abort
        }

        String baseDir = config.getSimpleValue(PROP_BASE_DIRECTORY, null);
        String confDir = config.getSimpleValue(PROP_CONFIG_DIRECTORY, "conf");
        String confRegex = config.getSimpleValue(PROP_CONFIG_REGEX, null);

        Map<String, URL> filesMap = null;

        File confDirFile = new File(baseDir, confDir);
        File[] confFiles = confDirFile.listFiles(new RegexFilenameFilter(confRegex));
        if (confFiles != null) {
            filesMap = new HashMap<String, URL>(confFiles.length);
            for (File confFile : confFiles) {
                filesMap.put(REPORT_CONFIG_DIRECTORY + '/' + confFile.getName(), confFile.toURI().toURL());
            }
        } else {
            // the directory probably doesn't exist, this may not be fatal so just log and keep going
            log.warn("Failed to get list of conf files from [" + confDirFile + "]");
        }

        return filesMap;
    }

    protected Map<String, URL> getLogFilesToSnapshot() throws Exception {
        Configuration config = getConfiguration();
        if (!"true".equals(config.getSimpleValue(PROP_SNAPSHOT_LOG_FILES, "false"))) {
            return null; // log files are not to be snapshotted into the report, abort
        }

        String baseDir = config.getSimpleValue(PROP_BASE_DIRECTORY, null);
        String logDir = config.getSimpleValue(PROP_LOG_DIRECTORY, "logs");
        String logRegex = config.getSimpleValue(PROP_LOG_REGEX, null);

        Map<String, URL> filesMap = null;

        File logDirFile = new File(baseDir, logDir);
        File[] logFiles = logDirFile.listFiles(new RegexFilenameFilter(logRegex));
        if (logFiles != null) {
            filesMap = new HashMap<String, URL>(logFiles.length);
            for (File logFile : logFiles) {
                filesMap.put(REPORT_LOG_DIRECTORY + '/' + logFile.getName(), logFile.toURI().toURL());
            }
        } else {
            // the directory probably doesn't exist, this may not be fatal so just log and keep going
            log.warn("Failed to get list of log files from [" + logDirFile + "]");
        }

        return filesMap;
    }

    protected Map<String, URL> getAdditionalFilesToSnapshot() throws Exception {
        Configuration config = getConfiguration();
        if (!"true".equals(config.getSimpleValue(PROP_SNAPSHOT_ADDITIONAL_FILES, "false"))) {
            return null; // any additional files are not to be snapshotted into the report, abort
        }

        String baseDir = config.getSimpleValue(PROP_BASE_DIRECTORY, null);
        PropertyList additionalList = config.getList(PROP_ADDITIONAL_FILES_LIST);
        if (additionalList == null || additionalList.getList() == null) {
            return null; // there are no additional files defined, just skip it
        }

        Map<String, URL> filesMap = new HashMap<String, URL>();

        for (Property property : additionalList.getList()) {
            PropertyMap additionalFileMap = (PropertyMap) property; // must be a map, let it throw exception if not
            String additionalFilesDir = additionalFileMap.getSimpleValue(PROP_ADDITIONAL_FILES_DIRECTORY, "");
            String additionalFilesRegex = additionalFileMap.getSimpleValue(PROP_ADDITIONAL_FILES_REGEX, null);

            // it is possible that the user wanted to just disable one of the additional files
            String additionalFilesEnabled = additionalFileMap.getSimpleValue(PROP_SNAPSHOT_ADDITIONAL_FILES, "true");
            if (!"true".equals(additionalFilesEnabled)) {
                continue;
            }

            File additionalFilesDirFile = new File(baseDir, additionalFilesDir);
            File[] additionalFiles = additionalFilesDirFile.listFiles(new RegexFilenameFilter(additionalFilesRegex));
            if (additionalFiles != null) {
                for (File additionalFile : additionalFiles) {
                    filesMap.put(additionalFilesDir + '/' + additionalFile.getName(), additionalFile.toURI().toURL());
                }
            } else {
                // the directory probably doesn't exist, this may not be fatal so just log and keep going
                log.warn("Failed to get list of additional files from [" + additionalFilesDirFile + "]");
            }
        }

        return filesMap;
    }

    /**
     * Filename filter that matches the filename if it matches a given regular expression.
     * If the given regular expression is <code>null</code>, this filter matches everything.
     */
    protected static class RegexFilenameFilter implements FilenameFilter {
        private String regex;

        public RegexFilenameFilter(String regex) {
            this.regex = regex;
        }

        public boolean accept(File dir, String name) {
            return this.regex == null || name.matches(this.regex);
        }
    }
}
