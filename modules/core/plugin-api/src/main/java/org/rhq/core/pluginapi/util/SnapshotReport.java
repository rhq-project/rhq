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
 * A common snapshot report that allows you to prepare a zip'ed file containing
 * a snapshot of content belonging to a resource. This report contains a snapshot
 * configuration, log and data files along with a customizable set of additional files.
 * Uses of this object can pick and choose which types of files to snapshot and filter
 * the files of those types.
 * 
 * This class can be subclasses if you wish to alter the way it collects and stores
 * the snapshot content. For example, if the configuration is not stored as files, but instead
 * stored in memory, you can subclass this to obtain the configuration by reading memory, writing
 * that data to a temporary file and then having this class add that temporary file to the
 * snapshot zip file.
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
     * The relative directory under the report where the data files can be found. This
     * is the directory that will be found inside the zip file.
     */
    public static final String REPORT_DATA_DIRECTORY = "data";

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
     * A config property whose value is the boolean flag to indicate if
     * the snapshot should include files found in subdirectories of the config directory.
     */
    public static final String PROP_CONFIG_RECURSIVE = "snapshotConfigRecursive";

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
     * A config property whose value is the boolean flag to indicate if
     * the snapshot should include files found in subdirectories of the log directory.
     */
    public static final String PROP_LOG_RECURSIVE = "snapshotLogRecursive";

    /**
     * A boolean config property that dictates if a snapshot of data files should be in the report.
     */
    public static final String PROP_SNAPSHOT_DATA_FILES = "snapshotDataEnabled";

    /**
     * A config property whose value is the directory relative to the base directory where the
     * data files are located.
     */
    public static final String PROP_DATA_DIRECTORY = "snapshotDataDirectory";

    /**
     * A config property whose value is a regular expression that matches all data files
     * that are to be snapshotted. This regex must match files under the data directory.
     */
    public static final String PROP_DATA_REGEX = "snapshotDataRegex";

    /**
     * A config property whose value is the boolean flag to indicate if
     * the snapshot should include files found in subdirectories of the data directory.
     */
    public static final String PROP_DATA_RECURSIVE = "snapshotDataRecursive";

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
     * A config property whose value is the boolean flag to indicate if
     * the snapshot should include files found in subdirectories of the additional files directory.
     */
    public static final String PROP_ADDITIONAL_FILES_RECURSIVE = "snapshotAdditionalFilesRecursive";

    /**
     * A config property whose value is the base directory where all config, logs and data files are under.
     * If a config, log or data directory was already specified as an absolute directory, this base directory
     * will not be used. Only when a config, log or data directory is relative will it be assumed to be under
     * the base directory.
     */
    public static final String PROP_BASE_DIRECTORY = "snapshotBaseDirectory";

    /**
     * Optional property that can be specified to define where to store the output snapshot report.
     * If not specified, the platform's tmp directory will be used.
     */
    public static final String PROP_REPORT_OUTPUT_DIRECTORY = "snapshotReportOutputDirectory";

    private final String name;
    private final String description;
    private final Configuration configuration;

    public SnapshotReport(String name, String description, Configuration config) {
        this.name = name;
        this.description = description;
        this.configuration = config;
    }

    public File generate() throws Exception {
        File outputFile = getSnapshotReportFile();

        if (log.isDebugEnabled()) {
            log.debug("Generating snapshot [" + outputFile + "]");
        }

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

        if (log.isDebugEnabled()) {
            log.debug("Generated snapshot [" + outputFile + "] of size [" + outputFile.length() + "]");
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

    /**
     * This only returns the file where the snapshot will be stored. This method does NOT
     * generate the actual snapshot report. Call {@link #generate()} to get the full snapshot content.
     * 
     * @return the file where the snapshot report will be stored when it is generated
     *
     * @throws Exception if the file could not be determined for some reason
     */
    protected File getSnapshotReportFile() throws Exception {
        String dirString = getConfiguration().getSimpleValue(PROP_REPORT_OUTPUT_DIRECTORY, null);
        File dir = null;
        if (dirString != null) {
            dir = new File(dirString);
            dir.mkdirs();
        }
        File outputFile = File.createTempFile(getName(), ".zip", dir);
        return outputFile;
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

        Map<String, URL> dataFiles = getDataFilesToSnapshot();
        if (dataFiles != null) {
            allFiles.putAll(dataFiles);
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
        String recursive = config.getSimpleValue(PROP_CONFIG_RECURSIVE, "false");

        Map<String, URL> filesMap = null;

        File confDirFile = new File(confDir);
        if (!confDirFile.isAbsolute()) {
            confDirFile = new File(baseDir, confDir);
        }

        RegexFilenameFilter filter = new RegexFilenameFilter(confRegex, recursive);
        File[] confFiles = confDirFile.listFiles(filter);
        if (confFiles != null) {
            filesMap = new HashMap<String, URL>(confFiles.length);
            populateFilesMap(confFiles, filesMap, REPORT_CONFIG_DIRECTORY, filter);
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
        String recursive = config.getSimpleValue(PROP_LOG_RECURSIVE, "false");

        Map<String, URL> filesMap = null;

        File logDirFile = new File(logDir);
        if (!logDirFile.isAbsolute()) {
            logDirFile = new File(baseDir, logDir);
        }

        RegexFilenameFilter filter = new RegexFilenameFilter(logRegex, recursive);
        File[] logFiles = logDirFile.listFiles(filter);
        if (logFiles != null) {
            filesMap = new HashMap<String, URL>(logFiles.length);
            populateFilesMap(logFiles, filesMap, REPORT_LOG_DIRECTORY, filter);
        } else {
            // the directory probably doesn't exist, this may not be fatal so just log and keep going
            log.warn("Failed to get list of log files from [" + logDirFile + "]");
        }

        return filesMap;
    }

    protected Map<String, URL> getDataFilesToSnapshot() throws Exception {
        Configuration config = getConfiguration();
        if (!"true".equals(config.getSimpleValue(PROP_SNAPSHOT_DATA_FILES, "false"))) {
            return null; // data files are not to be snapshotted into the report, abort
        }

        String baseDir = config.getSimpleValue(PROP_BASE_DIRECTORY, null);
        String dataDir = config.getSimpleValue(PROP_DATA_DIRECTORY, "data");
        String dataRegex = config.getSimpleValue(PROP_DATA_REGEX, null);
        String recursive = config.getSimpleValue(PROP_DATA_RECURSIVE, "false");

        Map<String, URL> filesMap = null;

        File dataDirFile = new File(dataDir);
        if (!dataDirFile.isAbsolute()) {
            dataDirFile = new File(baseDir, dataDir);
        }

        RegexFilenameFilter filter = new RegexFilenameFilter(dataRegex, recursive);
        File[] dataFiles = dataDirFile.listFiles(filter);
        if (dataFiles != null) {
            filesMap = new HashMap<String, URL>(dataFiles.length);
            populateFilesMap(dataFiles, filesMap, REPORT_DATA_DIRECTORY, filter);
        } else {
            // the directory probably doesn't exist, this may not be fatal so just log and keep going
            log.warn("Failed to get list of data files from [" + dataDirFile + "]");
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

            File additionalFilesDirFile = new File(additionalFilesDir);
            if (!additionalFilesDirFile.isAbsolute()) {
                additionalFilesDirFile = new File(baseDir, additionalFilesDir);
            }

            String recursive = additionalFileMap.getSimpleValue(PROP_ADDITIONAL_FILES_RECURSIVE, "false");
            RegexFilenameFilter filter = new RegexFilenameFilter(additionalFilesRegex, recursive);
            File[] additionalFiles = additionalFilesDirFile.listFiles(filter);
            if (additionalFiles != null) {
                populateFilesMap(additionalFiles, filesMap, additionalFilesDir, filter);
            } else {
                // the directory probably doesn't exist, this may not be fatal so just log and keep going
                log.warn("Failed to get list of additional files from [" + additionalFilesDirFile + "]");
            }
        }

        return filesMap;
    }

    private void populateFilesMap(File[] filesDirectories, Map<String, URL> filesMap, String directoryPrefix,
        RegexFilenameFilter filter) throws Exception {

        for (File fileDirectory : filesDirectories) {
            if (fileDirectory.isDirectory()) {
                File[] subDirectoryFiles = fileDirectory.listFiles(filter);
                if (subDirectoryFiles != null) {
                    populateFilesMap(subDirectoryFiles, filesMap, directoryPrefix + "/" + fileDirectory.getName(),
                        filter);
                } else {
                    log.warn("Failed to get subdirectory files for [" + fileDirectory + "]");
                }
            } else {
                filesMap.put(directoryPrefix + '/' + fileDirectory.getName(), fileDirectory.toURI().toURL());
            }
        }

        return;
    }

    /**
     * Filename filter that matches the filename if it matches a given regular expression.
     * If the given regular expression is <code>null</code>, this filter matches everything.
     */
    protected static class RegexFilenameFilter implements FilenameFilter {
        private String regex;
        private boolean recursive;

        public RegexFilenameFilter(String regex, String recursive) {
            this.regex = regex;
            this.recursive = Boolean.parseBoolean(recursive);
        }

        public boolean accept(File dir, String name) {
            if (new File(dir, name).isDirectory()) {
                return recursive;
            }
            return this.regex == null || name.matches(this.regex);
        }
    }
}
