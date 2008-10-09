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
package org.rhq.enterprise.communications.command.client;

import mazz.i18n.Logger;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.CommandTraceUtil;

/**
 * Object that will log outgoing commands, used mainly for debugging a client.
 *
 * @author John Mazzitelli
 */
public class OutgoingCommandTrace {
    private static final Logger LOG = CommI18NFactory.getLogger(OutgoingCommandTrace.class);

    /**
     * Call this when starting the command - that is, just prior to it going out over the wire.
     * 
     * @param command the command that is going to be sent
     */
    public static void start(Command command) {
        if (LOG.isTraceEnabled()) {
            try {
                String cmdStr = CommandTraceUtil.getCommandString(command);
                String config = CommandTraceUtil.getConfigString(command);
                LOG.trace(CommI18NResourceKeys.TRACE_OUTGOING_COMMAND_START, cmdStr, config);
            } catch (Throwable t) {
                // don't bomb if for some reason we fail to log the trace message (should never happen)
            }
        }
    }

    /**
     * Call this when the command is finished - that is, just after its response is returned.
     * 
     * @param command the command that has just finished
     * @param response the response object that came back
     */
    public static void finish(Command command, Object response) {
        if (LOG.isTraceEnabled()) {
            try {
                String cmdStr = CommandTraceUtil.getCommandString(command);
                String config = CommandTraceUtil.getConfigString(command);
                String respStr = CommandTraceUtil.getCommandResponseString(response);
                LOG.trace(CommI18NResourceKeys.TRACE_OUTGOING_COMMAND_FINISH, cmdStr, config, respStr);
            } catch (Throwable t) {
                // don't bomb if for some reason we fail to log the trace message (should never happen)
            }
        }
    }
}