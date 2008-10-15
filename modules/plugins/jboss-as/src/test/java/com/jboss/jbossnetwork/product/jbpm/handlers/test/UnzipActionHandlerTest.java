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
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import com.jboss.jbossnetwork.product.jbpm.handlers.ActionHandlerMessageLog;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployPackageStep;

/**
 * @author Jason Dobies
 */
public class UnzipActionHandlerTest {
    private File unzipToDir = new File("target" + File.separator + "unzip-location");

    private URL zipUrl;

    @BeforeMethod
    public void createUnzipDestination() {
        if (!unzipToDir.exists()) {
            unzipToDir.mkdir();
        }
    }

    @BeforeMethod
    public void loadSampleZipFile() {
        zipUrl = this.getClass().getClassLoader().getResource("handlers/UnzipActionHandlerTest-1.zip");
        assert zipUrl != null : "Test zip file could not be loaded";
    }

    @AfterMethod
    public void deleteUnzipDestination() {
        for (String deleteMe : unzipToDir.list()) {
            File doomed = new File(unzipToDir.getAbsolutePath() + File.separator + deleteMe);
            doomed.delete();
        }

        unzipToDir.delete();
    }

    @Test
    public void successfulUnzip() throws Exception {
        // Setup

        String[] parameters = new String[] { zipUrl.getFile(), unzipToDir.getAbsolutePath() };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-valid.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
        String[] unzippedFileList = unzipToDir.list();

        assert unzippedFileList.length == 2 : "Incorrect number of files unzipped. Expected: 2, Found: "
            + unzippedFileList.length;
    }

    @Test
    public void missingFile() throws Exception {
        String[] parameters = new String[] { "foo", unzipToDir.getAbsolutePath() };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-valid.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result was not indicated as a failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No message specified for failure";
                assert deployPackageStep.getStepErrorMessage().indexOf("does not exist") != -1 : "Error message does not indicate the file does not exist: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void missingDestination() throws Exception {
        String[] parameters = new String[] { zipUrl.getFile(), "foo" };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-valid.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result was not indicated as a failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No message specified for failure";
                assert deployPackageStep.getStepErrorMessage().indexOf("does not exist") != -1 : "Error message does not indicate the directory does not exist: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void invalidArchive() throws Exception {
        URL badZipUrl = this.getClass().getClassLoader().getResource("handlers/UnzipActionHandlerTest-empty.zip");

        String[] parameters = new String[] { badZipUrl.getFile(), unzipToDir.getAbsolutePath() };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-valid.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result was not indicated as a failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No message specified for failure";
                assert deployPackageStep.getStepErrorMessage().indexOf("Failed trying to unzip") != -1 : "Error message does not indicate the file could not be unzipped: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void noDestinationInProcess() throws Exception {
        String[] parameters = new String[] { zipUrl.getFile(), unzipToDir.getAbsolutePath() };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-no-destination.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result was not indicated as a failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No message specified for failure";
                assert deployPackageStep.getStepErrorMessage().indexOf("is not set") != -1 : "Error message does not indicate the missing parameter: "
                    + deployPackageStep.getStepErrorMessage();
                assert deployPackageStep.getStepErrorMessage().indexOf("destinationDirectoryLocation") != -1 : "Error message does not indicate the name of the parameter: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void noZipFileInProcess() throws Exception {
        String[] parameters = new String[] { zipUrl.getFile(), unzipToDir.getAbsolutePath() };
        String process = HandlerTestUtils.getProcessAsString("handlers/UnzipActionHandlerTest-no-zip-file.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result was not indicated as a failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No message specified for failure";
                assert deployPackageStep.getStepErrorMessage().indexOf("is not set") != -1 : "Error message does not indicate the missing parameter: "
                    + deployPackageStep.getStepErrorMessage();
                assert deployPackageStep.getStepErrorMessage().indexOf("fileToBeUnzippedLocation") != -1 : "Error message does not indicate the name of the parameter: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }
}