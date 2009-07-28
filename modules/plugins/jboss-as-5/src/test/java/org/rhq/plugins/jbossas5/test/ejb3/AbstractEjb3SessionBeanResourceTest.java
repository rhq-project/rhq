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

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.rhq.plugins.jbossas5.test.util.EjbSessionBeanTestTemplate;
import org.testng.annotations.BeforeGroups;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractEjb3SessionBeanResourceTest extends AbstractEjb3ResourceTest {

    protected static abstract class Ejb3SessionBeanTestTemplate extends EjbSessionBeanTestTemplate {

        protected Object getRemoteBean() throws Exception {
            String jndiName = getTestedBeanName() + "/remote";
            
            return AppServerUtils.getRemoteObject(jndiName, Object.class);
        }
    }
    
    private Ejb3SessionBeanTestTemplate testTemplate;
    
    protected AbstractEjb3SessionBeanResourceTest(Ejb3SessionBeanTestTemplate testTemplate) {
        this.testTemplate = testTemplate;
    }
    
    protected void setupBean() {
        testTemplate.setupBean();
    }

    @Override
    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if (!testTemplate.validateOperationResult(name, result, resource)) {
            super.validateOperationResult(name, result, resource);
        }
    }
    
    
}
