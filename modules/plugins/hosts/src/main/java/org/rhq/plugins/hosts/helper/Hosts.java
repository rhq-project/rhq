/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.hosts.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ian Springer
 */
public class Hosts {
    private static final Log LOG = LogFactory.getLog(Hosts.class);

    private static final Pattern ENTRY_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(.*)");

    private Map<String, HostsEntry> entries = new HashMap<String, HostsEntry>();

    public Hosts() {
    }

    public void addEntry(HostsEntry hostsEntry) {
        this.entries.put(hostsEntry.getIpAddress(), hostsEntry);
    }

    public Map<String, HostsEntry> getEntries() {
        return this.entries;
    }

    public static Hosts load(File hostsFile) throws IOException {
        Hosts hosts = new Hosts();
        SimpleUnixConfigFileReader reader = new SimpleUnixConfigFileReader(new FileReader(hostsFile));
        SimpleUnixConfigFileLine line;
        while ((line = reader.readLine()) != null) {
            String entry = line.getNonComment();
            Matcher matcher = ENTRY_PATTERN.matcher(entry);
            if (matcher.matches()) {
                String ipAddress = matcher.group(0);
                String canonicalName = matcher.group(1);
                String aliases = matcher.group(2);
                Set<String> aliasSet = new HashSet<String>();
                if (aliases != null) {
                    String[] aliasArray = aliases.split("\\s+");
                    aliasSet.addAll(Arrays.asList(aliasArray));
                }
                HostsEntry hostsEntry = new HostsEntry(ipAddress, canonicalName, aliasSet);
                hosts.addEntry(hostsEntry);
            }
        }
        return hosts;
    }

    public static void store(Hosts hosts, File hostsFile) throws IOException {
        SimpleUnixConfigFileReader reader = new SimpleUnixConfigFileReader(new FileReader(hostsFile));
        PrintWriter writer = new PrintWriter(new FileWriter(hostsFile));
        SimpleUnixConfigFileLine line;
        while ((line = reader.readLine()) != null) {
            StringBuilder newLine = createNewLine(hosts, hostsFile, line);
            if (newLine != null) {                
                writer.println(newLine);
            }
        }
        return;
    }

    @Nullable
    private static StringBuilder createNewLine(Hosts hosts, File hostsFile, SimpleUnixConfigFileLine existingLine) {
        StringBuilder newLine;
        String nonComment = existingLine.getNonComment();
        String trimmedNonComment = (nonComment == null) ? null : nonComment.trim();
        String comment = existingLine.getComment();
        if (trimmedNonComment != null && !trimmedNonComment.equals("")) {
            Matcher matcher = ENTRY_PATTERN.matcher(trimmedNonComment);
            if (matcher.matches()) {
                String ipAddress = matcher.group(0);
                String canonicalName = matcher.group(1);
                String aliases = matcher.group(2);
                String[] aliasArray = aliases.split("\\s+");
                Set<String> aliasSet = new HashSet<String>(Arrays.asList(aliasArray));
                HostsEntry existingEntry = new HostsEntry(ipAddress, canonicalName, aliasSet);
                if (hosts.getEntries().containsKey(ipAddress)) {
                    // replace the existing entry
                    HostsEntry newEntry = hosts.getEntries().get(ipAddress);
                    LOG.debug("Replacing existing entry in hosts file [" + existingEntry + "] with new entry ["
                            + newEntry + "]...");
                    newLine = new StringBuilder(newEntry.getIpAddress()).append("\t").append(newEntry.getCanonicalName());
                    for (String alias : newEntry.getAliases()) {
                        newLine.append("\t").append(alias);
                    }
                    // but still write out the existing comment
                    if (comment != null) {
                       newLine.append(" #").append(comment);
                    }
                } else {
                    LOG.debug("Removing entry [" + existingEntry + "] from hosts file...");
                    newLine = null;
                }
            } else {
                // invalid entry - log a warning but write it out as is
                LOG.warn("Hosts file [" + hostsFile + "] contains invalid entry [" + trimmedNonComment + "].");
                newLine = new StringBuilder(nonComment).append(" #").append(comment);
            }
        } else {
            newLine = new StringBuilder();
            // comment-only line - write it back out as is
            if (nonComment != null) {
                newLine.append(nonComment);
                if (comment != null) {
                    newLine.append(' ');
                }
            }
            if (comment != null) {
                newLine.append('#').append(comment);
            }
            newLine = new StringBuilder(nonComment).append("");
        }
        return newLine;
    }
}
