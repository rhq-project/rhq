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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the operations alert sender
 * @author Heiko W. Rupp
 */

public class OperationsBackingBean extends CustomAlertSenderBackingBean {

    private final Log log = LogFactory.getLog(OperationsBackingBean.class);

    private String resMode;
    Integer resId;
    private String operationName;
    private Map<String,String> operationNames = new HashMap<String,String>();
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private static final String ALERT_NOTIFICATIONS = "ALERT_NOTIFICATIONS";

    public String selectResource() {

        log.info("In select Resource, resId is " + resId + " resMode is " + resMode);

        if (resId != null)
            persistProperty(alertParameters, OperationsSender.RESOURCE_ID,resId);

        obtainOperationNames();

        return ALERT_NOTIFICATIONS;
    }



    public String selectOperation() {
        log.info("In selectOperation, resId is " + resId + " opName is " + operationName);

        if (operationName != null )
            persistProperty(alertParameters, OperationsSender.OPERATION_NAME,operationName);

        return ALERT_NOTIFICATIONS;
    }

    public String useConfiguration() {
        log.info("In useConfiguration");

        persistProperty(alertParameters, OperationsSender.USABLE,true);

        return ALERT_NOTIFICATIONS;
    }

    private void obtainOperationNames() {

        PropertySimple prop = alertParameters.getSimple(OperationsSender.RESOURCE_ID);
        if (prop!=null)
            resId = prop.getIntegerValue();

        if (resId!=null) {
            OperationManagerLocal opMan = LookupUtil.getOperationManager();

            Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO replace with real subject
            List<OperationDefinition> opDefs = opMan.findSupportedResourceOperations(subject, resId, false);
            for (OperationDefinition def : opDefs) {
                operationNames.put(def.getDisplayName(),def.getName()); // TODO add more distinctive stuff in display
            }
        }
    }



    public String getResMode() {
        return resMode;
    }

    public void setResMode(String resMode) {
        this.resMode = resMode;
    }

    public Integer getResId() {

        if (resId==null) {
            PropertySimple prop = alertParameters.getSimple(OperationsSender.RESOURCE_ID);
            if (prop!=null)
                resId = prop.getIntegerValue();
        }

        return resId;
    }

    public void setResId(Integer resId) {
        this.resId = resId;
    }

    public String getOperationName() {

        if (operationName==null) {
            PropertySimple prop = alertParameters.getSimple(OperationsSender.OPERATION_NAME);
            if (prop!=null)
                operationName = prop.getStringValue();
        }

        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String,String> getOperationNames() {

        obtainOperationNames();

        return operationNames;
    }

    public void setOperationNames(Map<String,String> operationNames) {
        this.operationNames = operationNames;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
