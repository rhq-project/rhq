/*
 * JBoss, a division of Red Hat.
 * Portions Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.iis;

import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.core.domain.measurement.calltime.CallTimeData;

import java.io.File;
import java.util.Map;


public class IISResponseTimeDelegate {

    // Default fields
    // time c-ip cs-method cs-uri-stem sc-status time-taken

    private Map<String, Long> logFilesLastChecked;
    private Map<String, ResponseTimeLogParser> parsers;
    private File logDirectory;

    public IISResponseTimeDelegate(File logDirectory) {
        // This directory holds the log files in exyymmdd.log
        
        this.logDirectory = logDirectory;

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
