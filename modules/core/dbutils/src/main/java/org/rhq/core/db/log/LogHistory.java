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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains a single item of log history.
 */
public class LogHistory {
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * when this history item was created
     */
    public Date logMessageTimeStamp = new Date();

    /**
     * The actual logged message
     */
    public String logMessage;

    /**
     * Creates a new {@link LogHistory} object.
     *
     * @param msg the message that was logged
     */
    public LogHistory(String msg) {
        this.logMessage = msg;
    }

    /**
     * Returns the timestamped log message.
     *
     * @return timestamped message
     */
    public String toString() {
        return FORMATTER.format(logMessageTimeStamp) + ": " + logMessage;
    }
}