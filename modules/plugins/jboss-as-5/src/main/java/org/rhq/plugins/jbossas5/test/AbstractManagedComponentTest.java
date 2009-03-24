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
package org.rhq.plugins.jbossas5.test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.testng.annotations.BeforeSuite;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.profileservice.spi.ProfileService;

/**
 * @author Ian Springer
 */
public class AbstractManagedComponentTest
{
    protected ManagementView managementView;

    @BeforeSuite(alwaysRun = true)
    public void initProfileService() {
        System.out.println("Initializing profile service...");
        ProfileService profileService = ProfileServiceFactory.getProfileService();
        ProfileServiceFactory.refreshCurrentProfileView();
        this.managementView = ProfileServiceFactory.getCurrentProfileView();
    }

    protected Set<ManagedProperty> getMandatoryProperties(DeploymentTemplateInfo template)
    {
        Map<String, ManagedProperty> managedProperties = template.getProperties();
        Set<ManagedProperty> mandatoryProperties = new HashSet();
        for (ManagedProperty managedProperty : managedProperties.values()) {
            if (managedProperty.isMandatory())
                mandatoryProperties.add(managedProperty);
        }
        return mandatoryProperties;
    }

    protected Set<ManagedProperty> getNonMandatoryProperties(DeploymentTemplateInfo template)
    {
        Map<String, ManagedProperty> managedProperties = template.getProperties();
        Set<ManagedProperty> mandatoryProperties = new HashSet();
        for (ManagedProperty managedProperty : managedProperties.values()) {
            if (!managedProperty.isMandatory())
                mandatoryProperties.add(managedProperty);
        }
        return mandatoryProperties;
    }
}
