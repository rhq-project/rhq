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
public class NotificationActionHandlerTest {
    @Test
    public void oneMessage() throws Exception {
        // Setup
        String[] parameters = new String[] { "Test Notification" };

        String process = HandlerTestUtils.getProcessAsString(
            "handlers/NotificationActionHandlerTest-one-notification.xml", parameters);

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
                assert deployPackageStep.getStepResult() == ContentResponseResult.NOT_PERFORMED : "Step result was not marked as not run. Found: "
                    + deployPackageStep.getStepResult();
                assert deployPackageStep.getStepErrorMessage() == null : "Non-null error message found for not run step: "
                    + deployPackageStep;
            }
        }
    }
}