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

package org.rhq.scripting.javascript;

import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.javascript.engine.RhinoScriptEngine;
import org.rhq.scripting.javascript.util.ScriptSourceToModuleSourceProviderAdapter;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class JsEngineInitializer implements ScriptEngineInitializer {

    private static final String WRAPPED_EXCEPTION_PREFIX = "Wrapped ";

    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public ScriptEngine instantiate(final Set<String> packages, final ScriptSourceProvider scriptSourceProvider,
        PermissionCollection permissions) throws ScriptException {
        
        if (permissions == null) {
            return instantiateUnsecured(packages, scriptSourceProvider);
        } else {
            try {
                CodeSource cs = new CodeSource(null, (Certificate[]) null);
                ProtectionDomain securityDomain = new ProtectionDomain(cs, permissions);

                AccessControlContext acc = new AccessControlContext(new ProtectionDomain[] { securityDomain });

                return AccessController.doPrivileged(new PrivilegedExceptionAction<ScriptEngine>() {
                    @Override
                    public ScriptEngine run() throws Exception {
                        return instantiateUnsecured(packages, scriptSourceProvider);
                    }
                }, acc);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ScriptException) {
                    throw (ScriptException) e.getCause();
                } else {
                    throw new ScriptException("Script execution failed.");
                }
            }
        }
    }

    @Override
    public Set<String> generateIndirectionMethods(String boundObjectName, Set<Method> methods) {
        if (methods.size() == 0) {
            return Collections.emptySet();
        }

        String methodName = methods.iterator().next().getName();

        StringBuilder functionBuilder = new StringBuilder("function ");
        functionBuilder.append(methodName).append("() { switch(arguments.length) { ");

        for (Method method : methods) {
            int argCnt = method.getParameterTypes().length;
            functionBuilder.append("case ").append(argCnt).append(": ");
            functionBuilder.append("return ").append(boundObjectName).append(".").append(methodName).append("(");
            for (int i = 0; i < argCnt; ++i) {
                if (i > 0) {
                    functionBuilder.append(", ");
                }

                functionBuilder.append("arguments[").append(i).append("]");
            }

            functionBuilder.append("); break; ");
        }

        functionBuilder.append(" default: throw \"Unsupported number of parameters.\"; } }");

        return Collections.singleton(functionBuilder.toString());
    }

    @Override
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

    private ScriptEngine instantiateUnsecured(Set<String> packages, ScriptSourceProvider scriptSourceProvider)
        throws ScriptException {
        RhinoScriptEngine eng = (RhinoScriptEngine) engineManager.getEngineByName("rhino-nonjdk");
        
        if (eng == null) {
            throw new IllegalStateException("Failed to instantiate the 'rhino-nonjdk' script engine. This means that either the required library is missing from the classpath or that there are some security issues preventing it from being instantiated.");
        }
        
        if (scriptSourceProvider != null) {
            eng.setModuleSourceProvider(new ScriptSourceToModuleSourceProviderAdapter(scriptSourceProvider));
        }

        for (String pkg : packages) {
            eng.eval("importPackage(" + pkg + ")");
        }

        return eng;
    }
}
