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

import java.net.URL;
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
public class CompareDigestActionHandlerTest {
    private String sampleDigestFile;

    {
        URL url = this.getClass().getClassLoader().getResource("handlers/CompareDigestActionHandlerTest-md5.xml");
        assert url != null : "Could not load sample file to check digest";
        sampleDigestFile = url.getFile();
    }

    private String sampleDigestFileMD5 = "c58587b872b8ed47b3fdba4734729c65";

    @Test
    public void matchingDigest() throws Exception {
        // Setup
        String[] parameters = new String[] { "MD5", sampleDigestFile, sampleDigestFileMD5 };
        String process = HandlerTestUtils.getProcessAsString("handlers/CompareDigestActionHandlerTest-md5.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
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
    }

    @Test
    public void noAlgorithmSpecified() throws Exception {
        // When no algorithm is specified, we default to MD5.

        // Setup
        String[] parameters = new String[] { sampleDigestFile, sampleDigestFileMD5 };
        String process = HandlerTestUtils.getProcessAsString(
            "handlers/CompareDigestActionHandlerTest-no-algorithm.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
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
    }

    @Test
    public void noDigestSpecified() throws Exception {
        // This test expects the workflow to fail, as the expected digest is omitted.

        // Setup
        String[] parameters = new String[] { sampleDigestFile };
        String process = HandlerTestUtils.getProcessAsString(
            "handlers/CompareDigestActionHandlerTest-no-expected-digest.xml", parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
        List logs = processInstance.getLoggingInstance().getLogs();

        for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
            Object uncastedLog = iterator.next();
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog log = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep deployPackageStep = log.getStep();

                assert deployPackageStep != null : "Step was not found in the action message log entry: " + log;
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result does not indicate failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No error message found in failing step";
                assert deployPackageStep.getStepErrorMessage().indexOf("expectedDigest") != -1 : "Error message does not indicate the missing expected digest: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void incorrectDigestValue() throws Exception {
        // Setup
        String[] parameters = new String[] { "MD5", sampleDigestFile, "fakeExpectedDigest" };
        String process = HandlerTestUtils.getProcessAsString("handlers/CompareDigestActionHandlerTest-md5.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
        List logs = processInstance.getLoggingInstance().getLogs();

        for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
            Object uncastedLog = iterator.next();
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog log = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep deployPackageStep = log.getStep();

                assert deployPackageStep != null : "Step was not found in the action message log entry: " + log;
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result did not indicate failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No error message found in failing step";
                assert deployPackageStep.getStepErrorMessage().indexOf("does not match expected") != -1 : "Error message does not indicate the incorrect digest";
            }
        }
    }

    @Test
    public void badAlgorithm() throws Exception {
        // Setup
        String[] parameters = new String[] { "foo", sampleDigestFile, sampleDigestFileMD5 };
        String process = HandlerTestUtils.getProcessAsString("handlers/CompareDigestActionHandlerTest-md5.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
        List logs = processInstance.getLoggingInstance().getLogs();

        for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
            Object uncastedLog = iterator.next();
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog log = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep deployPackageStep = log.getStep();

                assert deployPackageStep != null : "Step was not found in the action message log entry: " + log;
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result did not indicate failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No error message found in failing step";
                assert deployPackageStep.getStepErrorMessage().indexOf("java.security.NoSuchAlgorithmException") != -1 : "Error message does not indicate the bad algorithm: "
                    + deployPackageStep.getStepErrorMessage();
            }
        }
    }

    @Test
    public void noFileSpecified() throws Exception {
        // Setup
        String[] parameters = new String[] { "MD5", "", sampleDigestFileMD5 };
        String process = HandlerTestUtils.getProcessAsString("handlers/CompareDigestActionHandlerTest-md5.xml",
            parameters);

        // Test
        ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(process);
        ProcessInstance processInstance = new ProcessInstance(processDefinition);

        processInstance.signal();

        // Verify
        List logs = processInstance.getLoggingInstance().getLogs();

        for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
            Object uncastedLog = iterator.next();
            if (uncastedLog instanceof ActionHandlerMessageLog) {
                ActionHandlerMessageLog log = (ActionHandlerMessageLog) uncastedLog;
                DeployPackageStep deployPackageStep = log.getStep();

                assert deployPackageStep != null : "Step was not found in the action message log entry: " + log;
                assert deployPackageStep.getStepResult() == ContentResponseResult.FAILURE : "Step result did not indicate failure. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() != null : "No error message found in failing step";
            }
        }
    }
}