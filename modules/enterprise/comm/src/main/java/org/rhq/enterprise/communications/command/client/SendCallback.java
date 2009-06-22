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

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * This interface allows implementors to perform logic just before and just after the send.  For asynchronous
 * commands there may be a time lag between queuing and sending. And, the send may be performed on a different
 * thread than the queuing. The main purpose of this is to provide a chance to set thread-local information
 * that can affect the serialization logic.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public interface SendCallback {

    /**
     * This method provides the hook for pre-send logic. (such as setting ThreadLocals on the sender thread).
     *
     * @param command the command being sent
     */
    void sending(Command command);

    /**
     * This method provides the hook for post-send logic. The command response can be inspected/manipulated if desired.
     * If not modifying the response the method should return the passed-in <response>.
     *
     * @param command the command being sent
     * @return the command response. Not null, should return a new CommandRespose or the the passed in <response>.
     */
    CommandResponse sent(Command command, CommandResponse response);
}