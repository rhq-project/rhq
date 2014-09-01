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

package org.rhq.plugins.apache.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the logic to check whether any of the provided files has changed.
 * 
 * @author Lukas Krejci
 */
public class ConfigurationTimestamp {

    private Map<String, Long> lastModifiedTimes;

    public ConfigurationTimestamp() {
        lastModifiedTimes = new HashMap<String, Long>();
    }

    @Deprecated
    public ConfigurationTimestamp(List<File> files) {
        this((Iterable<File>) files);
    }

    public ConfigurationTimestamp(Iterable<File> files) {
        this();
        for (File f : files) {
            lastModifiedTimes.put(f.getAbsolutePath(), f.lastModified());
        }
    }
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfigurationTimestamp)) {
            return false;
        }

        ConfigurationTimestamp other = (ConfigurationTimestamp) o;

        Map<String, Long> oLastModified = other.lastModifiedTimes;

        if (lastModifiedTimes.size() != oLastModified.size())
            return false;

        for (Map.Entry<String, Long> entry : lastModifiedTimes.entrySet()) {
            Long otherModified = oLastModified.get(entry.getKey());
            if (otherModified == null)
                return false;

            if (!entry.getValue().equals(otherModified))
                return false;
        }

        return true;
    }
}
