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

package org.rhq.bindings;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;

import org.rhq.bindings.util.PackageFinder;

/**
 * This is a base class for tests that need to test the some functionality using
 * the scripting languages.
 * <p>
 * It enforces the naming convention for the tests:
 * <code>
 * @Test
 * public void myTestMethod_language() ...
 * </code>
 * The language is to be replaced with the name of the scripting language that the test method
 * uses (e.g. javacript, python). This class then makes sure that each test exists for all 
 * the scripting languages that are present on the classpath.
 * <p>
 * This class also provides the {@link #getScriptEngine(PackageFinder, StandardBindings)} and
 * {@link #getSecuredScriptEngine(PackageFinder, StandardBindings, PermissionCollection)} methods
 * that, if invoked from within a test method call-chain, will return the script engine for the
 * correct language under test (determined by the test method name suffix).
 * 
 * @author Lukas Krejci
 */
public abstract class ScriptedTestBase implements IHookable {

    private String currentLanguage;

    @AfterClass
    public void checkTestImplsForEachLanguage(ITestContext ctx) {
        Set<Method> methods = getAllTestMethods(getClass(), ctx);

        Set<String> supportedLanguages = ScriptEngineFactory.getSupportedLanguages();

        Map<String, Set<String>> methodBaseNamesByLanguage = new HashMap<String, Set<String>>();

        for (String lang : supportedLanguages) {
            methodBaseNamesByLanguage.put(lang, new HashSet<String>());
        }

        Set<Method> invalidTestMethods = new HashSet<Method>();

        for (Method m : methods) {
            //check that the method name ends with one of the supported language names  
            boolean valid = false;
            for (String lang : supportedLanguages) {
                String suffix = "_" + lang;

                if (!m.getName().endsWith(suffix)) {
                    continue;
                }

                //now put the method "basename" into our mapping array
                String baseName = m.getName().substring(0, m.getName().lastIndexOf("_"));
                methodBaseNamesByLanguage.get(lang).add(baseName);

                valid = true;
                break;
            }

            if (!valid) {
                invalidTestMethods.add(m);
            }
        }

        Set<String> missingTests = new HashSet<String>();

        //now check that all languages have all test methods
        for (Map.Entry<String, Set<String>> a : methodBaseNamesByLanguage.entrySet()) {
            for (Map.Entry<String, Set<String>> b : methodBaseNamesByLanguage.entrySet()) {
                String alang = a.getKey();
                String blang = b.getKey();
                Set<String> amethods = a.getValue();
                Set<String> bmethods = b.getValue();

                if (alang.equals(blang)) {
                    continue;
                }

                addMissing(amethods, bmethods, alang, missingTests);
                addMissing(bmethods, amethods, blang, missingTests);
            }
        }

        if (!invalidTestMethods.isEmpty() || !missingTests.isEmpty()) {
            StringBuilder msg = new StringBuilder("Scripted test " + getClass() + " is invalid:\n");

            if (!invalidTestMethods.isEmpty()) {
                msg.append("Invalid method names:\n");
                for (Method m : invalidTestMethods) {
                    msg.append(m.getName()).append("\n");
                }
            }

            if (!missingTests.isEmpty()) {
                msg.append("\nMissing tests for languages:\n");
                for (String m : missingTests) {
                    msg.append(m).append("\n");
                }
            }

            fail(msg.toString());
        }
    }

    @Override
    public final void run(IHookCallBack callBack, ITestResult testResult) {
        String methodName = testResult.getMethod().getMethodName();
        int underScoreIdx = methodName.lastIndexOf('_');
        if (underScoreIdx >= 0 && underScoreIdx < methodName.length() - 1) {
            currentLanguage = methodName.substring(underScoreIdx + 1);
        } else {
            currentLanguage = null;
        }
        
        callBack.runTestMethod(testResult);
        
        currentLanguage = null;
    }

    /**
     * Returns a new script engine implementation for the current test method.
     * <p>
     * The script engine implementation is determined based on the test method's name
     * suffix.
     * <p>
     * E.g. if the test method ends with "_javascript", this method will return the javascript
     * script engine.
     * <p>
     * This method is calling {@link ScriptEngineFactory#getScriptEngine(String, PackageFinder, StandardBindings)}
     * but determines the "language" parameter for you.
     * 
     * @param packageFinder the package finder to use for the script engine initialization
     * @param bindings the bindings to use in the script engine
     * @return the script engine
     * @throws ScriptException
     * @throws IOException
     */
    protected ScriptEngine getScriptEngine(PackageFinder packageFinder, StandardBindings bindings)
        throws ScriptException, IOException {

        return ScriptEngineFactory.getScriptEngine(currentLanguage, packageFinder, bindings);
    }

    /**
     * Similar to {@link #getScriptEngine(PackageFinder, StandardBindings)} but returns the 
     * secured version of the script engine.
     *  
     * @param packageFinder the package finder to use for the script engine initialization
     * @param bindings the bindings to use in the script engine
     * @param perms the permissions to run the scripts with
     * @return the script engine
     * @throws ScriptException
     * @throws IOException
     */
    protected ScriptEngine getSecuredScriptEngine(PackageFinder packageFinder, StandardBindings bindings,
        PermissionCollection perms) throws ScriptException, IOException {

        return ScriptEngineFactory.getSecuredScriptEngine(currentLanguage, packageFinder, bindings, perms);
    }

    private static Set<Method> getAllTestMethods(Class<?> cls, ITestContext ctx) {
        HashSet<Method> ret = new HashSet<Method>();
        for (ITestNGMethod m : ctx.getAllTestMethods()) {
            if (m.getTestClass().getRealClass().equals(cls)) {
                ret.add(m.getConstructorOrMethod().getMethod());
            }
        }

        return ret;
    }

    private static void addMissing(Set<String> methods, Set<String> referenceMethods, String lang, Set<String> missing) {
        for (String m : referenceMethods) {
            if (!methods.contains(m)) {
                String fullName = m + "_" + lang;
                missing.add(fullName);
            }
        }
    }
}
