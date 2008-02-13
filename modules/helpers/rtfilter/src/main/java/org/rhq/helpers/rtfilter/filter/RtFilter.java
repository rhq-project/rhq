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
package org.rhq.helpers.rtfilter.filter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.helpers.rtfilter.util.ServletUtility;

/**
 * ResponseTime filter. This filter takes all incoming requests and checks how long it takes to process them. When this
 * is deployed in Tomcat via conf/web.xml globally, there will be one instance of this filter per deployed webapp
 * inserted at the front of the whole filter chain.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public class RtFilter implements Filter {
    private static final String JAVA_IO_TMPDIR_SYSPROP = "java.io.tmpdir";
    private static final String JBOSS_SERVER_HOME_DIR_SYSPROP = "jboss.server.home.dir";

    private static final String DEFAULT_LOG_FILE_PREFIX = "";
    private static final long DEFAULT_FLUSH_TIMEOUT = 60 * 1000; // 1 minute
    private static final long DEFAULT_FLUSH_AFTER_LINES = 10;
    private static final long DEFAULT_MAX_LOG_FILE_SIZE = 1024 * 1024 * 5; // 5 MB
    private static final boolean DEFAULT_CHOP_QUERY_STRING = true;

    private final Log log = LogFactory.getLog(this.getClass());

    private boolean initialized = false;
    private long requestCount = 0;
    private boolean chopUrl = DEFAULT_CHOP_QUERY_STRING;
    private File logDirectory;
    private BufferedWriter writer;
    private Pattern dontLogPattern = null;
    private long timeBetweenFlushes = DEFAULT_FLUSH_TIMEOUT;
    private boolean matchOnUriOnly = true;
    private long flushAfterLines = DEFAULT_FLUSH_AFTER_LINES;
    private String logFilePrefix = DEFAULT_LOG_FILE_PREFIX;
    private boolean flushingNeeded = false;
    private long t2;
    private File logFile;
    private long lastLogFileSize = 0;
    private long maxLogFileSize = DEFAULT_MAX_LOG_FILE_SIZE;

    /**
     * Does the real magic. If a fatal exception occurs during processing, the filter will revert to an uninitialized
     * state and refuse to process any further requests.
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     *      javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
        ServletException {
        long t1 = 0;
        HttpServletRequest hreq = null;
        RtFilterResponseWrapper hresp = null;
        if (this.initialized) {
            try {
                t1 = System.currentTimeMillis();
                this.requestCount++;
                hreq = (HttpServletRequest) req;
                hresp = new RtFilterResponseWrapper(resp);
                if ((this.requestCount > 1) && (t1 > (this.t2 + this.timeBetweenFlushes))) {
                    this.flushingNeeded = true;
                }
            } catch (Exception e) {
                log
                    .fatal("JON response-time filter failed to process request. Please fix the following problem, then restart the servlet container: "
                        + e);
                this.initialized = false;
            }
        }

        try {
            chain.doFilter(req, resp);
        } finally {
            if (this.initialized) {
                try {
                    this.t2 = System.currentTimeMillis();

                    int statusCode = hresp.getStatus();

                    // Only log successful requests, since that's all JON 2.0 cares about for now...
                    if ((statusCode < 200) || (statusCode >= 300)) {
                        return;
                    }

                    String uri = hreq.getRequestURI();
                    String url = getRequestURL(hreq);

                    // If the input matches the passed don't log regexp, then don't log the request.
                    if (this.dontLogPattern != null) {
                        Matcher matcher = this.dontLogPattern.matcher((this.matchOnUriOnly) ? uri : url);
                        if (matcher.matches()) {
                            return;
                        }
                    }

                    truncateLogFileIfMaxSizeExceeded();

                    // If we got this far, write the request info to the log.
                    writeLogEntry(req, hresp, uri, url, t1);
                } catch (Exception e) {
                    log
                        .fatal("JON response-time filter failed to process request. Please fix the following problem, then restart the servlet container: "
                            + e);
                    this.initialized = false;
                }
            }
        }
    }

    /**
     * Read initialization parameters, determine the logfile name and open a writer where we send results to. If
     * initialization fails, no exceptions are thrown, as throwing any exception would cause the associated webapp to
     * fail to deploy - not something we ever want to do. Instead, the filter will simply not try to process any
     * requests (i.e. doFilter() will call the rest of the filter chain, and then just return).
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            log.debug("-- Filter init ");
            initializeParameters(filterConfig);
            ServletContext servletContext = filterConfig.getServletContext();
            String contextName = ServletUtility.getContextRoot(servletContext);
            String logFileName = this.logFilePrefix + contextName + "_rt.log";
            this.logFile = new File(this.logDirectory, logFileName);
            log.info("Writing response-time log for webapp with context root '" + contextName + "' to '" + this.logFile
                + "'...");
            boolean append = true;
            openFileWriter(append);
            this.initialized = true;
        } catch (Exception e) {
            log
                .fatal("JON response-time filter failed to initialize. Please fix the following problem, then restart the servlet container: "
                    + e);
        }
    }

    /**
     * Cleanup when the filter gets unloaded (at webapp shutdown).
     *
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        log.debug("-- Filter destroy, " + this.requestCount + " requests processed");
        closeFileWriter();
        this.initialized = false;
    }

    private void writeLogEntry(ServletRequest req, RtFilterResponseWrapper responseWrapper, String uri, String url,
        long t1) throws Exception {
        long duration = this.t2 - t1;
        if (duration == 0) {
            // Impossible - we must be on Windows, where the system clock is only accurate to about 15 ms
            // (see http://www.simongbrown.com/blog/2007/08/20/millisecond_accuracy_in_java.html).
            // NOTE: With some tweaking, it is possible to make the Windows clock accurate to millisecond precision
            // (see http://www.lochan.org/2005/keith-cl/useful/win32time.html).
            // TODO: We should explain the above in our docs.
            // Bump the duration up to 1, so at least we're not saying the request defied the laws of physics.
            duration = 1;
        }

        if (log.isDebugEnabled()) {
            log.debug("Request to [" + url + "] took " + duration + " ms");
        }

        String remoteIp = req.getRemoteAddr();

        // Format: <url> <when> <duration> <status> <IP>
        StringBuilder buf = new StringBuilder();
        buf.append((this.chopUrl) ? uri : url).append(" ").append(this.t2).append(" ").append(duration).append(" ")
            .append(responseWrapper.getStatus()).append(" ").append(remoteIp);

        // Check if log file was externally truncated before writing to it. NOTE: It's important to do this just prior
        // to writing to the file to minimize the chances of the file becoming corrupt (i.e. front-padded with NUL
        // characters).
        rewindLogFileIfSizeDecreased();
        this.writer.append(buf);
        this.writer.newLine();
        if (this.flushingNeeded || ((this.requestCount % this.flushAfterLines) == 0)) {
            this.writer.flush();
            this.flushingNeeded = false;
        }
    }

    /**
     * Initialize parameters from the web.xml filter init-params
     *
     * @param conf the filter configuration
     */
    private void initializeParameters(FilterConfig conf) throws UnavailableException {
        String chop = conf.getInitParameter(InitParams.CHOP_QUERY_STRING);
        if (chop != null) {
            this.chopUrl = Boolean.valueOf(chop.trim()).booleanValue();
        }

        String logDirectoryPath = conf.getInitParameter(InitParams.LOG_DIRECTORY);
        if (logDirectoryPath != null) {
            this.logDirectory = new File(logDirectoryPath.trim());
        } else {
            /*
             * Default to "${jboss.server.home.dir}/log/rt"; this is the same location that the JON jbossas plugin
             * defaults to. If, for some reason, the jboss.server.home.dir sysprop is not set, fall back to
             * "${java.io.tmpdir}/rhq/rt".
             */
            String jbossServerHomeDirPath = System.getProperty(JBOSS_SERVER_HOME_DIR_SYSPROP);
            if (jbossServerHomeDirPath != null) {
                File jbossServerLogDir = new File(jbossServerHomeDirPath, "log");
                this.logDirectory = new File(jbossServerLogDir, "rt");
            } else {
                this.logDirectory = new File(System.getProperty(JAVA_IO_TMPDIR_SYSPROP), "rhq/rt");
                log.warn(JBOSS_SERVER_HOME_DIR_SYSPROP + " system property is not set - defaulting log directory to '"
                    + this.logDirectory + "'...");
            }
        }

        if (this.logDirectory.exists()) {
            if (!this.logDirectory.isDirectory()) {
                throw new UnavailableException("Log directory '" + this.logDirectory
                    + "' exists but is not a directory.");
            }
        } else {
            try {
                this.logDirectory.mkdirs();
            } catch (Exception e) {
                throw new UnavailableException("Unable to create log directory '" + this.logDirectory + "' - cause: "
                    + e);
            }

            if (!logDirectory.exists()) {
                throw new UnavailableException("Unable to create log directory '" + this.logDirectory + "'.");
            }
        }

        String logFilePrefixString = conf.getInitParameter(InitParams.LOG_FILE_PREFIX);
        if (logFilePrefixString != null) {
            this.logFilePrefix = logFilePrefixString.trim();
        }

        String dontLog = conf.getInitParameter(InitParams.DONT_LOG_REG_EX);
        if (dontLog != null) {
            this.dontLogPattern = Pattern.compile(dontLog.trim());
        }

        String flushTimeout = conf.getInitParameter(InitParams.TIME_BETWEEN_FLUSHES_IN_SEC);
        if (flushTimeout != null) {
            try {
                timeBetweenFlushes = Long.parseLong(flushTimeout.trim()) * 1000;
            } catch (NumberFormatException nfe) {
                timeBetweenFlushes = DEFAULT_FLUSH_TIMEOUT;
            }
        }

        String uriOnly = conf.getInitParameter(InitParams.MATCH_ON_URI_ONLY);
        if (uriOnly != null) {
            matchOnUriOnly = Boolean.getBoolean(uriOnly.trim());
        }

        String lines = conf.getInitParameter(InitParams.FLUSH_AFTER_LINES);
        if (lines != null) {
            try {
                flushAfterLines = Long.parseLong(lines.trim());
                if (flushAfterLines <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                log.error("Invalid '" + InitParams.FLUSH_AFTER_LINES + "' init parameter: " + lines
                    + " (value must be a positive integer) - using default.");
                flushAfterLines = DEFAULT_FLUSH_AFTER_LINES;
            }
        }

        String maxLogFileSizeString = conf.getInitParameter(InitParams.MAX_LOG_FILE_SIZE);
        if (maxLogFileSizeString != null) {
            try {
                this.maxLogFileSize = Long.parseLong(maxLogFileSizeString.trim());
                if (this.maxLogFileSize <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                log.error("Invalid '" + InitParams.MAX_LOG_FILE_SIZE + "' init parameter: " + maxLogFileSizeString
                    + " (value must be a positive integer) - using default.");
                this.maxLogFileSize = DEFAULT_MAX_LOG_FILE_SIZE;
            }
        }
    }

    /**
     * Open a BufferedWriter that can be used to write the log lines.
     *
     * @throws Exception if the logfile cannot be opened for writing
     */
    private void openFileWriter(boolean append) throws Exception {
        // Check if the file exists. If not, attempt to create it.
        if (!this.logFile.exists()) {
            boolean success;
            try {
                success = this.logFile.createNewFile();
            } catch (Exception e) {
                throw new Exception("Response time log '" + this.logFile + "' could not be created - cause: " + e);
            }

            if (!success) {
                throw new Exception("Response time log '" + this.logFile + "' could not be created.");
            }
        }

        // File now exists - check if we can write it.
        if (!this.logFile.canWrite()) {
            throw new Exception("Response time log '" + this.logFile + "' is not writable.");
        }

        // File is writable - open it for writing.
        try {
            this.writer = new BufferedWriter(new FileWriter(this.logFile, append));
        } catch (IOException e) {
            throw new Exception("Failed to open response time log '" + this.logFile + "' for writing - cause: " + e);
        }

        this.lastLogFileSize = this.logFile.length();
    }

    private void closeFileWriter() {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException e) {
                log.error("Failed to close writer for response time log " + this.logFile);
            }
        }
    }

    private void truncateLogFileIfMaxSizeExceeded() throws Exception {
        if (this.logFile.length() > this.maxLogFileSize) {
            log.info("Response time log '" + this.logFile + "' has exceeded maximum file size (" + this.maxLogFileSize
                + " bytes) - truncating it...");
            closeFileWriter();
            boolean append = false;
            openFileWriter(append);
        }
    }

    /**
     * Check if the logfile has been truncated by an external process and rewind to its start.
     *
     * @throws Exception if the file cannot be rewound for any reason
     */
    private void rewindLogFileIfSizeDecreased() throws Exception {
        if (this.logFile.length() < this.lastLogFileSize) {
            if (log.isDebugEnabled()) {
                log.debug("Logfile " + this.logFile
                    + " has been truncated (probably by the JON agent) - rewinding writer...");
            }

            closeFileWriter();
            boolean append = true;
            openFileWriter(append);
        }
    }

    private static String getRequestURL(HttpServletRequest hreq) {
        String queryString = hreq.getQueryString();
        String url = hreq.getRequestURI();
        if (queryString != null) {
            url += "?" + queryString;
        }

        return url;
    }

    abstract class InitParams {
        public static final String CHOP_QUERY_STRING = "chopQueryString";
        public static final String LOG_DIRECTORY = "logDirectory";
        public static final String LOG_FILE_PREFIX = "logFilePrefix";
        public static final String DONT_LOG_REG_EX = "dontLogRegEx";
        public static final String TIME_BETWEEN_FLUSHES_IN_SEC = "timeBetweenFlushesInSec";
        public static final String MATCH_ON_URI_ONLY = "matchOnUriOnly";
        public static final String FLUSH_AFTER_LINES = "flushAfterLines";
        public static final String MAX_LOG_FILE_SIZE = "maxLogFileSize";
    }
}