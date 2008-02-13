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

import java.io.PrintStream;

/**
 * Logs to a specific stream.
 */
public abstract class StreamLoggerListener extends AbstractLoggerListener {
    private PrintStream m_output;

    /**
     * Constructor for {@link StreamLoggerListener} that logs notifications to the given stream.
     *
     * @param out
     */
    public StreamLoggerListener(PrintStream out) {
        m_output = out;
    }

    /**
     * @see AbstractLoggerListener#writeLogMessage(String)
     */
    protected void writeLogMessage(String msg) {
        m_output.println((new java.util.Date()).toString() + ": " + msg);
    }
}