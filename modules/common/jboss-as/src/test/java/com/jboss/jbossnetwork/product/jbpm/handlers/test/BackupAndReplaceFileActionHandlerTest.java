 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers.test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.jboss.jbossnetwork.product.jbpm.handlers.ActionHandlerMessageLog;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.testng.annotations.Test;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployPackageStep;

/**
 * @author Jason Dobies
 */
public class BackupAndReplaceFileActionHandlerTest {
	
	@Test
	public void oneFileReplaceTest() throws Exception {
		oneFileReplace("handlers/BackupAndReplaceFileActionHandlerTest-one-file-replace.xml");
	}
	
	@Test
	public void oneFileReplaceWithEntitiesTest() throws Exception {
		oneFileReplace("handlers/BackupAndReplaceFileActionHandlerTest-one-file-replace-with-entities.xml");
	}
	
    public void oneFileReplace(String processDefinitionLocation) throws Exception {
        // Setup
        File patchFileDir = new File("target" + File.separator + "patch-files");
        if (!patchFileDir.exists()) {
            patchFileDir.mkdir();
        }

        File currentDir = new File(".");
        String originalFileName = currentDir.getAbsolutePath() + File.separator + "target" + File.separator
            + "oneFileReplace-orig.txt";
        String replacementFileName = patchFileDir.getAbsolutePath() + File.separator + "oneFileReplace-replacement.txt";

        // Create the original file to be renamed
        HandlerTestUtils.createSampleFile(originalFileName);
        HandlerTestUtils.createSampleFile(replacementFileName);

        // Load and populate the JBPM process
        String[] parameters = new String[] { originalFileName, replacementFileName };
        String process = HandlerTestUtils.getProcessAsString(
        		processDefinitionLocation, parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date timestamp = new Date();
        String formattedTimestamp = format.format(timestamp);
        processInstance.getContextInstance().setVariable("timestamp", formattedTimestamp);

        processInstance.signal();

        // Verify

        // Check the audit trail
        List logs = processInstance.getLoggingInstance().getLogs();

        for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
            Object uncastedLog = iterator.next();
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog log = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep deployPackageStep = log.getStep();

                assert deployPackageStep != null : "Step was not found in the action message log entry: " + log;
                assert deployPackageStep.getStepResult() == ContentResponseResult.SUCCESS : "Step result was not successful. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() == null : "Non-null error message found for successful step: "
                    + deployPackageStep;
            }
        }

        // Check the files
        File writtenFile = new File(currentDir.getAbsolutePath() + File.separator
            + "target/oneFileReplace-replacement.txt");
        assert writtenFile.exists() : "File was not copied to " + writtenFile.getAbsolutePath();

        File originalFile = new File(originalFileName);
        assert !originalFile.exists() : "Original file was not removed in the process";

        File backupFile = new File(currentDir.getAbsolutePath() + File.separator + "target/oneFileReplace-orig.txt."
            + formattedTimestamp + ".old");
        assert backupFile.exists() : "Backup file was not created";

        // Cleanup, these will get removed on a clean, but in case the tests are run without a clean between
        // we don't want these hanging around

        for (String patchFile : patchFileDir.list()) {
            File deleteMe = new File(patchFile);
            deleteMe.delete();
        }

        patchFileDir.delete();

        originalFile.delete();
        writtenFile.delete();
        backupFile.delete();
    }
}