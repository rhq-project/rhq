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

package org.rhq.modules.plugins.wildfly10.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.system.ProcessExecutionResults;

/**
 * @author Thomas Segismont
 */
public class ProcessExecutionLogger {
    private static final Log LOG = LogFactory.getLog(ProcessExecutionLogger.class);

    private static final String SEPARATOR = "\n-----------------------\n";

    private ProcessExecutionLogger() {
        // Utility class
    }

    /**
     * Logs the result of a process execution.
     *
     * @param results the result of a process execution
     */
    public static void logExecutionResults(ProcessExecutionResults results) {
        // Always log the output at info level. On Unix we could switch depending on a exitCode being !=0, but ...
        LOG.info("Exit code from process execution: " + results.getExitCode());
        LOG.info("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
    }
}
