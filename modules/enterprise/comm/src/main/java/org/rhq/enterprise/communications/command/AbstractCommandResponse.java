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

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Superclass to all {@link Command} responses.
 *
 * <p>Note that all setters in this class are not <code>public</code> to prohibit users of this class from arbitrarily
 * modifying the response once it has been created.</p>
 *
 * @author John Mazzitelli
 */
public abstract class AbstractCommandResponse implements CommandResponse {
    /**
     * flag to indicate if the command was successfully executed or not
     */
    private boolean m_successful;

    /**
     * the actual response data that resulted from the command execution
     */
    private Object m_results;

    /**
     * an exception that typically is the result of a failed command
     */
    private Throwable m_exception;

    /**
     * if the command that was executed had requested itself to be saved in the response, this will be it
     */
    private Command m_command;

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link AbstractCommandResponse} to initialize this response with empty results. This sets the
     * {@link #isSuccessful()} flag to <code>true</code>, which assumes that the command was successful unless told
     * otherwise.
     *
     * <p>Note that if <code>command</code> is <code>null</code>, this response object will not store any command for
     * the client to review (which may or may not be what the client wanted - see {@link Command#isCommandInResponse()}
     * .</p>
     *
     * @param command the command that was executed (may be <code>null</code>)
     */
    public AbstractCommandResponse(Command command) {
        this(command, true, null, null);
    }

    /**
     * Constructor for {@link AbstractCommandResponse} that allows all fields to be initialized.
     *
     * <p>Note that if <code>command</code> is <code>null</code>, this response object will not store any command for
     * the client to review (which may or may not be what the client wanted - see {@link Command#isCommandInResponse()}
     * .</p>
     *
     * @param command   the command that was executed (may be <code>null</code>)
     * @param success   the flag to indicate if the command was successful or not
     * @param results   the results of the command (may be <code>null</code>)
     * @param exception an exception that is typically the result of a failed command (may be <code>null</code>)
     */
    public AbstractCommandResponse(Command command, boolean success, Object results, Throwable exception) {
        setCommand(command);
        setSuccessful(success);
        setResults(results);
        setException(exception);

        return;
    }

    /**
     * Constructor for {@link AbstractCommandResponse} that acts as a copy constructor and a command response
     * decorator/transformer. The given command reponse's parameter values will be copied to this new command response
     * object.
     *
     * <p>Transforming a command response allows the caller to "decorate" the given response with a concrete command
     * response implementation's API (which typically has more strongly typed methods to extract out result object
     * data).</p>
     *
     * <p>This is typically used when the given command response is a generic command response (one with no strongly
     * typed accessor methods to retrieve data from the result object for example) and the caller wants to convert it to
     * a more concrete command response implementation. This is usually due to the fact that the creator of the given
     * command object and its response did not know at compile time the specific concrete command type it needed.</p>
     *
     * <p>The only thing a subclass must do in order to support this transformer constructor is to override it and call
     * it via <code>super</code>.</p>
     *
     * @param responseToTransform the command response object to transform into this class type
     */
    public AbstractCommandResponse(CommandResponse responseToTransform) {
        this(responseToTransform.getCommand(), responseToTransform.isSuccessful(), responseToTransform.getResults(),
            responseToTransform.getException());
    }

    /**
     * @see CommandResponse#isSuccessful()
     */
    public boolean isSuccessful() {
        return m_successful;
    }

    /**
     * Sets the flag to indicate if the command execution was successful or not.
     *
     * <p>Note the scope of this method is <code>protected</code> - only subclasses will be able to modify this
     * flag.</p>
     *
     * @param success <code>true</code> if command was successful, <code>false</code> otherwise
     *
     * @see   #isSuccessful()
     */
    protected void setSuccessful(boolean success) {
        m_successful = success;
    }

    /**
     * @see CommandResponse#getResults()
     */
    public Object getResults() {
        return m_results;
    }

    /**
     * Sets the response data that was the result of the command execution. Note that this value may or may not be
     * <code>null</code>, irregardless of whether the command execution was successful or not. The semantics of this is
     * determined by each implementor of this object.
     *
     * @param results the command response data
     */
    protected void setResults(Object results) {
        m_results = results;
    }

    /**
     * Returns the exception that was the cause of a failed command. This typically will be <code>null</code> if the
     * command {@link #isSuccessful() succeeded} and will be non-<code>null</code> if the command failed. Subclasses are
     * free to define when or if this exception value is set.
     *
     * @return exception an exception that was the cause of a failed command
     */
    public Throwable getException() {
        return m_exception;
    }

    /**
     * Sets the exception that was the result of a failed command. Subclasses may or may not decide to include an
     * exception, even if the command failed.
     *
     * @param exception an exception that was the result of a failed command (may be <code>null</code>)
     */
    protected void setException(Throwable exception) {
        m_exception = exception;
    }

    /**
     * @see CommandResponse#getCommand()
     */
    public Command getCommand() {
        return m_command;
    }

    /**
     * Sets the command that was executed and whose results are being returned in this response object. Note that if the
     * {@link Command#isCommandInResponse()} is <code>false</code>, this method does nothing.
     *
     * @param command the command that was executed
     */
    protected void setCommand(Command command) {
        if ((command != null) && command.isCommandInResponse()) {
            m_command = command;
        }

        return;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer strBuf = new StringBuffer("Command Response: isSuccessful=[");
        strBuf.append(isSuccessful());
        strBuf.append("]; command=[");
        strBuf.append(getCommand());
        strBuf.append("]; results=[");
        strBuf.append(getResults());
        strBuf.append("]; exception=[");
        strBuf.append(ThrowableUtil.getAllMessages(getException(), true));
        strBuf.append("]");

        return strBuf.toString();
    }
}