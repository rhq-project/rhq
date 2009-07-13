 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
    private final Log log = LogFactory.getLog(this.getClass());

    private String eventType;
    private File logFile;
    private FileInfo logFileInfo;
    private LogEntryProcessor entryProcessor;

    public LogFileEventPoller(EventContext eventContext, String eventType, File logFile, LogEntryProcessor entryProcessor) {
        this.eventType = eventType;
        this.logFile = logFile;
        SigarProxy sigar = eventContext.getSigar();
        try {
            this.logFileInfo = new LogFileInfo(sigar.getFileInfo(logFile.getPath()));
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        this.entryProcessor = entryProcessor;
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
            log.warn("Log file [" + this.logFile + "' being polled does not exist.");
            return null;
        }
        if (this.logFile.isDirectory()) {
            log.error("Log file [" + this.logFile + "' being polled is a directory, not a regular file.");
            return null;
        }
        try {
            if (!this.logFileInfo.changed()) {
                return null;
            }
        } catch (SigarException e) {
            throw new RuntimeException(e);
        }
        return processNewLines();
    }

    private Set<Event> processNewLines() {
        Set<Event> events = null;
        Reader reader = null;
        try {
            reader = new FileReader(this.logFile);

            long offset = getOffset();

            if (offset > 0) {
                reader.skip(offset);
            }
            BufferedReader bufferedReader = new BufferedReader(reader);
            events = this.entryProcessor.processLines(bufferedReader);
        } catch (IOException e) {
            log.error("Failed to read log file being tailed: " + this.logFile, e);
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

    private long getOffset() {
        FileInfo previousFileInfo = this.logFileInfo.getPreviousInfo();

        if (previousFileInfo == null) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": first stat");
            }
            return this.logFileInfo.getSize();
        }

        if (this.logFileInfo.getInode() != previousFileInfo.getInode()) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": file inode changed");
            }
            return -1;
        }

        if (this.logFileInfo.getSize() < previousFileInfo.getSize()) {
            if (log.isDebugEnabled()) {
                log.debug(this.logFile + ": file truncated");
            }
            return -1;
        }

        if (log.isDebugEnabled()) {
            long diff = this.logFileInfo.getSize() - previousFileInfo.getSize();
            log.debug(this.logFile + ": " + diff + " new bytes");
        }

        return previousFileInfo.getSize();
    }
}
