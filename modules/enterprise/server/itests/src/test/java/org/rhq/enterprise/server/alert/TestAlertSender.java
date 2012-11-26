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

package org.rhq.enterprise.server.alert;

import org.testng.Assert;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderValidationResults;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class TestAlertSender extends AlertSender<ServerPluginComponent> {

    public static final String NAME = "Test Alert Sender";
    
    public static final String PERSISTENT_PROPERTY_NAME = "persistent";
    public static final String PERSISTEN_PROPERTY_EXPECTED_VALUE = "persistentephemeral";    
    public static final String EPHEMERAL_PROPERTY_NAME = "ephemeral";
    
    private static Subject EXPECTED_SUBJECT;
    private static volatile int VALIDATE_METHOD_CALL_COUNT;
    private static Runnable VALIDATION_CHECKER;
    
    public static void setExpectedSubject(Subject subject) {
        EXPECTED_SUBJECT = subject;         
    }

    public static void setValidationChecker(Runnable validationChecker) {
        VALIDATION_CHECKER = validationChecker;
    }
    
    public static int getValidateMethodCallCount() {
        return VALIDATE_METHOD_CALL_COUNT;
    }
    
    public static void resetValidateMethodCallCount() {
        VALIDATE_METHOD_CALL_COUNT = 0;
    }
    
    @Override
    public SenderResult send(Alert alert) {
        SenderResult ret = new SenderResult();
        ret.addSuccessMessage("kachny");
        
        return ret;
    }
    
    @Override
    public AlertSenderValidationResults validateAndFinalizeConfiguration(Subject subject) {
        ++VALIDATE_METHOD_CALL_COUNT;
        
        if (EXPECTED_SUBJECT != null && !subject.equals(EXPECTED_SUBJECT)) {
            throw new AssertionError("Unexpected subject. Expected " + EXPECTED_SUBJECT + " but was " + subject);
        }
    
        if (VALIDATION_CHECKER != null) {
            VALIDATION_CHECKER.run();
        }

        if (alertParameters.getSimple(EPHEMERAL_PROPERTY_NAME) == null) {
            Assert.fail("Ephemeral property not present in alert parameters during validation. This should never happen.");
        }
        
        if (extraParameters.getSimple(EPHEMERAL_PROPERTY_NAME) == null) {
            Assert.fail("Ephemeral property not present in extra parameters during validation. This should never happen.");
        }
        
        updateConfig(alertParameters);
        updateConfig(extraParameters);
        
        return new AlertSenderValidationResults(alertParameters, extraParameters);
    }
    
    private void updateConfig(Configuration configuration) {
        String persistentValue = configuration.getSimpleValue(PERSISTENT_PROPERTY_NAME, "");
        String ephemeralValue = configuration.getSimpleValue(EPHEMERAL_PROPERTY_NAME, "");
        
        configuration.put(new PropertySimple(PERSISTENT_PROPERTY_NAME, persistentValue + ephemeralValue));
        configuration.remove(EPHEMERAL_PROPERTY_NAME);
    }       
}
