/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.exception.ScheduleException;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Alert sender that triggers an operation on the resource
 * @author Heiko W. Rupp
 */
@Scope(value = ScopeType.PAGE)
public class OperationsSender extends AlertSender {

    private final Log log = LogFactory.getLog(OperationsSender.class);
    static final String RESOURCE_ID = "resourceId";
    static final String OPERATION_NAME = "operationName";
    static final String USABLE = "usable";
    protected static final String TOKEN_MODE = "tokenMode";
    private static final String LITERAL = "literal";
    private static final String INTERPRETED = "interpreted";
    public static final String PARAMETERS_CONFIG = "parametersConfig";

    @Override
    public SenderResult send(Alert alert) {

        PropertySimple resProp = alertParameters.getSimple(RESOURCE_ID);
        PropertySimple opNameProp = alertParameters.getSimple(OPERATION_NAME);
        if (resProp==null || resProp.getIntegerValue() == null || opNameProp == null || opNameProp.getStringValue() == null)
            return new SenderResult(ResultState.FAILURE, "Not enough parameters given");

        PropertySimple usableProp = alertParameters.getSimple(USABLE);
        if (usableProp==null || usableProp.getBooleanValue()== null || !usableProp.getBooleanValue())
            return new SenderResult(ResultState.FAILURE,"Not yet configured");

        Integer resourceId = resProp.getIntegerValue();
        String opName = opNameProp.getStringValue();

        OperationManagerLocal opMgr = LookupUtil.getOperationManager();
        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO get real subject

        PropertySimple parameterConfigProp = alertParameters.getSimple(PARAMETERS_CONFIG);
        Configuration parameters = null ;
        if (parameterConfigProp!=null) {
            Integer paramId = parameterConfigProp.getIntegerValue();
            if (paramId!=null) {
                ConfigurationManagerLocal cmgr = LookupUtil.getConfigurationManager();
                parameters = cmgr.getConfiguration(subject,paramId);
            }
        }


        String tokenMode = alertParameters.getSimpleValue(TOKEN_MODE, LITERAL);

        /*
         * If we have parameters and the user wants tokens to be interpreted, then loop
         * over the parameters and do token replacement.
         */
        if (parameters!=null && tokenMode.equals(INTERPRETED)) {
            Map<String,PropertySimple> propsMap = parameters.getSimpleProperties();
            if (!propsMap.isEmpty()) {
                TokenReplacer tr = new TokenReplacer(alert);
                for (PropertySimple prop  : propsMap.values()) {
                    String tmp = prop.getStringValue();
                    tmp = tr.replaceTokens(tmp);
                    prop.setStringValue(tmp);
                }
            }
        }



        /*
         * Now fire off the operation with no delay and no repetition.
         */
        ResourceOperationSchedule sched;
        try {
            sched = opMgr.scheduleResourceOperation(subject, resourceId, opName, 0, 0, 0, 0, parameters,
                    "Alert operation for " + alert.getAlertDefinition().getName());
        } catch (ScheduleException e) {
            return new SenderResult(ResultState.FAILURE, "Scheduling of operation " + opName + " on resource " + resourceId + " failed: " + e.getMessage());
        }

        // If op sending was successful
        return new SenderResult(ResultState.SUCCESS, "Scheduled operation " + opName + " on resource " + resourceId + " with jobId " + sched.getJobId() );

    }



}
