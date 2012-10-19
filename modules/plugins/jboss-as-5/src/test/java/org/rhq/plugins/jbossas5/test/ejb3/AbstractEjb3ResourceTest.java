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

package org.rhq.plugins.jbossas5.test.ejb3;

import static org.testng.Assert.fail;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pc.PluginContainer;
import org.rhq.plugins.jbossas5.test.AbstractResourceTest;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Parameters;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractEjb3ResourceTest extends AbstractResourceTest {

    @BeforeGroups(groups = "as5-plugin-ejb3")
    @Parameters("ejb3.test.jars.path")
    public void deployTestJars(String jarPaths) {
        try {
            System.out.println("Deploying EJB3 test jars to AS...");

            for (String jarPath : jarPaths.split(";")) {
                File jarFile = new File(jarPath);
                AppServerUtils.deployFileToAS(jarFile.getName(), jarFile, false);
            }
            //and discover the resources
            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        } catch (Exception e) {
            fail("Failed to deploy EJB3 test jars.", e);
        }
    }
    
    @AfterGroups(groups = "as5-plugin-ejb3")
    @Parameters("ejb3.test.jars.path")
    public void undeployTestJars(String jarPaths) {
        try {
            System.out.println("Undeploying EJB3 test jars from AS...");
            for (String jarPath : jarPaths.split(";")) {
                File jarFile = new File(jarPath);
                AppServerUtils.undeployFromAS(jarFile.getName());
            }
        } catch (Exception e) {
            fail("Failed to undeploy EJB3 test jars.", e);
        }
    }
    
    /**
     * None of the EJB3 resources has a resource level configuration.
     */
    protected Configuration getTestResourceConfiguration() {
        return new Configuration();
    }

}
