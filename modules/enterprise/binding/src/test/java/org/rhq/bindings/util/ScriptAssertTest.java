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

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import org.rhq.bindings.util.ScriptAssert;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptAssertTest {

    @Test
    public void dummyTest() {
        // Dummy test while the other tests are disabled
    }

    //    @BeforeClass
    public void verifyScriptEngineIsAvailable() {
        assertNotNull(createScriptEngine(), "ScriptEngine is not available. Are the required libraries on the classpath?");
    }

    //    @Test
    public void assertExistsShouldReturnTrueWhenVariableIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("foo", "bar");

        ScriptAssert scriptAssert = new ScriptAssert(scriptEngine);

        try {
            scriptAssert.assertExists("foo");
            assert true;
        } catch (AssertionError ae) {
            assert false : "Expected isDefined() to return true when the variable is bound.";
        }
    }

    //    @Test(expectedExceptions={ScriptAssertionException.class})
    public void assertExistsShouldReturnFalseWhenVariableIsNotBound() {
        ScriptEngine scriptEngine = createScriptEngine();

        ScriptAssert scriptAssert = new ScriptAssert(scriptEngine);

        scriptAssert.assertExists("foo");
    }

    //    @Test
    public void assertExistsShouldReturnTrueWhenFunctionIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("func", "function func() { return 123; }");

        ScriptAssert scriptAssert = new ScriptAssert(scriptEngine);

        try {
            scriptAssert.assertExists("func");
            assert true;
        } catch (AssertionError ae) {
            assert false : "Expected isDefined() to return true when function is bound.";
        }
    }

    ScriptEngine createScriptEngine() {
        ScriptEngineManager scriptEngineMgr = new ScriptEngineManager();
        return scriptEngineMgr.getEngineByName("JavaScript");
    }

}
