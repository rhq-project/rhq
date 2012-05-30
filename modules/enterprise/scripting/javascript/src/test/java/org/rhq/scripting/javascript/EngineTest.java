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

import static org.testng.Assert.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.testng.annotations.Test;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class EngineTest {

    public void engineAvailable() throws Exception {
        assertNotNull(getScriptEngine(), "Failed to obtain the script engine.");
    }
    
    public void printFunctionsAvailable() throws Exception {
        String output = captureScriptOutput("print('a'); println('b');");
        assertEquals(output, "ab\n", "Unexpected output printed by the print functions.");
    }
    
    public void importClassAndImportPackageAvailable() throws Exception {
        String script = "importClass(java.lang.System); print(System.currentTimeMillis());";
        String output = captureScriptOutput(script);
        assertFalse(output.isEmpty(), "importClass function doesn't seem to work.");
        
        script = "importPackage(java.util); println(new Date);";
        output = captureScriptOutput(script);
        assertFalse(output.isEmpty());
    }
    
    public void requireFunctionDefined() throws Exception {
        String output = captureScriptOutput("print(typeof(require))");
        assertEquals(output, "function", "The require function doesn't seem to be defined.");
    }
    
    public void modulesCanBeLoaded() throws Exception {
        String script = "" + 
        "var m1 = require('target/test-classes/test-module1.js'); \n" + 
        "var m2 = require('target/test-classes/test-module2.js'); \n" + 
        "println(typeof(m1.func1));\n" + 
        "println(typeof(m1.func2));\n" + 
        "println(typeof(m2.func3));\n" + 
        "println(typeof(m2.func4));\n" +
        "println(typeof(m2.func1));\n" + 
        "println(typeof(m2.func2));\n" + 
        "println(typeof(m1.func3));\n" + 
        "println(typeof(m1.func4));\n";
        String output = "function\nfunction\nfunction\nfunction\nundefined\nundefined\nundefined\nundefined\n";
        
        assertEquals(captureScriptOutput(script), output, "Unexpected functions found in modules");
    }

    private ScriptEngine getScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName("rhino-nonjdk");               
    }
    
    private String captureScriptOutput(String script) throws Exception {
        ScriptEngine engine = getScriptEngine();
        
        StringWriter stdOut = new StringWriter();
        PrintWriter wrt = new PrintWriter(stdOut);
        
        engine.getContext().setWriter(wrt);
        
        engine.eval(script);
        
        return stdOut.toString();
    }
}
