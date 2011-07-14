/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.bindings.engine;

import java.lang.reflect.Method;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Is able to instantiate a script engine and import packages into the context
 * of the engine. 
 *
 * @author Lukas Krejci
 */
public interface ScriptEngineInitializer {

    boolean implementsLanguage(String language);
    
    ScriptEngine instantiate(Set<String> packages) throws ScriptException;
    
    /**
     * This function returns a definition string in the script engine's language
     * that provides an indirection to calling the method on the bound object.
     * 
     * for example for parameters:
     * <ul>
     * <li> <code>boundObjectName = foo</code>
     * <li> <code> method = &lt;int bar(int)&gt;</code>
     * </ul>
     * The method would generate this javascript:<br/>
     * <code>
     * function bar(arg) { return foo.bar(arg); }
     * </code>
     * 
     * @param boundObjectName
     * @param method
     * @return a string with method definition in the scripting language
     */
    String generateIndirectionMethod(String boundObjectName, Method method);
    
    /**
     * At least the Rhino script engine for java script generates exceptions
     * whose error messages contain just "too much" information to be easily
     * decipherable by the end users.
     * <p>
     * This method extracts messages from the exception such that they are
     * presentable to the end user.
     * <p>
     * The returned string should only contain the error message. The filename, line
     * and column information should be stripped from it if at all possible.
     * 
     * @param e
     * @return
     */
    String extractUserFriendlyErrorMessage(ScriptException e);
}
