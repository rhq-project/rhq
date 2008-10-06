 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.util;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

public class ConfigurationPrinter {
    public static void prettyPrintConfiguration(Configuration configuration) {
        System.out.println("Configuration: " + configuration.getNotes());
        for (Property p : configuration.getProperties()) {
            prettyPrintProperty(p, 1);
        }
    }

    private static void prettyPrintProperty(Property property, int indent) {
        if (property instanceof PropertyList) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println("List Property [" + property.getName() + "]");

            for (Property p : ((PropertyList) property).getList()) {
                prettyPrintProperty(p, indent + 1);
            }
        } else if (property instanceof PropertyMap) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println("Map Property [" + property.getName() + "]");
            for (Property p : ((PropertyMap) property).getMap().values()) {
                prettyPrintProperty(p, indent + 1);
            }
        } else if (property instanceof PropertySimple) {
            for (int i = 0; i < indent; i++) {
                System.out.print("\t");
            }

            System.out.println(property.getName() + " = " + ((PropertySimple) property).getStringValue());
        }
    }
}