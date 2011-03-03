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

package org.rhq.bindings.util;

import javax.script.ScriptException;

/**
 * This is a wrapper exception class for {@link java.lang.AssertionError}. The default (i.e., rhino) scripting engine
 * catches any exception, checked or unchecked, thrown from a script and wraps it in an instance of
 * {@link javax.script.ScriptException}. ScriptException provides useful context information such as the script name
 * and line number on which the exception occurred. For reasons unknown instances of {@link Error} are not caught and
 * wrapped in a ScriptException; consequently, no context information is provided about the error, particularly, a line
 * number.
 */
public class ScriptAssertionException extends RuntimeException {

//    public ScriptAssertionException() {
//        super();
//    }
//
//    public ScriptAssertionException(String message) {
//        super(message);
//    }
//
//    public ScriptAssertionException(String message, Throwable cause) {
//        super(message, cause);
//    }
//
//    public ScriptAssertionException(Throwable cause) {
//        super(cause);
//    }

    public ScriptAssertionException(AssertionError error) {
        super(new ScriptException(error.getMessage()));
    }
}
