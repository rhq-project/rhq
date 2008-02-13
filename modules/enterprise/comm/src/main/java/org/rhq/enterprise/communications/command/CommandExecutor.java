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
package org.rhq.enterprise.communications.command;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to any object that can execute a {@link Command}.
 *
 * @author John Mazzitelli
 */
public interface CommandExecutor {
    /**
     * Executes the given command and returns its response.
     *
     * @param  command the command to execute
     * @param  in      input stream should the executor want to get streamed data from the client
     * @param  out     output stream should the executor want to stream data to the client
     *
     * @return the results of the command execution
     */
    CommandResponse execute(Command command, InputStream in, OutputStream out);
}