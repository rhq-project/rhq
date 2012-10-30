/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.io.FilePermission;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.security.AccessControlException;
import java.security.Permissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * @author Lukas Krejci
 */
@Test
public class PythonScriptEngineInitializerTest {

    public static class Tester {
        private int cnt = 0;

        public void increment() {
            ++cnt;
        }

        public int getInvocationCoung() {
            return cnt;
        }
    }

    private static final String EXPECTED_OUTPUT = "kachny";

    public static class SourceProvider implements ScriptSourceProvider {
        @Override
        public Reader getScriptSource(URI scriptUri) {
            if (scriptUri.toString().equals("test/test_module.py")) {
                return new StringReader("print '" + EXPECTED_OUTPUT + "'");
            }
            return null;
        }
    }
    
    public void testEngineInitialization() throws Exception {
        PythonScriptEngineInitializer initializer = new PythonScriptEngineInitializer();
        ScriptEngine engine = initializer.instantiate(Collections.<String> emptySet(), null);

        //just some code to test out this is python
        engine.eval("from java.util import HashMap\nHashMap()");
    }

    public void testMethodIndirection() throws Exception {
        PythonScriptEngineInitializer initializer = new PythonScriptEngineInitializer();
        ScriptEngine engine = initializer.instantiate(Collections.<String> emptySet(), null);

        Bindings bindings = engine.createBindings();
        Tester tester = new Tester();
        bindings.put("tester", tester);

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        engine.eval("tester.increment()");

        Assert.assertEquals(tester.getInvocationCoung(), 1, "Unexpected number of tester invocations.");

        Map<String, Set<Method>> methods = getMethodsByName(Tester.class);
        for (Set<Method> ms : methods.values()) {
            Set<String> fns = initializer.generateIndirectionMethods("tester", ms);
            for (String fn : fns) {
                engine.eval(fn);
            }
        }

        engine.eval("increment()");
        Assert.assertEquals(tester.getInvocationCoung(), 2,
            "Unexpected number of tester invocations after calling an indirected method.");
    }

    public void testSecuredEngine() throws Exception {
        PythonScriptEngineInitializer initializer = new PythonScriptEngineInitializer();

        //jython seems to need these two..
        Permissions perms = new Permissions();
        perms.add(new RuntimePermission("createClassLoader"));
        perms.add(new RuntimePermission("getProtectionDomain"));

        //add permission to read files so that modules can be loaded, but writing should fail
        perms.add(new FilePermission("<<ALL FILES>>", "read"));

        ScriptEngine engine = initializer.instantiate(Collections.<String> emptySet(), perms);

        try {
            engine.eval("import os\nfp = open('pom.xml', 'w')");
            Assert.fail("Opening a file for writing should have failed with a security exception.");
        } catch (ScriptException e) {
            checkIsCausedByAccessControlException(e);
        }
    }
    
    public void testSourceProvider() throws Exception {
        PythonScriptEngineInitializer initializer = new PythonScriptEngineInitializer();
        
        ScriptEngine engine = initializer.instantiate(Collections.<String> emptySet(), null);

        StringWriter wrt = new StringWriter();

        engine.getContext().setWriter(wrt);
        
        initializer.installScriptSourceProvider(engine, new SourceProvider());

        engine
            .eval("import sys\nsys.path.append('__rhq__:test-unsupported/')\nsys.path.append('__rhq__:test/')\nimport test_module");

        Assert.assertEquals(wrt.toString(), EXPECTED_OUTPUT + "\n", "Unexpected output from a custom module.");
    }

    private void checkIsCausedByAccessControlException(Throwable e) {
        Throwable ex = e;
        while (ex != null) {
            if (ex instanceof AccessControlException) {
                return;
            }

            ex = ex.getCause();
        }

        Assert.fail("Expected an AccessControlException but the exception doesn't seem to be caused by it.", e);
    }

    private static Map<String, Set<Method>> getMethodsByName(Class<?> cls) {
        Map<String, Set<Method>> ret = new HashMap<String, Set<Method>>();

        for (Method m : cls.getDeclaredMethods()) {
            int mods = m.getModifiers();

            if (Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
                continue;
            }

            Set<Method> methods = ret.get(m.getName());
            if (methods == null) {
                methods = new HashSet<Method>();
                ret.put(m.getName(), methods);
            }

            methods.add(m);
        }

        return ret;
    }
}
