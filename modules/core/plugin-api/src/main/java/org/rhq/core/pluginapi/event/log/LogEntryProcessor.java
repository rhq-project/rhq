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
package org.rhq.core.pluginapi.event.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;

/**
 * A processor for entries from a log file. Each LogEntryProcessor instance is associated with a
 * {@link org.rhq.core.pluginapi.event.log.LogFileEventPoller}. Whenever the LogFileEventPoller's poll() method
 * detects new lines have been appended to the log file is is tailing, it calls the
 * {@link #processLines(java.io.BufferedReader)} method, passing it a buffered reader containing the new lines.
 *
 * @author Ian Springer
 */
public interface LogEntryProcessor {
    /**
     * Processes the specified lines from a log file, and returns a set of Events if appropriate, or otherwise, null.
     *
     * @param bufferedReader a buffered reader from which the lines can be read
     *
     * @return a set of Events if appropriate, or otherwise, null
     * 
     * @throws java.io.IOException if reading lines from the supplied buffer reader fails
     */
    @Nullable
    Set<Event> processLines(BufferedReader bufferedReader) throws IOException;
}
