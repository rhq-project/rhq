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

package org.rhq.plugins.jmx;

import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Delegate for the old version of the class which got moved to util package
 * @author Heiko W. Rupp
 * @deprecated Use the version in the util package
 */
public class ObjectNameQueryUtility {

    private org.rhq.plugins.jmx.util.ObjectNameQueryUtility onu;

    public ObjectNameQueryUtility(String objectNameQueryTemplate) {
        onu = new org.rhq.plugins.jmx.util.ObjectNameQueryUtility(objectNameQueryTemplate);
    }

    public ObjectNameQueryUtility(String objectNameQueryTemplate, Configuration parentConfiguration) {
        onu = new org.rhq.plugins.jmx.util.ObjectNameQueryUtility(objectNameQueryTemplate,parentConfiguration);
    }

    public boolean setMatchedKeyValues(Map<String, String> keyProperties) {
        return onu.setMatchedKeyValues(keyProperties);
    }

    public String formatMessage(String message) {
        return onu.formatMessage(message);
    }

    public void resetVariables() {
        onu.resetVariables();
    }

    public String getQueryTemplate() {
        return onu.getQueryTemplate();
    }

    public Map<String, String> getVariableProperties() {
        return onu.getVariableProperties();
    }

    public Map<String, String> getVariableValues() {
        return onu.getVariableValues();
    }

    public String getTranslatedQuery() {
        return onu.getTranslatedQuery();
    }

    public boolean isContainsExtraKeyProperties(Set<String> strings) {
        return onu.isContainsExtraKeyProperties(strings);
    }
}
