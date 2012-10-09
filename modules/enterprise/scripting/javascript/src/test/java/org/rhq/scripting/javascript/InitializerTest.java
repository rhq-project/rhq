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

package org.rhq.scripting.javascript;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.FilePermission;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Permissions;
import java.util.Collections;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.Test;

import org.rhq.scripting.ScriptEngineInitializer;
import org.rhq.scripting.ScriptSourceProvider;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class InitializerTest {

    //for Rhino to see this class, it must be public
    public static class TestClass {
        public boolean[] functionsCalled;
        
        public TestClass() {
            functionsCalled = new boolean[6];
        }
        
        public void func1() {
            functionsCalled[0] = true;
        }
        
        public void func2() {
            functionsCalled[1] = true;
        }
        
        public void func3() {
            functionsCalled[2] = true;
        }
        
        public void func3(int a, int b) {
            functionsCalled[3] = true;
        }
        
        public void func3(String a, String b) {
            functionsCalled[4] = true;
        }
        
        public void func3(int a, int b, int c) {
            functionsCalled[5] = true;
        }                
    }
    
    public void engineSecured() throws Exception {
        Permissions perms = new Permissions();
        //we need to be able to read files and props so that the default module source provider can work
        perms.add(new FilePermission("<<ALL FILES>>", "read"));
        perms.add(new PropertyPermission("*", "read"));
        
        ScriptEngine eng = new JsEngineInitializer().instantiate(Collections.<String>emptySet(), perms);
        
        try {
            eng.eval("java.lang.System.exit(1)");
        } catch (Exception e) {
            assertSecurityExceptionPresent(e);
        }
    }
    
    public void scriptSourceProviderApplied() throws Exception {
        
        String script = "var m = require('rhq:/test-module1'); m.func1();";
        
        //first let's try to find the scripts with the default source provider...
        ScriptEngine eng = new JsEngineInitializer().instantiate(Collections.<String>emptySet(), null);
        try {
            eng.eval(script);
            fail("The module should not have been loaded using the default source provider.");
        } catch (ScriptException e) {
            //expected
        }
        
        eng = new JsEngineInitializer().instantiate(Collections.<String>emptySet(), null);
        new JsEngineInitializer().installScriptSourceProvider(eng, new ScriptSourceProvider() {
            @Override
            public Reader getScriptSource(URI location) {
                if (!"rhq".equals(location.getScheme())) {
                    return null;
                }
                String scriptName = location.getPath().substring(1); //remove the '/'
                InputStream src = getClass().getClassLoader().getResourceAsStream(scriptName);
                return new InputStreamReader(src);
            }
        });

        try {
            eng.eval(script);
        } catch (ScriptException e) {
            fail("The module should have been loaded using the custom source provider. Error message: " + e.getMessage(), e);
        }
    }
    
    public void indirectionMethodsValid() throws Exception {
        JsEngineInitializer initializer = new JsEngineInitializer();
        
        ScriptEngine eng = initializer.instantiate(Collections.<String>emptySet(), null);
        
        TestClass myObject = new TestClass();
                
        eng.put("myObject", myObject);
        
        generateIndirectionMethods(initializer, eng, myObject, "myObject", "func1");
        generateIndirectionMethods(initializer, eng, myObject, "myObject", "func2");
        generateIndirectionMethods(initializer, eng, myObject, "myObject", "func3");
        
        eng.eval("func1(); func2(); func3(); func3(1, 1); func3('a', 'b'); func3(1, 1, 1);");
        
        assertTrue(myObject.functionsCalled[0], "Function func1() should have been called.");
        assertTrue(myObject.functionsCalled[1], "Function func2() should have been called.");
        assertTrue(myObject.functionsCalled[2], "Function func3() should have been called.");
        assertTrue(myObject.functionsCalled[3], "Function func3(int, int) should have been called.");
        assertTrue(myObject.functionsCalled[4], "Function func3(String, String) should have been called.");
        assertTrue(myObject.functionsCalled[5], "Function func3(int, int, int) should have been called.");
    }
    
    private void assertSecurityExceptionPresent(Throwable t) {
        boolean ok = false;
        while (t != null) {
            if (t instanceof SecurityException) {
                ok = true;
                break;
            } else if ((t instanceof ScriptException) && 
                (t.getMessage().contains("java.security.AccessControlException")
                 || t.getMessage().contains("java.lang.SecurityException"))) {
                ok = true;
                break;
            }
            
            t = t.getCause();
        }
        
        assertTrue(ok, "Didn't find a SecurityException, which should have occured.");
    }
    
    private void generateIndirectionMethods(ScriptEngineInitializer initializer, ScriptEngine eng,  Object object, String objectName, String methodName) throws ScriptException {
        Set<Method> methods = new HashSet<Method>();
        for(Method m : object.getClass().getDeclaredMethods()) {
            if (methodName.equals(m.getName())) {
                methods.add(m);
            }
        }
        
        eng.put(objectName, object);
        
        for(String m : initializer.generateIndirectionMethods(objectName, methods)) {
            eng.eval(m);
        }
    }
}
