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

package org.rhq.test;

import java.lang.reflect.Method;

import org.jmock.Mockery;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGListener;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * This class can either be used as a base class for tests using JMock
 * or it also can be used as a TestNG listener to provide the JMock
 * context to test classes that need to inherit from another class.
 * <p>
 * In the former case, the JMock context is accessible through the {@link #context} protected
 * field, while in the latter case (when JMockTest is specified as a {@link Listeners listener}
 * of a test class), the JMock context is accessible using the {@link #getCurrentMockContext()}
 * static method.
 * <p>
 * Unlike the default behavior of the Listeners annotation which makes the supplied classes
 * the listeners on <b>ALL</b> test methods in <b>ALL</b> classes, this implementation behaves differntly. 
 * It checks whether the Listeners annotation is specified on the class that the current test 
 * method is being executed on (or its superclasses) and only if it does, the test method is 
 * "augmented". This means that the classes can specify if they want to be augmented by JMockTest
 * by specifying it as their listener.
 * 
 * @author John Sanda
 * @author Lukas Krejci
 */
public class JMockTest implements IHookable, IInvokedMethodListener {

    protected Mockery context;

    private static final ThreadLocal<Mockery> STATICALLY_ACCESSIBLE_CONTEXT = new ThreadLocal<Mockery>();

    /**
     * @return the JMock context of the current test or null if the calling test class doesn't have 
     * this class set as a listener or doesn't inherit from this class.
     */
    public static Mockery getCurrentMockContext() {
        return STATICALLY_ACCESSIBLE_CONTEXT.get();
    }

    @BeforeMethod
    public final void initMockContext(Method testMethod) {
        initBeforeTest(this, testMethod);
    }
    
    @AfterMethod
    public final void tearDownMockContext(ITestResult testResult) {
        tearDownAfterTest(testResult);
    }
    
    /**
     * This method runs {@link #initBeforeTest(ITestResult)}, followed by the actual test,
     * followed by {@link #tearDownAfterTest(ITestResult)}.
     * <p>
     * If you want to modify the behavior of this method, override the above mentioned
     * methods.
     * 
     * @see IHookable#run(IHookCallBack, ITestResult)
     */
    public final void run(IHookCallBack iHookCallBack, ITestResult iTestResult) {
        iHookCallBack.runTestMethod(iTestResult);
    }
    
    /**
     * Runs {@link #initBeforeTest(ITestResult)}.
     * 
     * @see IInvokedMethodListener#beforeInvocation(IInvokedMethod, ITestResult)
     */
    public final void beforeInvocation(IInvokedMethod method, ITestResult testResult) {  
        if (!isUsedAsSubClass(method) && isListenerDefinedOnTestClass(method)) {
            initBeforeTest(testResult.getInstance(), testResult.getMethod().getMethod());
        }
    }
    
    /**
     * Runs {@link #tearDownAfterTest(ITestResult)}.
     * 
     * @see IInvokedMethodListener#afterInvocation(IInvokedMethod, ITestResult)
     */
    public final void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!isUsedAsSubClass(method) && isListenerDefinedOnTestClass(method)) {
            tearDownAfterTest(testResult);
        }
    }
    
    /**
     * Does whatever needs done before the test is invoked.
     * <p>
     * If you override this method, be sure to call this method <b>before</b>
     * your code so that you gain access to the {@link #context}.
     * 
     * @param testResult
     */
    protected void initBeforeTest(Object testObject, Method testMethod) {
        initContext();
    }
    
    /**
     * Does whatever needs done after the test has been invoked.
     * <p>
     * This method calls {@link Mockery#assertIsSatisfied()} and nulls out the {@link #context}.
     * <p>
     * If you override this method, call this implmentation as the last call in your code.
     * @param result
     */
    protected void tearDownAfterTest(ITestResult result) {
        try {
            context.assertIsSatisfied();
        } catch (Throwable t) {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(t);
        } finally {
            tearDownContext();
        }
    }

    private void initContext() {
        context = new Mockery();
        STATICALLY_ACCESSIBLE_CONTEXT.set(context);
    }

    private void tearDownContext() {
        context = null;
        //null out the static field so that the GC can
        //collect the no-longer used context.
        STATICALLY_ACCESSIBLE_CONTEXT.set(null);
    }
    
    private boolean isUsedAsSubClass(IInvokedMethod method) {
        Class<?> testMethodClass = method.getTestMethod().getTestClass().getRealClass();
        
        return this.getClass().isAssignableFrom(testMethodClass);
    }    
    
    private boolean isListenerDefinedOnTestClass(IInvokedMethod method) {
        Class<?> cls = method.getTestMethod().getTestClass().getRealClass();
        
        while (cls != null) {
            Listeners annotation = cls.getAnnotation(Listeners.class);
            
            if (annotation != null) {
                for(Class<?> listener : annotation.value()) {
                    if (this.getClass().equals(listener)) {
                        return true;
                    }
                }
            }
            
            cls = cls.getSuperclass();
        }
        
        return false;
    }
}
