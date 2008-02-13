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

import java.io.Serializable;

/**
 * The interface to all command responses.
 *
 * @author John Mazzitelli
 */
public interface CommandResponse extends Serializable {
    /**
     * Returns <code>true</code> if the command was executed successfully, <code>false</code> if any error occurred that
     * caused the command to fail.
     *
     * @return <code>true</code> if the command was successful, <code>false</code> otherwise
     *
     * @see    #getResults()
     */
    boolean isSuccessful();

    /**
     * If the command was issued with {@link Command#isCommandInResponse()} set to <code>true</code>, this will return a
     * copy of the {@link Command} object that was executed. This is helpful in allowing for the client processing the
     * response to know what command was executed.
     *
     * <p>To determine the actual results of the executed command, see {@link #getResults()}.</p>
     *
     * @return the command that was executed or <code>null</code> if not available
     */
    Command getCommand();

    /**
     * Provides the command's results. This may or may not be <code>null</code>, irregardless of whether the command was
     * successful or not. The return value of this method may or may not be valid if {@link #isSuccessful()} returns
     * <code>false</code>. It is up to each implementor of this interface to determine the semantics of these
     * conditions.
     *
     * @return the command execution results
     */
    Object getResults();

    /**
     * If a command fails to execute, this may provide the exception (more specifically, a <code>
     * java.lang.Throwable</code>) that caused the failure. Typically, the returned value will be <code>null</code> if
     * {@link #isSuccessful()} returns <code>true</code> and will be non-<code>null</code> if {@link #isSuccessful()}
     * returns <code>false</code>. However, it is up to the implementor of this interface to determine whether or not to
     * follow these guidelines. Implementors of this interface will determine when or if it will supply an exception.
     * Note that it might be valid to have both a non-<code>null</code> exception <i>and</i> a
     * non-<code>null</code>{@link #getResults() result}.
     *
     * <p>Note that the exception may or may not have occurred on the server that executed the command.</p>
     *
     * @return an exception that caused the command to fail, or <code>null</code>
     */
    Throwable getException();
}