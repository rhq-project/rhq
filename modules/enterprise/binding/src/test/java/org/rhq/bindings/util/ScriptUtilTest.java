package org.rhq.bindings.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.testng.annotations.Test;

import org.rhq.bindings.util.ScriptUtil;

public class ScriptUtilTest {

    @Test
    public void getFileBytes() {
        ScriptUtil scriptUtil = new ScriptUtil(null);

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

}
