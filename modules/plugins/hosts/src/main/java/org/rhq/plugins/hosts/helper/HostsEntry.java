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
