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
package org.rhq.core.pluginapi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.measurement.calltime.CallTimeData;

/**
 * This is a very simple log parser that uses a StringTokenizer instead of a regular expression to parse a HTTP
 * response-time log file. This should greatly improve the performance. It requires that lines in the log file have the
 * following format (with one line per HTTP request):
 *
 * <p/><code>URL date_in_milliseconds time_taken [status_code [IP_address]]</code>
 *
 * <p/>This is the output format used by the Apache RT module, as well as the servlet RT filter.
 *
 * @author Ian Springer
 */
public class ResponseTimeLogParser {
    public static final int DEFAULT_TIME_MULTIPLIER = 1;

    private final Log log = LogFactory.getLog(this.getClass());

    // TODO: Do we even need to support a time multiplier?
    // Find out if durations in Apache RT logs are in something other than milliseconds.
    // (ips, 11/29/07)
    private double timeMultiplier;
    private long startingOffset;
    private File logFile;
    private List<Pattern> excludes;
    private List<RegexSubstitution> transforms;

    public ResponseTimeLogParser(File logFile) {
        this(logFile, DEFAULT_TIME_MULTIPLIER);
    }

    public ResponseTimeLogParser(File logFile, double timeMultiplier) {
        this.logFile = logFile;
        this.timeMultiplier = timeMultiplier;
    }

    /**
     * Parse the logfile, starting at the offset corresponding to the file's size after the last time this method was
     * called. Immediately after parsing, the file will be truncated, permissions permitting. If the log file does not
     * exist, a warning will be logged and the method will return. The parsed response-time data will be added to the
     * passed-in CallTimeData object.
     *
     * @param callTimeData the parsed response-time data will be added to this object
     */
    public synchronized void parseLog(CallTimeData callTimeData) throws IOException {
        log.debug("Parsing response-time log file " + this.logFile + "...");
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(this.logFile));
        } catch (FileNotFoundException e) {
            log.warn("Response-time log file '" + this.logFile + "' does not exist.");
            return;
        }

        in.skip(this.startingOffset);
        String currentLine;
        while ((currentLine = in.readLine()) != null) {
            LogEntry logEntry;
            try {
                logEntry = parseLine(currentLine);
            } catch (Exception e) {
                log.debug("Problem parsing line [" + currentLine + "] - cause: " + e);
                continue;
            }

            String url = logEntry.getUrl();

            // The URL should always begin with a slash. If it doesn't, log an error and skip the entry,
            // so we don't end up with bogus data in the DB.
            if (url.charAt(0) != '/') {
                String truncatedUrl = url.substring(0, Math.min(url.length(), 120));
                if (url.length() > 120)
                    truncatedUrl += "...";
                log.error("URL ('" + truncatedUrl + "') parsed from response-time log file does not begin with '/'. " +
                        "Line being parsed is [" + currentLine + "].");
                continue;
            }

            if (isExcluded(url)) {
                continue;
            }

            // Only collect stats for successful (2xx or 3xx) requests...
            if ((logEntry.getStatusCode() != null)
                && ((logEntry.getStatusCode() < 200) || (logEntry.getStatusCode() >= 400))) {
                continue;
            }

            String transformedUrl = applyTransforms(url);
            callTimeData.addCallData(transformedUrl, new Date(logEntry.getStartTime()), logEntry.getDuration());
        }

        in.close();

        /*
         * After we're done parsing the file, truncate it. This is kosher, assuming we own any file being parsed by this
         * parser.
         */
        truncateLog(this.logFile);
        this.startingOffset = this.logFile.length();
    }

    private boolean isExcluded(String url) {
        boolean excluded = false;
        if (this.excludes != null) {
            for (Pattern exclude : this.excludes) {
                Matcher matcher = exclude.matcher(url);
                if (matcher.find()) {
                    log.debug("URL '" + url + "' excluded by exclude '" + exclude + "'");
                    excluded = true;
                }
            }
        }

        return excluded;
    }

    private String applyTransforms(String url) {
        String transformedUrl = null;
        if (this.transforms != null) {
            for (RegexSubstitution transform : this.transforms) {
                Matcher matcher = transform.getPattern().matcher(url);
                if (matcher.find()) {
                    transformedUrl = matcher.replaceFirst(transform.getReplacement());
                    log.debug("URL '" + url + "' transformed to '" + transformedUrl + "' by transform '" + transform
                        + "'.");
                    break;
                }
            }
        }

        return (transformedUrl != null) ? transformedUrl : url;
    }

    /**
     * Parses a line from a response time log and returns a LogEntry.
     *
     * @param  line the line to be parsed
     *
     * @return a LogEntry representing the line
     */
    @NotNull
    protected LogEntry parseLine(String line) throws Exception {
        LogEntry logEntry;
        try {
            StringTokenizer tokenizer = new StringTokenizer(line);
            String url = tokenizer.nextToken();
            long startTime = Long.parseLong(tokenizer.nextToken());
            long duration = (long) (Double.parseDouble(tokenizer.nextToken()) * this.timeMultiplier);
            Integer statusCode = null;
            String ipAddress = null;
            if (tokenizer.hasMoreTokens()) {
                statusCode = Integer.valueOf(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens()) {
                    ipAddress = tokenizer.nextToken();
                }
            }

            logEntry = new LogEntry(url, startTime, duration, statusCode, ipAddress);
        } catch (RuntimeException e) {
            throw new Exception("Failed to parse response time log file line [" + line + "].", e);
        }

        return logEntry;
    }

    private void truncateLog(File logFile) throws IOException {
        log.debug("Truncating response-time log file '" + logFile + "'...");
        try {
            String mode = "rws";
            RandomAccessFile randomAccessFile = new RandomAccessFile(logFile, mode);
            randomAccessFile.setLength(0);
            randomAccessFile.close();
        } catch (SecurityException e) {
            /* User doesn't have permission to change the length, so
             * ignore this exception.
             */
            log.debug("Unable to truncate response-time log file.", e);
        } catch (FileNotFoundException e) {
            /* Can't happen.  We have just parsed this file.
             * Could be a permission error.  Log it.
             */
            log.error("Unable to truncate response-time log file.", e);
        }
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public double getTimeMultiplier() {
        return timeMultiplier;
    }

    public List<Pattern> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<Pattern> excludes) {
        this.excludes = excludes;
    }

    public List<RegexSubstitution> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<RegexSubstitution> transforms) {
        this.transforms = transforms;
    }

    private class LogEntry {
        LogEntry(@NotNull
        String url, long startTime, long duration, @Nullable
        Integer statusCode, @Nullable
        String ipAddress) {
            this.url = url;
            this.startTime = startTime;
            this.duration = duration;
            this.statusCode = statusCode;
            this.ipAddress = ipAddress;
        }

        private String url;
        private long startTime;
        private long duration;
        private Integer statusCode;
        private String ipAddress;

        @NotNull
        public String getUrl() {
            return url;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDuration() {
            return duration;
        }

        @Nullable
        public Integer getStatusCode() {
            return statusCode;
        }

        @Nullable
        public String getIpAddress() {
            return ipAddress;
        }
    }
}