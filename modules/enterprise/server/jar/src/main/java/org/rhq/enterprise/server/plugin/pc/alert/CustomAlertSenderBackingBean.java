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
package org.rhq.enterprise.server.plugin.pc.alert;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Abstract super class for custom alert sender backing beans
 *
 * @author Heiko W. Rupp
 */
public class CustomAlertSenderBackingBean {

    /** Configuration from the per alert definition parameters */
    protected Configuration alertParameters;
    protected Configuration extraParameters;
    protected Subject webUser;
    protected String context;
    protected String contextId;
    protected String contextSubId;
    protected Integer alertNotificationId;

    public Configuration getAlertParameters() {
        return alertParameters;
    }

    public Configuration getExtraParameters() {
        return extraParameters;
    }

    public void setAlertParameters(Configuration alertParameters) {
        this.alertParameters = alertParameters;
    }

    public void setExtraParameters(Configuration extraParameters) {
        this.extraParameters = extraParameters;
    }

    public Subject getWebUser() {
        return webUser;
    }

    public void setWebUser(Subject webUser) {
        this.webUser = webUser;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getContextSubId() {
        return contextSubId;
    }

    public void setContextSubId(String contextSubId) {
        this.contextSubId = contextSubId;
    }

    public Integer getAlertNotificationId() {
        return alertNotificationId;
    }

    public void setAlertNotificationId(Integer alertNotificationId) {
        this.alertNotificationId = alertNotificationId;
    }

    public void loadView() {
    }

    public void saveView() {
    }

    /**
     * Persist the passed configuration object. This can be a new object or one
     * that already exists in the database. If the input is null, not persistence
     * happens and null is returned.
     * @param config configuration to persist or update
     * @return  a merged copy of the configuration or null
     */
    protected Configuration persistConfiguration(Configuration config) {

        if (config == null)
            return null;

        ConfigurationManagerLocal mgr = LookupUtil.getConfigurationManager();
        config = mgr.mergeConfiguration(config);

        return config;

    }

    /**
     * Persist a single property of a given configuration. If the property does not yet exist,
     * it is created otherwise overwritten with the new value.
     * @param config configuration the property is on
     * @param propertyName name of the property to persist
     * @param value (new) value of the property to persist
     * @return the updated configuration
     */
    protected Configuration persistProperty(Configuration config, String propertyName, Object value) {

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            prop = new PropertySimple(propertyName, value);
            config.put(prop);
        } else {
            prop.setValue(value);
        }
        Configuration ret = persistConfiguration(config);

        return ret;
    }

    /**
     * Remove one property from the passed configuration. Returns the updated configuration
     * @param config configuration the property is on
     * @param propertyName name of the property to remove
     * @return the updated configuration
     */
    protected Configuration cleanProperty(Configuration config, String propertyName) {

        Configuration ret = config;
        PropertySimple prop = config.getSimple(propertyName);
        if (prop != null) {
            config.remove(propertyName);
            ret = persistConfiguration(config);
        }

        return ret;
    }

}
