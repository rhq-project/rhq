/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.scripting.python;

import java.lang.reflect.Method;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.util.SandboxedScriptEngine;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PythonScriptEngineInitializer implements ScriptEngineInitializer {

    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public ScriptEngine instantiate(Set<String> packages, ScriptSourceProvider scriptSourceProvider,
        PermissionCollection permissions) throws ScriptException {

        ScriptEngine eng = engineManager.getEngineByName("python");

        for (String pkg : packages) {
            eng.eval("from " + pkg + " import *\n");
        }

        //TODO add support for script source providers... possibly using http://www.python.org/dev/peps/pep-0302/
        
        //fingers crossed we can secure jython like this
        return new SandboxedScriptEngine(eng, permissions);
    }

    @Override
    public Set<String> generateIndirectionMethods(String boundObjectName, Set<Method> overloadedMethods) {
        if (overloadedMethods == null || overloadedMethods.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> argCnts = new HashSet<Integer>();
        for(Method m : overloadedMethods) {
            argCnts.add(m.getParameterTypes().length);
        }
        
        String methodName = overloadedMethods.iterator().next().getName();
        StringBuilder functionBody = new StringBuilder();

        functionBody.append("def ").append(methodName).append("(*args, **kwargs):\n");
        functionBody.append("\t").append("if len(kwargs) > 0:\n");
        functionBody.append("\t\t").append("raise ValueError(\"Named arguments not supported for Java methods\")\n");
        functionBody.append("\t").append("argCnt = len(args)\n");
                
        for(Integer argCnt : argCnts) {
            functionBody.append("\t").append("if argCnt == ").append(argCnt).append(":\n");
            functionBody.append("\t\treturn ").append(boundObjectName).append(".").append(methodName).append("(");
            int last = argCnt - 1;
            for(int i = 0; i < argCnt; ++i) {
                functionBody.append("args[").append(i).append("]");
                if (i < last) {
                    functionBody.append(", ");
                }
            }
            functionBody.append(")\n");            
        }

        return Collections.singleton(functionBody.toString());
    }

    @Override
    public String extractUserFriendlyErrorMessage(ScriptException e) {
        return e.getMessage();
    }

}
