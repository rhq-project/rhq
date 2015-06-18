/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
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
 * A servlet filter that measures how long it takes to service each request, and logs the stats to a log file. There
 * is one log file per webapp, and one line per request. When this filter is deployed globally in Tomcat via
 * conf/web.xml, there will be one instance of this filter per deployed webapp, inserted ahead of any per-webapp filters
 * in the filter chain. We assume the same is true for other servlet containers, but Tomcat is the only servlet
 * container that has been tested.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public class RtFilter implements Filter {

    private static final String JAVA_IO_TMPDIR_SYSPROP = "java.io.tmpdir";
    private static final String JBOSS_HOME_DIR_SYSPROP = "jboss.home.dir";
    private static final String JBOSS_SERVER_LOG_DIR_SYSPROP = "jboss.server.log.dir";
    private static final String JBOSS_DOMAIN_LOG_DIR_SYSPROP = "jboss.domain.log.dir";
    private static final String CATALINA_HOME_SYSPROP = "catalina.home";
    private static final String TOMCAT_SERVER_LOG_SUBDIR = "logs";

    private static final String DEFAULT_LOG_FILE_PREFIX = "";
    private static final long DEFAULT_FLUSH_TIMEOUT = 60L * 1000; // 1 minute
    private static final long DEFAULT_FLUSH_AFTER_LINES = 10L;
    private static final long DEFAULT_MAX_LOG_FILE_SIZE = 1024L * 1024 * 5; // 5 MB
    private static final boolean DEFAULT_CHOP_QUERY_STRING = true;

    private final Log log = LogFactory.getLog(this.getClass());

    private boolean initialized = false;
    private boolean fileDone = false;
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
    private String contextName;
    private String myHostName; // InetAddr.getHostname()
    private String myCHostName; // InetAddr.getCanonicalHostname()

    private final Object lock = new Object();
    private Properties vhostMappings = new Properties();
    private static final String HOST_TOKEN = "%HOST%";

    /**
     * Does the real magic. If a fatal exception occurs during processing, the filter will revert to an uninitialized
     * state and refuse to process any further requests (see {@link #handleFatalError(Exception)}).
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     *      javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
        ServletException {
        long t1 = 0;
        HttpServletRequest hreq = (HttpServletRequest) req;
        RtFilterResponseWrapper hresp = new RtFilterResponseWrapper(resp);

        synchronized (lock) {
            if (this.initialized) {
                try {
                    t1 = System.currentTimeMillis();
                    this.requestCount++;
                    if ((this.requestCount > 1) && (t1 > (this.t2 + this.timeBetweenFlushes))) {
                        this.flushingNeeded = true;
                    }
                } catch (Exception e) {
                    handleFatalError(e);
                }
            }
        }

        try {
            chain.doFilter(req, hresp);
        } finally {
            synchronized (lock) {
                if (this.initialized) {
                    try {
                        this.t2 = System.currentTimeMillis();

                        int statusCode = hresp.getStatus();
                        // Only log successful requests (2xx or 3xx) since that's all we care about for now...
                        if ((statusCode < 200) || (statusCode >= 400)) {
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

                        // If the logfile was not yet created, lets do it now.
                        // We only reach this point if the request was for a valid vhost.
                        if (!fileDone) {
                            openFile(req.getServerName());
                        }

                        truncateLogFileIfMaxSizeExceeded();

                        // If we got this far, write the request info to the log.
                        writeLogEntry(req, hresp, uri, url, t1);
                    } catch (Exception e) {
                        handleFatalError(e);
                    }
                }
            }
        }
    }

    /**
     * Open the logfile for the given serverName. If serverName is localhost, then no
     * vhost portion is added to the logfile name. Otherwise the logfile name is
     * prefix + vhost + "_" + contextRoot + "_rt.log"<br/>
     * E.g. '/devel/jboss-4.0.3SP1/server/default/log/rt/snert.home.bsd.de_test_rt.log'.
     * <br/>
     * After opening the file we open a writer where we send results to. If
     * opening fails, no exceptions are thrown, as throwing any exception would cause the associated webapp to
     * fail to deploy - not something we ever want to do. Instead, the filter will simply not try to process any
     * requests (i.e. doFilter() will call the rest of the filter chain, and then just return).
     * <p/>
     * This method will set the fileDone variable to true upon success.<br/>
     * This method will set the initialized variable to false upon failure.<br/>
     * @param serverName Name of the virtual host
     */
    private void openFile(String serverName) {
        String vhost = "";
        boolean found = false;

        if ("localhost".equals(serverName) || "127.0.0.1".equals(serverName)) {
            vhost = "";
            found = true;
        }

        // see if the user provided a mapping for this server name
        if (!found && vhostMappings.containsKey(serverName)) {
            found = true;
            vhost = vhostMappings.getProperty(serverName);
            if (vhost == null || vhost.equals(""))
                vhost = ""; // It is in the mapping, but no value set -> no vhost
            else
                vhost += "_"; // Otherwise take it and append the separator
            if (log.isDebugEnabled())
                log.debug("Vhost determined from mapping >" + vhost + "<");
        }

        // check server name against hostname and hostname.fqdn and see if they match
        if (!found && vhostMappings.containsKey(HOST_TOKEN)) {
            vhost = vhostMappings.getProperty(HOST_TOKEN);
            if (myHostName.startsWith(serverName) || myCHostName.startsWith(serverName)) {
                // Match, so take the mapping from the file
                if (vhost == null || vhost.equals("")) {
                    vhost = ""; // It is in the mapping, but no value set -> no vhost
                } else {
                    vhost += "_"; // Otherwise take it and append the separator
                }
                found = true;
                if (log.isDebugEnabled())
                    log.debug("Vhost determined from %HOST% token >" + vhost + "<");
            }
        }

        if (!found) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                if (localHost.getHostName().equalsIgnoreCase(serverName)
                    || localHost.getCanonicalHostName().equalsIgnoreCase(serverName)
                    || localHost.getHostAddress().equals(serverName)) {
                    vhost = "";
                    found = true;
                }
            } catch (Exception e) {
                found = false;
            }
        }

        // Nothing found? Fall back to serverName + _ as prefix
        if (!found) {
            vhost = serverName + "_"; // Not found in mapping? Take it literal + separator
        }

        // if this is a sub-context (e.g. news/radio), then replace the / by a _ to
        // prevent interpretation of the / as a dir separator.
        String contextFileName = contextName.replace('/', '_');

        String logFileName = this.logFilePrefix + vhost + contextFileName + "_rt.log";
        this.logFile = new File(this.logDirectory, logFileName);
        log.info("-- Filter openFile: Writing response-time log for webapp with context root '" + this.contextName
            + "' to '" + this.logFile + "' (hashCode=" + hashCode() + ")...");
        boolean append = true;
        try {
            openFileWriter(append);
            fileDone = true;
        } catch (Exception e) {
            // reset the initialized flag in case of error
            this.initialized = false;
            log.warn(e.getMessage());
        }

    }

    /**
     * Read initialization parameters, and determine the context root.
     * The logfile name will be determined later, as we don't know the vhost yet.
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            synchronized (lock) {
                myHostName = InetAddress.getLocalHost().getHostName();
                myCHostName = InetAddress.getLocalHost().getCanonicalHostName();
                initializeParameters(filterConfig);
                ServletContext servletContext = filterConfig.getServletContext();
                this.contextName = ServletUtility.getContextRoot(servletContext);

                /*
                 * We don't open the file here, as we have no way to know the vhost this filter instance is for.
                 * Instead we mark ourselves as initialized, and try to open the file in doFilter().
                 */

                this.initialized = true;
                log.info("Initialized response-time filter for webapp with context root '" + this.contextName + "'.");
            }
        } catch (Exception e) {
            handleFatalError(e);
        }
    }

    /**
     * Cleanup when the filter gets unloaded (at webapp shutdown).
     *
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        synchronized (lock) {
            log.info("-- Filter destroy: " + this.requestCount + " requests processed (hashCode=" + hashCode() + ").");
            closeFileWriter();
            this.initialized = false;
        }
    }

    private void writeLogEntry(ServletRequest req, RtFilterResponseWrapper responseWrapper, String uri, String url,
        long t1) throws Exception {
        long duration = this.t2 - t1;
        if (duration < 0) {
            log.error("Calculated response time for request to [" + url + "] (" + duration + " ms) is negative!");
            return;
        }
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
            this.lastLogFileSize = this.logFile.length();
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
             * If this is a JBossAS deployed container, or a Standalone TC container, use a logical
             * default (so those plugins can be written in a compatible way).
             * First, try to default to "${JBOSS_SERVER_HOME_DIR_SYSPROP}/JBOSSAS_SERVER_LOG_SUBDIR/rt";
             * If not set try "${TOMCAT_SERVER_HOME_DIR_SYSPROP}/TOMCAT_SERVER_LOG_SUBDIR/rt";
             * If, for some reason, neither property is set, fall back to "${java.io.tmpdir}/rhq/rt".
             */
            File serverLogDir = null;

            String jbossHomeDir = System.getProperty(JBOSS_HOME_DIR_SYSPROP);
            if (jbossHomeDir != null) {
                // JBoss AS
                log.debug(JBOSS_HOME_DIR_SYSPROP + " sysprop is set - assuming we are running inside JBoss AS.");
                String serverLogDirString = System.getProperty(JBOSS_SERVER_LOG_DIR_SYSPROP);
                if (serverLogDirString == null) {
                    serverLogDirString = System.getProperty(JBOSS_DOMAIN_LOG_DIR_SYSPROP);
                }
                if (serverLogDirString != null) {
                    serverLogDir = new File(serverLogDirString);
                }
            } else {
                String catalinaHome = System.getProperty(CATALINA_HOME_SYSPROP);
                if (catalinaHome != null) {
                    // Tomcat
                    log.debug(CATALINA_HOME_SYSPROP + " sysprop is set - assuming we are running inside Tomcat.");
                    serverLogDir = new File(catalinaHome, TOMCAT_SERVER_LOG_SUBDIR);
                }
            }

            if (serverLogDir != null) {
                this.logDirectory = new File(serverLogDir, "rt");
            } else {
                this.logDirectory = new File(System.getProperty(JAVA_IO_TMPDIR_SYSPROP), "rhq/rt");
                log
                    .warn("The 'logDirectory' filter init param was not set. Also, the standard system properties were not set ("
                        + JBOSS_SERVER_LOG_DIR_SYSPROP + ", " + JBOSS_DOMAIN_LOG_DIR_SYSPROP + ", " + CATALINA_HOME_SYSPROP
                        + "); defaulting RT log directory to '" + this.logDirectory + "'.");
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

        /*
         * Read mappings from a vhost mapping file in the format of a properties file
         * inputhost = mapped host
         * This file needs to live in the search path - e.g. in server/<config>/conf/
         * The name of it must be passed as init-param to the filter. Otherwise the mapping
         * will not be used.
         *  <param-name>vHostMappingFile</param-name>
         */
        String vhostMappingFileString = conf.getInitParameter(InitParams.VHOST_MAPPING_FILE);
        if (vhostMappingFileString != null) {
            String configDir = System.getProperty("jboss.server.config.dir");
            File mappingFile = new File(configDir + File.separator + vhostMappingFileString);
            if(mappingFile.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(mappingFile);
                    vhostMappings.load(fis);
                } catch (IOException e) {
                    log.warn("Can't read vhost mappings from " + vhostMappingFileString + " :" + e.getMessage());
                } finally {
                    if (fis != null)
                        try {
                            fis.close();
                        } catch (Exception e) {
                            log.debug(e);
                        }
                }
            } else {
                log.warn("Can't find vhost mappings file from " + mappingFile.getAbsolutePath());
            }
        }
    }

    /**
     * Open a BufferedWriter that can be used to write the log lines.
     *
     * @param append if true, append to the end of the file; if false, truncate the file and write from the beginning
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
                log.debug("Closed writer for response time log '" + this.logFile + "'.");
            } catch (IOException e) {
                log.error("Failed to close writer for response time log '" + this.logFile + "'.");
            }
        }
    }

    private void truncateLogFileIfMaxSizeExceeded() throws Exception {
        if (this.logFile.length() > this.maxLogFileSize) {
            log.warn("Response time log '" + this.logFile + "' has exceeded maximum file size (" + this.maxLogFileSize
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
                    + " has been truncated (probably by RHQ Agent) - rewinding writer...");
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

    private void handleFatalError(Exception e) {
        this.initialized = false;
        log.fatal(
            "RHQ response-time filter experienced an unrecoverable failure. Response-time collection is now disabled for context '"
                + this.contextName + "'.", e);
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
        public static final String VHOST_MAPPING_FILE = "vHostMappingFile";
    }

}
