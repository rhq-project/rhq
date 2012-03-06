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

package org.rhq.test.arquillian.impl;

import java.lang.reflect.Method;

import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.spi.PluginContainerPreparator;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class BeforeDiscoveryPreparator implements PluginContainerPreparator {

    @Override
    public void prepare(PluginContainer pluginContainer, TestEvent testEvent) {
        runBeforeDiscovery(testEvent.getTestInstance(), testEvent.getTestClass());
    }

    private void runBeforeDiscovery(Object testCase, TestClass testClass) {
        for(Method m : testClass.getMethods(BeforeDiscovery.class)) {
            try {
                m.invoke(testCase, (Object[]) null);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute a @" + BeforeDiscovery.class + " method " + m, e);
            }
        }
    }    
}
