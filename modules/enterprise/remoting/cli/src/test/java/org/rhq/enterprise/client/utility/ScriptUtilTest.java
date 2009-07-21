package org.rhq.enterprise.client.utility;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.testng.annotations.Test;

public class ScriptUtilTest {

    @Test
    public void isDefinedShouldReturnTrueWhenVariableIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("foo", "bar");

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        try {
            scriptUtil.assertExists("foo");
            assert true;
        } catch (AssertionError ae) {
            assert false : "Expected isDefined() to return true when the variable is bound.";
        }
    }

    @Test
    public void isDefinedShouldReturnFalseWhenVariableIsNotBound() {
        ScriptEngine scriptEngine = createScriptEngine();

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        try {
            scriptUtil.assertExists("foo");
            assert false : "Expected isDefined() to return false when the variable is not bound.";
        } catch (AssertionError ae) {
            assert true;
        }
    }

    @Test
    public void isDefinedShouldReturnTrueWhenFunctionIsBound() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("func", "function func() { return 123; }");

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        try {
            scriptUtil.assertExists("func");
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
