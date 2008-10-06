 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.util.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * This class can wrap up an exception in a non-object/serialized form to send to a remote system that might not have
 * the exception class available for deserialization. It extends {@link RuntimeException} so it can be thrown as a
 * normal exception to a remote VM without requiring it to be part of a remoting API signature.
 *
 * <p>Note that most of the {@link RuntimeException} methods are overridden by this class to work on the
 * {@link #getActualException() actual exception}. However, {@link #initCause(Throwable)}, {@link #fillInStackTrace()},
 * {@link #getStackTrace()} and {@link #setStackTrace(StackTraceElement[])} will operate on this exception object
 * itself, <i>not</i> of the {@link #getActualException() actual exception}. Typically, the use-cases in which this
 * exception object is to be used will not require those methods to be needed.</p>
 *
 * @author John Mazzitelli
 */
public class WrappedRemotingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * The actual exception that occurred.
     */
    private final ExceptionPackage actualException;

    /**
     * Constructor for {@link WrappedRemotingException} that wraps the actual exception information.
     *
     * @param  actualException the actual exception that occurred (must not be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>actualException</code> is <code>null</code>
     */
    public WrappedRemotingException(ExceptionPackage actualException) throws IllegalArgumentException {
        if (actualException == null) {
            throw new IllegalArgumentException("actualException == null");
        }

        this.actualException = actualException;
    }

    /**
     * Constructor for {@link WrappedRemotingException} that takes the actual exception and wraps it in a
     * {@link ExceptionPackage}. The severity will be that used by {@link ExceptionPackage#ExceptionPackage(Throwable)}.
     *
     * @param  throwable the actual exception
     *
     * @throws IllegalArgumentException if <code>throwable</code> is <code>null</code>
     */
    public WrappedRemotingException(Throwable throwable) throws IllegalArgumentException {
        this(new ExceptionPackage(throwable));
    }

    /**
     * Constructor for {@link WrappedRemotingException} that takes the actual exception and wraps it in a
     * {@link ExceptionPackage}. If <code>severity</code> is <code>null</code>, then it will be assigned a default (see
     * {@link ExceptionPackage#ExceptionPackage(Severity, Throwable)}).
     *
     * @param  severity  the severity of the exception (may be <code>null</code>)
     * @param  throwable the actual exception
     *
     * @throws IllegalArgumentException if <code>throwable</code> is <code>null</code>
     */
    public WrappedRemotingException(Severity severity, Throwable throwable) throws IllegalArgumentException {
        this(new ExceptionPackage(severity, throwable));
    }

    /**
     * Returns the actual exception that occurred wrapped in an {@link ExceptionPackage}.
     *
     * @return the information about the actual exception that occurred
     */
    public ExceptionPackage getActualException() {
        return actualException;
    }

    /**
     * Returns the message of the {@link #getActualException() actual exception}.
     *
     * @return action exception message
     */
    public String getMessage() {
        return actualException.getMessage();
    }

    /**
     * If the actual exception had a cause, this will return a {@link WrappedRemotingException} wrapping the cause's
     * {@link ExceptionPackage}.
     *
     * @see java.lang.Throwable#getCause()
     */
    public Throwable getCause() {
        ExceptionPackage cause = actualException.getCause();

        if (cause == null) {
            return null;
        }

        return new WrappedRemotingException(cause);
    }

    /**
     * Returns the {@link ExceptionPackage#toString()} of the {@link #getActualException() actual exception}.
     *
     * @see java.lang.Throwable#toString()
     */
    public String toString() {
        return actualException.toString();
    }

    /**
     * Prints the stack trace of the {@link #getActualException() actual exception} to <code>System.err</code>.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace of the {@link #getActualException() actual exception}.
     *
     * @param s the stream where the stack trace will be written to
     */
    public void printStackTrace(PrintStream s) {
        s.print("[" + actualException.getSeverity() + "] ");
        s.println(actualException.getStackTraceString());
    }

    /**
     * Prints the stack trace of the {@link #getActualException() actual exception}.
     *
     * @param w the writer that will print the stack trace
     */
    public void printStackTrace(PrintWriter w) {
        w.print("[" + actualException.getSeverity() + "] ");
        w.println(actualException.getStackTraceString());
    }
}