/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.configuration;

import javax.faces.application.FacesMessage;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.gui.RequestParameterNameConstants;
import org.rhq.core.gui.util.FacesContextUtility;

public abstract class AbstractAddNewOpenMapMemberPropertyUIBean {
    private static final String SUCCESS_OUTCOME = "success";
    private static final String FAILURE_OUTCOME = "failure";

    private String propertyName;
    private String propertyValue;

    // NOTE: We assume the Configuration is in session, otherwise the changes we make here will be lost when the
    //       user gets redirected back to the main config page.
    protected abstract Configuration getConfiguration();

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String addProperty() {
        String mapName = FacesContextUtility.getRequiredRequestParameter(RequestParameterNameConstants.MAP_NAME_PARAM);
        PropertyMap propertyMap = getConfiguration().getMap(mapName);
        if (propertyMap == null) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Configuration does not contain a map named '"
                + mapName + "'.");
            return FAILURE_OUTCOME;
        }

        // Assume any leading or trailing whitespace in the property name was not intended by the user.
        String propertyName = this.propertyName.trim();
        if (propertyMap.getMap().containsKey(propertyName)) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Map '" + mapName
                + "' already contains a property named '" + propertyName + "'.");
            return FAILURE_OUTCOME;
        }

        PropertySimple propertySimple = new PropertySimple(propertyName, this.propertyValue);
        // It is essential to set override to true in case this is an aggregate config.
        propertySimple.setOverride(true);
        propertyMap.put(propertySimple);
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Property '" + propertySimple.getName()
            + "' added to map '" + mapName + "'.");
        return SUCCESS_OUTCOME;
    }

    public String cancel() {
        return SUCCESS_OUTCOME;
    }
}