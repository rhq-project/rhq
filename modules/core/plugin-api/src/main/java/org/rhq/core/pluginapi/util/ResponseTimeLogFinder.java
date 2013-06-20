/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pluginapi.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Helps to find a response time filter log file. 
 *
 * @author Thomas Segismont
 */
public class ResponseTimeLogFinder {
    public static final File FALLBACK_RESPONSE_TIME_LOG_FILE_DIRECTORY = new File(System.getProperty("java.io.tmpdir")
        + File.separator + "rhq" + File.separator + "rt");

    private ResponseTimeLogFinder() {
        // Utility class
    }

    /**
     * @param contextPath context path of the monitored web application
     * @param responseTimeLogFileDirectory Where to search for the response time log file of the web application.
     * @return the absolute file path of the response time log file or null if not found or responseTimeLogFileDirectory
     * does not exist or is not a directory.
     * @throws IllegalArgumentException if context path is null or does not start with a slash, or if
     * responseTimeLogFileDirectory is null
     */
    public static String findResponseTimeLogFileInDirectory(String contextPath, File responseTimeLogFileDirectory) {
        if (contextPath == null) {
            throw new IllegalArgumentException("contextPath is null");
        }
        if (!contextPath.startsWith("/")) {
            throw new IllegalArgumentException("contextPath does not start with '/': " + contextPath);
        }
        if (responseTimeLogFileDirectory == null) {
            throw new IllegalArgumentException("responseTimeLogFileDirectory is null");
        }
        if (responseTimeLogFileDirectory.exists() && responseTimeLogFileDirectory.isDirectory()) {
            // Handle the root context path
            // Remove leading slash and convert slashes to underscores
            final String rtFileSuffix = (contextPath.equals("/") ? "ROOT" : contextPath).substring(1).replace('/', '_')
                + "_rt.log";
            File[] files = responseTimeLogFileDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(rtFileSuffix);
                }
            });
            if (files != null && files.length == 1) {
                return files[0].getAbsolutePath();
            }
        }
        return null;
    }
}
