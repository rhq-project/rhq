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

package org.rhq.plugins.jbossas5.test.ejb2;

import org.rhq.plugins.jbossas5.test.util.MethodArgDef;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = { "as5-plugin", "as5-plugin-ejb2", "as5-plugin-ejb2-sfsb" })
public class Ejb2SFSBResourceTest extends AbstractSessionBeanTest {

    private static class TestTemplate extends Ejb2SessionBeanTestTemplate {

        public MethodArgDef[] getEjbCreateMethodArgs() {
            return null;
        }

        public Class<?> getHomeInterface() {
            try {
                return Class.forName("org.jboss.test.ejb.proxy.beans.StatefulCounterHome");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not find Ejb2SFSB home interface test class.", e);
            }
        }

        public String getExpectedResourceKey() {
            return "jboss.j2ee:jndiName=ejb/StatefulCounterEjb,service=EJB";
        }

        public String getTestedBeanName() {
            return "ejb/StatefulCounterEjb";
        }

        public String getHomeInterfaceJndiName() {
            return "ejb/StatefulCounterEjb";
        }

        public MethodArgDef[] getTestedMethodArgs() {
            return null;
        }

        public String getTestedMethodName() {
            return "count";
        }
    }

    public Ejb2SFSBResourceTest() {
        super(new TestTemplate());
    }
    
    @BeforeGroups(groups = "as5-plugin-ejb2", dependsOnMethods = "deployEjb2TestJars")
    public void setupBean() {
        super.setupBean();
    }
    
    @Override
    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    @Override
    public void testOperations() throws Exception {
        super.testOperations();
    }

    protected String getResourceTypeName() {
        return "EJB2 Stateful Session Bean";
    }
}
