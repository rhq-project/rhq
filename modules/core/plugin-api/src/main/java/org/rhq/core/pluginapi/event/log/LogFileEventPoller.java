/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.pluginapi.event.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * An Event poller that polls a log file for new entries. 
 *
 * @author Ian Springer
 */
public class LogFileEventPoller implements EventPoller {
    private static final Log LOG = LogFactory.getLog(LogFileEventPoller.class);

    private String eventType;
    private File logFile;
    private FileInfo logFileInfo;
    private LogEntryProcessor entryProcessor;
    private EventContext eventContext;
    private boolean initialized;

    public LogFileEventPoller(EventContext eventContext, String eventType, File logFile,
        LogEntryProcessor entryProcessor) {
        this.eventType = eventType;
        this.logFile = logFile;
        this.entryProcessor = entryProcessor;
        this.eventContext = eventContext;
    }

    @NotNull
    public String getEventType() {
        return this.eventType;
    }

    @NotNull
    public String getSourceLocation() {
        return this.logFile.getPath();
    }

    @Nullable
    public Set<Event> poll() {
        if (!this.logFile.exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Log file [" + this.logFile + "] being polled does not exist.");
            }
            return null;
        }
        if (this.logFile.isDirectory()) {
            LOG.error("Log file [" + this.logFile + "] being polled is a directory, not a regular file.");
            return null;
        }
        if (!this.initialized) {
            init();
        }
        if (this.logFileInfo == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot poll log file [" + this.logFile
                    + "] because native integration is either disabled or unavailable.");
            }
            return null;
        }
        try {
            if (!this.logFileInfo.changed()) {
                return null;
            }
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        return processNewLines(this.logFileInfo);
    }

    /**
     * This performs any initialization that requires using the EventContext. It must *not* be called from our
     * constructor, because pollers are constructed during PC initialization, and at that time the PC EventManager,
     * which the EventContext relies on, is not yet available. Instead it is called from {@link #poll()} on the first
     * invocation of that method, at which point the PC will be initialized.
     */
    protected void init() {
        SigarProxy sigar = this.eventContext.getSigar();
        if (sigar != null) {
            try {
                this.logFileInfo = new LogFileInfo(sigar.getFileInfo(logFile.getPath()));
            } catch (SigarException e) {
                throw new RuntimeException("Failed to obtain file info for log file [" + this.logFile + "].", e);
            }
        } else {
            LOG.warn("SIGAR is unavailable - cannot poll log file [" + this.logFile + "] for events.");
        }

        this.initialized = true;
    }

    private Set<Event> processNewLines(FileInfo fileInfo) {
        Set<Event> events = null;
        Reader reader = null;
        try {
            reader = new FileReader(this.logFile);

            long offset = getOffset(fileInfo);

            if (offset > 0) {
                reader.skip(offset);
            }
            BufferedReader bufferedReader = new BufferedReader(reader);
            events = this.entryProcessor.processLines(bufferedReader);
        } catch (IOException e) {
            LOG.error("Failed to read log file being tailed: " + this.logFile, e);
        } finally {
            if (reader != null) {
                //noinspection EmptyCatchBlock
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return events;
    }

    private long getOffset(FileInfo fileInfo) {
        FileInfo previousFileInfo = fileInfo.getPreviousInfo();

        if (previousFileInfo == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this.logFile + ": first stat");
            }
            return fileInfo.getSize();
        }

        if (fileInfo.getInode() != previousFileInfo.getInode()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this.logFile + ": file inode changed");
            }
            return -1;
        }

        if (fileInfo.getSize() < previousFileInfo.getSize()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this.logFile + ": file truncated");
            }
            return -1;
        }

        if (LOG.isDebugEnabled()) {
            long diff = fileInfo.getSize() - previousFileInfo.getSize();
            LOG.debug(this.logFile + ": " + diff + " new bytes");
        }

        return previousFileInfo.getSize();
    }
}
