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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.auth.Subject;
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
 */
@Scope(ScopeType.PAGE)
public class OperationsBackingBean extends CustomAlertSenderBackingBean {

    private final Log log = LogFactory.getLog(OperationsBackingBean.class);

    private String resMode;
    private String tokenMode;
    Integer resId;
    private Integer operationName;
    private Map<String, Integer> operationNames = new HashMap<String, Integer>();
    private String resourceName;


    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private static final String ALERT_NOTIFICATIONS = "ALERT_NOTIFICATIONS";

    public OperationsBackingBean() {
        log.info("new " + hashCode());
    }

    @Create
    public void init() {
        log.info("init");
    }


    public String selectResource() {

        log.info("In select Resource, resId is " + resId + " resMode is " + resMode);

        if (resId != null) {
            persistProperty(alertParameters, OperationsSender.RESOURCE_ID,resId);
            cleanProperty(alertParameters,OperationsSender.OPERATION_NAME);
            cleanProperty(alertParameters,OperationsSender.USABLE);

        }

        obtainOperationNames();

        return ALERT_NOTIFICATIONS;
    }



    public String selectOperation() {
        log.info("In selectOperation, resId is " + resId + " opName is " + operationName);

        if (operationName != null ) {
            persistProperty(alertParameters, OperationsSender.OPERATION_NAME,operationName);
            lookupConfiguration();
        }

        return ALERT_NOTIFICATIONS;
    }

    private void lookupConfiguration() {


//        log.info("getCD: " + configurationDefinition);
        try {
//            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO replace with real subject

//            int operationId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("opId"));
            OperationManagerLocal opMan = LookupUtil.getOperationManager();
            obtainOperationNames();


            OperationDefinition operationDefinition = opMan.getOperationDefinition(subject, operationName);
            configurationDefinition = operationDefinition.getParametersConfigurationDefinition();


            // call a SLSB method to get around lazy initialization of configDefs and configTemplates
            ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
            configuration = configurationManager.getConfigurationFromDefaultTemplate(configurationDefinition);
//            Configuration newConfiguration = configuration.deepCopy(false);
log.info("gConfig: " + configuration + ", " + configuration.hashCode() + ", " + configuration.getSimpleValue("detailedDiscovery","-unset-"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String useConfiguration() {
        log.info("In useConfiguration, Configuration is " + configuration );

        // COnfiguration should be valid here ...
        super.persistConfiguration(configuration);
        persistProperty(alertParameters,OperationsSender.PARAMETERS_CONFIG,configuration.getId());
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
                operationNames.put(def.getDisplayName(),def.getId()); // TODO add more distinctive stuff in display
            }
        }
    }



    public String getResMode() {
        return resMode;
    }

    public void setResMode(String resMode) {
        this.resMode = resMode;
        log.info("setResMode: " + resMode);
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
        log.info("Set resid " + resId);
        if (resId!=null) {
            persistProperty(alertParameters,OperationsSender.RESOURCE_ID,resId);
        }
    }

    public String getResourceName() {
        if (resId==null)
            getResId();

        if (resId!=null) {
            ResourceManagerLocal resMgr = LookupUtil.getResourceManager();
            Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO replace with real subject
            Resource res = resMgr.getResource(subject,resId);

            resourceName = res.getName() + " (" + res.getResourceType().getName() + ")";
        }
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getOperationName() {

        if (operationName==null) {
            PropertySimple prop = alertParameters.getSimple(OperationsSender.OPERATION_NAME);
            if (prop!=null)
                operationName = prop.getIntegerValue();
        }

        return operationName;
    }

    public void setOperationName(Integer operationName) {
        this.operationName = operationName;
    }

    public Map<String, Integer> getOperationNames() {

        obtainOperationNames();

        return operationNames;
    }

    public void setOperationNames(Map<String, Integer> operationNames) {
        this.operationNames = operationNames;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;



    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        log.info("set CD: " + configurationDefinition);
        this.configurationDefinition = configurationDefinition;
    }

    public Configuration getConfiguration() {

        return configuration;


    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        log.info("setC: " + configuration);
    }

        public String getNullConfigurationDefinitionMessage() {
        return "This operation does not take any parameters.";
    }

    public String getNullConfigurationMessage() {
        return "This operation parameters definition has not been initialized.";
    }

    public String getTokenMode() {
        if (tokenMode==null) {
            PropertySimple prop = alertParameters.getSimple(OperationsSender.TOKEN_MODE);
            if (prop!=null)
                tokenMode = prop.getStringValue();
        }

        return tokenMode;
    }

    public void setTokenMode(String tokenMode) {
        this.tokenMode = tokenMode;
        log.info("token mode" + tokenMode);

        persistProperty(alertParameters, OperationsSender.TOKEN_MODE,tokenMode);
    }


}
