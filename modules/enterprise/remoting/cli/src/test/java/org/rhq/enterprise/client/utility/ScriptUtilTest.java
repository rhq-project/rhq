package org.rhq.enterprise.client.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

    @Test
    public void getFileBytes() {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("func", "function func() { return 123; }");

        ScriptUtil scriptUtil = new ScriptUtil(scriptEngine);

        try {
            String fileContents = "the rain in spain falls mainly on the plain";
            String tempDir = System.getProperty("java.io.tmpdir");
            File testFile = new File(tempDir, "getFileBytesTest.txt");
            byte[] contentBytes = fileContents.getBytes();
            FileOutputStream writer = new FileOutputStream(testFile);
            writer.write(contentBytes);
            writer.close();
            System.out.println("Test data written to " + testFile.getAbsolutePath());

            byte[] fileBytes = scriptUtil.getFileBytes(testFile.getAbsolutePath());
            String results = new String(fileBytes);

            System.out.println("Wrote: \"" + fileContents + "\"\nRead:  \"" + results + "\"");
            assert fileContents.equals(results) : "Wrote: \"" + fileContents + "\"\nRead:  \"" + results + "\"";
        } catch (IOException ioe) {
            assert false : "Failure testing getFileBytes: " + ioe.getMessage();
        }
    }

    ScriptEngine createScriptEngine() {
        ScriptEngineManager scriptEngineMgr = new ScriptEngineManager();
        return scriptEngineMgr.getEngineByName("JavaScript");
    }

}
