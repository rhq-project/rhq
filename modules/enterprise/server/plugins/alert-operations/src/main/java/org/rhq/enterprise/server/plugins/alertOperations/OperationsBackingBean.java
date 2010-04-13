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

import java.util.LinkedHashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;

/**
 * Backing bean for the operations alert sender
 * @author Heiko W. Rupp
 * @author Joseph Marques
 */
public class OperationsBackingBean extends CustomAlertSenderBackingBean {

    private String currentType = "";
    private String currentItem = "";
    private String favoriteCharacter = "";
    private String result;

    public Map<String, String> firstList = new LinkedHashMap<String, String>();
    public Map<String, String> secondList = new LinkedHashMap<String, String>();
    public Map<String, String> thirdList = new LinkedHashMap<String, String>();

    private static final String[] FRUITS = { "Banana", "Cranberry", "Blueberry", "Orange" };
    private static final String[] VEGETABLES = { "Potatoes", "Broccoli", "Garlic", "Carrot" };

    private boolean debug = false;

    @Override
    public void loadView() {
        currentType = get("temp-type");
        currentItem = get("temp-item");
        favoriteCharacter = get("temp-char");

        // always load first list
        firstList.put("Fruits", "fruits");
        firstList.put("Vegetables", "vegetables");

        // load secondList if currentType is selected
        if (currentType.equals("none")) {
            return;
        }

        String[] currentItems;
        if (currentType.equals("fruits")) {
            currentItems = FRUITS;
        } else {
            currentItems = VEGETABLES;
        }

        for (int i = 0; i < currentItems.length; i++) {
            secondList.put(currentItems[i], currentItems[i]);
        }

        // load thirdList if currentItem is selected
        if (currentItem.equals("none")) {
            return;
        }

        for (char nextChar : currentItem.toCharArray()) {
            thirdList.put(String.valueOf(nextChar), String.valueOf(nextChar));
        }

        // load result if favoriteCharacter is selected
        if (favoriteCharacter.equals("none")) {
            return;
        }

        result = getCurrentType() + " : " + getCurrentItem() + " : " + favoriteCharacter;
    }

    private String get(String propertyName) {
        return alertParameters.getSimpleValue(propertyName, "none");
    }

    @Override
    public void saveView() {
        boolean changed = set(currentType, "temp-type");
        changed = set(changed ? "none" : currentItem, "temp-item");
        changed = set(changed ? "none" : favoriteCharacter, "temp-char");
        alertParameters = persistConfiguration(alertParameters);
    }

    private boolean set(String value, String propertyName) {
        PropertySimple property = alertParameters.getSimple(propertyName);
        if (property == null) {
            property = new PropertySimple(propertyName, value);
            alertParameters.put(property);
            return true;
        } else {
            String oldStringValue = property.getStringValue();
            property.setStringValue(value);
            return !oldStringValue.equals(value);
        }
    }

    public Map<String, String> getFirstList() {
        debug("getFirstList() -> " + firstList);
        return firstList;
    }

    public Map<String, String> getSecondList() {
        debug("getSecondList() -> " + secondList);
        return secondList;
    }

    public Map<String, String> getThirdList() {
        debug("getThirdList() -> " + thirdList);
        return thirdList;
    }

    /*
    private boolean noEffect(ValueChangeEvent event) {
        Object oldValue = event.getOldValue();
        if (event.getNewValue() == null) {
            debug("noEffect: nothing selected");
            return true; // nothing was actually selected, thus no effect
        }

        Object newValue = event.getNewValue();
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            debug("nothing changed");
            return true; // nothing was changed, thus no effect
            // NOTE: ValueChangeEvent is sometimes suppressed client-side for no-change events; depends on the component 
        }

        debug("noEffect: change detected");
        return false;
    }

    public void currentTypeChanged(ValueChangeEvent event) {
        debug("currentTypeChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        secondList.clear();

        String[] currentItems;
        String selectedCurrentType = (String) event.getNewValue();

        if (selectedCurrentType.equals("none")) {
            currentItems = new String[0];
        } else {
            secondList.put("none", "Select...");
            if (selectedCurrentType.equals("fruits")) {
                currentItems = FRUITS;
            } else {
                currentItems = VEGETABLES;
            }
        }
        for (int i = 0; i < currentItems.length; i++) {
            secondList.put(currentItems[i], currentItems[i]);
        }

        // clean-up dependent form elements
        debug("currentTypeChanged: clearing thirdList, nulling-out result");
        thirdList.clear();
        result = null;
    }

    public void currentItemChanged(ValueChangeEvent event) {
        debug("currentItemChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        thirdList.clear();
        thirdList.put("none", "Select...");
        String selectedCurrentItem = (String) event.getNewValue();
        if (selectedCurrentItem.equals("none") == false) {
            for (char nextChar : selectedCurrentItem.toCharArray()) {
                secondList.put(String.valueOf(nextChar), String.valueOf(nextChar));
            }
        }

        // clean-up dependent form elements
        debug("currentItemChanged: nulling-out result");
        result = null;
    }

    public void currentCharChanged(ValueChangeEvent event) {
        debug("currentCharChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        // NOTE: calling getFavoriteCharacter results stale data here, because the
        //       ValueChangeEvent is fired before the setFavoriteCharacter method
        result = null;
        String selectedCurrentChar = (String) event.getNewValue();
        if (selectedCurrentChar.equals("none") == false) {
            result = getCurrentType() + " : " + getCurrentItem() + " : " + selectedCurrentChar;
        }

        // no dependent form elements
    }
    */

    public String getCurrentType() {
        debug("getCurrentType() -> " + currentType);
        return currentType;
    }

    public void setCurrentType(String currentType) {
        debug("setCurrentType(" + currentType + ")");
        this.currentType = currentType;
    }

    public String getCurrentItem() {
        debug("getCurrentItem() -> " + currentItem);
        return currentItem;
    }

    public void setCurrentItem(String currentItem) {
        debug("setCurrentItem(" + currentItem + ")");
        this.currentItem = currentItem;
    }

    public String getFavoriteCharacter() {
        debug("getFavoriteCharacter() -> " + favoriteCharacter);
        return favoriteCharacter;
    }

    public void setFavoriteCharacter(String favoriteCharacter) {
        debug("setFavoriteCharacter(" + favoriteCharacter + ")");
        this.favoriteCharacter = favoriteCharacter;
    }

    public String getResult() {
        debug("getResult() -> " + result);
        return result;
    }

    public void setResult(String result) {
        debug("setResult(" + result + ")");
        this.result = result;
    }

    private void debug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    /*
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
    */
}
