/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.test.testng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

/**
 * Prints messages to stdout during a TestNG execution, as suites, test classes, and tests are started and finished.
 *
 * @author Ian Springer
 */
public class StdoutReporter implements ISuiteListener, ITestListener {

    @Override
    public void onStart(ISuite suite) {
        List<String> testMethodNames = getTestMethodNames(suite);
        System.out.println("(" + getDateTime() + ")" + " Running suite [" + suite.getName() + "] containing tests "
            + testMethodNames + " --- excluding tests " + getTestMethodNames(suite.getExcludedMethods()) + "...");
    }

    @Override
    public void onFinish(ISuite suite) {
        System.out.println("(" + getDateTime() + ")" + " Done running suite [" + suite.getName() + "].");
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testMethodName = getQualifiedMethodName(result.getMethod());
        System.out.println("(" + getDateTime() + ")" + " Running test [" + testMethodName + "]...");
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        return;
    }

    @Override
    public void onTestFailure(ITestResult result) {
        return;
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        return;
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        return;
    }

    @Override
    public void onStart(ITestContext context) {
        String testClassName = getTestClassName(context);
        System.out.println("(" + getDateTime() + ")" + " Running test class [" + testClassName + "]...");
    }

    @Override
    public void onFinish(ITestContext context) {
        String testClassName = getTestClassName(context);
        System.out.println("(" + getDateTime() + ")" + " Done running test class [" + testClassName + "].");
    }

    private static String getDateTime() {
        return SimpleDateFormat.getTimeInstance().format(new Date());
    }

    private static List<String> getTestMethodNames(ISuite suite) {
        List<String> testMethodNames = new ArrayList<String>();
        Map<String, Collection<ITestNGMethod>> methodsByGroups = suite.getMethodsByGroups();
        for (String group : methodsByGroups.keySet()) {
            Collection<ITestNGMethod> methods = methodsByGroups.get(group);
            testMethodNames.addAll(getTestMethodNames(methods));
        }
        return testMethodNames;
    }

    private static List<String> getTestMethodNames(Collection<ITestNGMethod> methods) {
        List<String> testMethodNames = new ArrayList<String>();
        for (ITestNGMethod method : methods) {
            if (method.isTest()) {
                String testMethodName = getQualifiedMethodName(method);
                testMethodNames.add(testMethodName);
            }
        }
        return testMethodNames;
    }

    private static String getQualifiedMethodName(ITestNGMethod method) {
        return method.getTestClass().getRealClass().getSimpleName() + "." + method.getMethodName();
    }

    private static String getTestClassName(ITestContext context) {
        String testClassName;
        ITestNGMethod[] testMethods = context.getAllTestMethods();
        if (testMethods != null && testMethods.length != 0) {
            testClassName = testMethods[0].getTestClass().getRealClass().getSimpleName();
        } else {
            testClassName = "?";
        }
        return testClassName;
    }

}
