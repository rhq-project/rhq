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

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.alert.notification.SenderResult;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Alert sender that triggers an operation on the resource
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class OperationsSender extends AlertSender {

    @Override
    public SenderResult send(Alert alert) {

        OperationInfo info = OperationInfo.load(alertParameters);
        if (info.error != null) {
            return new SenderResult(ResultState.FAILURE, info.error);
        }

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO get real subject for authz?

        OperationDefinition operation = info.getOperationDefinition();
        Configuration parameters = info.getArguments();

        Configuration replacedParameters = null;
        try {
            if (parameters != null) {
                // the parameter-replaced configuration object will be persisted separately from the original
                replacedParameters = parameters.deepCopy(false);

                Resource resource = LookupUtil.getResourceManager().getResource(subject, info.resourceId);
                AlertTokenReplacer replacementEngine = new AlertTokenReplacer(alert, operation, resource);
                for (PropertySimple simpleProperty : replacedParameters.getSimpleProperties().values()) {
                    String temp = simpleProperty.getStringValue();
                    temp = replacementEngine.replaceTokens(temp);
                    simpleProperty.setStringValue(temp);
                }
            }
        } catch (Exception e) {
            String message = getResultMessage(info, "failed with " + e.getMessage());
            return new SenderResult(ResultState.FAILURE, message);
        }

        // Now fire off the operation with no delay and no repetition.
        try {
            Resource targetResource = info.getTargetResource(alert);
            String description = "Alert operation for " + alert.getAlertDefinition().getName();
            ResourceOperationSchedule schedule = LookupUtil.getOperationManager().scheduleResourceOperation(subject,
                targetResource.getId(), operation.getName(), 0, 0, 0, 0, replacedParameters, description);

            String message = getResultMessage(info, "jobId was " + schedule.getJobId());
            return new SenderResult(ResultState.SUCCESS, message);
        } catch (Throwable t) {
            String message = getResultMessage(info, "failed with " + t.getMessage());
            return new SenderResult(ResultState.FAILURE, message);
        }
    }

    private String getResultMessage(OperationInfo info, String details) {
        return info.getResourceInfo() + ": " + details;
    }

    @Override
    public String previewConfiguration() {
        OperationInfo info = OperationInfo.load(alertParameters);
        return info.toString();
    }

}
