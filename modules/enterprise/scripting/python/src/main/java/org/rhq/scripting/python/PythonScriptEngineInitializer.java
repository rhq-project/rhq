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
import java.util.Properties;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.util.SandboxedScriptEngine;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PythonScriptEngineInitializer implements ScriptEngineInitializer {

    private static final Log LOG = LogFactory.getLog(PythonScriptEngineInitializer.class);

    static {
        Properties props = new Properties();
        props.put("python.packages.paths", "java.class.path,sun.boot.class.path");
        props.put("python.packages.directories", "java.ext.dirs");
        props.put("python.cachedir.skip", false);
        PythonInterpreter.initialize(System.getProperties(), props, null);
    }

    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public ScriptEngine instantiate(Set<String> packages, PermissionCollection permissions) throws ScriptException {

        ScriptEngine eng = engineManager.getEngineByName("python");

        //XXX this might not work perfectly in jython
        //but we can't make it work perfectly either, so let's just
        //keep our fingers crossed..
        //http://www.jython.org/jythonbook/en/1.0/ModulesPackages.html#from-import-statements
        for (String pkg : packages) {
            try {
                eng.eval("from " + pkg + " import *\n");
            } catch (ScriptException e) {
                //well, let's just keep things going, this is not fatal...
                LOG.info("Python script engine could not pre-import members of package '" + pkg + "'.");
            }
        }

        //fingers crossed we can secure jython like this
        return permissions == null ? eng : new SandboxedScriptEngine(eng, permissions);
    }

    @Override
    public void installScriptSourceProvider(ScriptEngine scriptEngine, ScriptSourceProvider provider) {
        PySystemState sys = Py.getSystemState();
        if (sys != null) {
            sys.path_hooks.append(new PythonSourceProvider(provider));
        }
    }

    @Override
    public Set<String> generateIndirectionMethods(String boundObjectName, Set<Method> overloadedMethods) {
        if (overloadedMethods == null || overloadedMethods.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> argCnts = new HashSet<Integer>();
        for (Method m : overloadedMethods) {
            argCnts.add(m.getParameterTypes().length);
        }

        String methodName = overloadedMethods.iterator().next().getName();
        StringBuilder functionBody = new StringBuilder();

        functionBody.append("def ").append(methodName).append("(*args, **kwargs):\n");
        functionBody.append("\t").append("if len(kwargs) > 0:\n");
        functionBody.append("\t\t").append("raise ValueError(\"Named arguments not supported for Java methods\")\n");
        functionBody.append("\t").append("argCnt = len(args)\n");

        for (Integer argCnt : argCnts) {
            functionBody.append("\t").append("if argCnt == ").append(argCnt).append(":\n");
            functionBody.append("\t\treturn ").append(boundObjectName).append(".").append(methodName).append("(");
            int last = argCnt - 1;
            for (int i = 0; i < argCnt; ++i) {
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
