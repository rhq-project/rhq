package org.rhq.enterprise.client.utility;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

public class ScriptUtilTest {

    @Test
    public void isDefinedShouldReturnTrueWhenVariableIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("foo", "bar");

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        assertTrue(scriptUtil.isDefined("foo"), "Expected isDefined() to return true when the variable is bound.");
    }

    @Test
    public void isDefinedShouldReturnFalseWhenVariableIsNotBound() {
        ScriptEngine scriptEngine = createScriptEngine();

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        assertFalse(scriptUtil.isDefined("foo"), "Expected isDefined() to return false when the variable is not bound.");
    }

    @Test
    public void isDefinedShouldReturnTrueWhenFunctionIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("func", "function func() { return 123; }");

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        assertTrue(scriptUtil.isDefined("func"), "Expected isDefined() to return true when function is bound.");
    }

    ScriptEngine createScriptEngine() {
        ScriptEngineManager scriptEngineMgr = new ScriptEngineManager();
        return scriptEngineMgr.getEngineByName("JavaScript");        
    }

}
