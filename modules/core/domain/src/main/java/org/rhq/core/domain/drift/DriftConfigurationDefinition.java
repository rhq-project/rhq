/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * @author Jay Shaughnessy
 */
public class DriftConfigurationDefinition {

    static private final ConfigurationDefinition INSTANCE = new ConfigurationDefinition("GLOBAL_DRIFT_CONFIG_DEF",
        "The drift configuration definition");

    static {
        INSTANCE.setId(1);
        Map<String, PropertyDefinition> propDefs = new HashMap<String, PropertyDefinition>();
        PropertyDefinitionSimple propDef;

        propDef = new PropertyDefinitionSimple("name", "The drift configuration name", true, PropertySimpleType.STRING);
        propDefs.put("name", propDef);

        propDef = new PropertyDefinitionSimple("name", "The drift configuration name", true, PropertySimpleType.STRING);
        propDef.setId(1);
        propDef.setDisplayName("Drift Configuration Name");
        propDef.setOrder(0);
        propDef.setConfigurationDefinition(INSTANCE);
        propDefs.put("name", propDef);

        propDef = new PropertyDefinitionSimple("enabled", "Enabled", true, PropertySimpleType.BOOLEAN);
        propDef.setId(2);
        propDef.setDisplayName("Enables or disables drift detection for this configuration");
        propDef.setOrder(1);
        propDef.setDefaultValue("false");
        propDef.setConfigurationDefinition(INSTANCE);
        propDefs.put("enabled", propDef);

        propDef = new PropertyDefinitionSimple("basedir", "Base Directory", true, PropertySimpleType.STRING);
        propDef.setId(3);
        propDef.setDisplayName("The base directory from which files will be monitored for drift.");
        propDef.setOrder(2);
        propDef.setConfigurationDefinition(INSTANCE);
        propDefs.put("basedir", propDef);

        propDef = new PropertyDefinitionSimple("interval", "Interval", false, PropertySimpleType.LONG);
        propDef.setId(4);
        propDef
            .setDisplayName("The interval, in seconds, between drift detection scans for this configuration. Default is 30 minutes.");
        propDef.setOrder(3);
        propDef.setDefaultValue("1800");
        propDef.setConfigurationDefinition(INSTANCE);
        propDefs.put("interval", propDef);

        //TODO, add the include/exclude list of maps

        INSTANCE.setPropertyDefinitions(propDefs);
    }

    static public ConfigurationDefinition getInstance() {
        return INSTANCE;

    }
}
