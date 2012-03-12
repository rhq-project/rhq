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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.spi.PluginContainerOperationRemedy;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AfterDiscoveryRemedy implements PluginContainerOperationRemedy {

    private static final Comparator<Method> ORDERING = new Comparator<Method>() {

        @Override
        public int compare(Method m1, Method m2) {
            AfterDiscovery a1 = m1.getAnnotation(AfterDiscovery.class);
            AfterDiscovery a2 = m2.getAnnotation(AfterDiscovery.class);

            int o1 = a1.order();
            int o2 = a2.order();

            if (o1 > 0) {
                if (o2 > 0) {
                    return o1 - o2;
                } else {
                    //explicitly ordered stuff always precedes the unordered
                    return -1;
                }
            } else {
                //explicitly ordered stuff always precedes the unordered
                return o2 > 0 ? 1 : 0;
            }
        }
    };

    @Override
    public void cure(PluginContainer pc, TestEvent testEvent) {
        runAfterDiscovery(testEvent);
    }

    private void runAfterDiscovery(TestEvent event) {
        Object testCase = event.getTestInstance();
        TestClass testClass = event.getTestClass();
        String testMethodName = event.getTestMethod().getName();

        for (Method m : filterAndOrderMethods(testMethodName, testClass.getMethods(AfterDiscovery.class))) {
            try {
                m.invoke(testCase, (Object[]) null);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute a @" + AfterDiscovery.class + " method " + m, e);
            }
        }
    }

    private List<Method> filterAndOrderMethods(String targetTestMethodName, Method[] beforeDiscoveryMethods) {
        List<Method> ret = new ArrayList<Method>(Arrays.asList(beforeDiscoveryMethods));

        Iterator<Method> it = ret.iterator();

        //filter
        while (it.hasNext()) {
            Method m = it.next();
            AfterDiscovery annotation = m.getAnnotation(AfterDiscovery.class);

            if (annotation == null || annotation.testMethods().length > 0
                && !Arrays.asList(annotation.testMethods()).contains(targetTestMethodName)) {

                it.remove();
            }
        }

        //order
        Collections.sort(ret, ORDERING);

        return ret;
    }
    
}
