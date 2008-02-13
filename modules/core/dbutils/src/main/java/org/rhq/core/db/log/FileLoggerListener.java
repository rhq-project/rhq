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
package org.rhq.core.db.log;

import java.io.FileWriter;

/**
 * Logs notifications to a file. If the file already exists, the log messages are appended to it.
 */
public class FileLoggerListener extends AbstractLoggerListener {
    /**
     * The property whose value is the log file where this logger will write its messages.
     */
    public static final String PROP_LOGFILE = "jdbcLogFile";

    /**
     * Where the messages are logged.
     */
    private FileWriter m_fileWriter;

    /**
     * Creates a new {@link FileLoggerListener} object.
     */
    public FileLoggerListener() {
        this(System.getProperty(PROP_LOGFILE));
    }

    /**
     * Creates a new {@link FileLoggerListener} object.
     *
     * @param  filename the file to log to
     *
     * @throws RuntimeException failed to open the log file
     */
    public FileLoggerListener(String filename) {
        try {
            m_fileWriter = new FileWriter(filename, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see AbstractLoggerListener#writeLogMessage(String)
     */
    public void writeLogMessage(String msg) {
        try {
            m_fileWriter.write((new java.util.Date()).toString() + ": " + msg + "\n");
            m_fileWriter.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}