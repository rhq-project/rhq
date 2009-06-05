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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

@Test
public class SnapshotReportTest {
    private final File baseDir = new File(System.getProperty("java.io.tmpdir"), "test-snapshot");
    private final File configDir = new File(baseDir, "configdir");
    private final File logDir = new File(baseDir, "logdir");

    @BeforeTest
    public void prepareTest() {
        configDir.mkdirs();
        logDir.mkdirs();
    }

    @AfterTest
    public void cleanupTest() {
        File[] files = configDir.listFiles();
        if (files != null) {
            for (File doomed : files) {
                doomed.delete();
            }
        }
        configDir.delete();

        files = logDir.listFiles();
        if (files != null) {
            for (File doomed : files) {
                doomed.delete();
            }
        }
        logDir.delete();

        baseDir.delete();
    }

    public void testSnapshotReportNoRegex() throws Exception {
        writeFile(configDir, "one.config", "config 1 file");
        writeFile(configDir, "two.config", "config 2 file");
        writeFile(logDir, "first.log", "log 1 file");
        writeFile(logDir, "second.log", "log 2 file");
        writeFile(configDir, "not.a.config.file.txt", "this is not a config file");
        writeFile(logDir, "not.a.log.file.txt", "this is not a log file");

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, baseDir.getAbsolutePath()));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, "true"));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, "true"));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, configDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, logDir.getName())); // relative path
        //config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_REGEX, ".*\\.config"));
        //config.put(new PropertySimple(SnapshotReport.PROP_LOG_REGEX, ".*\\.log"));

        SnapshotReport report = new SnapshotReport("test-snapshot", "some desc", config);
        File snapshot = report.generate();
        try {
            FileInputStream fis = new FileInputStream(snapshot);
            ZipInputStream zip = new ZipInputStream(fis);
            ZipEntry zipEntry;
            Set<String> entryNames = new HashSet<String>();
            try {
                for (zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip.getNextEntry()) {
                    entryNames.add(zipEntry.getName());
                }
            } finally {
                zip.close();
            }

            // there should be the following files in the snapshot report:
            //   snapshot.properties
            //   config/one.config
            //   config/two.config
            //   config/not.a.config.file.txt (this should be here because there was no regex filtering)
            //   log/first.log
            //   log/second.log
            //   log/not.a.log.file.txt (this should be here because there was no regex filtering)
            assert entryNames.size() == 7 : entryNames;
            assert entryNames.contains("snapshot.properties") : entryNames;
            assert entryNames.contains("config/one.config") : entryNames;
            assert entryNames.contains("config/two.config") : entryNames;
            assert entryNames.contains("config/not.a.config.file.txt") : entryNames;
            assert entryNames.contains("log/first.log") : entryNames;
            assert entryNames.contains("log/second.log") : entryNames;
            assert entryNames.contains("log/not.a.log.file.txt") : entryNames;
        } finally {
            snapshot.delete();
        }
    }

    public void testSnapshotReportWithRegex() throws Exception {
        writeFile(configDir, "one.config", "config 1 file");
        writeFile(configDir, "two.config", "config 2 file");
        writeFile(logDir, "first.log", "log 1 file");
        writeFile(logDir, "second.log", "log 2 file");
        writeFile(configDir, "not.a.config.file.txt", "this is not a config file");
        writeFile(logDir, "not.a.log.file.txt", "this is not a log file");

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, baseDir.getAbsolutePath()));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, "true"));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, "true"));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, configDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, logDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_REGEX, ".*\\.config"));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_REGEX, ".*\\.log"));

        SnapshotReport report = new SnapshotReport("test-snapshot", "some desc", config);
        File snapshot = report.generate();
        try {
            FileInputStream fis = new FileInputStream(snapshot);
            ZipInputStream zip = new ZipInputStream(fis);
            ZipEntry zipEntry;
            Set<String> entryNames = new HashSet<String>();
            try {
                for (zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip.getNextEntry()) {
                    entryNames.add(zipEntry.getName());
                }
            } finally {
                zip.close();
            }

            // there should be the following files in the snapshot report:
            //   snapshot.properties
            //   config/one.config
            //   config/two.config
            //   log/first.log
            //   log/second.log
            assert entryNames.size() == 5 : entryNames;
            assert entryNames.contains("snapshot.properties") : entryNames;
            assert entryNames.contains("config/one.config") : entryNames;
            assert entryNames.contains("config/two.config") : entryNames;
            assert entryNames.contains("log/first.log") : entryNames;
            assert entryNames.contains("log/second.log") : entryNames;
        } finally {
            snapshot.delete();
        }
    }

    public void testSnapshotReportDisableConfig() throws Exception {
        writeFile(configDir, "one.config", "config 1 file");
        writeFile(configDir, "two.config", "config 2 file");
        writeFile(logDir, "first.log", "log 1 file");
        writeFile(logDir, "second.log", "log 2 file");
        writeFile(configDir, "not.a.config.file.txt", "this is not a config file");
        writeFile(logDir, "not.a.log.file.txt", "this is not a log file");

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, baseDir.getAbsolutePath()));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, "false"));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, "true"));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, configDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, logDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_REGEX, ".*\\.config"));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_REGEX, ".*\\.log"));

        SnapshotReport report = new SnapshotReport("test-snapshot", "some desc", config);
        File snapshot = report.generate();
        try {
            FileInputStream fis = new FileInputStream(snapshot);
            ZipInputStream zip = new ZipInputStream(fis);
            ZipEntry zipEntry;
            Set<String> entryNames = new HashSet<String>();
            try {
                for (zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip.getNextEntry()) {
                    entryNames.add(zipEntry.getName());
                }
            } finally {
                zip.close();
            }

            // there should be the following files in the snapshot report:
            //   snapshot.properties
            //   log/first.log
            //   log/second.log
            assert entryNames.size() == 3 : entryNames;
            assert entryNames.contains("snapshot.properties") : entryNames;
            assert entryNames.contains("log/first.log") : entryNames;
            assert entryNames.contains("log/second.log") : entryNames;
        } finally {
            snapshot.delete();
        }
    }

    public void testSnapshotReportDisableAll() throws Exception {
        writeFile(configDir, "one.config", "config 1 file");
        writeFile(configDir, "two.config", "config 2 file");
        writeFile(logDir, "first.log", "log 1 file");
        writeFile(logDir, "second.log", "log 2 file");
        writeFile(configDir, "not.a.config.file.txt", "this is not a config file");
        writeFile(logDir, "not.a.log.file.txt", "this is not a log file");

        Configuration config = new Configuration();
        config.put(new PropertySimple(SnapshotReport.PROP_BASE_DIRECTORY, baseDir.getAbsolutePath()));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_CONFIG_FILES, "false"));
        config.put(new PropertySimple(SnapshotReport.PROP_SNAPSHOT_LOG_FILES, "false"));
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_DIRECTORY, configDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_DIRECTORY, logDir.getName())); // relative path
        config.put(new PropertySimple(SnapshotReport.PROP_CONFIG_REGEX, ".*\\.config"));
        config.put(new PropertySimple(SnapshotReport.PROP_LOG_REGEX, ".*\\.log"));

        SnapshotReport report = new SnapshotReport("test-snapshot", "some desc", config);
        File snapshot = report.generate();
        try {
            FileInputStream fis = new FileInputStream(snapshot);
            ZipInputStream zip = new ZipInputStream(fis);
            ZipEntry zipEntry;
            Set<String> entryNames = new HashSet<String>();
            try {
                for (zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip.getNextEntry()) {
                    entryNames.add(zipEntry.getName());
                }
            } finally {
                zip.close();
            }

            // there should be the following files in the snapshot report:
            //   snapshot.properties
            assert entryNames.size() == 1 : entryNames;
            assert entryNames.contains("snapshot.properties") : entryNames;
        } finally {
            snapshot.delete();
        }
    }

    private void writeFile(File dir, String filename, String content) throws Exception {
        File newFile = new File(dir, filename);
        FileOutputStream fos = new FileOutputStream(newFile);
        try {
            fos.write(content.getBytes());
        } finally {
            fos.close();
        }
    }
}
