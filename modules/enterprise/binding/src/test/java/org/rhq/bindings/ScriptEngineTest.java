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

package org.rhq.bindings;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.Test;

import org.rhq.bindings.util.PackageFinder;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ScriptEngineTest {

    private static StandardBindings EMPTY_BINDINGS = new StandardBindings(new PrintWriter(System.out), new FakeRhqFacade());
        
    @Test
    public void testFactory() throws ScriptException, IOException {
        ScriptEngine engine = getScriptEngine();
        assertNotNull(engine);
    }
    
    @Test
    public void testSandbox() throws ScriptException, IOException {
        ScriptEngine engine = getScriptEngine();
        
        SandboxedScriptEngine sandbox = new SandboxedScriptEngine(engine, new StandardScriptPermissions());
        
        try {
            sandbox.eval("java.lang.System.exit(1);");
        } catch (Exception e) {
            assertSecurityExceptionPresent(e);
        }
        
        try {
            //try hard to get to the System.exit()
            sandbox.eval(
                "cls = java.lang.Class.forName('java.lang.System');" +
                "params = java.lang.reflect.Array.newInstance(java.lang.Object, 1);" +
                "params[0] = java.lang.Integer.valueOf('1');" +
                "st = new java.beans.Statement(cls, 'exit', params);" +
                "st.execute()");
            
        } catch (Exception e) {
            assertSecurityExceptionPresent(e);
        }
    }
    
    @Test
    public void testStandardBindings() throws ScriptException, IOException {
        ScriptEngine scriptEngine = getScriptEngine();
        
        for(String var : EMPTY_BINDINGS.keySet()) {
            boolean hasVar = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(var);
            assertTrue(hasVar, "The variable '" + var + "' is not present in the script context but should be.");
        }
    }
    
    private ScriptEngine getScriptEngine() throws ScriptException, IOException {
        return ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Collections.<File>emptyList()), EMPTY_BINDINGS);
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
}
