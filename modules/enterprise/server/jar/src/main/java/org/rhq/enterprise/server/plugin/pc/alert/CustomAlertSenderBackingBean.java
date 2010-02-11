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

    public Configuration getAlertParameters() {
        return alertParameters;
    }

    public void setAlertParameters(Configuration alertParameters) {
        this.alertParameters = alertParameters;
    }

    protected Configuration persistConfiguration(Configuration config) {

        ConfigurationManagerLocal mgr = LookupUtil.getConfigurationManager();
        config = mgr.mergeConfiguration(config);

        return config;

    }

    protected Configuration persistProperty(Configuration config, String propertyName, Object value) {

        PropertySimple prop = config.getSimple(propertyName);
        if (prop == null) {
            prop = new PropertySimple(propertyName,value);
            config.put(prop);
        }
        else {
            prop.setValue(value);
        }
        Configuration ret = persistConfiguration(config);

        return ret;
    }
}
