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

/**
 * This interface allows implementors to preprocess a command before it is queued and sent. The main purpose of this is
 * to allow a preprocessor to add things like out-of-band metadata to the command's
 * {@link Command#getConfiguration() configuration}.
 *
 * @author John Mazzitelli
 */
public interface CommandPreprocessor {
    /**
     * This method provides the hook for a command preprocessor to manipulate a command (such as add things to its
     * {@link Command#getConfiguration() configuration}.
     *
     * @param command the command to preprocess
     * @param sender  the sender object that is going to queue and send the command
     */
    void preprocess(Command command, ClientCommandSender sender);
}