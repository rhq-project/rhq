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

package org.rhq.plugins.jbossas5.util;

import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.SimpleValue;

/**
 * @author Lukas Krejci
 */
public class Ejb2BeanUtils {
    private static final ComponentType MDB_COMPONENT_TYPE = new ComponentType("EJB", "MDB");

    private Ejb2BeanUtils() {

    }

    public static String getUniqueBeanIdentificator(ManagedComponent component) {
        if (MDB_COMPONENT_TYPE.equals(component.getType())) {
            //for MDBs, the unique combination is the deployment name + (component name - @object-id)
            String componentName = stripObjectId(component.getName());
            String deploymentName = ((SimpleValue) component.getProperty("DeploymentName").getValue()).getValue()
                .toString();

            return deploymentName + "|" + componentName;
        } else {
            return component.getName();
        }
    }

    public static String parseResourceName(ManagedComponent component) {
        String resourceName = parseSimpleResourceName(component.getName());

        if (MDB_COMPONENT_TYPE.equals(component.getType())) {
            //we need to exclude the @object-id part from the name
            //in a given deployment, there's guaranteed to be at most 1
            //instance of an MDB with a given name
            resourceName = stripObjectId(resourceName);
        }

        return resourceName;
    }

    private static String parseSimpleResourceName(String componentName) {
        //jboss.j2ee:jndiName=eardeployment/SessionA,service=EJB

        int jndiNameIdx = componentName.indexOf("jndiName=");
        jndiNameIdx += 9; // = "jndiName=".length()

        int commaIdx = componentName.indexOf(',', jndiNameIdx);
        if (commaIdx == -1)
            commaIdx = componentName.length();

        int slashIdx = componentName.lastIndexOf('/', commaIdx);
        slashIdx++;

        int startIdx = slashIdx > jndiNameIdx ? slashIdx : jndiNameIdx;

        return componentName.substring(startIdx, commaIdx);
    }

    private static String stripObjectId(String resourceName) {
        int atIdx = resourceName.indexOf('@');
        if (atIdx != -1) {
            resourceName = resourceName.substring(0, atIdx);
        }

        return resourceName;
    }
}
