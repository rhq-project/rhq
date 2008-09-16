/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.iis;

import java.io.File;
import java.util.Map;

import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;

public class IISResponseTimeDelegate {

    // Default fields
    // time c-ip cs-method cs-uri-stem sc-status time-taken

    private Map<String, Long> logFilesLastChecked;
    private Map<String, ResponseTimeLogParser> parsers;
    private File logDirectory;

    public IISResponseTimeDelegate(String logDirectory) {
        // This directory holds the log files in exyymmdd.log

        this.logDirectory = new File(logDirectory);

    }

    public void parseLogs(CallTimeData data) {
        // E.g. format
        // 2008-04-18 19:56:18 127.0.0.1 GET /favicon.ico 404 0

    }

    private static class IISLogParser extends ResponseTimeLogParser {

        public IISLogParser(File logFile) {

            super(logFile);
        }
    }
}
