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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the operations alert sender
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class OperationsBackingBean extends CustomAlertSenderBackingBean {

    private final Log log = LogFactory.getLog(OperationsBackingBean.class);

    private String resMode;
    Integer resId;
    private Integer operationId;
    private Map<String, Integer> operationIds = new HashMap<String, Integer>();
    private String resourceName;

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private static final String ALERT_NOTIFICATIONS = "ALERT_NOTIFICATIONS";

    private String operationName;

    public OperationsBackingBean() {
        log.info("new " + hashCode());
    }

    @Override
    public void internalCleanup() {
        log.info("internalCleanup");
        PropertySimple parameterConfigProp = alertParameters
            .getSimple(OperationInfo.Constants.PARAMETERS_CONFIG.propertyName);
        if (parameterConfigProp != null) {
            Integer paramId = parameterConfigProp.getIntegerValue();
            if (paramId != null) {
                ConfigurationManagerLocal cmgr = LookupUtil.getConfigurationManager();
                cmgr.deleteConfigurations(Arrays.asList(paramId));
                cleanProperty(alertParameters, OperationInfo.Constants.PARAMETERS_CONFIG.propertyName);
            }
        }
    }

    public String selectResource() {

        log.info("In select Resource, resId is " + resId + " resMode is " + resMode);

        if (resId != null) {
            persistProperty(alertParameters, OperationInfo.Constants.RESOURCE.propertyName, resId);
            cleanProperty(alertParameters, OperationInfo.Constants.OPERATION.propertyName);
            cleanProperty(alertParameters, OperationInfo.Constants.USABLE.propertyName);
            operationIds = new HashMap<String, Integer>(); // Clean out operations dropdown
            operationId = null;
            operationName = null;

        }

        obtainOperationIds();

        return ALERT_NOTIFICATIONS;
    }

    public String selectOperation() {
        log.info("In selectOperation, resId is " + resId + " opName is " + operationId);

        if (operationId != null) {
            persistProperty(alertParameters, OperationInfo.Constants.OPERATION.propertyName, operationId);
            getOperationNameFromOperationIds();
            lookupConfiguration();
        }

        return ALERT_NOTIFICATIONS;
    }

    private void lookupConfiguration() {

        try {

            OperationManagerLocal opMan = LookupUtil.getOperationManager();
            obtainOperationIds();

            if (operationId != null) {
                OperationDefinition operationDefinition = opMan.getOperationDefinition(webUser, operationId);
                configurationDefinition = operationDefinition.getParametersConfigurationDefinition();

                // call a SLSB method to get around lazy initialization of configDefs and configTemplates
                ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
                configuration = configurationManager.getConfigurationFromDefaultTemplate(configurationDefinition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String useConfiguration() {
        log.info("In useConfiguration, Configuration is " + configuration);

        // COnfiguration should be valid here ...
        super.persistConfiguration(configuration);
        persistProperty(alertParameters, OperationInfo.Constants.PARAMETERS_CONFIG.propertyName, configuration.getId());
        persistProperty(alertParameters, OperationInfo.Constants.USABLE.propertyName, true);

        return ALERT_NOTIFICATIONS;
    }

    private void obtainOperationIds() {

        PropertySimple prop = alertParameters.getSimple(OperationInfo.Constants.RESOURCE.propertyName);
        if (prop != null)
            resId = prop.getIntegerValue();

        if (resId != null) {
            OperationManagerLocal opMan = LookupUtil.getOperationManager();

            List<OperationDefinition> opDefs = opMan.findSupportedResourceOperations(webUser, resId, false);
            for (OperationDefinition def : opDefs) {
                operationIds.put(def.getDisplayName(), def.getId()); // TODO add more distinctive stuff in display
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
        if (resId == null) {
            PropertySimple prop = alertParameters.getSimple(OperationInfo.Constants.RESOURCE.propertyName);
            if (prop != null)
                resId = prop.getIntegerValue();
        }

        return resId;
    }

    public void setResId(Integer resId) {
        this.resId = resId;
        if (resId != null) {
            persistProperty(alertParameters, OperationInfo.Constants.RESOURCE.propertyName, resId);
        }
    }

    public String getResourceName() {
        if (resId == null)
            getResId();

        if (resId != null) {
            ResourceManagerLocal resMgr = LookupUtil.getResourceManager();
            Resource res = resMgr.getResource(webUser, resId);

            resourceName = res.getName() + " (" + res.getResourceType().getName() + ")";
        }
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getOperationId() {
        System.out.println("OperationsBackingBean.getOperationId, operationId=" + operationId);
        if (operationId == null) {
            PropertySimple prop = alertParameters.getSimple(OperationInfo.Constants.OPERATION.propertyName);
            if (prop != null)
                operationId = prop.getIntegerValue();

        }

        if (operationIds == null || operationIds.isEmpty())
            obtainOperationIds();
        getOperationNameFromOperationIds();

        return operationId;
    }

    private void getOperationNameFromOperationIds() {
        for (Map.Entry<String, Integer> ent : operationIds.entrySet()) {
            if (ent.getValue().equals(operationId))
                operationName = ent.getKey();
        }
    }

    public void setOperationId(Integer operationId) {
        this.operationId = operationId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, Integer> getOperationIds() {

        obtainOperationIds();

        return operationIds;
    }

    public void setOperationIds(Map<String, Integer> operationIds) {
        this.operationIds = operationIds;
    }

    public ConfigurationDefinition getConfigurationDefinition() {

        if (configurationDefinition == null)
            lookupConfiguration();

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

    public String getNullConfigurationDefinitionMessage() {
        return "This operation does not take any parameters.";
    }

    public String getNullConfigurationMessage() {
        return "This operation parameters definition has not been initialized.";
    }
}
