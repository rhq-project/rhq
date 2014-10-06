/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5;

import java.util.ArrayList;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * A Resource component for JBoss AS 5 JBoss Messaging JMS topic and queues.
 *
 * @author Ian Springer
 */
public class JmsDestinationComponent extends ManagedComponentComponent
{
    private static final String[] OBJECT_NAME_PROPERTY_NAMES = new String[]{"DLQ", "expiryQueue", "serverPeer"};

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport)
    {
        Configuration resourceConfig = configurationUpdateReport.getConfiguration();
        List<String> invalidObjectNamePropertyNames = new ArrayList();
        for (String objectNamePropertyName : OBJECT_NAME_PROPERTY_NAMES)
        {
            PropertySimple propertySimple = resourceConfig.getSimple(objectNamePropertyName);
            try
            {
                validateObjectNameProperty(propertySimple);
            }
            catch (MalformedObjectNameException e)
            {
                propertySimple.setErrorMessage("Invalid ObjectName: " + e.getLocalizedMessage());
                invalidObjectNamePropertyNames.add(propertySimple.getName());
            }
        }
        if (!invalidObjectNamePropertyNames.isEmpty())
            configurationUpdateReport.setErrorMessage("The following ObjectName properties have invalid values: "
                    + invalidObjectNamePropertyNames);
        else {
            super.updateResourceConfigurationChangesOnly(configurationUpdateReport);
        }

    }

    private static void validateObjectNameProperty(PropertySimple propertySimple) throws MalformedObjectNameException
    {
        if (propertySimple != null)
        {
            String value = propertySimple.getStringValue();
            if (value != null)
            {
                ObjectName objectName = new ObjectName(value);
                if (objectName.isPattern())
                    throw new MalformedObjectNameException("Patterns are not allowed.");
            }
        }
    }
}