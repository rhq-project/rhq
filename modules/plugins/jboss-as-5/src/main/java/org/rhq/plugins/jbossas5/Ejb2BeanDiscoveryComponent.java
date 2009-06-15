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

import java.util.List;

import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.TableValue;

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * A discovery component for EJB 1/2 beans.
 * @author Lukas Krejci
 */
public class Ejb2BeanDiscoveryComponent extends ManagedComponentDiscoveryComponent<AbstractManagedDeploymentComponent> {

    @Override
    protected String getResourceName(ManagedComponent component) {
        //jboss.j2ee:jndiName=eardeployment/SessionA,service=EJB
        String fullName = component.getName();

        int jndiNameIdx = fullName.indexOf("jndiName=");
        jndiNameIdx += 9; // = "jndiName=".length()

        int commaIdx = fullName.indexOf(',', jndiNameIdx);
        if (commaIdx == -1)
            commaIdx = fullName.length();

        int slashIdx = fullName.lastIndexOf('/', commaIdx);
        slashIdx++;

        int startIdx = slashIdx > jndiNameIdx ? slashIdx : jndiNameIdx;

        return fullName.substring(startIdx, commaIdx);
    }

    @Override
    protected boolean accept(ResourceDiscoveryContext<AbstractManagedDeploymentComponent> discoveryContext,
        ManagedComponent component) {
        String deploymentName = ((SimpleValue) component.getProperty("DeploymentName").getValue()).getValue()
            .toString();

        AbstractManagedDeploymentComponent parentDeployment = discoveryContext.getParentResourceComponent();

        String parentDeploymentName = parentDeployment.getDeploymentName();

        //compare ignoring the trailing slash
        if (deploymentName.endsWith("/"))
            deploymentName = deploymentName.substring(0, deploymentName.length() - 1);
        if (parentDeploymentName.endsWith("/"))
            parentDeploymentName = parentDeploymentName.substring(0, parentDeploymentName.length() - 1);

        return parentDeploymentName.equals(deploymentName);
    }    
}
