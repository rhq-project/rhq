/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.plugins.hosts.helper;

import java.util.Collections;
import java.util.Set;

/**
 * @author Ian Springer
 */
public class HostsEntry {
    private String ipAddress;
    private String canonicalName;
    private Set<String> aliases;

    public HostsEntry(String ipAddress, String canonicalName, Set<String> aliases) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress parameter is null.");
        }
        this.ipAddress = ipAddress;
        if (canonicalName == null) {
            throw new IllegalArgumentException("canonicalName parameter is null.");
        }
        this.canonicalName = canonicalName;
        this.aliases = (aliases != null) ? aliases : Collections.<String>emptySet();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public Set<String> getAliases() {
        return aliases;
    }
}
