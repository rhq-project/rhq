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
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class JsEngineInitializer implements ScriptEngineInitializer {

    private static final String WRAPPED_EXCEPTION_PREFIX = "Wrapped ";

    private ScriptEngineManager engineManager = new ScriptEngineManager();
    
    public boolean implementsLanguage(String language) {
        return language != null && ("JavaScript".equals(language) || "ECMAScript".equals(language));
    }
    
    public ScriptEngine instantiate(Set<String> packages) throws ScriptException {
        ScriptEngine eng = engineManager.getEngineByName("JavaScript");
        
        for(String pkg : packages) {
            eng.eval("importPackage(" + pkg + ")");
        }
        
        return eng;
    }

    public String generateIndirectionMethod(String boundObjectName, Method method) {
        String methodName = method.getName();
        int argCount = method.getParameterTypes().length;

        StringBuilder functionBuilder = new StringBuilder();
        functionBuilder.append(methodName).append("(");
        for (int i = 0; i < argCount; ++i) {
            if (i != 0) {
                functionBuilder.append(", ");
            }
            functionBuilder.append("arg_" + i);
        }
        functionBuilder.append(")");
        String functionFragment = functionBuilder.toString();
        boolean returnsVoid = method.getReturnType().equals(Void.TYPE);

        String functionDefinition = "function " + functionFragment + " { " + (returnsVoid ? "" : "return ")
            + boundObjectName + "." + functionFragment + "; }";
        
        return functionDefinition;
    }
    
    public String extractUserFriendlyErrorMessage(ScriptException e) {
        String errorMessage = e.getMessage();
        
        int wrappedIdx = errorMessage.lastIndexOf(WRAPPED_EXCEPTION_PREFIX);
        
        if (wrappedIdx < 0) {
            return errorMessage;
        }
        
        errorMessage = errorMessage.substring(wrappedIdx + WRAPPED_EXCEPTION_PREFIX.length());        
        
        int sourceInfoStartIdx = errorMessage.indexOf(" (<Unknown source>#");
        
        if (sourceInfoStartIdx >= 0) {
            errorMessage = errorMessage.substring(0, sourceInfoStartIdx);
        }
        
        return errorMessage;
    }
}
