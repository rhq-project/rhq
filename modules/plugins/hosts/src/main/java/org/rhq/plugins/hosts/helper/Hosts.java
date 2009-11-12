/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

/**
 * A set of hosts file entries.
 *
 * @author Ian Springer
 */
public class Hosts {
    private static final Log LOG = LogFactory.getLog(Hosts.class);

    private Set<HostsEntry> entries = new LinkedHashSet<HostsEntry>();
    private Map<String, Set<HostsEntry>> nameToIpv4EntriesMap = new HashMap<String, Set<HostsEntry>>();
    private Map<String, Set<HostsEntry>> nameToIpv6EntriesMap = new HashMap<String, Set<HostsEntry>>();

    public Hosts() {
    }

    public void addEntry(HostsEntry entry) {
        this.entries.add(entry);
        String canonicalName = entry.getCanonicalName();
        updateNameToEntriesMaps(entry, canonicalName);
    }

    private void updateNameToEntriesMaps(HostsEntry entry, String canonicalName) {
        Map<String, Set<HostsEntry>> nameToEntriesMap = (entry.getIpVersion() == 4) ?
                this.nameToIpv4EntriesMap : this.nameToIpv6EntriesMap;
        Set<HostsEntry> entriesForCanonicalName = nameToEntriesMap.get(canonicalName);
        if (entriesForCanonicalName == null) {
            entriesForCanonicalName = new LinkedHashSet(1);
            nameToEntriesMap.put(canonicalName, entriesForCanonicalName);
        }
        entriesForCanonicalName.add(entry);
    }

    public Set<HostsEntry> getEntries() {
        return this.entries;
    }

    public Set<HostsEntry> getIpv4EntriesByName(String canonicalName) {
        Set<HostsEntry> entries = this.nameToIpv4EntriesMap.get(canonicalName);
        return (entries != null) ? entries : Collections.<HostsEntry> emptySet();
    }

    public Set<HostsEntry> getIpv6EntriesByName(String canonicalName) {
        Set<HostsEntry> entries = this.nameToIpv6EntriesMap.get(canonicalName);
        return (entries != null) ? entries : Collections.<HostsEntry> emptySet();
    }

    public static Hosts load(File hostsFile) throws IOException {
        Hosts hosts = new Hosts();
        SimpleUnixConfigFileReader reader = new SimpleUnixConfigFileReader(new FileReader(hostsFile));
        try {
            SimpleUnixConfigFileLine line;
            while ((line = reader.readLine()) != null) {
                String nonComment = line.getNonComment();
                HostsEntry entry = parseEntry(nonComment, hostsFile);
                if (entry != null) {
                    hosts.addEntry(entry);
                }
            }
        } finally {
            reader.close();
        }

        return hosts;
    }

    public static void store(Hosts hosts, File hostsFile) throws IOException {
        if (!hostsFile.exists()) {
            // If a hosts file doesn't already exist, create an empty one.
            FileOutputStream hostsFileWriter = new FileOutputStream(hostsFile);
            hostsFileWriter.close();
        }

        Set<String> storedIpv4CanonicalNames = new HashSet<String>();
        Set<String> storedIpv6CanonicalNames = new HashSet<String>();
        File newHostsFile = new File(hostsFile.getPath() + "-" + System.currentTimeMillis());
        PrintWriter newHostsFileWriter = new PrintWriter(new FileWriter(newHostsFile));
        try {
            SimpleUnixConfigFileLine line;
            SimpleUnixConfigFileReader hostsFileReader = new SimpleUnixConfigFileReader(new FileReader(hostsFile));
            try {
                while ((line = hostsFileReader.readLine()) != null) {
                    StringBuilder newLine = createNewLine(hosts, hostsFile, line, storedIpv4CanonicalNames,
                            storedIpv6CanonicalNames);
                    if (newLine != null) {
                        newHostsFileWriter.println(newLine);
                    }
                }
            } finally {
                hostsFileReader.close();
            }

            // Write out any entries for IP addresses that did not have entries in the original file.
            for (HostsEntry newEntry : hosts.getEntries()) {
                Set<String> storedCanonicalNames = (newEntry.getIpVersion() == 4) ? storedIpv4CanonicalNames :
                        storedIpv6CanonicalNames;
                if (!storedCanonicalNames.contains(newEntry.getCanonicalName())) {
                    StringBuilder newLine = new StringBuilder(newEntry.getIpAddress()).append("\t").append(
                        newEntry.getCanonicalName());
                    for (String alias : newEntry.getAliases()) {
                        newLine.append("\t").append(alias);
                    }
                    newHostsFileWriter.println(newLine);
                }
            }
        } finally {
            newHostsFileWriter.close();
        }

        // Backup the original hosts file (e.g. /etc/hosts) to hosts~ (e.g. /etc/hosts~).
        File backupHostsFile = new File(hostsFile.getPath() + "~");
        if (backupHostsFile.exists()) {
            // renameTo() will fail on Windows if the destination file already exists. Lame, eh?
            boolean deleteSucceeded = backupHostsFile.delete();
        }
        boolean backupSucceeded = hostsFile.renameTo(backupHostsFile);
        if (!backupSucceeded) {
            throw new IOException("Failed to backup original hosts file [" + hostsFile + "] to [" + backupHostsFile
                + "].");
        }

        // And finally, rename the new hosts file (e.g. /etc/hosts-1234567890) to the actual hosts file (e.g. /etc/hosts).
        if (hostsFile.exists()) {
            // renameTo() will fail on Windows if the destination file already exists. Lame, eh?
            boolean deleteSucceeded = hostsFile.delete();
        }
        boolean updateSucceeded = newHostsFile.renameTo(hostsFile);
        if (!updateSucceeded) {
            throw new IOException("Failed to rename updated hosts file [" + newHostsFile + "] to [" + hostsFile + "].");
        }

        return;
    }

    private static HostsEntry parseEntry(String string, File hostsFile) {
        if (string == null) {
            // comment-only line
            return null;
        }

        String trimmedString = string.trim();
        if (trimmedString.equals("")) {
            // comment-only line
            return null;
        }

        String[] tokens = trimmedString.split("\\s+");
        if (tokens.length == 0) {
            // comment-only line
            return null;
        }

        String ipAddress = tokens[0];
        if (tokens.length == 1) {
            LOG.warn("Hosts file [" + hostsFile + "] contains invalid entry for IP address " + ipAddress
                + " - no canonical name is specified.");
        }

        String canonicalName = null;
        Set<String> aliasSet = null;
        if (tokens.length >= 2) {
            canonicalName = tokens[1];
            if (tokens.length >= 3) {
                aliasSet = new LinkedHashSet<String>(tokens.length - 2);
                for (int i = 2; i < tokens.length; i++) {
                    aliasSet.add(tokens[i]);
                }
            }
        }

        return new HostsEntry(ipAddress, canonicalName, aliasSet);
    }

    @Nullable
    private static StringBuilder createNewLine(Hosts hosts, File hostsFile, SimpleUnixConfigFileLine existingLine,
                                               Set<String> storedIpv4CanonicalNames,
                                               Set<String> storedIpv6CanonicalNames) {
        StringBuilder newLine;
        String nonComment = existingLine.getNonComment();
        String comment = existingLine.getComment();
        HostsEntry existingEntry = parseEntry(nonComment, hostsFile);
        if (existingEntry != null) {
            Set<HostsEntry> newEntries = (existingEntry.getIpVersion() == 4) ?
                    hosts.getIpv4EntriesByName(existingEntry.getCanonicalName()) :
                    hosts.getIpv6EntriesByName(existingEntry.getCanonicalName());
            // TODO: Look up entries for existing entry's aliases too?
            if (!newEntries.isEmpty()) {
                // replace the existing entry
                HostsEntry newEntry = newEntries.iterator().next();
                LOG.debug("Replacing existing entry in hosts file [" + existingEntry + "] with new entry [" + newEntry
                    + "]...");
                newLine = new StringBuilder(newEntry.getIpAddress()).append("\t").append(newEntry.getCanonicalName());
                for (String alias : newEntry.getAliases()) {
                    newLine.append("\t").append(alias);
                }
                // but still write out the existing comment
                if (comment != null) {
                    newLine.append(" #").append(comment);
                }
                Set<String> storedCanonicalNames = (newEntry.getIpVersion() == 4) ? storedIpv4CanonicalNames :
                        storedIpv6CanonicalNames;
                storedCanonicalNames.add(newEntry.getCanonicalName());
            } else {
                LOG.debug("Removing entry [" + existingEntry + "] from hosts file...");
                newLine = null;
            }
        } else {
            // comment-only line - write it back out exactly as is
            newLine = new StringBuilder();

            if (nonComment != null) {
                newLine.append(nonComment);
            }
            if (comment != null) {
                newLine.append('#').append(comment);
            }
        }
        return newLine;
    }
}
