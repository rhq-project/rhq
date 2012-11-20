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

package org.rhq.plugins.hadoop.calltime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.event.log.Log4JLogEntryProcessor;

/**
 * This is an extension of the log entry processor that can also gather data for calltime metrics
 * we gather on the hadoop jobs.
 *
 * @author Lukas Krejci
 */
public class HadoopEventAndCalltimeDelegate extends Log4JLogEntryProcessor {
    
    private Set<JobSummary> accumulatedJobEntries = new HashSet<JobSummary>();
        
    public HadoopEventAndCalltimeDelegate(String eventType, File logFile) {
        super(eventType, logFile);
    }

    public synchronized Set<JobSummary> drainAccumulatedJobs() {
        HashSet<JobSummary> ret = new HashSet<JobSummary>(accumulatedJobEntries);
        accumulatedJobEntries.clear();

        return ret;
    }

    /**
     * This does the very same thing as the super class method but the method is synchronized
     * so that accumulated calltime data can be drained without race conditions using the 
     * {@link #drainAccumulatedJobs()} method.
     */
    @Override
    public synchronized Set<Event> processLines(BufferedReader bufferedReader) throws IOException {
        return super.processLines(bufferedReader);
    }
    
    @Override
    protected LogEntry processPrimaryLine(Matcher matcher) throws ParseException {
        LogEntry logEntry = super.processPrimaryLine(matcher);
        
        //call this fragile ;) but LogEntry doesn't have much of a public API
        String detail = matcher.group(3);
        
        if (detail.startsWith(JobSummary.EXPECTED_LOGGER)) {
            JobSummary summary = JobSummary.parseJobSummaryLogEntry(matcher.group());
            if (summary != null) {
                accumulatedJobEntries.add(summary);
            }
        }
        
        return logEntry;
    }
}
