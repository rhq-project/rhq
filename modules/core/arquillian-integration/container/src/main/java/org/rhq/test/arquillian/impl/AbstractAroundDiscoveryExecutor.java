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

import java.lang.annotation.Annotation;
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
import org.rhq.test.arquillian.RunDiscovery;

/**
 * 
 *
 * @author Lukas Krejci
 */
public abstract class AbstractAroundDiscoveryExecutor<T extends Annotation> {

    protected static class ApplicableTestMethodsAndOrder {
        private String[] applicableTestMethodNames;
        private int order;
        
        public ApplicableTestMethodsAndOrder(String[] applicableTestMethodNames, int order) {
            this.applicableTestMethodNames = applicableTestMethodNames;
            this.order = order;
        }

        public String[] getApplicableTestMethodNames() {
            return applicableTestMethodNames;
        }
        
        public int getOrder() {
            return order;
        }                
    }
    
    private static class AnnotatedMethod {
        private Method testMethod;
        private ApplicableTestMethodsAndOrder methodsAndOrder;
        
        public AnnotatedMethod(Method testMethod, ApplicableTestMethodsAndOrder methodsAndOrder) {
            this.testMethod = testMethod;
            this.methodsAndOrder = methodsAndOrder;
        }

        public Method getTestMethod() {
            return testMethod;
        }
        
        public ApplicableTestMethodsAndOrder getApplicablTestMethodsAndOrder() {
            return methodsAndOrder;
        }
    }
    
    private Class<T> annotationClass;
    
    protected AbstractAroundDiscoveryExecutor(Class<T> annotationClass) {
        this.annotationClass = annotationClass;
    }
    
    protected abstract ApplicableTestMethodsAndOrder getApplicableTestMethodsAndOrder(T annotation);
    
    private static final Comparator<AnnotatedMethod> ORDERING = new Comparator<AnnotatedMethod>() {

        @Override
        public int compare(AnnotatedMethod m1, AnnotatedMethod m2) {

            int o1 = m1.getApplicablTestMethodsAndOrder().getOrder();
            int o2 = m2.getApplicablTestMethodsAndOrder().getOrder();

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

    protected void process(PluginContainer pluginContainer, TestEvent testEvent) {
        if (isRunningDiscovery(testEvent)) {
            doProcess(testEvent);
        }
    }

    private void doProcess(TestEvent event) {
        Object testCase = event.getTestInstance();
        TestClass testClass = event.getTestClass();
        String testMethodName = event.getTestMethod().getName();

        for (Method m : filterAndOrderMethods(testMethodName, testClass.getMethods(annotationClass))) {
            try {
                m.invoke(testCase, (Object[]) null);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute a @" + annotationClass + " method " + m, e);
            }
        }
    }

    private boolean isRunningDiscovery(TestEvent event) {
        RunDiscovery runDiscovery = RunDiscoveryExecutor.getRunDiscoveryForTest(event);
        return runDiscovery != null && (runDiscovery.discoverServers() || runDiscovery.discoverServices());
    }
        
    private List<Method> filterAndOrderMethods(String targetTestMethodName, Method[] beforeDiscoveryMethods) {
        List<AnnotatedMethod> ordered = new ArrayList<AbstractAroundDiscoveryExecutor.AnnotatedMethod>(beforeDiscoveryMethods.length);
        
        for(int i = 0; i < beforeDiscoveryMethods.length; ++i) {
            T annotation = beforeDiscoveryMethods[i].getAnnotation(annotationClass);
            ApplicableTestMethodsAndOrder o = getApplicableTestMethodsAndOrder(annotation);
            
            AnnotatedMethod m = new AnnotatedMethod(beforeDiscoveryMethods[i], o);
            
            ordered.add(m);
        }
        
        Iterator<AnnotatedMethod> it = ordered.iterator();
        
        //filter
        while (it.hasNext()) {
            AnnotatedMethod m = it.next();            

            String[] applicableTestMethodNames = m.getApplicablTestMethodsAndOrder().getApplicableTestMethodNames();
            
            if (applicableTestMethodNames.length > 0
                && !Arrays.asList(applicableTestMethodNames).contains(targetTestMethodName)) {

                it.remove();
            }
        }

        //order
        Collections.sort(ordered, ORDERING);

        //convert to List<Method>
        
        List<Method> ret = new ArrayList<Method>(ordered.size());
        
        for(AnnotatedMethod m : ordered) {
            ret.add(m.getTestMethod());
        }
        
        return ret;
    }    
}
