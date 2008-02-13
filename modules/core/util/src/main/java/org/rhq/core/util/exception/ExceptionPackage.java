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
package org.rhq.core.util.exception;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * This class can package up an exception in a non-object-serialized form to send to a remote system that might not have
 * the exception class available for deserialization.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @see    WrappedRemotingException
 */
public class ExceptionPackage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String exceptionName;
    private final Severity severity;
    private final String message;
    private final ExceptionPackage cause;
    private final String stackTrace;
    private final String allMessages;

    /**
     * Constructor for {@link ExceptionPackage} that assumes the severity is {@link Severity#Warning}.
     *
     * @param  throwable
     *
     * @throws IllegalArgumentException if <code>throwable</code> is <code>null</code>
     */
    public ExceptionPackage(Throwable throwable) throws IllegalArgumentException {
        this(null, throwable);
    }

    /**
     * Creates a new {@link ExceptionPackage} object. If the <code>severity</code> is <code>null</code>, then
     * {@link Severity#Warning} is assumed.
     *
     * @param  severity  the importance of the exception (may be <code>null</code>)
     * @param  throwable
     *
     * @throws IllegalArgumentException if <code>throwable</code> is <code>null</code>
     */
    public ExceptionPackage(Severity severity, Throwable throwable) throws IllegalArgumentException {
        if (throwable == null) {
            throw new IllegalArgumentException("throwable==null");
        }

        this.exceptionName = throwable.getClass().getName();
        this.severity = (severity == null) ? Severity.Warning : severity;
        this.message = throwable.getMessage();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        throwable.printStackTrace(pw);
        pw.flush();
        this.stackTrace = baos.toString();

        if (throwable.getCause() != null) {
            // this will iterate over all child causes by recursively calling the constructor
            this.cause = new ExceptionPackage(severity, throwable.getCause());
        } else {
            this.cause = null;
        }

        this.allMessages = ThrowableUtil.getAllMessages(throwable);

        return;
    }

    /**
     * Returns the name of the actual exception being packaged. This is equivalent to the <code>
     * getClass().getName()</code> value of the exception.
     *
     * @return the name of the exception
     */
    public String getExceptionName() {
        return exceptionName;
    }

    /**
     * Returns the severity of the exception, which marks how important or severe the problem is.
     *
     * @return exception severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the message associated with the exception.
     *
     * @return exception message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the cause of this exception in the same non-object-serialized form.
     *
     * @return exception cause
     */
    public ExceptionPackage getCause() {
        return cause;
    }

    /**
     * Returns the stack trace for this exception but not of its cause.
     *
     * @return the exception stack trace of this particular exception.
     */
    public String getStackTraceString() {
        return stackTrace;
    }

    /**
     * Returns all the messages for this throwable and all of its causes in one long string. This is useful for logging
     * an exception when you don't want to dump the entire stack trace but you still want to see the throwable and all
     * of its causes.
     *
     * @return string containing the throwable's message and its causes' messages in the order of the causes (the lowest
     *         nested throwable's message appears last in the string)
     */
    public String getAllMessages() {
        return allMessages;
    }

    /**
     * Returns a string consisting of the exception's name, the severity and message of the exception.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return exceptionName + ": [" + severity.toString() + "] " + message;
    }
}