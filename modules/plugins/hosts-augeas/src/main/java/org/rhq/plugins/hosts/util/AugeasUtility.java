/*
 * Jopr Management Platform
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
package org.rhq.plugins.hosts.util;

import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

/**
 * @author Ian Springer
 */
public class AugeasUtility {
    public static String getParentPath(String path) {
        String trimmedPath = trimTrailingSlashes(path);
        if (trimmedPath.equals("/")) {
            return "/";
        }
        int lastSlashIndex = trimmedPath.lastIndexOf("/");
        String parentPath = trimmedPath.substring(0, lastSlashIndex);
        return parentPath;
    }

    public static List<String> matchFilter(Augeas augeas, String expression, String value) {
        List<String> matches = new ArrayList<String>();
        List<String> paths = augeas.match(expression);
        for (String path : paths) {
            String pathValue = augeas.get(path);
            if (pathValue.equals(value)) {
                matches.add(path);
            }
        }
        return matches;
    }

    private static String trimTrailingSlashes(String path) {
        int trailingSlashCount = 0;
        for (int i = path.length() - 1; i > 0; i--) {
            if (path.charAt(i) == '/') {
                trailingSlashCount++;
            }
        }
        String trimmedPath;
        if (trailingSlashCount > 0) {
            trimmedPath = path.substring(0, path.length() - trailingSlashCount);
        } else {
            trimmedPath = path;
        }
        return trimmedPath;
    }

    // prevent instantiation
    private AugeasUtility() {
    }
}
